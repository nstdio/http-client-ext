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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;

class PlainSubscription implements Flow.Subscription {
  private final Optional<ExecutorService> executor;
  private final Subscriber<List<ByteBuffer>> subscriber;
  private final List<ByteBuffer> buffers;
  private final Iterator<ByteBuffer> it;
  private boolean completed;

  PlainSubscription(Subscriber<List<ByteBuffer>> subscriber, List<ByteBuffer> buffers) {
    this(subscriber, buffers, true);
  }

  PlainSubscription(Subscriber<List<ByteBuffer>> subscriber, List<ByteBuffer> buffers, boolean async) {
    this.subscriber = subscriber;
    this.buffers = buffers;
    this.it = buffers.iterator();
    this.executor = async ? Optional.of(Executors.newSingleThreadExecutor()) : Optional.empty();
  }

  @Override
  public void request(long n) {
    if (completed)
      return;

    request0(n);
  }

  private void request0(long n) {
    while (n != 0 && it.hasNext()) {
      List<ByteBuffer> next = List.of(it.next());
      execute(() -> subscriber.onNext(next));
      n--;
    }

    if (!it.hasNext()) {
      completed = true;
      clean();

      subscriber.onComplete();
    }
  }

  private void execute(Runnable cmd) {
    executor.ifPresentOrElse(service -> service.execute(cmd), cmd);
  }

  @Override
  public void cancel() {
    if (completed) {
      return;
    }

    completed = true;
    request0(Integer.MAX_VALUE);

    clean();
  }

  private void clean() {
    buffers.clear();

    executor.ifPresent(service -> {
      service.shutdown();
      try {
        //noinspection ResultOfMethodCallIgnored
        service.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // noop
      }
    });
  }
}
