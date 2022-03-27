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

import java.net.http.HttpResponse;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static io.github.nstdio.http.ext.Throwables.sneakyThrow;

/**
 * The function to pass to {@link java.util.concurrent.CompletableFuture#handleAsync(BiFunction)} or {@link
 * java.util.concurrent.CompletableFuture#handle(BiFunction)}. The type alias and handy chaining for other {@code
 * FutureHandler}s.
 *
 * @param <T> The response body type.
 */
@FunctionalInterface
interface FutureHandler<T> extends BiFunction<HttpResponse<T>, Throwable, HttpResponse<T>> {
  static <T> FutureHandler<T> of(UnaryOperator<HttpResponse<T>> op) {
    return (r, th) -> {
      if (r != null) {
        return op.apply(r);
      }
      throw sneakyThrow(th);
    };
  }

  default FutureHandler<T> andThen(FutureHandler<T> other) {
    return (r, th) -> {
      HttpResponse<T> result;
      try {
        result = apply(r, th);
      } catch (Exception e) {
        return other.apply(null, e);
      }

      return other.apply(result, th);
    };
  }
}
