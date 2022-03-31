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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.github.nstdio.http.ext.Assertions.assertThat
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal class ExtendedHttpClientIntegrationTest {
    @RegisterExtension
    var wm: WireMockExtension = WireMockExtension.newInstance()
        .configureStaticDsl(true)
        .failOnUnmatchedRequests(true)
        .options(WireMockConfiguration.wireMockConfig().dynamicPort())
        .build()

    private fun resolve(path: String): URI {
        return URI.create(wm.runtimeInfo.httpBaseUrl).resolve(path)
    }

    @Nested
    internal inner class TransparentDecompressionTest {
        @Test
        @Throws(Exception::class)
        fun shouldTransparentlyDecompressAndCache() {
            //given
            val cache = Cache.newInMemoryCacheBuilder().build()
            val client: HttpClient = ExtendedHttpClient.newBuilder()
                .cache(cache)
                .transparentEncoding(true)
                .build()
            val expectedBody = RandomStringUtils.randomAlphabetic(16)
            val testUrl = "/gzip"
            stubFor(
                get(urlEqualTo(testUrl))
                    .withHeader("Accept-Encoding", equalTo("gzip,deflate"))
                    .willReturn(
                        ok()
                            .withHeader("Cache-Control", "max-age=86000")
                            .withBody(expectedBody)
                    )
            )
            val request1 = HttpRequest.newBuilder(resolve(testUrl))
                .build()
            val request2 = HttpRequest.newBuilder(resolve(testUrl))
                .build()

            //when + then
            val r1 = client.send(request1, HttpResponse.BodyHandlers.ofString())
            assertThat(r1)
                .isNetwork
                .hasBody(expectedBody)
                .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
            Assertions.awaitFor {
                val r2 = client.send(request2, HttpResponse.BodyHandlers.ofString())
                assertThat(r2)
                    .isCached
                    .hasBody(expectedBody)
                    .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
            }
        }

        @Test
        @Throws(Exception::class)
        fun shouldTransparentlyDecompress() {
            //given
            val client: HttpClient = ExtendedHttpClient.newBuilder()
                .cache(Cache.noop())
                .transparentEncoding(true)
                .build()
            val expectedBody = RandomStringUtils.randomAlphabetic(16)
            val testUrl = "/gzip"
            stubFor(
                get(urlEqualTo(testUrl))
                    .withHeader("Accept-Encoding", equalTo("gzip,deflate"))
                    .willReturn(ok().withBody(expectedBody))
            )
            val request = HttpRequest.newBuilder(resolve(testUrl)).build()

            //when
            val r1 = client.send(request, HttpResponse.BodyHandlers.ofString())
            val r2 = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join()

            //then
            assertThat(r1)
                .isNetwork
                .hasBody(expectedBody)
                .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
            assertThat(r2)
                .isNetwork
                .hasBody(expectedBody)
                .hasNoHeader(Headers.HEADER_CONTENT_ENCODING)
        }
    }
}