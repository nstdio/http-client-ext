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

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class HeadersAddingInterceptor implements Interceptor {
  private final Map<String, String> headers;
  private final Map<String, Supplier<String>> resolvableHeaders;

  HeadersAddingInterceptor(Map<String, String> headers, Map<String, Supplier<String>> resolvableHeaders) {
    this.headers = headers;
    this.resolvableHeaders = resolvableHeaders;
  }
  
  HeadersAddingInterceptor(Map<String, String> headers) {
    this(headers, Map.of());
  }

  @Override
  public <T> Chain<T> intercept(Chain<T> in) {
    if (in.response().isPresent() || !hasHeaders()) {
      return in;
    }

    return in.withRequest(apply(in.request()));
  }

  private HttpRequest apply(HttpRequest request) {
    var headers = addHeaders(request.headers());

    var builder = HttpRequests.toBuilderOmitHeaders(request);
    headers.forEach((name, values) -> values.forEach(v -> builder.header(name, v)));

    return builder.build();
  }

  private Map<String, List<String>> addHeaders(HttpHeaders h) {
    var headersBuilder = new HttpHeadersBuilder(h);

    headers.forEach(headersBuilder::addIfNotExist);
    resolvableHeaders.forEach((name, valueSupplier) -> headersBuilder.addIfNotExist(name, valueSupplier.get()));

    return headersBuilder.map();
  }

  private boolean hasHeaders() {
    return !headers.isEmpty() || !resolvableHeaders.isEmpty();
  }
}
