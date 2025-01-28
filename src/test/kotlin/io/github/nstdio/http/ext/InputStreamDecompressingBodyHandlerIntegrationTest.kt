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
@file:Suppress("ArrayInDataClass")

package io.github.nstdio.http.ext

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets

@ExtendWith(MockWebServerExtension::class)
internal class InputStreamDecompressingBodyHandlerIntegrationTest(private val mockWebServer: MockWebServer) {
  private val httpClient = HttpClient.newHttpClient()

  @ParameterizedTest
  @MethodSource("compressedData")
  fun shouldCreate(compressedString: CompressedString) {
    //given
    val uri = mockWebServer.url("/data").toUri()
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Encoding", compressedString.compression)
        .setBody(compressedString.compressed)
    )

    val request = HttpRequest.newBuilder(uri)
      .build()

    //when
    val body = httpClient.send(request, BodyHandlers.ofDecompressing()).body()
    val actual = body.readAllBytes().toString(StandardCharsets.UTF_8)

    //then
    actual shouldBe compressedString.original
  }

  companion object {
    @JvmStatic
    fun compressedData(): List<CompressedString> {
      val stringArb = Arb.string(32..64)
      return listOf(
        stringArb.map { CompressedString(it, "gzip", Compression.gzip(it)) }.next(),
        stringArb.map { CompressedString(it, "deflate", Compression.deflate(it)) }.next()
      )
    }
  }

  internal data class CompressedString(val original: String, val compression: String, val compressed: ByteArray)
}