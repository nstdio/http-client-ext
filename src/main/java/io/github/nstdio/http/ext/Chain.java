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
import java.util.Optional;

import static io.github.nstdio.http.ext.Throwables.sneakyThrow;

class Chain<T> {
  private final RequestContext ctx;
  private final FutureHandler<T> futureHandler;
  private final Optional<HttpResponse<T>> response;

  private Chain(RequestContext ctx, FutureHandler<T> futureHandler, Optional<HttpResponse<T>> response) {
    this.ctx = ctx;
    this.futureHandler = futureHandler;
    this.response = response;
  }

  public static <T> Chain<T> of(RequestContext ctx) {
    return Chain.of(ctx, (r, th) -> {
      if (r != null)
        return r;
      throw sneakyThrow(th);
    });
  }

  public static <T> Chain<T> of(RequestContext ctx, FutureHandler<T> futureHandler) {
    return new Chain<>(ctx, futureHandler, Optional.empty());
  }

  public static <T> Chain<T> of(RequestContext ctx, FutureHandler<T> futureHandler, Optional<HttpResponse<T>> response) {
    return new Chain<>(ctx, futureHandler, response);
  }

  Chain<T> withResponse(HttpResponse<T> response) {
    return of(ctx, futureHandler, Optional.of(response));
  }

  RequestContext ctx() {
    return this.ctx;
  }

  FutureHandler<T> futureHandler() {
    return this.futureHandler;
  }

  Optional<HttpResponse<T>> response() {
    return this.response;
  }
}
