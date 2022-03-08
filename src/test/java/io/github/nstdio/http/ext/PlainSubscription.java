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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;

class PlainSubscription implements Flow.Subscription {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Subscriber<List<ByteBuffer>> subscriber;
    private final List<ByteBuffer> buffers;
    private final Iterator<ByteBuffer> it;
    private boolean canceled;

    PlainSubscription(Subscriber<List<ByteBuffer>> subscriber, List<ByteBuffer> buffers) {
        this.subscriber = subscriber;
        this.buffers = buffers;
        this.it = buffers.iterator();
    }

    @Override
    public void request(long n) {
        if (canceled)
            return;

        request0(n);
    }

    private void request0(long n) {
        while (n != 0 && it.hasNext()) {
            List<ByteBuffer> next = List.of(it.next());
            executor.execute(() -> subscriber.onNext(next));
            n--;
        }
    }

    @Override
    public void cancel() {
        if (canceled) {
            return;
        }
        canceled = true;
        request0(Integer.MAX_VALUE);

        buffers.clear();
        try {
            executor.shutdown();
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
