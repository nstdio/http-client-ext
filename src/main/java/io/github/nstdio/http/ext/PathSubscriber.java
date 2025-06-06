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

import static io.github.nstdio.http.ext.IOUtils.closeQuietly;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class PathSubscriber implements HttpResponse.BodySubscriber<Path> {
  private final StreamFactory streamFactory;
  private final Path path;
  private final CompletableFuture<Path> future = new CompletableFuture<>();
  private final Lock lock = new ReentrantLock();
  private WritableByteChannel out;

  PathSubscriber(Path path) {
    this(new SimpleStreamFactory(), path);
  }

  PathSubscriber(StreamFactory streamFactory, Path path) {
    this.streamFactory = streamFactory;
    this.path = path;
  }

  @Override
  public CompletionStage<Path> getBody() {
    return future;
  }

  @Override
  public void onSubscribe(Flow.Subscription subscription) {
    createChannel();
  }

  private void createChannel() {
    lock.lock();
    try {
      if (out != null) {
        return;
      }

      try {
        out = streamFactory.writable(path, WRITE, TRUNCATE_EXISTING);
      } catch (IOException e) {
        future.completeExceptionally(e);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onNext(List<ByteBuffer> item) {
    try {
      write(item);
    } catch (IOException ex) {
      onError(ex);
    }
  }

  private void write(List<ByteBuffer> item) throws IOException {
    if (out instanceof GatheringByteChannel) {
      ((GatheringByteChannel) out).write(item.toArray(ByteBuffer[]::new));
    } else {
      for (ByteBuffer buffer : item) {
        out.write(buffer);
      }
    }
  }

  private void close() {
    lock.lock();
    try {
      closeQuietly(out);
      out = null;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onError(Throwable throwable) {
    close();
    future.completeExceptionally(throwable);
  }

  @Override
  public void onComplete() {
    close();
    future.complete(path);
  }
}
