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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

class PathSubscriber implements HttpResponse.BodySubscriber<Path> {
    private static final ByteBuffer[] EMPTY = new ByteBuffer[0];
    private final Path path;
    private final CompletableFuture<Path> future = new CompletableFuture<>();
    private volatile FileChannel out;

    PathSubscriber(Path path) {
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

    private synchronized void createChannel() {
        if (out != null) {
            return;
        }

        try {
            out = FileChannel.open(path, CREATE, WRITE);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        createChannel();

        try {
            out.write(item.toArray(EMPTY));
        } catch (IOException ex) {
            close();
            future.completeExceptionally(ex);
        }
    }

    private void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        close();
        future.complete(path);
    }
}
