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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.ResponseInfo
import java.nio.charset.StandardCharsets

internal class DecompressingSubscriberIntegrationTest {
    private val client = HttpClient.newHttpClient()

    @RegisterExtension
    var wm = WireMockExtension.newInstance()
        .configureStaticDsl(true)
        .failOnUnmatchedRequests(true)
        .options(WireMockConfiguration.wireMockConfig().dynamicPort())
        .build()

    @RepeatedTest(32)
    @Throws(Exception::class)
    fun shouldDecompressLargeBodyWithStringHandler() {
        //given
        val data = setupLargeBodyDecompressionTest()
        val request = data.request
        val expectedBody = data.expectedBody

        //when
        val stringResponse = client.send(request) { info: ResponseInfo? ->
            DecompressingSubscriber(
                HttpResponse.BodyHandlers.ofString().apply(info)
            )
        }

        //then
        org.junit.jupiter.api.Assertions.assertEquals(expectedBody.length, stringResponse.body().length)
        Assertions.assertThat(stringResponse).hasBody(expectedBody)
    }

    @RepeatedTest(1)
    @Disabled("Having problems with InputStream")
    @Throws(
        Exception::class
    )
    fun shouldHandleLargeBodyWithInputStream() {
        //given
        val data = setupLargeBodyDecompressionTest()
        val request = data.request
        val expectedBody = data.expectedBody

        //when
        val response = client.send(request) { info: ResponseInfo? ->
            DecompressingSubscriber(
                HttpResponse.BodyHandlers.ofInputStream().apply(info)
            )
        }
        val body = IOUtils.toString(response.body(), StandardCharsets.UTF_8)
        org.junit.jupiter.api.Assertions.assertEquals(expectedBody.length, body.length)
        org.junit.jupiter.api.Assertions.assertEquals(expectedBody, body)
    }

    private fun setupLargeBodyDecompressionTest(body: String = RandomStringUtils.randomAlphabetic(16384 * 10)): LargeBodyDataDecompression {
        val gzippedBody = Compression.gzip(body)
        val testUrl = "/gzip"
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo(testUrl))
                .willReturn(WireMock.ok().withBody(gzippedBody))
        )
        val uri = URI.create(wm.runtimeInfo.httpBaseUrl).resolve(testUrl)
        val request = HttpRequest.newBuilder(uri).build()
        return LargeBodyDataDecompression(request, body)
    }

    internal class LargeBodyDataDecompression(val request: HttpRequest, val expectedBody: String)
}