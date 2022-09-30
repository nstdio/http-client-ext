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

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The request/response predicates to use with
 * {@link io.github.nstdio.http.ext.Cache.InMemoryCacheBuilder#requestFilter(Predicate)} and
 * {@link io.github.nstdio.http.ext.Cache.InMemoryCacheBuilder#responseFilter(Predicate)}.
 */
public final class Predicates {
  private Predicates() {
  }

  static <T> Predicate<T> alwaysTrue() {
    return t -> true;
  }

  /**
   * The {@code Predicate} that matches {@code HttpRequest}s with given {@code uri}.
   *
   * @param uri The uri to match.
   *
   * @return The {@code Predicate} that matches {@code HttpRequest}s with given {@code uri}.
   */
  public static Predicate<HttpRequest> uri(URI uri) {
    Objects.requireNonNull(uri);
    return r -> r.uri().equals(uri);
  }

  /**
   * The {@code Predicate} that matches only {@code HttpResponse} with given header.
   *
   * @param name  The header name.
   * @param value The header value.
   * @param <T>   The response body type.
   *
   * @return The {@code Predicate} that matches {@code HttpResponse} with given header.
   */
  public static <T> Predicate<HttpResponse<T>> hasHeader(String name, String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);

    return r -> r.headers().firstValue(name).filter(value::equals).isPresent();
  }

  /**
   * The {@code Predicate} that matches only {@code HttpRequest} with given header.
   *
   * @param name The header name.
   * @param <T>  The response body type.
   *
   * @return The {@code Predicate} that matches {@code HttpRequest} with given header.
   */
  public static <T> Predicate<HttpResponse<T>> hasHeader(String name) {
    Objects.requireNonNull(name);

    return r -> r.headers().firstValue(name).isPresent();
  }
}
