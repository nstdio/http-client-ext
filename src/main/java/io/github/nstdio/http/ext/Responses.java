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

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

class Responses {
  private Responses() {
  }

  static <T> HttpResponse<T> headersReplacing(HttpResponse<T> response, HttpHeaders headers) {
    return new DelegatingHttpResponse<>(response) {
      @Override
      public HttpHeaders headers() {
        return headers;
      }
    };
  }

  static <T> HttpResponse<T> gatewayTimeoutResponse(HttpRequest request) {
    return StaticHttpResponse.<T>builder()
        .statusCode(504)
        .request(request)
        .uri(request.uri())
        .version(HttpClient.Version.HTTP_1_1)
        .build();
  }

  static boolean isSuccessful(HttpResponse<?> response) {
    int statusCode;
    return (statusCode = response.statusCode()) >= 200 && statusCode < 400;
  }

  static boolean isSafeRequest(HttpResponse<?> response) {
    var method = response.request().method();
    return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
  }

  static class DelegatingHttpResponse<T> implements HttpResponse<T> {
    private final HttpResponse<T> delegate;

    DelegatingHttpResponse(HttpResponse<T> delegate) {
      this.delegate = delegate;
    }

    HttpResponse<T> delegate() {
      return delegate;
    }

    public int statusCode() {
      return delegate.statusCode();
    }

    public HttpRequest request() {
      return delegate.request();
    }

    public Optional<HttpResponse<T>> previousResponse() {
      return delegate.previousResponse();
    }

    public HttpHeaders headers() {
      return delegate.headers();
    }

    public T body() {
      return delegate.body();
    }

    public Optional<SSLSession> sslSession() {
      return delegate.sslSession();
    }

    public URI uri() {
      return delegate.uri();
    }

    public HttpClient.Version version() {
      return delegate.version();
    }
  }
}
