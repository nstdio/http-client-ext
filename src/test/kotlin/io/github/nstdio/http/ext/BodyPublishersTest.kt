/*
 * Copyright (C) 2022, 2025 Edgar Asatryan
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

import io.github.nstdio.http.ext.spi.Classpath
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.discarding
import java.nio.charset.StandardCharsets.UTF_8

@ExtendWith(MockWebServerExtension::class)
class BodyPublishersTest(private val mockWebServer: MockWebServer) {
  @Nested
  internal inner class OfJsonTest {
    @BeforeEach
    fun setUp() {
      assumeTrue { ALL_JSON.any { Classpath.isPresent(it) } }
    }

    @Test
    fun `Should publish body as JSON`() {
      //given
      val client = ExtendedHttpClient.newHttpClient()
      val body = mapOf("a" to 1, "b" to 2)
      val request = HttpRequest.newBuilder(mockWebServer.url("/test").toUri())
        .POST(BodyPublishers.ofJson(body))
        .build()

      mockWebServer.enqueue(MockResponse().setResponseCode(200))

      //when
      client.send(request, discarding())

      //then
      val actual = mockWebServer.takeRequest()

      actual.body.readString(UTF_8)
        .shouldBe("{\"a\":1,\"b\":2}")

      actual.headers["Content-Type"]
        .shouldNotBeNull()
        .shouldBe("application/json")
      actual.headers["Content-Length"]
        .shouldNotBeNull()
        .shouldBe("13")
    }
  }

}
