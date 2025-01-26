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

import com.aayushatharva.brotli4j.Brotli4jLoader
import io.github.nstdio.http.ext.jupiter.FilteredClassLoaderTest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.assertj.core.api.Assertions.assertThat
import org.brotli.dec.BrotliInputStream
import org.junit.jupiter.api.Test

internal class OptionalBrotliCompressionFactoryTest {
  @FilteredClassLoaderTest(BrotliInputStream::class, Brotli4jLoader::class)
  fun shouldSupportNothing() {
    //given
    val factory = OptionalBrotliCompressionFactory()

    //when
    val actual = factory.supported()

    //then
    assertThat(actual).isEmpty()
  }

  @Test
  fun shouldSupportSomething() {
    //given
    val factory = OptionalBrotliCompressionFactory()

    //when
    val actual = factory.supported()

    //then
    actual.shouldContainExactly("br")
  }

  @FilteredClassLoaderTest(BrotliInputStream::class)
  fun `Should support if BrotliInputStream is missing`() {
    //given
    val factory = OptionalBrotliCompressionFactory()

    //when
    val actual = factory.supported()

    //then
    actual.shouldContainExactly("br")
  }

  @FilteredClassLoaderTest(Brotli4jLoader::class)
  fun `Should support if Brotli4jLoader is missing`() {
    //given
    val factory = OptionalBrotliCompressionFactory()

    //when
    val actual = factory.supported()

    //then
    actual.shouldContainExactly("br")
  }
}