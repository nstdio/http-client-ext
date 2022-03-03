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

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

final class CachedHttpResponse<T> implements HttpResponse<T> {
    private final BodyHandler<T> bodyHandler;
    private final HttpRequest request;
    private final Cache.CacheEntry entry;

    private T body;

    CachedHttpResponse(BodyHandler<T> bodyHandler, HttpRequest request, Cache.CacheEntry entry) {
        this.bodyHandler = bodyHandler;
        this.request = request;
        this.entry = entry;
        this.entry.metadata().updateWarnings();
    }

    @Override
    public int statusCode() {
        return entry.metadata().response().statusCode();
    }

    @Override
    public HttpRequest request() {
        return request;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return entry.metadata().response().headers();
    }

    @Override
    public T body() {
        if (body != null) {
            return body;
        }

        BodySubscriber<T> sub = bodyHandler.apply(entry.metadata().response());
        entry.subscribeTo(sub);

        return body = sub.getBody().toCompletableFuture().join();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return request.uri();
    }

    @Override
    public HttpClient.Version version() {
        return entry.metadata().response().version();
    }
}
