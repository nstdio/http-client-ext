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
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Cache {
    /**
     * Creates a new {@code InMemoryCacheBuilder} instance.
     *
     * @return the new {@code  InMemoryCacheBuilder}.
     */
    static InMemoryCacheBuilder newInMemoryCacheBuilder() {
        return new InMemoryCacheBuilder();
    }

    /**
     * Gets the {@code Cache} effectively does not do anything.
     *
     * @return a stub cache.
     */
    static Cache noop() {
        return NullCache.INSTANCE;
    }

    /**
     * Gets the cache entry associated with {@code request}.
     *
     * @param request The request.
     *
     * @return the cache entry associated with {@code request} or {@code null}.
     */
    CacheEntry get(HttpRequest request);

    /**
     * Associates {@code request} with {@code entry} and stores it in this cache. The previously (if any) associated
     * entry will be evicted.
     *
     * @param request The request.
     * @param entry   The entry.
     */
    void put(HttpRequest request, CacheEntry entry);

    /**
     * Evicts cache entry (if any) associated with {@code  request}.
     *
     * @param request The request.
     */
    void evict(HttpRequest request);

    /**
     * Evicts all cache entries associated with {@code request}.
     *
     * @param request The request.
     */
    void evictAll(HttpRequest request);

    /**
     * Removes all cache entries. This cache will be empty after method invocation.
     */
    void evictAll();

    /**
     * Creates a {@code Writer}.
     *
     * @param metadata The metadata.
     * @param <T>      The response body type.
     *
     * @return a {@code Writer}.
     */
    <T> Writer<T> writer(CacheEntryMetadata metadata);

    interface Writer<T> {
        /**
         * The body subscriber to collect response body.
         *
         * @return The body subscriber.
         */
        BodySubscriber<T> subscriber();

        /**
         * The consumer to be invoked after response body fully read.
         *
         * @return The response body consumer.
         */
        Consumer<T> finisher();
    }

    interface CacheEntry {
        void subscribeTo(Subscriber<List<ByteBuffer>> sub);

        CacheEntryMetadata metadata();
    }

    interface CacheBuilder {
        Cache build();
    }

    class InMemoryCacheBuilder implements CacheBuilder {
        private int maxItems = 1 << 13;
        private Predicate<HttpRequest> requestFilter;
        private Predicate<ResponseInfo> responseFilter;

        private long size = -1;

        InMemoryCacheBuilder() {
        }

        /**
         * The maximum number of cache entries. After reaching the limit the eldest entries will be evicted. Default is
         * 8192.
         *
         * @param maxItems The maximum number of cache entries. Should be positive.
         */
        public InMemoryCacheBuilder maxItems(int maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        /**
         * The amount of bytes allowed to be stored. Negative value means no memory restriction is made. Note that only
         * response body bytes are counted.
         */
        public InMemoryCacheBuilder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Adds given predicate to predicated chain. The calls with requests that did not pass given predicate will not
         * be subjected to caching facility. Semantically request filter is equivalent to {@code Cache-Control:
         * no-store} header in request.
         *
         * @param filter The request filter.
         */
        public InMemoryCacheBuilder requestFilter(Predicate<HttpRequest> filter) {
            Objects.requireNonNull(filter);

            if (requestFilter == null) {
                requestFilter = filter;
            } else {
                requestFilter = requestFilter.and(filter);
            }

            return this;
        }

        /**
         * Adds given predicate to predicated chain. The calls resulting with response that did not pass given predicate
         * will not be subjected to caching facility. Semantically response filter is equivalent to {@code
         * Cache-Control: no-store} header in response.
         *
         * @param filter The request filter.
         */
        public InMemoryCacheBuilder responseFilter(Predicate<ResponseInfo> filter) {
            Objects.requireNonNull(filter);

            if (responseFilter == null) {
                responseFilter = filter;
            } else {
                responseFilter = responseFilter.and(filter);
            }

            return this;
        }

        @Override
        public Cache build() {
            if (maxItems <= 0) {
                throw new IllegalStateException("maxItems should be positive");
            }
            var memCache = new InMemoryCache(maxItems, size);
            Cache c;

            if (requestFilter != null || responseFilter != null) {
                Predicate<HttpRequest> req = requestFilter == null ? alwaysTrue() : requestFilter;
                Predicate<ResponseInfo> resp = responseFilter == null ? alwaysTrue() : responseFilter;

                c = new FilteringCache(memCache, req, resp);
            } else {
                c = memCache;
            }

            return new SynchronizedCache(c);
        }
    }
}
