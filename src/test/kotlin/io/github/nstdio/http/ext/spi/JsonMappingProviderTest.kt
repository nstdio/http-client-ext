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

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import io.github.nstdio.http.ext.ALL_JSON
import io.github.nstdio.http.ext.jupiter.FilteredClassLoaderTest
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

internal class JsonMappingProviderTest {
  @AfterEach
  fun `tear down`() {
    JsonMappingProviders.clear()
  }

  @Test
  fun shouldThrowWhenProviderNotFound() {
    //given
    val providerName = "abc"

    //when + then
    shouldThrowExactly<JsonMappingProviderNotFoundException> { JsonMappingProvider.provider(providerName) }
      .shouldHaveMessage("JsonMappingProvider not found: $providerName")
  }

  @Test
  fun `Should add provider and get it`() {
    //given
    val mockProvider = mock(JsonMappingProvider::class.java)
    val providerName = mockProvider.javaClass.name

    //when
    JsonMappingProvider.addProvider(mockProvider)

    //then
    JsonMappingProvider.provider().shouldBeSameInstanceAs(mockProvider)
    JsonMappingProvider.provider(providerName).shouldBeSameInstanceAs(mockProvider)
  }

  @FilteredClassLoaderTest(ObjectMapper::class, Gson::class)
  fun `Should remove provider`() {
    //given
    val mockProvider = mock(JsonMappingProvider::class.java)
    val providerName = mockProvider.javaClass.name

    //when
    JsonMappingProvider.addProvider(mockProvider)
    JsonMappingProvider.removeProvider(providerName)

    //then
    shouldThrowExactly<JsonMappingProviderNotFoundException> {
      JsonMappingProvider.provider()
    }
    shouldThrowExactly<JsonMappingProviderNotFoundException> {
      JsonMappingProvider.provider(providerName)
    }
  }

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

  @FilteredClassLoaderTest(ObjectMapper::class)
  fun shouldLoadGsonIfJacksonMissing() {
    //when
    val provider = JsonMappingProvider.provider()
    val jsonMapping = provider.get()

    //then
    provider.shouldBeTypeOf<CompositeJsonMappingProvider>()
    jsonMapping.shouldBeTypeOf<GsonJsonMapping>()
  }

  @FilteredClassLoaderTest(Gson::class)
  fun shouldLoadJacksonIfGsonIsMissing() {
    //when
    val provider = JsonMappingProvider.provider()
    val jsonMapping = provider.get()

    //then
    provider.shouldBeTypeOf<CompositeJsonMappingProvider>()
    jsonMapping.shouldBeTypeOf<JacksonJsonMapping>()
  }

  @FilteredClassLoaderTest(ObjectMapper::class, Gson::class)
  fun shouldThrowWhenNothingIsPresent() {
    //when + then
    shouldThrowExactly<JsonMappingProviderNotFoundException> { JsonMappingProvider.provider() }
  }

  @FilteredClassLoaderTest(ObjectMapper::class, Gson::class)
  fun shouldThrowWhenNothingIsPresentAndRequestedByName() {
    //given
    val providerName = CompositeJsonMappingProvider::class.java.name

    //when + then
    shouldThrowExactly<JsonMappingProviderNotFoundException> { JsonMappingProvider.provider(providerName) }
  }

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