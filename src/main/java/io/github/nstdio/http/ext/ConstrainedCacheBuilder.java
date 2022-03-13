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

import static io.github.nstdio.http.ext.Predicates.alwaysTrue;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.Objects;
import java.util.function.Predicate;

abstract class ConstrainedCacheBuilder<B extends ConstrainedCacheBuilder<B>> implements Cache.CacheBuilder {
    int maxItems = 1 << 13;
    long size = -1;
    private Predicate<HttpRequest> requestFilter;
    private Predicate<ResponseInfo> responseFilter;

    ConstrainedCacheBuilder() {
    }

    /**
     * The maximum number of cache entries. After reaching the limit the eldest entries will be evicted. Default is
     * 8192.
     *
     * @param maxItems The maximum number of cache entries. Should be positive.
     */
    public B maxItems(int maxItems) {
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems should be positive");
        }

        this.maxItems = maxItems;
        return self();
    }

    /**
     * The amount of bytes allowed to be stored. Negative value means no memory restriction is made. Note that only
     * response body bytes are counted.
     */
    public B size(long size) {
        this.size = size;
        return self();
    }

    /**
     * Adds given predicate to predicated chain. The calls with requests that did not pass given predicate will not be
     * subjected to caching facility. Semantically request filter is equivalent to {@code Cache-Control: no-store}
     * header in request.
     *
     * @param filter The request filter.
     */
    public B requestFilter(Predicate<HttpRequest> filter) {
        Objects.requireNonNull(filter);

        if (requestFilter == null) {
            requestFilter = filter;
        } else {
            requestFilter = requestFilter.and(filter);
        }

        return self();
    }

    /**
     * Adds given predicate to predicated chain. The calls resulting with response that did not pass given predicate
     * will not be subjected to caching facility. Semantically response filter is equivalent to {@code Cache-Control:
     * no-store} header in response.
     *
     * @param filter The request filter.
     */
    public B responseFilter(Predicate<ResponseInfo> filter) {
        Objects.requireNonNull(filter);

        if (responseFilter == null) {
            responseFilter = filter;
        } else {
            responseFilter = responseFilter.and(filter);
        }

        return self();
    }

    Cache build(SizeConstrainedCache cache) {
        Cache c;

        if (requestFilter != null || responseFilter != null) {
            Predicate<HttpRequest> req = requestFilter == null ? alwaysTrue() : requestFilter;
            Predicate<ResponseInfo> resp = responseFilter == null ? alwaysTrue() : responseFilter;

            c = new FilteringCache(cache, req, resp);
        } else {
            c = cache;
        }

        return new SynchronizedCache(c);
    }

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }
}
