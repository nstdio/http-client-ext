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
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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
      val r1 = client.send(request1, HttpResponse.BodyHandlers.ofString())
      assertThat(r1)
        .isNetwork
        .hasStatusCode(200)
        .hasURI(request1.uri())
        .hasBody(expectedBody)
        .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
      awaitFor {
        val r2 = client.send(request2, HttpResponse.BodyHandlers.ofString())
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
      val r1 = client.send(request, HttpResponse.BodyHandlers.ofString())
      val r2 = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join()

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
}