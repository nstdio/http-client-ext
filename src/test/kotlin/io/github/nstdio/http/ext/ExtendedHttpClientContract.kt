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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import io.github.nstdio.http.ext.Assertions.assertThat
import io.github.nstdio.http.ext.Assertions.await
import io.github.nstdio.http.ext.Assertions.awaitFor
import io.github.nstdio.http.ext.FixedRateTickClock.Companion.of
import io.github.nstdio.http.ext.Matchers.isCached
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.awaitility.core.ThrowingRunnable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant

interface ExtendedHttpClientContract {
    /**
     * The client under the test.
     */
    fun client(): ExtendedHttpClient

    /**
     * The cache (if any) used by [.client].
     */
    fun cache(): Cache

    /**
     * The client created with `clock` under the test.
     */
    fun client(clock: Clock): ExtendedHttpClient

    fun wiremockRuntimeInfo(): WireMockRuntimeInfo

    @Throws(IOException::class, InterruptedException::class)
    fun send(request: HttpRequest): HttpResponse<String> {
        return client().send(request, ofString())
    }

    /**
     * The clock to use when performing freshness calculations.
     */
    fun clock(): Clock {
        return Clock.systemUTC()
    }

    fun path(): String {
        return "/resource"
    }

    fun requestBuilder(): HttpRequest.Builder {
        return HttpRequest.newBuilder(resolve(path()))
    }

