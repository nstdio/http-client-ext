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

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIOException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.function.Consumer
import java.util.stream.IntStream
import java.util.stream.Stream

internal class ByteBufferInputStreamTest {
  @ParameterizedTest
  @MethodSource("fullReadingData")
  @Throws(IOException::class)
  fun fullReading(bytes: ByteArray) {
    //given
    val `is` = ByteBufferInputStream()
    Helpers.toBuffers(bytes, false).forEach(Consumer { b: ByteBuffer -> `is`.add(b) })

    //when
    val actual = IOUtils.toByteArray(`is`)

    //then
    Assertions.assertArrayEquals(bytes, actual)
  }

  @ParameterizedTest
  @MethodSource("fullReadingData")
  @Throws(IOException::class)
  fun shouldReadAllBytes(bytes: ByteArray?) {
    //given
    val `is` = ByteBufferInputStream()
    Helpers.toBuffers(bytes, false).forEach(Consumer { b: ByteBuffer? -> `is`.add(b) })

    //when
    val actual = `is`.readAllBytes()

    //then
    Assertions.assertArrayEquals(bytes, actual)
  }

  @Test
  @Throws(IOException::class)
  fun shouldReturnNegativeWhenInputIsEmpty() {
    //given
    val `is` = ByteBufferInputStream()
    val bytes = RandomUtils.nextBytes(8)
    `is`.add(ByteBuffer.wrap(bytes))

    //when
    val actual = `is`.read(ByteArray(0))

    //then
    assertEquals(0, actual)
  }

  @RepeatedTest(4)
  @Throws(IOException::class)
  fun shouldReadSingleProperly() {
    //given
    val `is` = ByteBufferInputStream()
    val randomString = RandomStringUtils.randomAlphabetic(64)
    Helpers.toBuffers(randomString.toByteArray(), false).forEach(Consumer { b: ByteBuffer? -> `is`.add(b) })
    val out = ByteArrayOutputStream()

    //when
    var read: Int
    while (`is`.read().also { read = it } != -1) {
      out.write(read)
    }

    //then
    assertEquals(-1, `is`.read())
    assertEquals(randomString, out.toString())
  }

  @Test
  @Throws(IOException::class)
  fun shouldFlipBuffer() {
    //given
    val bytes = RandomUtils.nextBytes(32)
    val `is` = ByteBufferInputStream()
    Helpers.toBuffers(bytes, false)
      .stream()
      .map { buffer: ByteBuffer -> buffer.position(buffer.limit()) }
      .forEach { b: ByteBuffer? -> `is`.add(b) }
    `is`.add(ByteBuffer.wrap(ByteArray(0)))

    //when
    val actual = `is`.readAllBytes()

    //then
    Assertions.assertArrayEquals(bytes, actual)
  }

  @Test
  fun shouldThrowWhenClosed() {
    //given
    val s = ByteBufferInputStream()

    //when
    s.close()

    //then
    assertThatIOException().isThrownBy { s.read() }
    assertThatIOException().isThrownBy { s.read(ByteArray(0)) }
    assertThatIOException().isThrownBy { s.read(ByteArray(5), 0, 5) }
    assertThatIOException().isThrownBy { s.readAllBytes() }
    assertThatIOException().isThrownBy { s.available() }
  }

  @Test
  @Throws(Exception::class)
  fun shouldReportAvailable() {
    //given
    val bytes = RandomUtils.nextBytes(32)
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes, false).forEach(Consumer { b: ByteBuffer? -> s.add(b) })

    //when
    val actual = s.available()

    //then
    assertEquals(bytes.size, actual)
  }

  @Test
  @Throws(Exception::class)
  fun shouldReadAllBytes() {
    //given
    val bytes = RandomUtils.nextBytes(32)
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes, false).forEach(Consumer { b: ByteBuffer? -> s.add(b) })

    //when
    val actual = s.readAllBytes()

    //then
    Assertions.assertArrayEquals(bytes, actual)
  }

  @Test
  fun shouldThrowWhenRequestedBytesNegative() {
    //given
    val `is` = ByteBufferInputStream()

    //when + then
    assertThrows(IllegalArgumentException::class.java) { `is`.readNBytes(-1) }
  }

  @Test
  @Throws(IOException::class)
  fun shouldReadUpToNBytes() {
    //given
    val count = 16
    val bytes = RandomUtils.nextBytes(count)
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes, false).forEach(Consumer { b: ByteBuffer? -> s.add(b) })

    //when
    val actual = s.readNBytes(count + 1)

    //then
    Assertions.assertArrayEquals(bytes, actual)
  }

  @Test
  fun shouldSupportMark() {
    //given + when + then
    ByteBufferInputStream().markSupported().shouldBeTrue()
  }

  @Test
  @Throws(IOException::class)
  fun shouldDumpBuffersToList() {
    //given
    val s = ByteBufferInputStream()
    val buffers = Helpers.toBuffers(RandomUtils.nextBytes(96), false)
    buffers.forEach(Consumer { b: ByteBuffer? -> s.add(b) })

    //when
    val actual = s.drainToList()

    //then
    assertEquals(-1, s.read())
    assertThat(actual)
      .hasSameSizeAs(buffers)
      .containsExactlyElementsOf(buffers)
  }

  @Test
  fun `Should not add when closed`() {
    //given
    val s = ByteBufferInputStream().also { it.close() }

    //when
    s.add(ByteBuffer.wrap(RandomUtils.nextBytes(96)))

    //then
    s.drainToList().shouldBeEmpty()
  }

  @Test
  fun `Should throw when reset called but not marked`() {
    //given
    val s = ByteBufferInputStream()

    //when + then
    shouldThrowExactly<IOException> { s.reset() }
      .shouldHaveMessage("nothing to reset")
  }

  @Test
  fun `Should restore marked on reset`() {
    //given
    val bytes = "abcd".toByteArray()
    val s = ByteBufferInputStream().also { it.add(ByteBuffer.wrap(bytes)) }

    //when
    s.mark(2)
    s.read(); s.read()
    s.reset()

    //then
    s.read().shouldBe(bytes[0])
    s.read().shouldBe(bytes[1])
  }

  @Test
  fun `Should drop mark when read limit exceeds`() {
    //given
    val bytes = "abcd".toByteArray()
    val s = ByteBufferInputStream().also { it.add(ByteBuffer.wrap(bytes)) }

    //when
    s.mark(1)
    s.read(); s.read()

    //then
    s.read().shouldBe(bytes[2])
    s.read().shouldBe(bytes[3])
  }

  @Test
  fun `Should drop mark when limit is negative`() {
    //given
    val bytes = "abcd".toByteArray()
    val s = ByteBufferInputStream().also { it.add(ByteBuffer.wrap(bytes)) }

    //when
    s.mark(1)
    s.mark(-1)

    //then
    s.read().shouldBe(bytes[0])
    shouldThrowExactly<IOException> { s.reset() }
      .shouldHaveMessage("nothing to reset")
  }

  companion object {
    @JvmStatic
    fun fullReadingData(): Stream<Named<ByteArray?>> {
      return IntStream.of(100, 8192, 16384, 65536, 131072)
        .mapToObj { n: Int ->
          Named.named(
            "Size: $n", RandomUtils.nextBytes(n)
          )
        }
    }
  }
}