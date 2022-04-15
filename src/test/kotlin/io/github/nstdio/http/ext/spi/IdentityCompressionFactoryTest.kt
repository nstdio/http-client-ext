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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.io.InputStream

internal class IdentityCompressionFactoryTest {
  private val factory = IdentityCompressionFactory()

  @Test
  fun shouldSupportIdentityDirective() {
    //when
    val actual = factory.supported()

    //then
    assertThat(actual).containsOnly("identity")
  }

  @Test
    fun shouldReturnSameInputStream() {
    //given
    val inputStream = InputStream.nullInputStream()

    //when
    val actual = factory.decompressing(inputStream, "identity")

    //then
    assertThat(actual).isSameAs(inputStream)
  }

  @Test
  fun shouldThrowWhenTypeIsNotSupported() {
    //when + then
    assertThatIllegalArgumentException()
      .isThrownBy { factory.decompressing(InputStream.nullInputStream(), "abc") }
  }
}