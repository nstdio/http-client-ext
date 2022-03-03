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

class StaticHttpResponse<T> implements HttpResponse<T> {
    private final int statusCode;
    private final HttpRequest request;
    private final HttpHeaders headers;
    private final Optional<SSLSession> sslSession;
    private final URI uri;
    private final HttpClient.Version version;
    private final T body;

    private StaticHttpResponse(int statusCode, HttpRequest request, HttpHeaders headers, SSLSession sslSession, URI uri, HttpClient.Version version, T body) {
        this.statusCode = statusCode;
        this.request = request;
        this.headers = headers;
        this.sslSession = Optional.ofNullable(sslSession);
        this.uri = uri;
        this.version = version;
        this.body = body;
    }

    static <T> StaticHttpResponseBuilder<T> builder() {
        return new StaticHttpResponseBuilder<>();
    }

    @Override
    public int statusCode() {
        return statusCode;
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
        return headers;
    }

    @Override
    public T body() {
        return body;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return sslSession;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public HttpClient.Version version() {
        return version;
    }

    static class StaticHttpResponseBuilder<T> {
        private int statusCode;
        private HttpRequest request;
        private HttpHeaders headers;
        private SSLSession sslSession;
        private URI uri;
        private HttpClient.Version version;
        private T body;

        StaticHttpResponseBuilder() {
        }

        StaticHttpResponseBuilder<T> statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        StaticHttpResponseBuilder<T> request(HttpRequest request) {
            this.request = request;
            return this;
        }

        StaticHttpResponseBuilder<T> headers(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        StaticHttpResponseBuilder<T> sslSession(SSLSession sslSession) {
            this.sslSession = sslSession;
            return this;
        }

        StaticHttpResponseBuilder<T> uri(URI uri) {
            this.uri = uri;
            return this;
        }

        StaticHttpResponseBuilder<T> version(HttpClient.Version version) {
            this.version = version;
            return this;
        }

        StaticHttpResponseBuilder<T> body(T body) {
            this.body = body;
            return this;
        }

        StaticHttpResponse<T> build() {
            return new StaticHttpResponse<>(statusCode, request, headers, sslSession, uri, version, body);
        }

        public String toString() {
            return "StaticHttpResponse.StaticHttpResponseBuilder(statusCode=" + this.statusCode + ", request=" + this.request + ", headers=" + this.headers + ", sslSession=" + this.sslSession + ", uri=" + this.uri + ", version=" + this.version + ", body=" + this.body + ")";
        }
    }
}
