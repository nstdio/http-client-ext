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

import io.github.nstdio.http.ext.ALL_JSON
import io.github.nstdio.http.ext.GSON
import io.github.nstdio.http.ext.JACKSON
import io.github.nstdio.http.ext.jupiter.DisabledIfOnClasspath
import io.github.nstdio.http.ext.spi.CompositeJsonMappingProvider.hasAnyImplementation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

internal class CompositeJsonMappingProviderSpiTest {
  @Test
  @DisabledIfOnClasspath(JACKSON, GSON)
  fun shouldThrowExceptionIfNothingFound() {
    //give
    val provider = CompositeJsonMappingProvider()

    //when + then
    assertThatExceptionOfType(JsonMappingProviderNotFoundException::class.java)
      .isThrownBy { provider.get() }

    assertThat(hasAnyImplementation())
      .withFailMessage {
        String.format(
          "CompositeJsonMappingProvider#hasAnyImplementation returned true, but none of classes on classpath: %s",
          ALL_JSON.contentToString()
        )
      }
      .isFalse
  }
}