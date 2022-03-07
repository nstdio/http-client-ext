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

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    @RequiredArgsConstructor
    static class DelegatingHttpResponse<T> implements HttpResponse<T> {
        @Delegate
        private final HttpResponse<T> delegate;

        HttpResponse<T> delegate() {
            return delegate;
        }
    }
}
