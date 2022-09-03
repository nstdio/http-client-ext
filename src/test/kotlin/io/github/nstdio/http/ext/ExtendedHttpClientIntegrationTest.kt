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

import io.github.nstdio.http.ext.Assertions.assertThat
import io.github.nstdio.http.ext.Assertions.awaitFor
import io.github.nstdio.http.ext.Compression.deflate
import io.github.nstdio.http.ext.Compression.gzip
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpResponse.BodyHandlers.discarding
import java.util.*
import java.util.concurrent.LinkedBlockingDeque

@MockWebServerTest
internal class ExtendedHttpClientIntegrationTest(private val mockWebServer: MockWebServer) {

  @Nested
  internal inner class TransparentDecompressionTest {
    @Test
    fun shouldTransparentlyDecompressAndCache() {
      //given
      val cache = Cache.newInMemoryCacheBuilder().build()
      val client: HttpClient = ExtendedHttpClient.newBuilder()
        .cache(cache)
        .transparentEncoding(true)
        .build()
      val expectedBody = Arb.string(16).next()
      val testUri = mockWebServer.url("/gzip").toUri()

      (0..1).forEach { _ ->
        mockWebServer.enqueue(
          MockResponse()
            .setResponseCode(200)
            .addHeader("Cache-Control", "max-age=86000")
            .addHeader("Content-Encoding", "gzip")
            .setBody(gzip(expectedBody))
        )
      }

      val request1 = HttpRequest.newBuilder(testUri)
        .build()
      val request2 = HttpRequest.newBuilder(testUri)
        .build()

      //when + then
      val r1 = client.send(request1, BodyHandlers.ofString())
      assertThat(r1)
        .isNetwork
        .hasStatusCode(200)
        .hasURI(request1.uri())
        .hasBody(expectedBody)
        .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
      awaitFor {
        val r2 = client.send(request2, BodyHandlers.ofString())
        assertThat(r2)
          .isCached
          .hasStatusCode(200)
          .hasBody(expectedBody)
          .hasURI(request2.uri())
          .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
      }
    }

    @Test
    fun shouldTransparentlyDecompress() {
      //given
      val client: HttpClient = ExtendedHttpClient.newBuilder()
        .cache(Cache.noop())
        .transparentEncoding(true)
        .build()

      val expectedBody = Arb.string(16).next()
      val testUri = mockWebServer.url("/gzip").toUri()

      (0..1).forEach { _ ->
        mockWebServer.enqueue(
          MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Encoding", "gzip,deflate")
            .setBody(gzip(deflate(expectedBody)))
        )
      }

      val request = HttpRequest.newBuilder(testUri).build()

      //when
      val r1 = client.send(request, BodyHandlers.ofString())
      val r2 = client.sendAsync(request, BodyHandlers.ofString()).join()

      //then
      assertThat(r1)
        .isNetwork
        .hasStatusCode(200)
        .hasBody(expectedBody)
        .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
      assertThat(r2)
        .isNetwork
        .hasStatusCode(200)
        .hasBody(expectedBody)
        .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
    }
  }

  @Nested
  internal inner class DefaultHeadersTest {
    @Test
    fun `Should add default headers`() {
      //given
      val client: HttpClient = ExtendedHttpClient.newBuilder()
        .defaultHeader("X-Testing-Value", "1")
        .defaultHeader("X-Testing-Supplier") { "2" }
        .build()

      val request = HttpRequest.newBuilder(mockWebServer.url("/test").toUri()).build()
      mockWebServer.enqueue(MockResponse().setResponseCode(200))

      //when
      val response = client.send(request, discarding())

      //then
      response.request().headers().map().should {
        it.shouldContain("X-Testing-Value", listOf("1"))
        it.shouldContain("X-Testing-Supplier", listOf("2"))
      }

      val actualRequest = mockWebServer.takeRequest()
      actualRequest.headers.should {
        it["X-Testing-Value"]
          .shouldNotBeNull()
          .shouldBe("1")

        it["X-Testing-Supplier"]
          .shouldNotBeNull()
          .shouldBe("2")
      }
    }

    @Test
    fun `Should resolve supplier by each call`() {
      //given
      val requestIds = (0..3).map { UUID.randomUUID().toString() }
      val queue = LinkedBlockingDeque(requestIds)

      val headerName = "X-Testing-Supplier-Resolved"
      val client: HttpClient = ExtendedHttpClient.newBuilder()
        .defaultHeader(headerName) { queue.pop() }
        .build()

      val request = HttpRequest.newBuilder(mockWebServer.url("/test").toUri()).build()
      mockWebServer.enqueue(MockResponse().setResponseCode(200), requestIds.size)

      //when
      val headerValues = requestIds
        .map { client.send(request, discarding()) }
        .map { mockWebServer.takeRequest() }
        .map { it.headers[headerName] }

      //then
      headerValues.shouldContainExactly(requestIds)
    }
  }
}