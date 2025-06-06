/*
 * Copyright (C) 2022-2025 the original author or authors.
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.WebSocket;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ExtendedHttpClient extends HttpClient {
  private final CompressionInterceptor compressionInterceptor;
  private final CachingInterceptor cachingInterceptor;
  private final HeadersAddingInterceptor headersAddingInterceptor;
  private final ContentTypeInterceptor contentTypeInterceptor;

  private final HttpClient delegate;
  private final boolean allowInsecure;

  ExtendedHttpClient(HttpClient delegate, Cache cache, Clock clock) {
    this(
        null,
        cache instanceof NullCache ? null : new CachingInterceptor(cache, clock),
        null,
        delegate,
        true
    );
  }

  private ExtendedHttpClient(CompressionInterceptor compressionInterceptor,
                             CachingInterceptor cachingInterceptor,
                             HeadersAddingInterceptor headersAddingInterceptor,
                             HttpClient delegate, boolean allowInsecure) {
    this.compressionInterceptor = compressionInterceptor;
    this.cachingInterceptor = cachingInterceptor;
    this.headersAddingInterceptor = headersAddingInterceptor;
    this.contentTypeInterceptor = new ContentTypeInterceptor("application/json");
    this.delegate = delegate;
    this.allowInsecure = allowInsecure;
  }

  /**
   * Creates a new {@code ExtendedHttpClient} builder.
   *
   * @return an {@code ExtendedHttpClient.Builder}
   */
  public static ExtendedHttpClient.Builder newBuilder() {
    return new ExtendedHttpClient.Builder(HttpClient.newBuilder());
  }

  /**
   * Create a new {@code ExtendedHttpClient} with in memory cache and transparent encoding enabled.
   *
   * @return an {@code ExtendedHttpClient} with default settings.
   */
  public static ExtendedHttpClient newHttpClient() {
    return newBuilder()
        .cache(Cache.newInMemoryCacheBuilder().build())
        .transparentEncoding(true)
        .build();
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

  @Override
  public WebSocket.Builder newWebSocketBuilder() {
    return delegate.newWebSocketBuilder();
  }
  //</editor-fold>

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
    return result(send0(request, bodyHandler, syncSender()));
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler) {
    return send0(request, bodyHandler, asyncSender());
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler,
                                                          PushPromiseHandler<T> pushPromiseHandler) {
    return send0(request, bodyHandler, asyncSender(pushPromiseHandler));
  }

  private <T> CompletableFuture<HttpResponse<T>> send0(HttpRequest request, BodyHandler<T> bodyHandler, Sender<T> sender) {
    if (!allowInsecure) {
      checkInsecureScheme(request);
    }

    Chain<T> chain = buildAndExecute(RequestContext.of(request, bodyHandler));
    FutureHandler<T> handler = chain.futureHandler();

    var future = chain.response()
        .map(CompletableFuture::completedFuture)
        .orElseGet(() -> sender.apply(chain.ctx()));

    return future.isDone() ? future.handle(handler) : future.handleAsync(handler);
  }

  private void checkInsecureScheme(HttpRequest request) {
    URI uri = request.uri();
    if ("http".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("Client does not allow insecure HTTP requests. URI: " + uri);
    }
  }

  private <T> Chain<T> buildAndExecute(RequestContext ctx) {
    Chain<T> chain = Chain.of(ctx);
    chain = possiblyApply(compressionInterceptor, chain);
    chain = possiblyApply(cachingInterceptor, chain);
    chain = possiblyApply(headersAddingInterceptor, chain);
    chain = possiblyApply(contentTypeInterceptor, chain);

    return chain;
  }

  private <T> Chain<T> possiblyApply(Interceptor i, Chain<T> c) {
    return i != null ? i.intercept(c) : c;
  }

  /**
   * The {@code future} DOES NOT represent ongoing computation it's always either completed or failed.
   */
  private <T> HttpResponse<T> result(CompletableFuture<HttpResponse<T>> future) throws IOException, InterruptedException {
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

  private <T> Sender<T> syncSender() {
    return ctx -> {
      try {
        return completedFuture(delegate.send(ctx.request(), ctx.bodyHandler()));
      } catch (Throwable e) {
        return CompletableFuture.failedFuture(e);
      }
    };
  }

  private <T> Sender<T> asyncSender() {
    return ctx -> delegate.sendAsync(ctx.request(), ctx.bodyHandler());
  }

  private <T> Sender<T> asyncSender(PushPromiseHandler<T> pushPromiseHandler) {
    return ctx -> delegate.sendAsync(ctx.request(), ctx.bodyHandler(), pushPromiseHandler);
  }

  /**
   * Type alias.
   */
  interface Sender<T> extends Function<RequestContext, CompletableFuture<HttpResponse<T>>> {
  }

  public static class Builder implements HttpClient.Builder {
    private final HttpClient.Builder delegate;
    private boolean transparentEncoding;
    private boolean allowInsecure = true;
    private Cache cache = Cache.noop();
    private Map<String, String> headers = Map.of();
    private Map<String, Supplier<String>> resolvableHeaders = Map.of();

    Builder(HttpClient.Builder delegate) {
      this.delegate = delegate;
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

    /**
     * Sets the flag whether automatically decompress response or not. If set to {@code true} requests made by created
     * client will be enhanced with {@code Accept-Encoding} header and will decompress response body if appropriate
     * response headers found.
     *
     * @param transparentEncoding Apply transparent encoding or not.
     *
     * @return builder itself.
     */
    public Builder transparentEncoding(boolean transparentEncoding) {
      this.transparentEncoding = transparentEncoding;
      return this;
    }

    /**
     * Sets the flag whether client accept requests with {@code http} scheme. Default is {@code true}.
     *
     * @param allowInsecure Whether accept insecure {@code http} scheme or not.
     *
     * @return builder itself.
     */
    public Builder allowInsecure(boolean allowInsecure) {
      this.allowInsecure = allowInsecure;
      return this;
    }

    /**
     * Provided header will be included on each request.
     *
     * @param name  The header name.
     * @param value The header value.
     *
     * @return builder itself.
     */
    public Builder defaultHeader(String name, String value) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(value);

      if (headers.isEmpty()) {
        headers = new HashMap<>(1);
      }
      headers.put(name, value);

      return this;
    }

    /**
     * Provided header will be included on each request. Note that {@code valueSupplier} will be resolved before each
     * request.
     *
     * @param name          The header name.
     * @param valueSupplier The header value supplier.
     *
     * @return builder itself.
     */
    public Builder defaultHeader(String name, Supplier<String> valueSupplier) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(valueSupplier);

      if (resolvableHeaders.isEmpty()) {
        resolvableHeaders = new HashMap<>(1);
      }
      resolvableHeaders.put(name, valueSupplier);

      return this;
    }

    @Override
    public ExtendedHttpClient build() {
      HttpClient client = delegate.build();

      return new ExtendedHttpClient(
          transparentEncoding ? new CompressionInterceptor() : null,
          cache instanceof NullCache ? null : new CachingInterceptor(cache, Clock.systemUTC()),
          new HeadersAddingInterceptor(Map.copyOf(headers), Map.copyOf(resolvableHeaders)),
          client,
          allowInsecure
      );
    }
  }
}
