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

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.function.Consumer;

class NullCache implements Cache {
    static final NullCache INSTANCE = new NullCache();

    private static final Consumer<Object> NOOP_CONSUMER = t -> {
    };
    private static final Writer<Object> WRITER = new Writer<>() {
        @Override
        public HttpResponse.BodySubscriber<Object> subscriber() {
            return BodySubscribers.mapping(BodySubscribers.discarding(), unused -> null);
        }

        @Override
        public Consumer<Object> finisher() {
            return NOOP_CONSUMER;
        }
    };

    private NullCache() {
    }

    @SuppressWarnings("unchecked")
    static <T> Writer<T> writer() {
        return (Writer<T>) WRITER;
    }

    @Override
    public CacheEntry get(HttpRequest request) {
        return null;
    }

    @Override
    public void put(HttpRequest request, CacheEntry entry) {
        // intentional noop
    }

    @Override
    public void evict(HttpRequest request) {
        // intentional noop
    }

    @Override
    public void evictAll(HttpRequest request) {
        // intentional noop
    }

    @Override
    public void evictAll() {
        // intentional noop
    }

    @Override
    public <T> Writer<T> writer(CacheEntryMetadata metadata) {
        return writer();
    }
}
