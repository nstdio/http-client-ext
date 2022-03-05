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

import java.io.InputStream;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;
import java.util.function.UnaryOperator;

final class InputStreamDecompressingBodySubscriber implements BodySubscriber<InputStream> {

    private final BodySubscriber<InputStream> delegate = BodySubscribers.ofInputStream();
    private final UnaryOperator<InputStream> decompressingFn;

    InputStreamDecompressingBodySubscriber(UnaryOperator<InputStream> decompressingFn) {
        this.decompressingFn = decompressingFn;
    }

    @Override
    public CompletionStage<InputStream> getBody() {
        return delegate.getBody().thenApplyAsync(decompressingFn);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        delegate.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
        delegate.onComplete();
    }
}
