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

import io.github.nstdio.http.ext.Assertions.assertThat
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HttpHeadersBuilderTest {
  private lateinit var builder: HttpHeadersBuilder

  @BeforeEach
  fun setUp() {
    builder = HttpHeadersBuilder()
  }

  @Test
  fun shouldSetHeader() {
    //when
    builder.add("abc", "1")
    builder.set("abc", "2")

    //then
    assertThat(builder.build())
      .hasHeaderWithOnlyValue("abc", "2")
  }

  @Test
  fun `Should remove header single value`() {
    //when
    builder.add("abc", "1")
    builder.add("abc", "2")
    builder.remove("abc", "1")

    //then
    assertThat(builder.build())
      .hasHeaderWithOnlyValue("abc", "2")
  }

  @Test
  fun `Should safely remove not existing header`() {
    //when
    builder.remove("abc", "1")

    //then
    assertThat(builder.build())
      .isEmpty()
  }

  @Test
  fun `Should remove all entry if all removed single`() {
    //when
    builder.add("abc", "1")
    builder.remove("abc", "1")

    //then
    assertThat(builder.build())
      .isEmpty()
  }

  @Test
  fun `Should remove all entry if all removed`() {
    //when
    builder.add("abc", "1")
    builder.add("abc", "2")
    builder.add("abcd", "2")
    builder.remove("abc")

    //then
    assertThat(builder.build())
      .hasNoHeader("abc")
      .hasHeaderWithOnlyValue("abcd", "2")
  }

  @Test
  fun `Should add bulk`() {
    //when
    builder.add("abc", listOf("1", "2"))

    //then
    assertThat(builder.build())
      .hasHeaderWithValues("abc", "1", "2")
  }

  @Test
  fun `Should not add empty list`() {
    //when
    builder.add("abc", listOf())

    //then
    assertThat(builder.build())
      .isEmpty()
  }

  @Test
  fun `Should set bulk list`() {
    //when
    builder.add("abc", "1")
    builder.set("abc", listOf("2", "3"))

    //then
    assertThat(builder.build())
      .hasHeaderWithValues("abc", "2", "3")
  }

  @Test
  fun `Should not set bulk list empty`() {
    //when
    builder.add("abc", "1")
    builder.set("abc", listOf())

    //then
    assertThat(builder.build())
      .isEmpty()
  }

  @Test
  fun `Should not create new empty instances`() {
    //when
    val h1 = builder.build()
    val h2 = builder.build()

    //then
    h1 shouldBeSameInstanceAs h2
    h1.map().shouldBeEmpty()
  }
  
  @Test
  fun `Should have proper toString`() {
    //when
   builder
      .add("b", listOf("3", "4"))
      .add("a", listOf("1", "2"))
    
    //then
    builder.toString().should {
      it shouldStartWith "${HttpHeadersBuilder::class.qualifiedName}@"
      it shouldEndWith "{ {a=[1, 2], b=[3, 4]} }"
    }
  }
}