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
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.extension.ExtendWith
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.ResponseInfo

@ExtendWith(MockWebServerExtension::class)
internal class DecompressingSubscriberIntegrationTest(private val mockWebServer: MockWebServer) {
  private val client = HttpClient.newHttpClient()

  @RepeatedTest(8)
  fun shouldDecompressLargeBodyWithStringHandler() {
    //given
    val data = setupLargeBodyDecompressionTest()
    val request = data.request
    val expectedBody = data.expectedBody

    //when
    val stringResponse = client.send(request) { info: ResponseInfo? ->
      DecompressingSubscriber(
        HttpResponse.BodyHandlers.ofByteArray().apply(info)
      )
    }

    //then
    assertThat(stringResponse).hasBody(expectedBody)
  }

  private fun setupLargeBodyDecompressionTest(body: ByteArray = Arb.byteArray(16384 * 10).next()): LargeBodyDataDecompression {
    val gzippedBody = Compression.gzip(body)
    val testUrl = "/gzip"
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(gzippedBody)
    )

    val uri = mockWebServer.url(testUrl).toUri()
    val request = HttpRequest.newBuilder(uri).build()
    return LargeBodyDataDecompression(request, body)
  }

  internal data class LargeBodyDataDecompression(val request: HttpRequest, val expectedBody: ByteArray)
}