/*
 * Copyright (C) 2022 Edgar Asatryan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.nstdio.http.ext;

import io.github.nstdio.http.ext.Cache.CacheEntry;
import io.github.nstdio.http.ext.Cache.CacheStats;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.github.nstdio.http.ext.Headers.HEADER_IF_MODIFIED_SINCE;
import static io.github.nstdio.http.ext.Headers.HEADER_IF_NONE_MATCH;
import static io.github.nstdio.http.ext.HttpRequests.toBuilder;
import static io.github.nstdio.http.ext.Responses.gatewayTimeoutResponse;
import static io.github.nstdio.http.ext.Responses.isSafeRequest;
import static io.github.nstdio.http.ext.Responses.isSuccessful;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

class CachingInterceptor implements Interceptor {
  private final static List<String> INVALIDATION_HEADERS = List.of("Location", "Content-Location");

  private final Cache cache;
  private final Clock clock;

  CachingInterceptor(Cache cache, Clock clock) {
    this.cache = cache;
    this.clock = clock;
  }

  private static HttpRequest applyConditions(HttpRequest request, CacheEntryMetadata metadata) {
    var builder = toBuilder(request);
    metadata.date().ifPresent(date -> builder.setHeader(HEADER_IF_MODIFIED_SINCE, date));
    metadata.etag().ifPresent(etag -> builder.setHeader(HEADER_IF_NONE_MATCH, etag));

    return builder.build();
  }

  private static boolean isCacheable(HttpResponse.ResponseInfo info) {
    //@formatter:off
    switch (info.statusCode()) {
      case 200:
      case 203:
      case 204:
      case 206:
      case 300:
      case 301:
      case 404:
      case 405:
      case 410:
      case 414:
      case 501:
        break;
      default:
        return false;
    }
    //@formatter:on

    var headers = info.headers();
    if (Headers.isVaryAll(headers)) {
      return false;
    }

    CacheControl cc = CacheControl.of(headers);
    return !cc.noStore();
  }

  private static boolean hasConditions(HttpRequest request) {
    return Headers.hasConditions(request.headers());
  }

  @Override
  public <T> Chain<T> intercept(Chain<T> in) {
    RequestContext ctx = in.ctx();

    if (ctx.isCacheable()) {
      final CacheEntry entry = cacheEntry(ctx);

      if (ctx.cacheControl().onlyIfCached()) {
        return in.withResponse(forcedCacheResponse(ctx, entry));
      } else if (isFresh(ctx, entry)) {
        return in.withResponse(createCachedResponse(ctx, entry));
      } else {
        return sendAndCache(in, entry);
      }
    } else {
      FutureHandler<T> fn = FutureHandler.of(r -> shouldInvalidate(r) ? invalidate(r) : r);

      return Chain.of(ctx, in.futureHandler().andThen(fn));
    }
  }

  private <T> HttpResponse<T> forcedCacheResponse(RequestContext ctx, CacheEntry entry) {
    final var request = ctx.request();
    CacheEntry forcedEntry = entry == null ? getCacheEntry(request) : entry;
    final HttpResponse<T> response;
    if (forcedEntry == null || !forcedEntry.metadata().isFresh(ctx.cacheControl())) {
      response = gatewayTimeoutResponse(request);
    } else {
      response = createCachedResponse(ctx, forcedEntry);
    }

    return response;

  }

  private <T> Chain<T> sendAndCache(Chain<T> in, CacheEntry entry) {
    var metadata = Optional.ofNullable(entry).map(CacheEntry::metadata);
    var newCtx = metadata
        .map(m -> applyConditions(in.ctx().request(), m))
        .map(in.ctx()::withRequest)
        .orElseGet(in::ctx);

    newCtx.requestTime().compareAndSet(0, clock.millis());

    var bodyHandler = cacheAware(newCtx);
    FutureHandler<T> handler = (r, th) -> {
      if (r != null) {
        return possiblyCached(newCtx, entry, r);
      } else {
        if (metadata.map(CacheEntryMetadata::responseCacheControl).filter(CacheControl::mustRevalidate).isPresent()
            && hasConditions(newCtx.request())) {
          return gatewayTimeoutResponse(newCtx.request());
        }
      }

      throw Throwables.sneakyThrow(th);
    };

    return Chain.of(newCtx.withBodyHandler(bodyHandler), in.futureHandler().andThen(handler));
  }

  private <T> BodyHandler<T> cacheAware(RequestContext ctx) {
    return info -> {
      ctx.responseTime().compareAndSet(0, clock.millis());
      BodySubscriber<T> sub = ctx.<T>bodyHandler().apply(info);

      if (isCacheable(info)) {
        var metadata = CacheEntryMetadata.of(ctx.requestTime().get(), ctx.responseTime().get(),
            info, ctx.request(), clock);

        if (metadata.isApplicable()) {
          trackMiss();
          var writer = cache.writer(metadata);
          sub = new CachingBodySubscriber<>(sub, writer.subscriber(), writer.finisher());
        }
      }

      return sub;
    };
  }

  private <T> HttpResponse<T> invalidate(HttpResponse<T> response) {
    HttpHeaders headers = response.headers();
    URI uri = response.uri();
    String host = uri.getHost();

    for (String headerName : INVALIDATION_HEADERS) {
      Headers.effectiveUri(headers, headerName, uri)
          .filter(u -> host.equals(u.getHost()))
          .map(u -> HttpRequest.newBuilder(u).build())
          .forEach(cache::evictAll);
    }

    cache.evictAll(response.request());

    return response;
  }

  private <T> boolean shouldInvalidate(HttpResponse<T> response) {
    return isSuccessful(response) && !isSafeRequest(response);
  }

  private CacheEntry cacheEntry(RequestContext ctx) {
    if (ctx.cacheControl().noCache()) {
      // TODO: check if entry is stale and remove it
      return null;
    }

    return getCacheEntry(ctx.request());
  }

  private CacheEntry getCacheEntry(HttpRequest request) {
    return cache.get(request);
  }

  private boolean isFresh(RequestContext ctx, CacheEntry entry) {
    return Optional.ofNullable(entry)
        .map(CacheEntry::metadata)
        .filter(m -> m.isFresh(ctx.cacheControl()))
        .isPresent();
  }

  private <T> HttpResponse<T> possiblyCached(RequestContext ctx, CacheEntry entry, HttpResponse<T> r) {
    CacheEntryMetadata metadata = entry != null ? entry.metadata() : null;
    if (metadata != null) {
      switch (r.statusCode()) {
        case 304: {
          metadata.update(r.headers(), ctx.requestTime().get(), ctx.responseTime().get());
          return createCachedResponse(ctx, entry);
        }
        case 500:
        case 502:
        case 503:
        case 504: {
          long staleIfError = Math.max(
              ctx.cacheControl().staleIfError(MILLISECONDS),
              metadata.responseCacheControl().staleIfError(MILLISECONDS)
          );

          if (staleIfError > metadata.staleFor()) {
            return createCachedResponse(ctx, entry);
          }

          break;
        }
        default:
          // won't handle other codes
          break;
      }
    }

    return r;
  }

  private <T> HttpResponse<T> createCachedResponse(RequestContext ctx, CacheEntry entry) {
    trackHit();
    return new CachedHttpResponse<>(ctx.bodyHandler(), ctx.request(), entry);
  }


  private void trackMiss() {
    executeIfTrackable(TrackableCacheStats::trackMiss);
  }

  private void trackHit() {
    executeIfTrackable(TrackableCacheStats::trackHit);
  }

  private void executeIfTrackable(Consumer<TrackableCacheStats> action) {
    CacheStats stats = cache.stats();
    if (stats instanceof TrackableCacheStats) {
      action.accept((TrackableCacheStats) stats);
    }
  }
}