    @Test
    @Throws(Exception::class)
    fun shouldSupportETagForCaching() {
        //given
        val cache = cache()
        val etag = "v1"
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_ETAG, etag)
                        .withBody("abc")
                )
        )
        stubFor(
            get(urlEqualTo(path()))
                .withHeader(Headers.HEADER_IF_NONE_MATCH, equalTo(etag))
                .willReturn(status(304))
        )

        //when + then
        val r1 = send(requestBuilder().build())
        assertThat(r1).isNotCached
        assertThat(cache).hasNoHits().hasMiss(1)
        awaitFor(ThrowingRunnable {
            val r2 = send(requestBuilder().build())
            assertThat(r2).isCached
                .hasStatusCode(200)
                .hasBody("abc")
        })
        assertThat(cache).hasHits(1).hasMiss(1)
    }

    @Test
    @Throws(Exception::class)
    fun shouldApplyHeuristicFreshness() {
        //given
        val cache = cache()
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_LAST_MODIFIED, Headers.toRFC1123(Instant.now().minusSeconds(60)))
                        .withBody("abc")
                )
        )

        //when + then
        val r1 = send(requestBuilder().build())
        assertThat(r1).isNotCached
        assertThat(cache).hasNoHits().hasMiss(1)
        awaitFor(ThrowingRunnable { assertThat(send(requestBuilder().build())).isCached })
        assertThat(cache).hasHits(1).hasAtLeastMiss(1)
    }

    @Test
    @Throws(Exception::class)
    fun shouldWorkWithOnlyIfCached() {
        //given
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1")
                        .withBody("abc")
                )
        )

        //when + then
        val r1 = send(requestBuilder().build())
        assertThat(r1).isNotCached.hasStatusCode(200)
        awaitFor(ThrowingRunnable {
            val r2 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "only-if-cached").build())
            assertThat(r2).isCached
        })
        awaitFor(ThrowingRunnable {
            val r3 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "only-if-cached,max-age=2").build())
            assertThat(r3).isCached
        })
        val r4 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "only-if-cached,max-age=0").build())
        assertThat(r4)
            .isNotCached
            .hasStatusCode(504)
    }

    @ParameterizedTest
    @ValueSource(ints = [201, 202, 205, 207, 226, 302, 303, 304, 305, 306, 307, 308, 400, 401, 402, 403, 406, 407, 408, 409, 411, 412, 413, 415, 416, 417, 418, 421, 422, 423, 424, 425, 426, 428, 429, 431, 451, 500, 502, 503, 504, 505, 506, 507, 508, 510, 511])
    @Throws(
        Exception::class
    )
    fun shouldNotCacheStatusCodesOtherThen(statusCode: Int) {
        //given
        val body = "abc"
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    status(statusCode)
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=360")
                        .withBody(body)
                )
        )

        //when
        val r1 = send(requestBuilder().build())
        val r2 = send(requestBuilder().build())

        //then
        assertThat(listOf(r1, r2)).allSatisfy(
            ThrowingConsumer { r: HttpResponse<String> ->
                assertThat(r).isNotCached.hasStatusCode(statusCode).hasBody(body)
            })
    }

    @Test
    @Throws(Exception::class)
    fun shouldFailWhenOnlyIfCachedWithEmptyCache() {
        //given
        val request = requestBuilder()
            .header(Headers.HEADER_CACHE_CONTROL, CacheControl.builder().onlyIfCached().build().toString())
            .build()

        //when
        val r1 = send(request)

        //then
        assertThat(r1).hasStatusCode(504)
    }

    @Test
    @Throws(Exception::class)
    fun shouldRespectMinFreshRequests() {
        //given
        val clock = of(clock(), Duration.ofSeconds(1))
        val client = client(clock)
        val bodyHandler = ofString()
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=5")
                        .withBody("abc")
                )
        )

        //when + then
        val r1 = client.send(requestBuilder().build(), bodyHandler)
        assertThat(r1).isNotCached
        awaitFor(ThrowingRunnable {
            val r2 =
                client.send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "min-fresh=4").build(), bodyHandler)
            assertThat(r2).isCached
        })
        val r3 = client.send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "min-fresh=1").build(), bodyHandler)
        assertThat(r3).isNotCached
    }

    @ParameterizedTest
    @ValueSource(strings = ["no-cache", "max-age=0"])
    @Throws(
        IOException::class, InterruptedException::class
    )
    fun shouldNotRespondWithCacheWhenNoCacheProvided(cacheControl: String?) {
        //given
        val cache = cache()
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=84600")
                        .withHeader("Content-Type", "text/plain")
                        .withBody("abc")
                )
        )
        val count = 5L
        for (i in 0 until count) {
            val request = requestBuilder()
                .header(Headers.HEADER_CACHE_CONTROL, cacheControl)
                .build()
            val noCacheControlRequest = requestBuilder().build()

            //when + then
            assertThat(cache).hasHits(i).hasMiss(i)
            val r1 = send(request)
            assertThat(r1).isNotCached.hasBody("abc")
            awaitFor(ThrowingRunnable {
                val r2 = send(noCacheControlRequest)
                assertThat(r2).isCached.hasBody("abc")
            })
            assertThat(cache).hasHits(i + 1).hasAtLeastMiss(i + 1)
        }
        verify(
            count.toInt(),
            getRequestedFor(urlPattern)
                .withHeader(Headers.HEADER_CACHE_CONTROL, equalTo(cacheControl))
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["no-store", "no-store, no-cache"])
    @Throws(
        IOException::class, InterruptedException::class
    )
    fun shouldNotCacheWhenRequestNoStoreProvided(cacheControl: String?) {
        //given
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=84600")
                        .withHeader("Content-Type", "text/plain")
                        .withBody("abc")
                )
        )
        send(requestBuilder().build()) // make r2 cached
        val count = 5
        for (i in 0 until count) {
            val request = requestBuilder()
                .header(Headers.HEADER_CACHE_CONTROL, cacheControl)
                .build()

            //when + then
            val r1 = send(request)
            assertThat(r1).isNotCached.hasBody("abc")
            awaitFor(ThrowingRunnable {
                val r2 = send(requestBuilder().build())
                assertThat(r2).isCached.hasBody("abc")
            })
        }
        verify(
            count,
            getRequestedFor(urlPattern)
                .withHeader(Headers.HEADER_CACHE_CONTROL, equalTo(cacheControl))
        )
        verify(1, getRequestedFor(urlPattern).withoutHeader(Headers.HEADER_CACHE_CONTROL))
    }

    @ParameterizedTest
    @ValueSource(strings = ["no-store", "no-store, no-cache"])
    @Throws(
        IOException::class, InterruptedException::class
    )
    fun shouldNotCacheWhenResponseNoStoreProvided(cacheControl: String?) {
        //given
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, cacheControl)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("abc")
                )
        )
        val count = 5
        for (i in 0 until count) {

            //when + then
            val r1 = send(requestBuilder().build())
            assertThat(r1).isNotCached.hasBody("abc")
        }
        verify(count, getRequestedFor(urlPattern))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun shouldCacheWhenHeadersDifferWithoutVary() {
        //given
        val cacheControlValue = "public,max-age=20"
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .withHeader("Accept", equalTo("text/plain"))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")
                )
        )

        //when + then
        val r1 = send(requestBuilder().header("Accept", "text/plain").build())
        assertThat(r1)
            .isNotCached
            .hasBody("Hello world!")
        awaitFor(ThrowingRunnable {
            val r2 = send(requestBuilder().header("Accept", "application/json").build())
            assertThat(r2)
                .isCached
                .hasBody("Hello world!")
        })
        verify(1, getRequestedFor(urlPattern).withHeader("Accept", equalTo("text/plain")))
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotCacheWithVaryAsterisk() {
        //given
        val cacheControlValue = "public,max-age=20"
        val count = 9
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .withHeader("Accept", equalTo("text/plain"))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Vary", "*")
                        .withBody("Hello world!")
                )
        )
        val request = requestBuilder()
            .header("Accept", "text/plain")
            .build()

        //when + then
        for (i in 0 until count) {
            val r1 = send(request)
            assertThat(r1)
                .isNotCached
                .hasBody("Hello world!")
        }
        verify(
            count,
            getRequestedFor(urlPattern).withHeader("Accept", equalTo("text/plain"))
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldCacheWithVary() {
        //given
        val cacheControlValue = "public,max-age=20"
        val count = 9
        val varyValues = arrayOf("Accept", "Accept-Encoding", "User-Agent")
        val textBody = "Hello world!"
        val jsonBody = "\"Hello world!\""
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .withHeader("Accept", equalTo("text/plain"))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Vary", *varyValues)
                        .withBody(textBody)
                )
        )
        stubFor(
            get(urlPattern)
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Vary", *varyValues)
                        .withBody(jsonBody)
                )
        )
        val textRequest = requestBuilder()
            .header("Accept", "text/plain")
            .build()
        val jsonRequest = requestBuilder()
            .header("Accept", "application/json")
            .build()

        //when + then
        val r1 = send(textRequest)
        val r2 = send(jsonRequest)
        assertThat(r1)
            .isNotCached
            .hasBody(textBody)
        assertThat(r2)
            .isNotCached
            .hasBody(jsonBody)
        for (i in 0 until count) {
            awaitFor(ThrowingRunnable {
                assertThat(send(textRequest))
                    .isCached
                    .hasBody(textBody)
            })
            awaitFor(ThrowingRunnable {
                assertThat(send(jsonRequest))
                    .isCached
                    .hasBody(jsonBody)
            })
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldUpdateExistingCacheWithNoCacheProvided() {
        //given
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=200")
                        .withBody("abc")
                )
        )
        stubFor(
            get(urlPattern)
                .withHeader(Headers.HEADER_CACHE_CONTROL, equalTo("no-cache"))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=200")
                        .withBody("abc: Updated")
                )
        )
        val request = requestBuilder().build()

        //when + then
        val r1 = send(request)
        assertThat(r1).isNotCached.hasBody("abc")
        val r2 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "no-cache").build())
        assertThat(r2).isNotCached.hasBody("abc: Updated")
        await().until { cache()[request] != null }
        val r3 = send(request)
        assertThat(r3).isCached.hasBody("abc: Updated")
    }

    @Test
    @Throws(Exception::class)
    fun shouldRespectMaxAgeRequests() {
        //given
        val clock = of(clock(), Duration.ofSeconds(1))
        val client = client(clock)
        val bodyHandler = ofString()
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=16")
                        .withBody("abc")
                )
        )

        //when + then
        val r1 = client.send(requestBuilder().build(), bodyHandler)
        assertThat(r1).isNotCached
        awaitFor(ThrowingRunnable {
            val r2 =
                client.send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "max-age=8").build(), bodyHandler)
            assertThat(r2).isCached
        })
        val r3 = client.send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "max-age=1").build(), bodyHandler)
        assertThat(r3).isNotCached
    }

    @ParameterizedTest(name = "{0}: Should invalidate existing cache when unsafe HTTP methods are used")
    @ValueSource(strings = ["POST", "PUT", "DELETE"])
    @Throws(
        Exception::class
    )
    fun shouldInvalidateWhenUnsafe(method: String?) {
        //given
        val urlPattern = urlEqualTo(path())
        val locationPath = path() + "/1"
        val contentLocationPath = path() + "/2"
        val locationUri = resolve(locationPath)
        val contentLocationUri = resolve(contentLocationPath)
        stubFor(
            get(urlPattern)
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc")
                )
        )
        stubFor(
            get(urlEqualTo(locationPath))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc1")
                )
        )
        stubFor(
            get(urlEqualTo(contentLocationPath))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc2")
                )
        )
        stubFor(
            post(urlPattern).willReturn(
                created()
                    .withHeader("Location", locationPath)
                    .withHeader("Content-Location", contentLocationPath)
                    .withBody("abc")
            )
        )
        stubFor(
            put(urlPattern).willReturn(
                noContent()
                    .withHeader("Location", locationPath)
                    .withHeader("Content-Location", contentLocationPath)
            )
        )
        stubFor(
            delete(urlPattern).willReturn(
                noContent()
                    .withHeader("Location", locationPath)
                    .withHeader("Content-Location", contentLocationPath)
            )
        )
        val bodyPublisher = BodyPublishers.noBody()
        val timeout = Duration.ofMillis(1000)

        //when + then
        val uris = listOf(resolve(path()), locationUri, contentLocationUri)
        for (uri in uris) {
            val cached = isCached<String>()
            await().atMost(timeout).until({ send(HttpRequest.newBuilder(uri).build()) }, cached)
        }
        send(requestBuilder().method(method, bodyPublisher).build()) // this request should invalidate
        for (uri in uris) {
            val response = send(HttpRequest.newBuilder(uri).build())
            assertThat(response).isNetwork.isNotCached
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldWorkWithPathSubscriber(@TempDir tempDir: Path) {
        //given
        val file = tempDir.resolve("download")
        val body = RandomStringUtils.randomAlphabetic(32)
        stubFor(
            get(urlEqualTo(path()))
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=16")
                        .withBody(body)
                )
        )
        val bodyHandler = HttpResponse.BodyHandlers.ofFile(file)
        val request = requestBuilder().build()

        //when
        val r1 = client().send(request, bodyHandler)
        val r1Path = r1.body()
        val r2 = await().until({ client().send(request, bodyHandler) }, isCached())
        val r2Path = r2!!.body()

        //then
        assertThat(r1Path).exists().hasContent(body)
        assertThat(r2Path).exists().hasContent(body)
    }

    /**
     * https://datatracker.ietf.org/doc/html/rfc5861#section-4
     */
    @Test
    @Throws(Exception::class)
    fun shouldRespectStaleIfError() {
        //given
        val clock = of(clock(), Duration.ofSeconds(1))
        val client = client(clock)
        val bodyHandler = ofString()
        val urlPattern = urlEqualTo(path())
        stubFor(
            get(urlPattern)
                .willReturn(
                    ok()
                        .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1,stale-if-error=10")
                        .withBody("abc")
                )
        )
        stubFor(
            get(urlPattern)
                .withHeader(Headers.HEADER_IF_MODIFIED_SINCE, matching(".+"))
                .willReturn(aResponse().withStatus(500))
        )

        //when
        val r1 = client.send(requestBuilder().build(), bodyHandler)
        val r2 = await().until(
            { client.send(requestBuilder().header("Cache-Control", "stale-if-error=4").build(), bodyHandler) },
            isCached()
        )
        val r3 = await().until(
            { client.send(requestBuilder().header("Cache-Control", "stale-if-error=100").build(), bodyHandler) },
            isCached()
        )
        val r4 = client.send(requestBuilder().header("Cache-Control", "stale-if-error=1").build(), bodyHandler)

        //then
        assertThat(r1).isNotCached.hasBody("abc")
        assertThat(r2).isCached.hasBody("abc")
        assertThat(r3).isCached.hasBody("abc")
        assertThat(r4).isNotCached.hasStatusCode(500)
    }

    @Test
    @Disabled("https://datatracker.ietf.org/doc/html/rfc5861#section-3")
    fun shouldRespectStaleWhileRevalidate() {
    }

    fun rfc1123Date(): String? {
        return Headers.toRFC1123(clock().instant())
    }

    fun ok(): ResponseDefinitionBuilder {
        return WireMock.ok().withHeader(Headers.HEADER_DATE, rfc1123Date())
    }

    fun resolve(path: String): URI {
        return URI.create(wiremockRuntimeInfo().httpBaseUrl).resolve(path)
    }
}