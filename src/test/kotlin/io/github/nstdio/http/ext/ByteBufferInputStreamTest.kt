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
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
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
import java.util.stream.IntStream
import java.util.stream.Stream

internal class ByteBufferInputStreamTest {
  private val arbByteBuffer = Arb.byteBuffer(Arb.int(8..32))
  private val arbByteArray = Arb.byteArray(Arb.int(16, 48))

  @ParameterizedTest
  @MethodSource("fullReadingData")
  fun fullReading(bytes: ByteArray) {
    //given
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes).forEach(s::add)

    //when
    val available = s.available()
    val actual = s.readAllBytes()

    //then
    available shouldBe bytes.size
    actual.shouldBe(bytes)
  }

  @Test
  fun shouldReturnNegativeWhenInputIsEmpty() {
    //given
    val stream = ByteBufferInputStream()
    stream.add(arbByteBuffer.next())

    //when
    val actual = stream.read(ByteArray(0))

    //then
    actual.shouldBeZero()
  }

  @RepeatedTest(4)
  fun shouldReadSingleProperly() {
    //given
    val s = ByteBufferInputStream()
    val bytes = arbByteArray.next()
    Helpers.toBuffers(bytes).forEach(s::add)
    val out = ByteArrayOutputStream()

    //when
    var read: Int
    while (s.read().also { read = it } != -1) {
      out.write(read)
    }

    //then
    s.read().shouldBe(-1)
    out.toByteArray().shouldBe(bytes)
  }

  @Test
  fun shouldFlipBuffer() {
    //given
    val bytes = arbByteArray.next()
    val stream = ByteBufferInputStream()
    Helpers.toBuffers(bytes)
      .map { it.position(it.limit()) }
      .forEach(stream::add)

    stream.add(ByteArray(0).toBuffer())

    //when
    val actual = stream.readAllBytes()

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
  fun shouldReportAvailable() {
    //given
    val bytes = arbByteArray.next()
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes).forEach(s::add)

    //when
    val actual = s.available()

    //then
    assertEquals(bytes.size, actual)
  }

  @Test
  fun shouldReadAllBytes() {
    //given
    val bytes = arbByteArray.next()
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes).forEach(s::add)

    //when
    val actual = s.readAllBytes()

    //then
    actual.shouldBe(bytes)
  }

  @Test
  fun shouldThrowWhenRequestedBytesNegative() {
    //given
    val `is` = ByteBufferInputStream()

    //when + then
    assertThrows(IllegalArgumentException::class.java) { `is`.readNBytes(-1) }
  }

  @Test
  fun shouldReadUpToNBytes() {
    //given
    val bytes = arbByteArray.next()
    val count = bytes.size
    val s = ByteBufferInputStream()
    Helpers.toBuffers(bytes).forEach(s::add)

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
  fun shouldDumpBuffersToList() {
    //given
    val s = ByteBufferInputStream()
    val buffers = Helpers.toBuffers(arbByteArray.next())
    buffers.forEach(s::add)

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
    s.add(arbByteBuffer.next())

    //then
    s.drainToList().shouldBeEmpty()
  }

  @Test
  fun `Should throw when reset called but not marked`() {
    //given
    val s = ByteBufferInputStream()

    //when + then
    shouldThrowExactly<IOException>(s::reset)
      .shouldHaveMessage("nothing to reset")
  }

  @Test
  fun `Should restore marked on reset`() {
    //given
    val bytes = "abcd".toByteArray()
    val s = ByteBufferInputStream().also { it.add(bytes.toBuffer()) }

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
    val s = ByteBufferInputStream().also { it.add(bytes.toBuffer()) }

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
    val s = ByteBufferInputStream().also { it.add(bytes.toBuffer()) }

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
    fun fullReadingData(): Stream<Named<ByteArray>> {
      return IntStream.of(8192, 16384, 65536)
        .mapToObj { n: Int ->
          Named.named(
            "Size: $n", Arb.byteArray(n).next()
          )
        }
    }
  }
}