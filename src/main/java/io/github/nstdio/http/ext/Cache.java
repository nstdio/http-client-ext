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

import io.github.nstdio.http.ext.spi.Classpath;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
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
   * Creates a new {@code DiskCacheBuilder} instance. Requires Jackson form dumping cache files on disk.
   *
   * @return the new {@code  DiskCacheBuilder}.
   *
   * @throws IllegalStateException When Jackson (a.k.a. ObjectMapper) is not in classpath.
   */
  static DiskCacheBuilder newDiskCacheBuilder() {
    if (!Classpath.isJacksonPresent()) {
      throw new IllegalStateException("In order to use disk cache please add 'com.fasterxml.jackson.core:jackson-databind' to your dependencies");
    }

    return new DiskCacheBuilder();
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
   * Associates {@code request} with {@code entry} and stores it in this cache. The previously (if any) associated entry
   * will be evicted.
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
   * Gets the statistics for this cache.
   *
   * @return The statistics for this cache.
   *
   * @see CacheStats
   */
  CacheStats stats();

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

    default long bodySize() {
      return -1;
    }
  }

  interface CacheBuilder {
    Cache build();
  }

  interface CacheStats {
    /**
     * The number the cache serves stored response.
     *
     * @return The number of times the cache serves stored response.
     */
    long hit();

    /**
     * The number the cache does not have stored response which resulted in network call.
     *
     * @return The number the cache does not have stored response which resulted in network call.
     */
    long miss();
  }

  /**
   * The builder for in memory cache.
   */
  class InMemoryCacheBuilder extends ConstrainedCacheBuilder<InMemoryCacheBuilder> {
    InMemoryCacheBuilder() {
    }

    @Override
    public Cache build() {
      return build(new InMemoryCache(maxItems, size));
    }
  }

  /**
   * The builder for in persistent cache.
   */
  class DiskCacheBuilder extends ConstrainedCacheBuilder<DiskCacheBuilder> {
    private Path dir;

    DiskCacheBuilder() {
    }

    /**
     * Sets the directory to store cache files.
     *
     * @param dir The directory to store cache files.
     *
     * @return builder itself.
     */
    public DiskCacheBuilder dir(Path dir) {
      this.dir = Objects.requireNonNull(dir);
      return this;
    }

    @Override
    public Cache build() {
      if (dir == null) {
        throw new IllegalStateException("dir cannot be null");
      }

      return build(new DiskCache(maxItems, size, dir));
    }
  }
}
