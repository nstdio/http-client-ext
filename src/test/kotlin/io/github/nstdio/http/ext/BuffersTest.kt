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
package io.github.nstdio.http.ext

import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.util.stream.Stream

internal class BuffersTest {
  @ParameterizedTest
  @MethodSource("listBuffersData")
  fun shouldDuplicatedBufferList(buffers: List<ByteBuffer?>) {
    //when
    val actual = Buffers.duplicate(buffers)

    //then
    assertThat(actual)
      .isNotSameAs(buffers)
      .hasSameSizeAs(buffers)
      .allMatch({ obj: ByteBuffer -> obj.isReadOnly }, "Expecting duplicated buffet to be read-only")
      .containsExactlyElementsOf(buffers)
  }

  @Test
  fun shouldDuplicatedSingleBuffer() {
    //given
    val buffer = Arb.byteArray(16).map { it.toBuffer() }.next()

    //when
    val actual = Buffers.duplicate(buffer)
    buffer.get()

    //then
    assertThat(actual.position()).isZero
  }

  @Test
  fun shouldNotCreateNewBufferIfInputIsEmpty() {
    //given
    val buffer = ByteBuffer.allocate(0)

    //when
    val actual = Buffers.duplicate(buffer)

    //then
    actual.shouldBeSameInstanceAs(buffer)   
  }

  companion object {
    @JvmStatic
    fun listBuffersData(): Stream<List<ByteBuffer>> {
      return Stream.of(
        listOf(),
        listOf("abcde".repeat(8).toByteBuffer()),
        listOf(
          "ab".repeat(8).toByteBuffer(),
          "cd".repeat(8).toByteBuffer()
        ),
        listOf(
          "ab".repeat(8).toByteBuffer(),
          "cd".repeat(8).toByteBuffer(),
          "ef".repeat(8).toByteBuffer()
        ),
        Arb.byteArray(96).next().toChunkedBuffers(true)
      )
    }
  }
}