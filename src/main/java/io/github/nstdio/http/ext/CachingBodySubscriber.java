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

import static java.util.stream.Collectors.toList;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

class CachingBodySubscriber<T, C> implements HttpResponse.BodySubscriber<T> {
    private final HttpResponse.BodySubscriber<T> originalSub;
    private final Consumer<C> finisher;
    private final HttpResponse.BodySubscriber<C> cachingSub;

    CachingBodySubscriber(HttpResponse.BodySubscriber<T> originalSub, HttpResponse.BodySubscriber<C> sub, Consumer<C> finisher) {
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
        originalSub.onSubscribe(subscription);
        cachingSub.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        originalSub.onNext(item);
        cachingSub.onNext(dup(item));
    }

    private List<ByteBuffer> dup(List<ByteBuffer> item) {
        return item.stream().map(ByteBuffer::duplicate).collect(toList());
    }

    @Override
    public void onError(Throwable throwable) {
        originalSub.onError(throwable);
        cachingSub.onError(throwable);
    }

    @Override
    public void onComplete() {
        originalSub.onComplete();
        cachingSub.onComplete();
    }
}
