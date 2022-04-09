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

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import org.junit.jupiter.api.AfterEach
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

  @Test
  fun `Should remove provider`() {
    //given
    val mockProvider = mock(JsonMappingProvider::class.java)
    val providerName = mockProvider.javaClass.name

    //when
    JsonMappingProvider.addProvider(mockProvider)
    JsonMappingProvider.removeProvider(providerName)

    //then
    JsonMappingProvider.provider().shouldNotBeSameInstanceAs(mockProvider)
    shouldThrowExactly<JsonMappingProviderNotFoundException> {
      JsonMappingProvider.provider(providerName).shouldNotBeSameInstanceAs(mockProvider)
    }
  }
}