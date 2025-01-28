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

import io.github.nstdio.http.ext.BodyPublishers.JsonPublisher;

import java.net.http.HttpRequest.BodyPublisher;
import java.util.Map;
import java.util.Optional;

import static io.github.nstdio.http.ext.Headers.HEADER_CONTENT_TYPE;
import static java.util.function.Predicate.not;

class ContentTypeInterceptor implements Interceptor {
  private final Interceptor headersAdding;

  ContentTypeInterceptor(String contentType) {
    headersAdding = new HeadersAddingInterceptor(Map.of(HEADER_CONTENT_TYPE, contentType));
  }

  @Override
  public <T> Chain<T> intercept(Chain<T> in) {
    if (!isJsonPublisher(in.request().bodyPublisher())) {
      return in;
    }

    return headersAdding.intercept(in);
  }

  private static boolean isJsonPublisher(Optional<BodyPublisher> bodyPublisher) {
    return bodyPublisher.filter(not(JsonPublisher.class::isInstance)).isEmpty();
  }
}
