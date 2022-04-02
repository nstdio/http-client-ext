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

import io.github.nstdio.http.ext.MetadataSerializer.findAvailable
import io.github.nstdio.http.ext.MetadataSerializer.requireAvailability
import io.github.nstdio.http.ext.jupiter.DisabledIfOnClasspath
import io.github.nstdio.http.ext.jupiter.EnabledIfOnClasspath
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class MetadataSerializerSpiTest {
  @Nested
  @EnabledIfOnClasspath(GSON)
  @DisabledIfOnClasspath(JACKSON)
  internal inner class GsonPresentJacksonMissing {
    @Test
    fun `Should not throw when gson present`() {
      requireAvailability()
    }

    @Test
    fun `Should return Gson serializer`() {
      //when
      val serializer = findAvailable(SimpleStreamFactory())

      //then
      assertThat(serializer).isInstanceOf(GsonMetadataSerializer::class.java)
    }
  }

  @Nested
  @EnabledIfOnClasspath(JACKSON)
  @DisabledIfOnClasspath(GSON)
  internal inner class JacksonPresentGsonMissing {
    @Test
    fun `Should not throw when gson present`() {
      requireAvailability()
    }

    @Test
    fun `Should return Jackson serializer`() {
      //when
      val serializer = findAvailable(SimpleStreamFactory())

      //then
      assertThat(serializer).isInstanceOf(JacksonMetadataSerializer::class.java)
    }
  }

  @Nested
  @DisabledIfOnClasspath(GSON, JACKSON)
  internal inner class AllMissing {
    @Test
    fun `Should throw nothing present`() {
      assertThatIllegalStateException()
        .isThrownBy { requireAvailability() }
    }

    @Test
    fun `Should return null`() {
      //when
      val serializer = findAvailable(SimpleStreamFactory())

      //then
      assertThat(serializer).isNull()
    }
  }
}