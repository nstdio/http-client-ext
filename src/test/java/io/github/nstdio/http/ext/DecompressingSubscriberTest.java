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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.net.http.HttpResponse.BodySubscribers.ofByteArray;
import static java.net.http.HttpResponse.BodySubscribers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DecompressingSubscriberTest {
  static Stream<Named<String>> randomLargeStrings() {
    return IntStream.rangeClosed(0, 100)
        .mapToObj(value -> {
          String s = Helpers.randomString(8192, 40960);

          return Named.of("Length: " + s.length(), s);
        });
  }

  @ParameterizedTest
  @MethodSource("randomLargeStrings")
  void shouldDecompressLargeBodies(String bodyString) {
    //given
    byte[] body = bodyString.getBytes(UTF_8);
    byte[] gzip = Compression.gzip(bodyString);

    var subscriber = new DecompressingSubscriber<>(ofByteArray());
    var sub = new PlainSubscription(subscriber, Helpers.toBuffers(gzip, true));

    //when
    subscriber.onSubscribe(sub);
    sub.cancel();

    byte[] actual = subscriber.getBody().toCompletableFuture().join();

    //then
    assertArrayEquals(body, actual);
  }

  @ParameterizedTest
  @ValueSource(ints = {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 32, 64, 128
  })
  void shouldDecompressWithVerySmallChunks(int len) {
    //given
    String body = RandomStringUtils.randomAlphabetic(len);
    byte[] gzip = Compression.gzip(body);
    var subscriber = new DecompressingSubscriber<>(ofString(UTF_8));
    var buffers = new ArrayList<ByteBuffer>();
    for (byte b : gzip) buffers.add(ByteBuffer.wrap(new byte[]{b}));
    var sub = new PlainSubscription(subscriber, buffers);

    //when
    subscriber.onSubscribe(sub);
    sub.cancel();

    String actual = subscriber.getBody().toCompletableFuture().join();

    //then
    assertEquals(body, actual);
  }

  @ParameterizedTest
  @ValueSource(ints = {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 32, 64, 128
  })
  void shouldNotDamageBodyIfNotCompressed(int len) {
    //given
    String body = RandomStringUtils.randomAlphabetic(len);
    var subscriber = new DecompressingSubscriber<>(ofString(UTF_8));
    var buffers = new ArrayList<ByteBuffer>();
    for (byte b : body.getBytes(UTF_8)) buffers.add(ByteBuffer.wrap(new byte[]{b}));
    var sub = new PlainSubscription(subscriber, buffers);

    //when
    subscriber.onSubscribe(sub);
    sub.cancel();

    String actual = subscriber.getBody().toCompletableFuture().join();

    //then
    assertEquals(body, actual);
  }

  @Test
  void shouldReportErrorToDownStreamWhenErrorOccures() {
    //given
    String body = RandomStringUtils.randomAlphabetic(32);
    byte[] gzip = Compression.gzip(body);

    for (int len = gzip.length, mid = len / 2, i = mid; i < Math.min(mid + 20, len); i++)
      gzip[i] = (byte) RandomUtils.nextInt();

    var subscriber = new DecompressingSubscriber<>(ofString(UTF_8));
    var sub = new PlainSubscription(subscriber, Helpers.toBuffers(gzip, false));

    //when
    subscriber.onSubscribe(sub);
    sub.cancel();

    CompletableFuture<String> actual = subscriber.getBody().toCompletableFuture();

    //then
    assertThat(actual).isCompletedExceptionally();
  }
}