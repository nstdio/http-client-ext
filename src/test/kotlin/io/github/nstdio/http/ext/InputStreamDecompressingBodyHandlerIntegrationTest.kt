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
@file:Suppress("ArrayInDataClass")

package io.github.nstdio.http.ext

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets

internal class InputStreamDecompressingBodyHandlerIntegrationTest {
  private val httpClient = HttpClient.newHttpClient()

  @ParameterizedTest
  @MethodSource("compressedData")
  fun shouldCreate(compressedString: CompressedString) {
    //given
    val uri = URI.create("${wm.runtimeInfo.httpBaseUrl}/data")
    stubFor(
      get("/data").willReturn(
        ok()
          .withHeader("Content-Encoding", compressedString.compression)
          .withBody(compressedString.compressed)
      )
    )

    val request = HttpRequest.newBuilder(uri)
      .build()

    //when
    val body = httpClient.send(request, BodyHandlers.ofDecompressing()).body()
    val actual = IOUtils.toString(body, StandardCharsets.UTF_8)

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

    @RegisterExtension
    @JvmStatic
    var wm = WireMockExtension.newInstance()
      .configureStaticDsl(true)
      .failOnUnmatchedRequests(true)
      .options(WireMockConfiguration.wireMockConfig().dynamicPort())
      .build()
  }

  internal data class CompressedString(val original: String, val compression: String, val compressed: ByteArray)
}