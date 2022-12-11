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
package io.github.nstdio.http.ext

import io.kotest.matchers.future.shouldBeCompletedExceptionally
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.net.http.HttpResponse.BodySubscribers.ofByteArray
import java.net.http.HttpResponse.BodySubscribers.ofString
import java.nio.charset.StandardCharsets.UTF_8
import java.util.stream.IntStream
import java.util.stream.Stream

internal class DecompressingSubscriberTest {
  @ParameterizedTest
  @MethodSource("randomLargeByteArray")
  fun shouldDecompressLargeBodies(bytes: ByteArray) {
    //given
    val gzip = Compression.gzip(bytes)
    val subscriber = DecompressingSubscriber(ofByteArray())
    val sub = PlainSubscription(subscriber, gzip.toChunkedBuffers())

    //when
    subscriber.onSubscribe(sub)
    sub.cancel()
    val actual = subscriber.body.toCompletableFuture().join()

    //then
    actual.shouldBe(bytes)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 32, 64, 128])
  fun shouldDecompressWithVerySmallChunks(len: Int) {
    //given
    val body = Arb.string(len).next()
    val subscriber = DecompressingSubscriber(ofString(UTF_8))
    val buffers = Compression.gzip(body)
      .map { byteArrayOf(it).toBuffer() }
      .toMutableList()
    val sub = PlainSubscription(subscriber, buffers)

    //when
    subscriber.onSubscribe(sub)
    sub.cancel()
    val actual = subscriber.body.toCompletableFuture().join()

    //then
    actual.shouldBe(body)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 32, 64, 128])
  fun shouldNotDamageBodyIfNotCompressed(len: Int) {
    //given
    val body = Arb.string(len).next()
    val subscriber = DecompressingSubscriber(ofString(UTF_8))
    val buffers = body.toByteArray()
      .map { byteArrayOf(it).toBuffer() }
      .toMutableList()
    val sub = PlainSubscription(subscriber, buffers)

    //when
    subscriber.onSubscribe(sub)
    sub.cancel()
    val actual = subscriber.body.toCompletableFuture().join()

    //then
    actual.shouldBe(body)
  }

  @Test
  fun shouldReportErrorToDownStreamWhenErrorOccures() {
    //given
    val body = Arb.string(32).next()
    val gzip = Compression.gzip(body)
    val len = gzip.size
    val mid = len / 2
    var i = mid
    val byte = Arb.byte()
    while (i < (mid + 20).coerceAtMost(len)) {
      gzip[i] = byte.next()
      i++
    }
    val subscriber = DecompressingSubscriber(ofString(UTF_8))
    val sub = PlainSubscription(subscriber, gzip.toChunkedBuffers(false))

    //when
    subscriber.onSubscribe(sub)
    sub.cancel()
    val actual = subscriber.body.toCompletableFuture()

    //then
    actual.shouldBeCompletedExceptionally()
  }

  companion object {
    @JvmStatic
    fun randomLargeByteArray(): Stream<Named<ByteArray>> = IntStream.rangeClosed(0, 15)
      .mapToObj {
        val bytes = Arb.byteArray(Arb.int(8192, 20480)).next()

        Named.of("Size: ${bytes.size}", bytes)
      }
  }
}