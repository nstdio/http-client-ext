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
import io.github.nstdio.http.ext.jupiter.EnabledIfOnClasspath
import io.kotest.assertions.json.shouldBeValidJson
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString

@EnabledIfOnClasspath(JACKSON) // required by WireMock
internal class DecompressingSubscriberSpiTest {
  private val client = HttpClient.newHttpClient()

  @ParameterizedTest
  @ValueSource(strings = ["br", "gzip", "deflate"])
  fun shouldDecompress(compressionType: String) {
    //given
    val uri = URI.create(wm.baseUrl()).resolve("/data")
    stubFor(
      get("/data").willReturn(
        ok()
          .withHeader("Content-Encoding", compressionType)
          .withBodyFile(compressionType)
      )
    )

    //when
    val response = client.send(
      HttpRequest.newBuilder(uri).build(),
      BodyHandlers.ofDecompressing(ofString())
    )

    //then
    response.body().shouldBeValidJson()
  }

  companion object {
    @RegisterExtension
    @JvmStatic
    var wm = WireMockExtension.newInstance()
      .configureStaticDsl(true)
      .failOnUnmatchedRequests(true)
      .options(
        WireMockConfiguration.wireMockConfig()
          .gzipDisabled(true)
          .usingFilesUnderDirectory("src/spiTest/resources")
          .dynamicPort()
      )
      .build()
  }
}