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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.zip.GZIPInputStream;

import static io.github.nstdio.http.ext.IOUtils.closeQuietly;

class DecompressingSubscriber<T> implements BodySubscriber<T> {
  private static final int MIN_BYTES_TO_INIT = 10;
  private static final List<ByteBuffer> LAST_ITEM = List.of();

  private final BodySubscriber<T> downstream;
  private final int bufferSize;
  private final ByteBufferInputStream is = new ByteBufferInputStream();
  private final UnaryOperator<InputStream> fn;
  private final AtomicBoolean completed = new AtomicBoolean();

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

    item.forEach(is::add);

    if (item == LAST_ITEM) {
      InputStream decStream;

      if (!hasEnoughBytesForInit() || (decStream = initDecompressingStream()) == null) {
        pushRemainingBytes();
        return;
      }

      try {
        List<ByteBuffer> dec = decompress(decStream);
        pushNext(dec);
      } catch (IOException e) {
        completed.set(true);

        onError(e);
        onComplete();
      }
    }
  }

  private List<ByteBuffer> decompress(InputStream decStream) throws IOException {
    List<ByteBuffer> dec = new ArrayList<>(1);
    ByteBuffer buf = newBuffer();

    try (var stream = decStream) {
      int r;
      while ((r = stream.read()) != -1) {
        if (!buf.hasRemaining()) {
          add(dec, buf);
          buf = newBuffer();
        }

        buf.put((byte) r);
      }

      add(dec, buf);
    }

    return Collections.unmodifiableList(dec);
  }

  private boolean hasEnoughBytesForInit() {
    // trying to buffer at least 10 bytes
    // to normally initialize decompressingStream

    return available() >= MIN_BYTES_TO_INIT;
  }

  private void pushRemainingBytes() {
    pushNext(is.drainToList());
    close();
  }

  private void pushNext(List<ByteBuffer> item) {
    if (!item.isEmpty()) {
      downstream.onNext(item);
    }
  }

  private ByteBuffer newBuffer() {
    return ByteBuffer.allocate(bufferSize);
  }

  private void add(Collection<ByteBuffer> decompressed, ByteBuffer buf) {
    if (buf.position() > 0) {
      decompressed.add(buf.flip());
    }
  }

  private InputStream initDecompressingStream() {
    try {
      is.mark(32);
      return fn.apply(is);
    } catch (UncheckedIOException e) {
      try {
        is.reset();
      } catch (IOException ignored) {
        // not relevant
      }

      return null;
    }
  }

  private int available() {
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
  }

  @Override
  public void onComplete() {
    onNext(LAST_ITEM);
    completed.set(true);

    close();
    downstream.onComplete();
  }
}
