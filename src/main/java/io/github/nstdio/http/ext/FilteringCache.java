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
import java.net.http.HttpResponse.ResponseInfo;
import java.util.function.Predicate;

import static io.github.nstdio.http.ext.NullCache.blackhole;

class FilteringCache implements Cache {
  private final Cache delegate;
  private final Predicate<HttpRequest> requestFilter;
  private final Predicate<ResponseInfo> responseFilter;

  FilteringCache(Cache delegate, Predicate<HttpRequest> requestFilter, Predicate<ResponseInfo> responseFilter) {
    this.delegate = delegate;
    this.requestFilter = requestFilter;
    this.responseFilter = responseFilter;
  }

  @Override
  public CacheEntry get(HttpRequest request) {
    if (matches(request))
      return delegate.get(request);

    return null;
  }

  @Override
  public void put(HttpRequest request, CacheEntry entry) {
    if (matches(request))
      delegate.put(request, entry);
  }

  @Override
  public void evict(HttpRequest request) {
    if (matches(request))
      delegate.evict(request);
  }

  @Override
  public void evictAll(HttpRequest request) {
    if (matches(request))
      delegate.evictAll(request);
  }

  private boolean matches(HttpRequest k) {
    return requestFilter.test(k);
  }

  @Override
  public void evictAll() {
    delegate.evictAll();
  }

  @Override
  public CacheStats stats() {
    return delegate.stats();
  }

  @Override
  public <T> Writer<T> writer(CacheEntryMetadata metadata) {
    if (responseFilter.test(metadata.response()) && requestFilter.test(metadata.request())) {
      return delegate.writer(metadata);
    }

    return blackhole();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
