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
import java.net.http.HttpResponse;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

class SynchronizedCache implements Cache {
  private final Cache delegate;
  private final Lock lock = new ReentrantLock();

  SynchronizedCache(Cache delegate) {
    this.delegate = delegate;
  }

  @Override
  public CacheEntry get(HttpRequest request) {
    lock.lock();
    try {
      return delegate.get(request);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void put(HttpRequest request, CacheEntry entry) {
    lock.lock();
    try {
      delegate.put(request, entry);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void evict(HttpRequest request) {
    lock.lock();
    try {
      delegate.evict(request);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void evictAll(HttpRequest request) {
    lock.lock();
    try {
      delegate.evictAll(request);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void evictAll() {
    lock.lock();
    try {
      delegate.evictAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public /* synchronized */ CacheStats stats() {
    return delegate.stats();
  }

  @Override
  public <T> Writer<T> writer(CacheEntryMetadata metadata) {
    Writer<T> writer = delegate.writer(metadata);
    return new Writer<>() {
      @Override
      public HttpResponse.BodySubscriber<T> subscriber() {
        return writer.subscriber();
      }

      @Override
      public Consumer<T> finisher() {
        return t -> {
          synchronized (this) {
            writer.finisher().accept(t);
          }
        };
      }
    };
  }

  @Override
  public void close() throws IOException {
    lock.lock();
    try {
      delegate.close();
    } finally {
      lock.unlock();
    }
  }
  
  Cache delegate() {
    return delegate;
  }
}
