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

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.throwable.shouldHaveCause
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.UncheckedIOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest

internal class BodyHandlersTest {
  @Nested
  internal inner class OfJsonTest {
    private val client = HttpClient.newHttpClient()

    @Test
    fun shouldProperlyReadJson() {
      //given
      val request = HttpRequest.newBuilder(URI.create("${wm.baseUrl()}/get")).build()

      stubFor(
        get("/get").willReturn(
          ok().withBody(
            """
        {
          "args": {},
          "headers": {
            "Accept": "application/json",
            "Accept-Encoding": "gzip, deflate, br",
            "Accept-Language": "en-US,en;q=0.5",
            "Host": "httpbin.org",
            "Referer": "https://httpbin.org/",
            "Sec-Fetch-Dest": "empty",
            "Sec-Fetch-Mode": "cors",
            "Sec-Fetch-Site": "same-origin",
            "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:99.0) Gecko/20100101 Firefox/99.0",
            "X-Amzn-Trace-Id": "Root=1-6259c64f-002692993e9394f43151cfd7"
          },
          "origin": "37.252.83.253",
          "url": "https://httpbin.org/get"
        }
      """.trimIndent()
          )
        )
      )
      //when
      val body1 = client.sendAsync(request, BodyHandlers.ofJson(Any::class.java))
        .thenApply { it.body().get() }
        .join()

      //then
      body1.shouldNotBeNull()
    }

    @Test
    fun shouldThrowUncheckedExceptionIfCannotRead() {
      //given
      val request = HttpRequest.newBuilder(URI.create("${wm.baseUrl()}/get")).build()

      stubFor(get("/get").willReturn(ok().withBody("<html></html>")))

      //when
      shouldThrowExactly<UncheckedIOException> {
        client.send(request, BodyHandlers.ofJson(Any::class.java)).body().get()
      }.shouldHaveCause()
    }
  }

  companion object {
    @RegisterExtension
    @JvmStatic
    var wm = WireMockExtension.newInstance()
      .configureStaticDsl(true)
      .failOnUnmatchedRequests(true)
      .options(
        WireMockConfiguration.wireMockConfig().dynamicPort()
      )
      .build()
  }
}