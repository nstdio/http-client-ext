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
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

class InMemoryCache implements Cache {
    private final LruMultimap<URI, InMemoryCacheEntry> cache;
    private final long maxBytes;
    private long size;

    InMemoryCache(int maxItems, long maxBytes) {
        cache = new LruMultimap<>(maxItems, evictListener());
        this.maxBytes = maxBytes;
    }

    static int indexOf(HttpRequest r, List<? extends CacheEntry> es) {
        //TODO: Check if es is RandomAccess
        var headers = r.headers();

        outer:
        for (int i = 0; i < es.size(); i++) {
            var cacheEntry = es.get(i);
            var varyHeaders = cacheEntry.metadata().varyHeaders().map();

            for (var entry : varyHeaders.entrySet()) {
                if (!entry.getValue().equals(headers.allValues(entry.getKey()))) {
                    continue outer;
                }
            }

            return i;
        }

        return -1;
    }

    private Consumer<InMemoryCacheEntry> evictListener() {
        return e -> size -= e.bodySize();
    }

    int multimapSize() {
        return cache.size();
    }

    int mapSize() {
        return cache.mapSize();
    }

    long bytes() {
        return size;
    }

    @Override
    public CacheEntry get(HttpRequest request) {
        return cache.getSingle(request.uri(), idxFn(request));
    }

    @Override
    public void put(HttpRequest request, CacheEntry entry) {
        var e = (InMemoryCacheEntry) entry;

        if (isUnbounded()) {
            putInternal(request, e);
        } else if (e.bodySize() <= maxBytes) {
            while (needSpace(e)) cache.evictEldest();

            putInternal(request, e);
        }
    }

    private void putInternal(HttpRequest k, InMemoryCacheEntry e) {
        size += e.bodySize();
        cache.putSingle(k.uri(), e, idxFn(k));
    }

    @Override
    public void evict(HttpRequest request) {
        cache.remove(request.uri(), idxFn(request));
    }

    @Override
    public void evictAll(HttpRequest r) {
        cache.evictAll(r.uri());
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    private ToIntFunction<List<InMemoryCacheEntry>> idxFn(HttpRequest r) {
        return l -> indexOf(r, l);
    }

    private boolean needSpace(InMemoryCacheEntry e) {
        return size + e.bodySize() > maxBytes;
    }

    private boolean isUnbounded() {
        return maxBytes <= 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer<byte[]> writer(CacheEntryMetadata metadata) {
        return new Writer<>() {
            @Override
            public BodySubscriber<byte[]> subscriber() {
                return BodySubscribers.ofByteArray();
            }

            @Override
            public Consumer<byte[]> finisher() {
                return body -> {
                    var e = new InMemoryCacheEntry(body, metadata);
                    put(metadata.request(), e);
                };
            }
        };
    }

    static class InMemoryCacheEntry implements CacheEntry {
        private final byte[] body;
        private final CacheEntryMetadata metadata;

        InMemoryCacheEntry(byte[] body, CacheEntryMetadata metadata) {
            this.body = body;
            this.metadata = metadata;
        }

        int bodySize() {
            return body.length;
        }

        @Override
        public void subscribeTo(Flow.Subscriber<List<ByteBuffer>> sub) {
            Flow.Subscription subscription = new Flow.Subscription() {
                private final AtomicBoolean completed = new AtomicBoolean(false);

                @Override
                public void request(long n) {
                    if (completed.get()) {
                        return;
                    }

                    if (n <= 0) {
                        sub.onError(new IllegalArgumentException("n <= 0"));
                        return;
                    }

                    completed.set(true);
                    sub.onNext(List.of(ByteBuffer.wrap(body).asReadOnlyBuffer()));
                    sub.onComplete();
                }

                @Override
                public void cancel() {
                    completed.set(true);
                }
            };

            sub.onSubscribe(subscription);
        }

        @Override
        public CacheEntryMetadata metadata() {
            return metadata;
        }
    }
}
