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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
final class RequestContext {
    @With
    private final HttpRequest request;
    @With
    private final BodyHandler<?> bodyHandler;
    private final CacheControl cacheControl;
    private final AtomicLong requestTime = new AtomicLong();
    private final AtomicLong responseTime = new AtomicLong();

    static RequestContext of(HttpRequest request, BodyHandler<?> responseBodyHandler) {
        return new RequestContext(request, responseBodyHandler, CacheControl.of(request));
    }

    boolean isCacheable() {
        if (!"GET".equals(request.method())) return false;

        return !cacheControl.noStore();
    }

    long requestTimeLong() {
        return requestTime.get();
    }

    long responseTimeLong() {
        return responseTime.get();
    }

    @SuppressWarnings("unchecked")
    <T> BodyHandler<T> bodyHandler() {
        return (BodyHandler<T>) bodyHandler;
    }
}
