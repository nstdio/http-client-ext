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

import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

class CachingBodySubscriber<T, C> implements BodySubscriber<T> {
  private final BodySubscriber<T> originalSub;
  private final Consumer<C> finisher;
  private final BodySubscriber<C> cachingSub;

  CachingBodySubscriber(BodySubscriber<T> originalSub, BodySubscriber<C> sub, Consumer<C> finisher) {
    this.originalSub = originalSub;
    this.cachingSub = sub;
    this.finisher = finisher;
  }

  @Override
  public CompletionStage<T> getBody() {
    return originalSub.getBody()
        .thenApplyAsync(t -> {
          cachingSub.getBody()
              .thenApplyAsync(body -> {
                finisher.accept(body);
                return body;
              });
          return t;
        });
  }

  @Override
  public void onSubscribe(Flow.Subscription subscription) {
    cachingSub.onSubscribe(subscription);
    originalSub.onSubscribe(subscription);
  }

  @Override
  public void onNext(List<ByteBuffer> item) {
    cachingSub.onNext(item);
    originalSub.onNext(Buffers.duplicate(item));
  }

  @Override
  public void onError(Throwable throwable) {
    cachingSub.onError(throwable);
    originalSub.onError(throwable);
  }

  @Override
  public void onComplete() {
    cachingSub.onComplete();
    originalSub.onComplete();
  }
}
