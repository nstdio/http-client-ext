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

import io.kotest.assertions.json.shouldBeValidJson
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import okio.Buffer
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString

@ExtendWith(MockWebServerExtension::class)
internal class DecompressingSubscriberSpiTest(private val mockWebServer: MockWebServer) {
  private val client = HttpClient.newHttpClient()

  @ParameterizedTest
  @ValueSource(strings = ["br", "gzip", "deflate"])
  fun shouldDecompress(compressionType: String) {
    //given
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Encoding", compressionType)
        .setBodyResource("/__files/$compressionType")
    )
    val uri = mockWebServer.url("/data").toUri()

    //when
    val response = client.send(
      HttpRequest.newBuilder(uri).build(), BodyHandlers.ofDecompressing(ofString())
    )

    //then
    response.body().shouldBeValidJson()
  }

  private fun MockResponse.setBodyResource(resName: String): MockResponse {
    val stream = DecompressingSubscriberSpiTest::class.java.getResourceAsStream(resName)
    stream?.let { it -> it.use { setBody(Buffer().readFrom(it)) } }
    return this
  }
}