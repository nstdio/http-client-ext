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
import io.github.nstdio.http.ext.Assertions.await
import io.github.nstdio.http.ext.Assertions.awaitFor
import io.github.nstdio.http.ext.FixedRateTickClock.Companion.of
import io.github.nstdio.http.ext.Matchers.isCached
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.awaitility.core.ThrowingRunnable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.noBody
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
   * The mock web server.
   */
  fun mockWebServer(): MockWebServer

  /**
   * The cache (if any) used by [.client].
   */
  fun cache(): Cache

  /**
   * The client created with `clock` under the test.
   */
  fun client(clock: Clock): ExtendedHttpClient

  fun send(request: HttpRequest): HttpResponse<String> = client().send(request, ofString())

  /**
   * The clock to use when performing freshness calculations.
   */
  fun clock(): Clock = Clock.systemUTC()

  fun path(): String = "/resource"

  fun requestBuilder(): HttpRequest.Builder = HttpRequest.newBuilder(mockWebServer().url(path()).toUri())

  @Test
  fun shouldSupportETagForCaching() {
    //given
    val cache = cache()
    val etag = "v1"
    mockWebServer().enqueue(
      ok()
        .addHeader(Headers.HEADER_ETAG, etag)
        .setBody("abc")
    )
    mockWebServer().enqueue(MockResponse().notModified())

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
  fun shouldApplyHeuristicFreshness() {
    //given
    val cache = cache()
    val value = Instant.now().minusSeconds(60)
    mockWebServer().enqueue(
      ok()
        .addDateHeader(Headers.HEADER_LAST_MODIFIED, value)
        .setBody("abc"),
      2
    )

    //when + then
    val r1 = send(requestBuilder().build())
    assertThat(r1).isNotCached
    assertThat(cache).hasNoHits().hasMiss(1)
    awaitFor { assertThat(send(requestBuilder().build())).isCached }
    assertThat(cache).hasHits(1).hasAtLeastMiss(1)
  }

  @Test
  fun shouldWorkWithOnlyIfCached() {
    //given
    mockWebServer().enqueue(
      ok()
        .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=4")
        .setBody("abc"),
      5
    )

    //when + then
    val r1 = send(requestBuilder().build())
    assertThat(r1).isNotCached.hasStatusCode(200)
    awaitFor {
      val r2 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "only-if-cached").build())
      assertThat(r2).isCached
    }
    awaitFor {
      val r3 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "only-if-cached,max-age=8").build())
      assertThat(r3).isCached
    }
    val r4 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "only-if-cached,max-age=0").build())
    assertThat(r4)
      .isNotCached
      .hasStatusCode(504)
  }

  @ParameterizedTest
  @ValueSource(ints = [201, 202, 205, 207, 226, 302, 303, 304, 305, 306, 307, 308, 400, 401, 402, 403, 406, 407, 408, 409, 411, 412, 413, 415, 416, 417, 418, 421, 422, 423, 424, 425, 426, 428, 429, 431, 451, 500, 502, 503, 504, 505, 506, 507, 508, 510, 511])
  fun shouldNotCacheStatusCodesOtherThen(statusCode: Int) {
    //given
    val body = "abc"
    mockWebServer().enqueue(
      ok()
        .setResponseCode(statusCode)
        .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=360")
        .setBody(body),
      2
    )

    //when
    val r1 = send(requestBuilder().build())
    val r2 = send(requestBuilder().build())

    //then
    assertThat(listOf(r1, r2)).allSatisfy(
      ThrowingConsumer { r: HttpResponse<String> ->
        assertThat(r).isNotCached.hasStatusCode(statusCode)
      })
  }

  @Test
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
  fun shouldRespectMinFreshRequests() {
    //given
    val clock = of(clock(), Duration.ofSeconds(1))
    val client = client(clock)
    val bodyHandler = ofString()
    val mockResponse = ok()
      .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=5")
      .setBody("abc")
    mockWebServer().enqueue(mockResponse, 3)

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
  fun shouldNotRespondWithCacheWhenNoCacheProvided(cacheControl: String) {
    //given
    val cache = cache()
    val mockWebServer = mockWebServer()
    val mockResponse = ok()
      .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=84600")
      .addHeader("Content-Type", "text/plain")
      .setBody("abc")

    val count = 5L
    mockWebServer.enqueue(mockResponse, count.toInt())

    for (i in 0 until count) {
      val request = requestBuilder()
        .header(Headers.HEADER_CACHE_CONTROL, cacheControl)
        .build()
      val noCacheControlRequest = requestBuilder().build()

      //when + then
      assertThat(cache).hasHits(i).hasMiss(i)

      val r1 = send(request)
      assertThat(r1).isNotCached.hasBody("abc")

      awaitFor {
        val r2 = send(noCacheControlRequest)
        assertThat(r2).isCached.hasBody("abc")
      }

      assertThat(cache).hasHits(i + 1).hasAtLeastMiss(i + 1)
    }

    mockWebServer.requestCount.shouldBe(count)
  }

  @ParameterizedTest
  @ValueSource(strings = ["no-store", "no-store, no-cache"])
  fun shouldNotCacheWhenRequestNoStoreProvided(cacheControl: String?) {
    //given
    val mockWebServer = mockWebServer()
    val mockResponse = ok()
      .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=84600")
      .addHeader("Content-Type", "text/plain")
      .setBody("abc")
    val count = 5
    mockWebServer.enqueue(mockResponse, count)

    send(requestBuilder().build()) // make r2 cached

    for (i in 0 until count) {
      val request = requestBuilder()
        .header(Headers.HEADER_CACHE_CONTROL, cacheControl)
        .build()

      //when + then
      val r1 = send(request)
      assertThat(r1).isNotCached.hasBody("abc")

      awaitFor {
        val r2 = send(requestBuilder().build())
        assertThat(r2).isCached.hasBody("abc")
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["no-store", "no-store, no-cache"])
  fun shouldNotCacheWhenResponseNoStoreProvided(cacheControl: String) {
    //given
    val mockWebServer = mockWebServer()
    val mockResponse = ok()
      .addHeader(Headers.HEADER_CACHE_CONTROL, cacheControl)
      .addHeader("Content-Type", "text/plain")
      .setBody("abc")
    val count = 5
    mockWebServer.enqueue(mockResponse, count)

    for (i in 0 until count) {
      //when + then
      val r1 = send(requestBuilder().build())
      assertThat(r1).isNotCached.hasBody("abc")
    }
  }

  @Test
  fun shouldCacheWhenHeadersDifferWithoutVary() {
    //given
    val mockResponse = ok()
      .addHeader(Headers.HEADER_CACHE_CONTROL, "public,max-age=20")
      .addHeader("Content-Type", "text/plain")
      .setBody("Hello world!")

    mockWebServer().enqueue(mockResponse, 2)

    //when + then
    val r1 = send(requestBuilder().header("Accept", "text/plain").build())
    assertThat(r1)
      .isNotCached
      .hasBody("Hello world!")
    awaitFor {
      val r2 = send(requestBuilder().header("Accept", "application/json").build())
      assertThat(r2)
        .isCached
        .hasBody("Hello world!")
    }
  }

  @Test
  fun shouldNotCacheWithVaryAsterisk() {
    //given
    val count = 9
    val mockResponse = ok()
      .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=20")
      .addHeader("Content-Type", "text/plain")
      .addHeader("Vary", "*")
      .setBody("Hello world!")

    mockWebServer().enqueue(mockResponse, count)

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
  }

  @Test
  fun shouldCacheWithVary() {
    //given
    val varyValues = listOf("Accept", "Accept-Encoding", "User-Agent").joinToString(", ")
    val textBody = "Hello world!"
    val jsonBody = "\"Hello world!\""
    val mockWebServer = mockWebServer()

    val mockTextResponse = ok()
      .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=20")
      .setHeader("Content-Type", "text/plain")
      .setHeader("Vary", varyValues)
      .setBody(textBody)

    val mockJsonResponse = ok()
      .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=20")
      .setHeader("Content-Type", "application/json")
      .setHeader("Vary", varyValues)
      .setBody(jsonBody)

    val count = 9
    (0..count).forEach { _ ->
      mockWebServer.enqueue(mockTextResponse)
      mockWebServer.enqueue(mockJsonResponse)
    }

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
      awaitFor {
        assertThat(send(textRequest))
          .isCached
          .hasBody(textBody)
      }

      awaitFor {
        assertThat(send(jsonRequest))
          .isCached
          .hasBody(jsonBody)
      }
    }
  }

  @Test
  fun shouldUpdateExistingCacheWithNoCacheProvided() {
    //given
    val mockWebServer = mockWebServer()

    mockWebServer.enqueue(
      ok()
        .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=200")
        .setBody("abc")
    )
    mockWebServer.enqueue(
      ok()
        .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=200")
        .setBody("abc: Updated")
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
  fun shouldRespectMaxAgeRequests() {
    //given
    val clock = of(clock(), Duration.ofSeconds(1))
    val client = client(clock)
    val bodyHandler = ofString()

    val mockResponse = ok()
      .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=16")
      .setBody("abc")

    mockWebServer().enqueue(mockResponse, 2)

    //when + then
    val r1 = client.send(requestBuilder().build(), bodyHandler)
    assertThat(r1).isNotCached

    awaitFor {
      val r2 = client.send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "max-age=8").build(), bodyHandler)
      assertThat(r2).isCached
    }

    val r3 = client.send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "max-age=1").build(), bodyHandler)
    assertThat(r3).isNotCached
  }

  @ParameterizedTest(name = "{0}: Should invalidate existing cache when unsafe HTTP methods are used")
  @ValueSource(strings = ["POST", "PUT", "DELETE"])
  fun shouldInvalidateWhenUnsafe(method: String) {
    //given
    val locationPath = path() + "/1"
    val contentLocationPath = path() + "/2"

    (0..2)
      .forEach { i ->
        mockWebServer().enqueue(
          ok().setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=512")
            .setBody("abc-$i")
        )
      }

    (0..4).forEach { i ->
      val response = when (i) {
        2 -> MockResponse().setResponseCode(500)
        else -> ok()
      }

      mockWebServer().enqueue(
        response
          .setHeader("Location", locationPath)
          .setHeader("Content-Location", contentLocationPath)
          .setBody("abc")
      )
    }
    //when + then
    val requests = listOf(path(), locationPath, contentLocationPath)
      .map { mockWebServer().url(it).toUri() }
      .map { HttpRequest.newBuilder(it).build() }

    requests.forEach { await().until({ send(it) }, isCached()) }

    val r1 = send(requestBuilder().method(method, noBody()).build()) // this request should invalidate

    requests.forEach {
      val assertion = assertThat(send(it))

      when (r1.statusCode()) {
        500 -> assertion.isCached
        else -> assertion.isNetwork.isNotCached
      }
    }
  }

  @Test
  fun shouldWorkWithPathSubscriber(@TempDir tempDir: Path) {
    //given
    val file = tempDir.resolve("download")
    val body = Arb.string(32).next()
    mockWebServer().enqueue(
      ok()
        .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=16")
        .setBody(body),
      2
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
  fun shouldRespectStaleIfError() {
    //given
    val clock = of(clock(), Duration.ofSeconds(1))
    val client = client(clock)
    val bodyHandler = ofString()
    mockWebServer().enqueue(
      ok()
        .setHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1,stale-if-error=10")
        .setBody("abc")
    )

    mockWebServer().enqueue(
      MockResponse()
        .setResponseCode(500),
      3
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

  fun ok(): MockResponse {
    return MockResponse().ok().addHeaderDate(clock().instant())
  }
}