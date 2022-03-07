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

import static lombok.Lombok.sneakyThrow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.http.HttpResponse;
import java.util.Optional;

interface Interceptor {
    <T> Chain<T> intercept(Chain<T> in);

    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor(staticName = "of")
    class Chain<T> {
        RequestContext ctx;
        AsyncHandler<T> asyncHandler;
        Optional<HttpResponse<T>> response;

        public static <T> Chain<T> of(RequestContext ctx) {
            return Chain.of(ctx, (r, th) -> {
                if (r != null)
                    return r;
                throw sneakyThrow(th);
            });
        }

        public static <T> Chain<T> of(RequestContext ctx, AsyncHandler<T> asyncHandler) {
            return new Chain<>(ctx, asyncHandler, Optional.empty());
        }

        Chain<T> withResponse(HttpResponse<T> response) {
            return of(ctx, asyncHandler, Optional.of(response));
        }
    }
}
