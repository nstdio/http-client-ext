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
}
