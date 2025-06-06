/*
 * Copyright (C) 2022-2025 the original author or authors.
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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.nstdio.http.ext.IOUtils.closeQuietly;

class PathReadingSubscription implements Subscription {
  private static final int DEFAULT_BUFF_CAPACITY = 1 << 14;
  private final Subscriber<List<ByteBuffer>> subscriber;
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private final StreamFactory streamFactory;
  private final Path path;
  private ReadableByteChannel channel;

  PathReadingSubscription(Subscriber<List<ByteBuffer>> subscriber, StreamFactory streamFactory, Path path) {
    this.subscriber = subscriber;
    this.streamFactory = streamFactory;
    this.path = path;
  }

  @Override
  public void request(long n) {
    if (completed.get()) {
      return;
    }

    if (n <= 0) {
      subscriber.onError(new IllegalArgumentException("non-positive request"));
      return;
    }

    try {
      if (channel == null) {
        channel = streamFactory.readable(path);
      }

      var chan = channel;
      var sub = subscriber;
      while (n-- > 0) {
        ByteBuffer buff = ByteBuffer.allocate(DEFAULT_BUFF_CAPACITY);
        if (chan.read(buff) != -1) {
          buff.flip();
          sub.onNext(Collections.singletonList(buff));
        } else {
          cancel();
          sub.onComplete();
          break;
        }
      }

    } catch (IOException e) {
      subscriber.onError(e);
    }
  }

  @Override
  public void cancel() {
    completed.set(true);
    closeQuietly(channel);
  }
}
