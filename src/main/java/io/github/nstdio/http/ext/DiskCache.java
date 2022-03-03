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

import static io.github.nstdio.http.ext.InMemoryCache.indexOf;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

class DiskCache implements Cache {
    private final LruMultimap<URI, DiskCacheEntry> map;
    private final Path dir;

    DiskCache(Path dir) {
        this.map = new LruMultimap<>(1 << 13, this::deleteQuietly);
        this.dir = dir;
    }

    @Override
    public CacheEntry get(HttpRequest request) {
        return map.getSingle(request.uri(), l -> indexOf(request, l));
    }

    @Override
    public void put(HttpRequest request, CacheEntry entry) {
        map.putSingle(request.uri(), (DiskCacheEntry) entry, l -> indexOf(request, l));
    }

    @Override
    public void evict(HttpRequest request) {
        map.remove(request.uri(), l -> indexOf(request, l));
    }

    @Override
    public void evictAll(HttpRequest r) {
        map.evictAll(r.uri());
    }

    @Override
    public void evictAll() {
        map.clear();
    }

    private void deleteQuietly(DiskCacheEntry e) {
        try {
            Files.delete(e.path());
        } catch (IOException ignored) {
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer<Path> writer(CacheEntryMetadata metadata) {
        return new Writer<>() {
            @Override
            public BodySubscriber<Path> subscriber() {
                var fileName = UUID.randomUUID().toString().replace("-", "");

                var path = dir.resolve(fileName);

                return BodySubscribers.ofFile(path);
            }

            @Override
            public Consumer<Path> finisher() {
                return path -> put(metadata.request(), new DiskCacheEntry(path, metadata));
            }
        };
    }

    private static class DiskCacheEntry implements CacheEntry {
        private final Path path;
        private final CacheEntryMetadata metadata;

        private DiskCacheEntry(Path path, CacheEntryMetadata metadata) {
            this.path = path;
            this.metadata = metadata;
        }

        @Override
        public void subscribeTo(Subscriber<List<ByteBuffer>> sub) {
            Subscription subscription = new PathReadingSubscription(sub, path);
            sub.onSubscribe(subscription);
        }

        @Override
        public CacheEntryMetadata metadata() {
            return metadata;
        }

        Path path() {
            return path;
        }
    }
}
