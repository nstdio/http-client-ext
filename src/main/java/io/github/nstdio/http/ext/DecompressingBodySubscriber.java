/*
 * Copyright (C) 2021 Edgar Asatryan
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.nstdio.http.ext;

import java.io.InputStream;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;

final class DecompressingBodySubscriber implements BodySubscriber<InputStream> {

    private final BodySubscriber<InputStream> delegate = BodySubscribers.ofInputStream();
    private final Function<InputStream, InputStream> decompressingFn;

    DecompressingBodySubscriber(Function<InputStream, InputStream> decompressingFn) {
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
