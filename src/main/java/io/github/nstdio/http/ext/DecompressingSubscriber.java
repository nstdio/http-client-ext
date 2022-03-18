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

import static io.github.nstdio.http.ext.IOUtils.closeQuietly;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.zip.GZIPInputStream;

class DecompressingSubscriber<T> implements BodySubscriber<T> {
    private static final List<ByteBuffer> LAST_ITEM = List.of();

    private final BodySubscriber<T> downstream;
    private final int bufferSize;
    private final ByteBufferInputStream is = new ByteBufferInputStream();
    private final UnaryOperator<InputStream> fn;
    private final AtomicBoolean initError = new AtomicBoolean();
    private final AtomicBoolean completed = new AtomicBoolean();
    private final AtomicInteger lastAvailable = new AtomicInteger(-1);

    /**
     * Created by {@code fn} having {@code is} as input.
     */
    private volatile InputStream decompressingStream;

    DecompressingSubscriber(BodySubscriber<T> downstream) {
        this(downstream, in -> {
            try {
                return new GZIPInputStream(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    DecompressingSubscriber(BodySubscriber<T> downstream, UnaryOperator<InputStream> fn) {
        this(downstream, fn, 8192);
    }

    private DecompressingSubscriber(BodySubscriber<T> downstream, UnaryOperator<InputStream> fn, int bufferSize) {
        this.downstream = downstream;
        this.bufferSize = bufferSize;
        this.fn = fn;
    }

    @Override
    public CompletionStage<T> getBody() {
        return downstream.getBody();
    }

    @Override
    public void onSubscribe(Flow.Subscription sub) {
        downstream.onSubscribe(sub);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        if (completed.get()) {
            return;
        }

        if (initError.get()) {
            onNext0(item);
            return;
        }

        item.forEach(is::add);

        if (decompressingStream == null) {
            // trying to buffer at least 10 bytes
            // to normally initialize decompressingStream

            int available = currentlyAvailable();
            if (available < 10) {
                // nothing changed since last execution
                if (lastAvailable.getAndSet(available) == available) {
                    pushRemainingBytes();
                }

                return;
            }
        }

        if (!initDecompressingStream()) {
            pushRemainingBytes();
            return;
        }

        List<ByteBuffer> dec = new ArrayList<>();
        ByteBuffer buf = newBuffer();

        try {
            var stream = decompressingStream;
            while (stream.available() > 0) {
                if (!buf.hasRemaining()) {
                    add(dec, buf);
                    buf = newBuffer();
                }

                int r = stream.read();
                if (r != -1) {
                    buf.put((byte) r);
                }
            }

            pushNext(dec, buf);
        } catch (EOFException e) {
            pushNext(dec, buf);
        } catch (IOException e) {
            if (item == LAST_ITEM) {
                completed.set(true);
            }

            onError(e);
            onComplete();

        }
    }

    private void pushRemainingBytes() {
        try {
            List<ByteBuffer> wrap = List.of(ByteBuffer.wrap(is.readAllBytes()));
            onNext0(wrap);
        } catch (IOException ignored) {
            // cannot happen because using ByteBufferInputStream which
            // is not connect to any I/O device.
        }

        close();
    }

    private void onNext0(List<ByteBuffer> item) {
        if (item != LAST_ITEM) {
            downstream.onNext(item);
        }
    }

    private void pushNext(List<ByteBuffer> dec, ByteBuffer buf) {
        add(dec, buf);
        if (!dec.isEmpty()) {
            onNext0(dec);
        }
    }

    private ByteBuffer newBuffer() {
        return ByteBuffer.allocate(bufferSize);
    }

    private void add(List<ByteBuffer> decompressed, ByteBuffer buf) {
        if (buf.position() > 0) {
            decompressed.add(buf.flip());
        }
    }

    private boolean initDecompressingStream() {
        if (decompressingStream != null) {
            return true;
        }

        try {
            is.mark(32);
            decompressingStream = fn.apply(is);
        } catch (UncheckedIOException e) {
            initError.set(true);
            try {
                is.reset();
            } catch (IOException ignored) {
                // not relevant
            }
        }

        return !initError.get();
    }

    private int currentlyAvailable() {
        int available = 0;
        try {
            available = is.available();
        } catch (IOException ignored) {
            // not relevant
        }

        return available;
    }

    @Override
    public void onError(Throwable throwable) {
        downstream.onError(throwable);
    }

    private void close() {
        closeQuietly(is);
        closeQuietly(decompressingStream);
    }

    @Override
    public void onComplete() {
        onNext(LAST_ITEM);

        close();
        completed.set(true);
        downstream.onComplete();
    }
}
