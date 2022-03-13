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
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;

final class AsyncMappingSubscriber<T, U> implements BodySubscriber<U> {
    private final BodySubscriber<T> upstream;
    private final Function<? super T, ? extends U> mapper;

    AsyncMappingSubscriber(BodySubscriber<T> upstream, Function<? super T, ? extends U> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public CompletionStage<U> getBody() {
        return upstream.getBody().thenApplyAsync(mapper);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        upstream.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        upstream.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        upstream.onError(throwable);
    }

    @Override
    public void onComplete() {
        upstream.onComplete();
    }
}
