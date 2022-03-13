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
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

class InMemoryCache extends SizeConstrainedCache {

    private static final Consumer<CacheEntry> EMPTY_CONSUMER = entry -> {
    };

    InMemoryCache(int maxItems, long maxBytes) {
        super(maxItems, maxBytes, EMPTY_CONSUMER);
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

        @Override
        public long bodySize() {
            return body.length;
        }

        @Override
        public void subscribeTo(Flow.Subscriber<List<ByteBuffer>> sub) {
            Flow.Subscription subscription = new ByteArraySubscription(sub, body);
            sub.onSubscribe(subscription);
        }

        @Override
        public CacheEntryMetadata metadata() {
            return metadata;
        }
    }
}
