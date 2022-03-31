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

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.net.http.HttpResponse.BodySubscribers.ofByteArray
import java.net.http.HttpResponse.BodySubscribers.ofString
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.util.stream.IntStream
import java.util.stream.Stream

internal class DecompressingSubscriberTest {
    @ParameterizedTest
    @MethodSource("randomLargeStrings")
    fun shouldDecompressLargeBodies(bodyString: String) {
        //given
        val body = bodyString.toByteArray(UTF_8)
        val gzip = Compression.gzip(bodyString)
        val subscriber = DecompressingSubscriber(ofByteArray())
        val sub = PlainSubscription(subscriber, Helpers.toBuffers(gzip, true))

        //when
        subscriber.onSubscribe(sub)
        sub.cancel()
        val actual = subscriber.body.toCompletableFuture().join()

        //then
        assertArrayEquals(body, actual)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 32, 64, 128])
    fun shouldDecompressWithVerySmallChunks(len: Int) {
        //given
        val body = RandomStringUtils.randomAlphabetic(len)
        val gzip = Compression.gzip(body)
        val subscriber = DecompressingSubscriber(ofString(UTF_8))
        val buffers = ArrayList<ByteBuffer>()
        for (b in gzip) buffers.add(ByteBuffer.wrap(byteArrayOf(b)))
        val sub = PlainSubscription(subscriber, buffers)

        //when
        subscriber.onSubscribe(sub)
        sub.cancel()
        val actual = subscriber.body.toCompletableFuture().join()

        //then
        Assertions.assertEquals(body, actual)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 32, 64, 128])
    fun shouldNotDamageBodyIfNotCompressed(len: Int) {
        //given
        val body = RandomStringUtils.randomAlphabetic(len)
        val subscriber = DecompressingSubscriber(ofString(UTF_8))
        val buffers = ArrayList<ByteBuffer>()
        for (b in body.toByteArray(UTF_8)) buffers.add(ByteBuffer.wrap(byteArrayOf(b)))
        val sub = PlainSubscription(subscriber, buffers)

        //when
        subscriber.onSubscribe(sub)
        sub.cancel()
        val actual = subscriber.body.toCompletableFuture().join()

        //then
        Assertions.assertEquals(body, actual)
    }

    @Test
    fun shouldReportErrorToDownStreamWhenErrorOccures() {
        //given
        val body = RandomStringUtils.randomAlphabetic(32)
        val gzip = Compression.gzip(body)
        val len = gzip.size
        val mid = len / 2
        var i = mid
        while (i < (mid + 20).coerceAtMost(len)) {
            gzip[i] = RandomUtils.nextInt().toByte()
            i++
        }
        val subscriber = DecompressingSubscriber(ofString(UTF_8))
        val sub = PlainSubscription(subscriber, Helpers.toBuffers(gzip, false))

        //when
        subscriber.onSubscribe(sub)
        sub.cancel()
        val actual = subscriber.body.toCompletableFuture()

        //then
        assertThat(actual).isCompletedExceptionally
    }

    companion object {
        @JvmStatic
        fun randomLargeStrings(): Stream<Named<String>> {
            return IntStream.rangeClosed(0, 100)
                .mapToObj {
                    val s = Helpers.randomString(8192, 40960)
                    Named.of("Length: " + s.length, s)
                }
        }
    }
}