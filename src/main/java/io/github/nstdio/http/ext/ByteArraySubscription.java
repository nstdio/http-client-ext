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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.runAsync;

class ByteArraySubscription<T> implements Subscription {
  private final Subscriber<T> subscriber;
  private final Executor executor;

  private final Function<byte[], T> mapper;
  private final Supplier<byte[]> bytes;

  private final AtomicBoolean completed = new AtomicBoolean(false);
  Future<?> result;

  ByteArraySubscription(Subscriber<T> subscriber, Executor executor, Supplier<byte[]> bytes, Function<byte[], T> mapper) {
    this.subscriber = subscriber;
    this.executor = executor;
    this.bytes = bytes;
    this.mapper = mapper;
  }

  static ByteArraySubscription<List<ByteBuffer>> ofByteBufferList(Subscriber<List<ByteBuffer>> subscriber, byte[] bytes) {
    return new ByteArraySubscription<>(subscriber, DirectExecutor.INSTANCE, () -> bytes, o -> List.of(ByteBuffer.wrap(o).asReadOnlyBuffer()));
  }

  static ByteArraySubscription<? super ByteBuffer> ofByteBuffer(Subscriber<? super ByteBuffer> subscriber, Supplier<byte[]> bytes, Executor executor) {
    return new ByteArraySubscription<>(subscriber, executor, bytes, ByteBuffer::wrap);
  }

  @Override
  public void request(long n) {
    if (!completed.getAndSet(true)) {
      if (n > 0) {
        submit(() -> {
          try {
            T item = mapper.apply(bytes.get());

            subscriber.onNext(item);
            subscriber.onComplete();
          } catch (Throwable th) {
            subscriber.onError(th);
          }
        });
      } else {
        var e = new IllegalArgumentException("n <= 0");
        submit(() -> subscriber.onError(e));
      }
    }
  }

  @Override
  public void cancel() {
    completed.set(true);

    if (result != null) {
      result.cancel(false);
    }
  }

  private void submit(Runnable r) {
    result = runAsync(r, executor);
  }

  private enum DirectExecutor implements Executor {
    INSTANCE;

    @Override
    public void execute(Runnable command) {
      command.run();
    }
  }
}
