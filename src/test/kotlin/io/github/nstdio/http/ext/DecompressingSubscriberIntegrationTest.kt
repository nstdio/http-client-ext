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
package io.github.nstdio.http.ext

import io.github.nstdio.http.ext.Assertions.assertThat
import io.kotest.assertions.json.shouldBeValidJson
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import okio.Buffer
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofInputStream
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.net.http.HttpResponse.ResponseInfo
import kotlin.text.Charsets.UTF_8

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

  @ParameterizedTest
  @MethodSource("compressionTypes")
  fun shouldDecompress(compressionType: String) {
    //given
    val uri = enqueueAndGetUri(compressionType)

    //when
    val response = client.send(
      HttpRequest.newBuilder(uri).build(), BodyHandlers.ofDecompressing(ofString())
    )

    //then
    response.body().shouldBeValidJson()
  }

  @ParameterizedTest
  @MethodSource("compressionTypes")
  fun shouldDecompressInputStream(compressionType: String) {
    //given
    val uri = enqueueAndGetUri(compressionType)

    //when
    val response = client.send(
      HttpRequest.newBuilder(uri).build(), BodyHandlers.ofDecompressing()
    )
    val body = response.body().use { it.readAllBytes().toString(UTF_8) }

    //then
    body.shouldBeValidJson()
  }

  @ParameterizedTest
  @MethodSource("compressionTypes")
  fun shouldDecompressInputStreamExplicit(compressionType: String) {
    //given
    val uri = enqueueAndGetUri(compressionType)

    //when
    val response = client.send(HttpRequest.newBuilder(uri).build(), BodyHandlers.ofDecompressing(ofInputStream()))
    val body = response.body().use { it.readAllBytes().toString(UTF_8) }

    //then
    body.shouldBeValidJson()
  }

  private fun enqueueAndGetUri(compressionType: String): URI {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Encoding", compressionType)
        .setBodyResource("/__files/$compressionType")
    )
    val uri = mockWebServer.url("/data").toUri()
    return uri
  }

  private fun MockResponse.setBodyResource(resName: String): MockResponse {
    val stream = DecompressingSubscriberIntegrationTest::class.java.getResourceAsStream(resName)
    stream?.let { it -> it.use { setBody(Buffer().readFrom(it)) } }
    return this
  }

  companion object {
    @JvmStatic
    fun compressionTypes() = arrayOf("br", "gzip", "deflate", "zstd")
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