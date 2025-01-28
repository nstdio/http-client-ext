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
package io.github.nstdio.http.ext.spi

import com.tngtech.archunit.thirdparty.com.google.common.reflect.TypeToken
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8

internal interface JsonMappingContract {
  fun get(): JsonMapping

  @Test
  fun shouldThrowIOExceptionWhenParsingException() {
    //given
    val bytes = "{".toByteArray()
    val mapping = get()

    //when
    shouldThrow<IOException> { mapping.read(bytes, Any::class.java) }
  }

  @Test
  fun shouldThrowIOExceptionWhenParsingExceptionWithComplexType() {
    //given
    val bytes = ByteArrayInputStream("{".toByteArray())
    val mapping = get()
    val targetType = object : TypeToken<List<String>>() {}.type

    //when
    shouldThrow<IOException> { mapping.read(bytes, targetType) }
  }

  @Test
  fun shouldCloseInputStream() {
    //given
    val jsonBytes = "{}".toByteArray()
    val inSpy = spy(TestInputStream(ByteArrayInputStream(jsonBytes)))
    val mapping = get()

    //when
    mapping.read(inSpy, Any::class.java)

    //then
    verify(inSpy, atLeastOnce()).close()
  }

  @Test
  fun shouldReadFromByteArray() {
    //given
    val jsonBytes = "{}".toByteArray()
    val mapping = get()

    //when
    val read = mapping.read(jsonBytes, Any::class.java)

    //then
    read.shouldNotBeNull()
  }

  @Test
  fun shouldReadFromByteArrayUsingComplexType() {
    //given
    val jsonBytes = "{\"a\": 1, \"b\": 2}".toByteArray()
    val mapping = get()
    val targetType = object : TypeToken<Map<String, Int>>() {}.type

    //when
    val read = mapping.read<Map<String, Int>>(jsonBytes, targetType)

    //then
    assertThat(read)
      .hasSize(2)
      .containsEntry("a", 1)
      .containsEntry("b", 2)
  }

  @Test
  fun shouldReadFromInputStreamUsingComplexType() {
    //given
    val jsonBytes = ByteArrayInputStream("{\"a\": 1, \"b\": 2}".toByteArray())
    val mapping = get()
    val targetType = object : TypeToken<Map<String, Int>>() {}.type

    //when
    val read = mapping.read<Map<String, Int>>(jsonBytes, targetType)

    //then
    assertThat(read)
      .hasSize(2)
      .containsEntry("a", 1)
      .containsEntry("b", 2)
  }

  @Test
  fun `Should write object as JSON`() {
    //given
    val obj = mapOf("a" to 1, "b" to 2)
    val mapping = get()
    val out = ByteArrayOutputStream()

    //when
    mapping.write(obj, out)

    //then
    out.toString(UTF_8).shouldBeJsonObject()
  }

  open class TestInputStream(private val inputStream: InputStream) : InputStream() {
    override fun read(): Int = inputStream.read()
    override fun close() = inputStream.close()
  }
}