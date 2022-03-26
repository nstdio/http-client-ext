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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

class CachingBodySubscriberTest {

    @Test
    void shouldWriteAndReadFromEmptyFile(@TempDir Path dir) throws IOException {
        //given
        Path p1 = dir.resolve("p1");
        Path p2 = dir.resolve("p2");
        Files.createFile(p2);

        assertThat(p2).isEmptyFile();
        assertShouldReadAndWriteResponse(p1, p2);
    }

    @Test
    void shouldWriteAndReadFromNotEmptyFile(@TempDir Path dir) throws IOException {
        //given
        Path p1 = dir.resolve("p1");
        Path p2 = dir.resolve("p2");
        Files.write(p2, "random-stuff".repeat(300).getBytes(), CREATE, WRITE);

        assertThat(p2).isNotEmptyFile();
        assertShouldReadAndWriteResponse(p1, p2);
    }

    private void assertShouldReadAndWriteResponse(Path p1, Path p2) {
        //given
        String body = Helpers.randomString(32, 128);
        var original = HttpResponse.BodySubscribers.ofFile(p1);
        var other = new PathSubscriber(p2);
        Consumer<Path> finisher = s -> {
        };

        var subscriber = new CachingBodySubscriber<>(original, other, finisher);

        //when
        List<ByteBuffer> buffers = Helpers.toBuffers(body);
        var subscription = new PlainSubscription(subscriber, buffers, false);
        subscriber.onSubscribe(subscription);

        var actual1 = subscriber.getBody().toCompletableFuture().join();
        var actual2 = other.getBody().toCompletableFuture().join();

        //then
        assertThat(actual1).isEqualTo(p1);
        assertThat(actual2).isEqualTo(p2);

        assertThat(actual1).hasContent(body);
        assertThat(actual2).hasContent(body);
    }

    @Test
    void shouldInvokeDelegatesInOrder() {
        @SuppressWarnings("unchecked")
        BodySubscriber<String> original = mock(BodySubscriber.class);
        @SuppressWarnings("unchecked")
        BodySubscriber<String> other = mock(BodySubscriber.class);
        var subscriber = new CachingBodySubscriber<>(original, other, s -> {
        });

        //when
        subscriber.onSubscribe(null);
        subscriber.onNext(List.of());
        subscriber.onError(null);
        subscriber.onComplete();

        //then
        InOrder inOrder = inOrder(original, other);

        inOrder.verify(other).onSubscribe(any());
        inOrder.verify(original).onSubscribe(any());

        inOrder.verify(other).onNext(any());
        inOrder.verify(original).onNext(any());

        inOrder.verify(other).onError(any());
        inOrder.verify(original).onError(any());

        inOrder.verify(other).onComplete();
        inOrder.verify(original).onComplete();
    }
}