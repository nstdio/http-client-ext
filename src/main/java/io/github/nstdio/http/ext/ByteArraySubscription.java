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
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

class ByteArraySubscription implements Subscription {
    private final Subscriber<List<ByteBuffer>> subscriber;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final byte[] bytes;

    ByteArraySubscription(Subscriber<List<ByteBuffer>> subscriber, byte[] bytes) {
        this.subscriber = subscriber;
        this.bytes = bytes;
    }

    @Override
    public void request(long n) {
        if (completed.get()) {
            return;
        }

        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("n <= 0"));
            return;
        }

        completed.set(true);

        ByteBuffer buffer = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
        List<ByteBuffer> item = List.of(buffer);

        subscriber.onNext(item);
        subscriber.onComplete();
    }

    @Override
    public void cancel() {
        completed.set(true);
    }
}
