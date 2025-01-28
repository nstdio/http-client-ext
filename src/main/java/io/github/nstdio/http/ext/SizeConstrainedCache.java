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

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import static io.github.nstdio.http.ext.InMemoryCache.indexOf;

abstract class SizeConstrainedCache implements Cache {
  private final LruMultimap<URI, CacheEntry> cache;
  private final CacheStats stats = new DefaultCacheStats();
  private final long maxBytes;
  private long size;

  SizeConstrainedCache(int maxItems, long maxBytes, Consumer<CacheEntry> evictionListener) {
    Consumer<CacheEntry> reduceSize = entry -> size -= entry.bodySize();
    var listener = evictionListener == null ? reduceSize : evictionListener.andThen(reduceSize);

    this.cache = new LruMultimap<>(maxItems, listener);
    this.maxBytes = maxBytes;
  }

  @Override
  public CacheEntry get(HttpRequest request) {
    return cache.getSingle(request.uri(), idxFn(request));
  }

  @Override
  public void put(HttpRequest request, CacheEntry e) {
    if (isUnbounded()) {
      putInternal(request, e);
    } else if (e.bodySize() <= maxBytes) {
      while (needSpace(e)) cache.evictEldest();

      putInternal(request, e);
    }
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

  @Override
  public CacheStats stats() {
    return stats;
  }

  @Override
  public void close() {
    cache.clear();
  }

  private void putInternal(HttpRequest k, CacheEntry e) {
    size += e.bodySize();
    cache.putSingle(k.uri(), e, idxFn(k));
  }

  private boolean needSpace(CacheEntry e) {
    return size + e.bodySize() > maxBytes;
  }

  private boolean isUnbounded() {
    return maxBytes <= 0;
  }

  private ToIntFunction<List<CacheEntry>> idxFn(HttpRequest r) {
    return l -> indexOf(r, l);
  }

  void addEvictionListener(Consumer<CacheEntry> l) {
    cache.addEvictionListener(l);
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
}
