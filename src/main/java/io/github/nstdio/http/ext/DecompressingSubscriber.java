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
import java.util.function.UnaryOperator;
import java.util.zip.GZIPInputStream;

class DecompressingSubscriber<T> implements BodySubscriber<T> {
    private final BodySubscriber<T> downstream;
    private final int bufferSize;
    private final ByteBufferInputStream is = new ByteBufferInputStream();
    private final UnaryOperator<InputStream> fn;
    private final AtomicBoolean initError = new AtomicBoolean();
    private final AtomicBoolean completed = new AtomicBoolean();

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

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {}
        }
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
            downstream.onNext(item);
            return;
        }

        item.forEach(is::addBuffer);
        if (!initDecompressingStream()) {
            downstream.onNext(item);
            return;
        }

        List<ByteBuffer> dec = new ArrayList<>();
        ByteBuffer buf = newBuffer();

        try {
            while (decompressingStream.available() > 0) {
                if (!buf.hasRemaining()) {
                    add(dec, buf);
                    buf = newBuffer();
                }

                int r = decompressingStream.read();
                if (r != -1) {
                    byte b = (byte) r;
                    buf.put(b);
                }
            }
            pushNext(dec, buf);
        } catch (EOFException e) {
            pushNext(dec, buf);
        } catch (IOException e) {
            downstream.onError(e);
            onComplete();
        }
    }

    private void pushNext(List<ByteBuffer> dec, ByteBuffer buf) {
        add(dec, buf);
        if (!dec.isEmpty()) {
            downstream.onNext(dec);
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
        if (initError.get()) {
            return false;
        }

        if (decompressingStream == null) {
            try {
                decompressingStream = fn.apply(is);
                return true;
            } catch (UncheckedIOException e) {
                initError.set(true);
                is.close();
            }
        }

        return !initError.get();
    }

    @Override
    public void onError(Throwable throwable) {
        downstream.onError(throwable);
        close();
    }

    private void close() {
        closeQuietly(is);
        closeQuietly(decompressingStream);
    }

    @Override
    public void onComplete() {
        close();
        completed.set(true);
        downstream.onComplete();
    }
}
