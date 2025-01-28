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

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE

class SimpleStreamFactoryTest {
  private val anyPath = Path.of("any")

  @TempDir
  private lateinit var tempDir: Path

  @Test
  fun `Should not allow read option on write method`() {
    //given
    val factory = SimpleStreamFactory()

    //when + then
    shouldThrowExactly<IllegalArgumentException> {
      factory.writable(anyPath, READ, WRITE)
    }.shouldHaveMessage("READ not allowed")
  }

  @ParameterizedTest
  @ValueSource(strings = ["WRITE", "APPEND"])
  fun `Should not allow write option on read method`(option: StandardOpenOption) {
    //given
    val factory = SimpleStreamFactory()

    //when + then
    shouldThrowExactly<IllegalArgumentException> {
      factory.readable(anyPath, READ, option)
    }.shouldHaveMessage("$option not allowed")
  }

  @Test
  fun `Should create channel`() {
    //given
    val file = tempDir.resolve("temp")
    Files.write(file, listOf("a"), StandardOpenOption.CREATE)

    val factory = SimpleStreamFactory()

    //when
    val channel = factory.readable(file)

    //then
    channel.read(ByteBuffer.allocate(1)) shouldBe 1
  }
}