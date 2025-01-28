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

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.atomic.AtomicLong;

final class RequestContext {
  private final HttpRequest request;
  private final BodyHandler<?> bodyHandler;
  private final CacheControl cacheControl;
  private final AtomicLong requestTime = new AtomicLong();
  private final AtomicLong responseTime = new AtomicLong();

  private RequestContext(HttpRequest request, BodyHandler<?> bodyHandler, CacheControl cacheControl) {
    this.request = request;
    this.bodyHandler = bodyHandler;
    this.cacheControl = cacheControl;
  }

  static RequestContext of(HttpRequest request, BodyHandler<?> responseBodyHandler) {
    return new RequestContext(request, responseBodyHandler, CacheControl.of(request));
  }

  boolean isCacheable() {
    if (!"GET".equals(request.method())) return false;

    return !cacheControl.noStore();
  }

  @SuppressWarnings("unchecked")
  <T> BodyHandler<T> bodyHandler() {
    return (BodyHandler<T>) bodyHandler;
  }

  RequestContext withRequest(HttpRequest request) {
    return new RequestContext(request, bodyHandler, cacheControl);
  }

  RequestContext withBodyHandler(BodyHandler<?> bodyHandler) {
    return new RequestContext(request, bodyHandler, this.cacheControl);
  }

  HttpRequest request() {
    return request;
  }

  CacheControl cacheControl() {
    return cacheControl;
  }

  AtomicLong requestTime() {
    return requestTime;
  }

  AtomicLong responseTime() {
    return responseTime;
  }
}
