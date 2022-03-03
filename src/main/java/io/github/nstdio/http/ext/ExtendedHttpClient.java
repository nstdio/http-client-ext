package io.github.nstdio.http.ext;

import static io.github.nstdio.http.ext.Headers.HEADER_IF_MODIFIED_SINCE;
import static io.github.nstdio.http.ext.Headers.HEADER_IF_NONE_MATCH;
import static java.util.stream.Collectors.toList;

import io.github.nstdio.http.ext.Cache.CacheEntry;
import lombok.SneakyThrows;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.HttpResponse.ResponseInfo;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ExtendedHttpClient extends HttpClient {
    private final Cache cache;
    private final boolean isNullCache;

    private final Clock clock;
    private final HttpClient delegate;

    ExtendedHttpClient(HttpClient delegate, Cache cache, Clock clock) {
        this.delegate = delegate;
        this.cache = cache;
        this.isNullCache = cache instanceof NullCache;
        this.clock = clock;
    }

    /**
     * Creates a new {@code ExtendedHttpClient} builder.
     *
     * @return an {@code ExtendedHttpClient.Builder}
     */
    public static ExtendedHttpClient.Builder newBuilder() {
        return new ExtendedHttpClient.Builder();
    }

    private static HttpRequest.Builder toBuilder(HttpRequest r) {
        var builder = HttpRequest.newBuilder();
        builder
                .uri(r.uri())
                .method(r.method(), r.bodyPublisher().orElseGet(BodyPublishers::noBody))
                .expectContinue(r.expectContinue());

        r.version().ifPresent(builder::version);
        r.timeout().ifPresent(builder::timeout);
        r.headers().map().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));

        return builder;
    }

    @SneakyThrows
    private static void sneakyThrow(Throwable e) {
        throw e;
    }

    //<editor-fold desc="Delegate Methods">
    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }
    //</editor-fold>

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        return delegate.newWebSocketBuilder();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        if (isNullCache) {
            return delegate.send(request, bodyHandler);
        }

        final RequestContext ctx = RequestContext.of(request, bodyHandler);

        CompletableFuture<HttpResponse<T>> future = sendAndCache(ctx, (r, h) -> {
            try {
                return CompletableFuture.completedFuture(delegate.send(r, h));
            } catch (IOException | InterruptedException e) {
                return CompletableFuture.failedFuture(e);
            }
        });

        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            }

            throw e;
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler) {
        if (isNullCache) {
            return delegate.sendAsync(request, bodyHandler);
        }

        var ctx = RequestContext.of(request, bodyHandler);

        return sendAndCache(ctx, delegate::sendAsync);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler,
                                                            PushPromiseHandler<T> pushPromiseHandler) {
        if (isNullCache) {
            return delegate.sendAsync(request, bodyHandler, pushPromiseHandler);
        }

        var ctx = RequestContext.of(request, bodyHandler);

        return sendAndCache(ctx, (r, h) -> delegate.sendAsync(r, h, pushPromiseHandler));
    }

    private <T> CompletableFuture<HttpResponse<T>> sendAndCache(RequestContext ctx, Sender<T> fn) {
        final CompletableFuture<HttpResponse<T>> future;
        if (ctx.isCacheable()) {
            final CacheEntry entry = cacheEntry(ctx);

            if (ctx.cacheControl().onlyIfCached()) {
                future = forcedCacheResponse(ctx, entry);
            } else {
                if (isFresh(ctx, entry)) {
                    future = CompletableFuture.completedFuture(new CachedHttpResponse<>(ctx.bodyHandler(), ctx.request(), entry));
                } else {
                    future = sendAndCache(ctx, fn, entry);
                }
            }
        } else {
            future = fn.apply(ctx.request(), ctx.bodyHandler())
                    .thenApplyAsync(response -> {
                        if (shouldInvalidate(response)) {
                            invalidate(response);
                        }

                        return response;
                    });
        }

        return future;
    }

    private <T> void invalidate(HttpResponse<T> response) {
        List<HttpRequest> toEvict = Stream.of("Location", "Content-Location")
                .flatMap(s -> Headers.effectiveUri(response.headers(), s, response.uri()).stream())
                .filter(uri -> response.uri().getHost().equals(uri.getHost()))
                .map(uri -> HttpRequest.newBuilder(uri).build())
                .collect(toList());
        toEvict.add(response.request());

        toEvict.forEach(cache::evictAll);
    }

    private <T> CompletableFuture<HttpResponse<T>> sendAndCache(RequestContext ctx, Sender<T> fn, CacheEntry entry) {
        var metadata = Optional.ofNullable(entry).map(CacheEntry::metadata);
        var newCtx = metadata
                .map(m -> applyConditions(ctx.request(), m))
                .map(ctx::withRequest)
                .orElse(ctx);

        newCtx.requestTime().set(clock.millis());

        return fn.apply(newCtx.request(), cacheAware(newCtx))
                .handleAsync((r, th) -> {
                    if (r != null) {
                        r = possibleCached(newCtx, entry, r);
                    } else {
                        if (metadata.map(CacheEntryMetadata::responseCacheControl).filter(CacheControl::mustRevalidate).isPresent()
                                && hasConditions(newCtx.request())) {
                            return gatewayTimeoutResponse(newCtx.request());
                        }

                        sneakyThrow(th);
                    }

                    return r;
                });
    }

    private <T> HttpResponse<T> possibleCached(RequestContext ctx, CacheEntry entry, HttpResponse<T> r) {
        CacheEntryMetadata metadata = entry != null ? entry.metadata() : null;
        if (metadata != null && r.statusCode() == 304) {
            metadata.update(r.headers(), ctx.requestTimeLong(), ctx.responseTimeLong());
            return new CachedHttpResponse<>(ctx.bodyHandler(), ctx.request(), entry);
        }

        return r;
    }

    private <T> boolean shouldInvalidate(HttpResponse<T> response) {
        return isSuccessful(response) && isUnsafe(response.request());
    }

    private boolean isSuccessful(HttpResponse<?> response) {
        int statusCode;
        return (statusCode = response.statusCode()) >= 200 && statusCode < 400;
    }

    private boolean isUnsafe(HttpRequest request) {
        var method = request.method();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean hasConditions(HttpRequest request) {
        var headers = request.headers();
        return headers.firstValue(HEADER_IF_MODIFIED_SINCE).isPresent()
                || headers.firstValue(HEADER_IF_NONE_MATCH).isPresent();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean isFresh(RequestContext ctx, CacheEntry entry) {
        return Optional.ofNullable(entry)
                .map(CacheEntry::metadata)
                .filter(m -> m.isFresh(ctx.cacheControl()))
                .isPresent();
    }

    private HttpRequest applyConditions(HttpRequest request, CacheEntryMetadata metadata) {
        var builder = toBuilder(request);
        metadata.date().ifPresent(date -> builder.setHeader(HEADER_IF_MODIFIED_SINCE, date));
        metadata.etag().ifPresent(etag -> builder.setHeader(HEADER_IF_NONE_MATCH, etag));

        return builder.build();
    }

    private <T> CompletableFuture<HttpResponse<T>> forcedCacheResponse(RequestContext ctx, CacheEntry entry) {
        var request = ctx.request();
        CacheEntry forcedEntry = entry == null ? getCacheEntry(request) : entry;
        final HttpResponse<T> response;
        if (forcedEntry == null || !forcedEntry.metadata().isFresh(ctx.cacheControl())) {
            response = gatewayTimeoutResponse(request);
        } else {
            response = new CachedHttpResponse<>(ctx.bodyHandler(), request, forcedEntry);
        }

        return CompletableFuture.completedFuture(response);
    }

    private <T> StaticHttpResponse<T> gatewayTimeoutResponse(HttpRequest request) {
        return StaticHttpResponse.<T>builder()
                .statusCode(504)
                .request(request)
                .uri(request.uri())
                .version(Version.HTTP_1_1)
                .build();
    }

    private boolean isCacheable(ResponseInfo info) {
        //@formatter:off
        switch (info.statusCode()) {
            case 200: case 203: case 204: case 206:
            case 300: case 301:
            case 404: case 405: case 410: case 414:
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

    private <T> BodyHandler<T> cacheAware(RequestContext ctx) {
        return info -> {
            ctx.responseTime().set(clock.millis());
            BodySubscriber<T> sub = ctx.<T>bodyHandler().apply(info);

            if (isCacheable(info)) {
                var metadata = CacheEntryMetadata.of(ctx.requestTime().get(), ctx.responseTime().get(),
                        info, ctx.request(), clock);

                if (metadata.isApplicable()) {
                    var writer = cache.writer(metadata);
                    sub = new CachingBodySubscriber<>(sub, writer.subscriber(), writer.finisher());
                }
            }

            return sub;
        };
    }

    /**
     * Type alias.
     */
    private interface Sender<T> extends BiFunction<HttpRequest, BodyHandler<T>, CompletableFuture<HttpResponse<T>>> {
    }

    private static class CachingBodySubscriber<T, C> implements BodySubscriber<T> {
        private final BodySubscriber<T> originalSub;
        private final Consumer<C> finisher;
        private final BodySubscriber<C> cachingSub;

        CachingBodySubscriber(BodySubscriber<T> originalSub, BodySubscriber<C> sub, Consumer<C> finisher) {
            this.originalSub = originalSub;
            this.cachingSub = sub;
            this.finisher = finisher;
        }

        @Override
        public CompletionStage<T> getBody() {
            return originalSub.getBody()
                    .thenApplyAsync(t -> {
                        cachingSub.getBody()
                                .thenApplyAsync(body -> {
                                    finisher.accept(body);
                                    return body;
                                });
                        return t;
                    });
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            originalSub.onSubscribe(subscription);
            cachingSub.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            originalSub.onNext(item);
            cachingSub.onNext(dup(item));
        }

        private List<ByteBuffer> dup(List<ByteBuffer> item) {
            return item.stream().map(ByteBuffer::duplicate).collect(toList());
        }

        @Override
        public void onError(Throwable throwable) {
            originalSub.onError(throwable);
            cachingSub.onError(throwable);
        }

        @Override
        public void onComplete() {
            originalSub.onComplete();
            cachingSub.onComplete();
        }
    }

    public static class Builder implements HttpClient.Builder {
        private final HttpClient.Builder delegate = HttpClient.newBuilder();
        private Cache cache = Cache.noop();

        Builder() {
        }

        //<editor-fold desc="Delegating Methods">

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder cookieHandler(CookieHandler cookieHandler) {
            delegate.cookieHandler(cookieHandler);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder connectTimeout(Duration duration) {
            delegate.connectTimeout(duration);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder sslContext(SSLContext sslContext) {
            delegate.sslContext(sslContext);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder sslParameters(SSLParameters sslParameters) {
            delegate.sslParameters(sslParameters);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder executor(Executor executor) {
            delegate.executor(executor);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder followRedirects(Redirect policy) {
            delegate.followRedirects(policy);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder version(Version version) {
            delegate.version(version);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder priority(int priority) {
            delegate.priority(priority);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder proxy(ProxySelector proxySelector) {
            delegate.proxy(proxySelector);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder authenticator(Authenticator authenticator) {
            delegate.authenticator(authenticator);
            return this;
        }
        //</editor-fold>

        /**
         * Sets the cache.
         *
         * @param cache The cache. Use {@link Cache#noop()} instead of {@code null}.
         *
         * @return builder itself.
         */
        public Builder cache(Cache cache) {
            Objects.requireNonNull(cache);
            this.cache = cache;
            return this;
        }

        @Override
        public ExtendedHttpClient build() {
            HttpClient client = delegate.build();

            return new ExtendedHttpClient(client, cache, Clock.systemUTC());
        }
    }
}
