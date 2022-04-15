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
package io.github.nstdio.http.ext.spi

import com.google.common.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

internal interface JsonMappingContract {
  fun get(): JsonMapping

  @Test
  fun shouldThrowIOExceptionWhenParsingException() {
    //given
    val bytes = "{".toByteArray(StandardCharsets.UTF_8)
    val mapping = get()

    //when
    assertThrows(IOException::class.java) { mapping.read(bytes, Any::class.java) }
  }

  @Test
  fun shouldThrowIOExceptionWhenParsingExceptionWithComplexType() {
    //given
    val bytes = ByteArrayInputStream("{".toByteArray(StandardCharsets.UTF_8))
    val mapping = get()
    val targetType = object : TypeToken<List<String?>?>() {}.type

    //when
    assertThrows(IOException::class.java) { mapping.read<Any>(bytes, targetType) }
  }

  @Test
    fun shouldCloseInputStream() {
    //given
    val jsonBytes = "{}".toByteArray(StandardCharsets.UTF_8)
    val inSpy = Mockito.spy(ByteArrayInputStream(jsonBytes))
    val mapping = get()

    //when
    val read = mapping.read(inSpy, Any::class.java)

    //then
    assertThat(read).isNotNull
    Mockito.verify(inSpy, Mockito.atLeastOnce()).close()
  }

  @Test
    fun shouldReadFromByteArray() {
    //given
    val jsonBytes = "{}".toByteArray(StandardCharsets.UTF_8)
    val mapping = get()

    //when
    val read = mapping.read(jsonBytes, Any::class.java)

    //then
    assertThat(read).isNotNull
  }

  @Test
    fun shouldReadFromByteArrayUsingComplexType() {
    //given
    val jsonBytes = "{\"a\": 1, \"b\": 2}".toByteArray(StandardCharsets.UTF_8)
    val mapping = get()
    val targetType = object : TypeToken<Map<String?, Int?>?>() {}.type

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
    val jsonBytes = ByteArrayInputStream("{\"a\": 1, \"b\": 2}".toByteArray(StandardCharsets.UTF_8))
    val mapping = get()
    val targetType = object : TypeToken<Map<String?, Int?>?>() {}.type

    //when
    val read = mapping.read<Map<String, Int>>(jsonBytes, targetType)

    //then
    assertThat(read)
      .hasSize(2)
      .containsEntry("a", 1)
      .containsEntry("b", 2)
  }
}