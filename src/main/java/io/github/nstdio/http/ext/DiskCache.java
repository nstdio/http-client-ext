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

import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.regex.Pattern;

class DiskCache extends SizeConstrainedCache {
    private final MetadataSerializer metadataSerializer = new JsonMetadataSerializer();
    private final Executor executor;
    private final Path dir;

    DiskCache(Path dir) {
        this(1 << 13, -1, dir);
    }

    DiskCache(int maxItems, long maxBytes, Path dir) {
        super(maxItems, maxBytes, null);
        addEvictionListener(this::deleteQuietly);

        this.dir = dir;
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "disk-cache-io"));

        restore();
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    private static void delete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ignored) {
        }
    }

    private void restore() {
        var namePredicate = Pattern.compile("[a-f0-9]{32}_m").asMatchPredicate();

        try (var stream = Files.list(dir)) {
            stream
                    .filter(path -> namePredicate.test(path.getFileName().toString()))
                    .filter(Files::isRegularFile)
                    .map(metadataPath -> {
                        Path bodyPath = metadataPath.resolveSibling(metadataPath.getFileName().toString().substring(0, 32));
                        return EntryPaths.of(bodyPath, metadataPath);
                    })
                    .filter(entryPaths -> Files.exists(entryPaths.body()))
                    .map(entryPaths -> {
                        var metadata = metadataSerializer.read(entryPaths.metadata());
                        return metadata != null ? new DiskCacheEntry(entryPaths, metadata) : null;
                    })
                    .filter(Objects::nonNull)
                    .forEach(entry -> put(entry.metadata().request(), entry));
        } catch (IOException ignored) {
            // noop
        }
    }

    @Override
    public void put(HttpRequest request, CacheEntry entry) {
        super.put(request, entry);

        writeMetadata((DiskCacheEntry) entry);
    }

    private void writeMetadata(DiskCacheEntry diskEntry) {
        executor.execute(() -> metadataSerializer.write(diskEntry.metadata(), diskEntry.path().metadata()));
    }

    private void deleteQuietly(CacheEntry entry) {
        EntryPaths paths = ((DiskCacheEntry) entry).path();

        executor.execute(() -> delete(paths.body()));
        executor.execute(() -> delete(paths.metadata()));
    }

    private EntryPaths pathsFor() {
        String s = UUID.randomUUID().toString().replace("-", "");

        Path bodyPath = dir.resolve(s);
        Path metadataPath = dir.resolve(s + "_m");

        return EntryPaths.of(bodyPath, metadataPath);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer<Path> writer(CacheEntryMetadata metadata) {
        EntryPaths entryPaths = pathsFor();
        return new Writer<>() {
            @Override
            public BodySubscriber<Path> subscriber() {
                return new PathSubscriber(entryPaths.body());
            }

            @Override
            public Consumer<Path> finisher() {
                return path -> put(metadata.request(), new DiskCacheEntry(entryPaths, metadata));
            }
        };
    }

    @Value(staticConstructor = "of")
    @Accessors(fluent = true)
    private static class EntryPaths {
        Path body;
        Path metadata;
    }

    @Getter
    @Accessors(fluent = true)
    private static class DiskCacheEntry implements CacheEntry {
        private final EntryPaths path;
        private final CacheEntryMetadata metadata;

        private final long bodySize;

        private DiskCacheEntry(EntryPaths path, CacheEntryMetadata metadata) {
            this.path = path;
            this.metadata = metadata;
            this.bodySize = size(path.body());
        }

        @Override
        public void subscribeTo(Subscriber<List<ByteBuffer>> sub) {
            Subscription subscription = new PathReadingSubscription(sub, path.body());
            sub.onSubscribe(subscription);
        }

        @Override
        public long bodySize() {
            return bodySize;
        }
    }
}
