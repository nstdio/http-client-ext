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
import io.github.nstdio.http.ext.jupiter.EnabledIfOnClasspath
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class JsonMappingProviderSpiTest {
  @Test
  fun shouldGetDefaultProviderByName() {
    assumeTrue { ALL_JSON.any { Classpath.isPresent(it) } }

    //given
    val providerName = CompositeJsonMappingProvider::class.java.name

    //when
    val provider = JsonMappingProvider.provider(providerName)

    //then
    provider.shouldBeTypeOf<CompositeJsonMappingProvider>()
  }

  @Nested
  @EnabledIfOnClasspath(GSON)
  @DisabledIfOnClasspath(JACKSON)
  internal inner class GsonPresentJacksonMissingTest {
    @Test
    fun shouldLoadGsonIfJacksonMissing() {
      //when
      val provider = JsonMappingProvider.provider()
      val jsonMapping = provider.get()

      //then
      provider.shouldBeTypeOf<CompositeJsonMappingProvider>()
      jsonMapping.shouldBeTypeOf<GsonJsonMapping>()
    }
  }

  @Nested
  @EnabledIfOnClasspath(JACKSON)
  @DisabledIfOnClasspath(GSON)
  internal inner class JacksonPresentGsonMissingTest {
    @Test
    fun shouldLoadDefaultJackson() {
      //when
      val provider = JsonMappingProvider.provider()
      val jsonMapping = provider.get()

      //then
      provider.shouldBeTypeOf<CompositeJsonMappingProvider>()
      jsonMapping.shouldBeTypeOf<JacksonJsonMapping>()
    }
  }

  @Nested
  @DisabledIfOnClasspath(JACKSON, GSON)
  internal inner class AllMissingTest {
    @Test
    fun shouldThrowWhenNothingIsPresent() {
      //when + then
      shouldThrowExactly<JsonMappingProviderNotFoundException> { JsonMappingProvider.provider() }
    }

    @Test
    fun shouldThrowWhenNothingIsPresentAndRequestedByName() {
      //given
      val providerName = CompositeJsonMappingProvider::class.java.name

      //when + then
      shouldThrowExactly<JsonMappingProviderNotFoundException> { JsonMappingProvider.provider(providerName) }
    }
  }

  @Nested
  @EnabledIfOnClasspath(JACKSON, GSON)
  internal inner class AllPresentTest {
    @Test
    fun shouldLoadDefaultJackson() {
      //when
      val provider = JsonMappingProvider.provider()
      val jsonMapping = provider.get()

      //then
      provider.shouldBeTypeOf<CompositeJsonMappingProvider>()
      jsonMapping.shouldBeTypeOf<JacksonJsonMapping>()
    }
  }
}