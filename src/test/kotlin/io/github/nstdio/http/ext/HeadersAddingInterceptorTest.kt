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

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.*
import java.util.function.Supplier

class HeadersAddingInterceptorTest {
  private val bodyHandler = BodyHandlers.discarding()

  @Test
  fun `Should add headers to the chain`() {
    //given
    val interceptor = HeadersAddingInterceptor(
      mapOf("a" to "4", "d" to "6"),
      mapOf("b" to Supplier { "5" }, "e" to Supplier { "7" }),
    )

    val request = HttpRequest.newBuilder("https://example.com".toUri())
      .header("a", "1")
      .header("b", "2")
      .header("c", "3")
      .build()
    val chain = Chain.of<Any>(RequestContext.of(request, bodyHandler))

    //when
    val newChain = interceptor.intercept(chain)

    //then
    newChain.request().headers().map().should {
      it.shouldContain("a", listOf("1", "4"))
      it.shouldContain("b", listOf("2", "5"))
      it.shouldContain("c", listOf("3"))
      it.shouldContain("d", listOf("6"))
      it.shouldContain("e", listOf("7"))
    }
  }

  @Test
  fun `Should not duplicate headers`() {
    //given
    val interceptor = HeadersAddingInterceptor(
      mapOf("a" to "1", "b" to "3"),
      mapOf("b" to Supplier { "2" }),
    )

    val request = HttpRequest.newBuilder("https://example.com".toUri())
      .header("a", "1")
      .header("b", "2")
      .header("b", "3")
      .build()
    val chain = Chain.of<Any>(RequestContext.of(request, bodyHandler))

    //when
    val actual = interceptor.intercept(chain).request().headers()

    //then
    actual.map().should { 
      it.shouldContain("a", listOf("1"))
      it.shouldContain("b", listOf("2", "3"))
    }
  }

  @Test
  fun `Should not add headers to the chain when response is present`() {
    //given
    val interceptor = HeadersAddingInterceptor(
      mapOf("a" to "1"),
      mapOf("b" to Supplier { "2" }),
    )

    val request = HttpRequest.newBuilder("https://example.com".toUri())
      .header("c", "3")
      .build()
    val response = StaticHttpResponse.builder<Any>()
      .request(request)
      .build()
    val chain = Chain.of(RequestContext.of(request, bodyHandler), { t, _ -> t }, Optional.of(response))

    //when
    val actual = interceptor.intercept(chain)

    //then
    actual.shouldBeSameInstanceAs(chain)
    actual.request().shouldBeSameInstanceAs(request)
  }

  @Test
  fun `Should alter chain when no headers to add`() {
    //given
    val interceptor = HeadersAddingInterceptor(mapOf(), mapOf())

    val request = HttpRequest.newBuilder("https://example.com".toUri())
      .header("a", "1")
      .build()
    val chain = Chain.of<Any>(RequestContext.of(request, bodyHandler))

    //when
    val actual = interceptor.intercept(chain)

    //then
    actual.shouldBeSameInstanceAs(chain)
    actual.request().shouldBeSameInstanceAs(request)
  }
}