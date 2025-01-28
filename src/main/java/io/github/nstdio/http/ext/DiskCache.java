/*
 * Copyright (C) 2022-2025 the original author or authors.
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

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.github.nstdio.http.ext.IOUtils.createFile;
import static io.github.nstdio.http.ext.IOUtils.delete;
import static io.github.nstdio.http.ext.IOUtils.size;

class DiskCache extends SizeConstrainedCache {
  private final MetadataSerializer metadataSerializer;
  private final StreamFactory streamFactory;
  private final ExecutorService executor;
  private final Path dir;

  DiskCache(long maxBytes, int maxItems, MetadataSerializer metadataSerializer, StreamFactory streamFactory, Path dir) {
    super(maxItems, maxBytes, null);
    addEvictionListener(this::deleteQuietly);

    this.metadataSerializer = metadataSerializer;
    this.streamFactory = streamFactory;
    this.dir = dir;
    this.executor = Executors.newFixedThreadPool(1, r -> new Thread(r, "disk-cache-io"));

    restore();
  }

  private void restore() {
    var fileNamePattern = Pattern.compile("[a-f0-9]{32}_m").asMatchPredicate();
    Predicate<Path> pathPredicate = p -> fileNamePattern.test(p.getFileName().toString());

    try (var stream = Files.list(dir)) {
      stream
          .filter(pathPredicate)
          .filter(Files::isRegularFile)
          .map(metadataPath -> {
            Path bodyPath = metadataPath.resolveSibling(metadataPath.getFileName().toString().substring(0, 32));
            return EntryPaths.of(bodyPath, metadataPath);
          })
          .filter(entryPaths -> Files.exists(entryPaths.body()))
          .map(entryPaths -> {
            var metadata = metadataSerializer.read(entryPaths.metadata());
            return metadata != null ? new DiskCacheEntry(entryPaths, streamFactory, metadata) : null;
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

  @Override
  public void close() {
    super.close();
    executor.shutdown();
    try {
      //noinspection ResultOfMethodCallIgnored
      executor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException ignored) {
    }
  }

  private void writeMetadata(DiskCacheEntry diskEntry) {
    CacheEntryMetadata metadata = diskEntry.metadata();
    Path metadataPath = diskEntry.path().metadata();
    executor.execute(() -> metadataSerializer.write(metadata, metadataPath));
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
    if (!createFile(entryPaths.body())) {
      return NullCache.blackhole();
    }

    return new Writer<>() {
      @Override
      public BodySubscriber<Path> subscriber() {
        return new PathSubscriber(streamFactory, entryPaths.body());
      }

      @Override
      public Consumer<Path> finisher() {
        return path -> put(metadata.request(), new DiskCacheEntry(entryPaths, streamFactory, metadata));
      }
    };
  }

  private static class EntryPaths {
    private final Path body;
    private final Path metadata;

    private EntryPaths(Path body, Path metadata) {
      this.body = body;
      this.metadata = metadata;
    }

    public static EntryPaths of(Path body, Path metadata) {
      return new EntryPaths(body, metadata);
    }

    Path body() {
      return body;
    }

    Path metadata() {
      return metadata;
    }
  }

  private static class DiskCacheEntry implements CacheEntry {
    private final EntryPaths path;
    private final StreamFactory streamFactory;
    private final CacheEntryMetadata metadata;

    private final long bodySize;

    private DiskCacheEntry(EntryPaths path, StreamFactory streamFactory, CacheEntryMetadata metadata) {
      this.path = path;
      this.streamFactory = streamFactory;
      this.metadata = metadata;
      this.bodySize = size(path.body());
    }

    @Override
    public void subscribeTo(Subscriber<List<ByteBuffer>> sub) {
      Subscription subscription = new PathReadingSubscription(sub, streamFactory, path.body());
      sub.onSubscribe(subscription);
    }

    @Override
    public long bodySize() {
      return bodySize;
    }

    EntryPaths path() {
      return path;
    }

    public CacheEntryMetadata metadata() {
      return this.metadata;
    }
  }
}
