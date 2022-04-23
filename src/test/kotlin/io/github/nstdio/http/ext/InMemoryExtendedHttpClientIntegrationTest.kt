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
import io.github.nstdio.http.ext.FixedRateTickClock.Companion.of
import io.github.nstdio.http.ext.Matchers.isCached
import io.kotest.matchers.nulls.shouldBeNull
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketPolicy
import org.hamcrest.CoreMatchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Clock
import java.time.Duration

@MockWebServerTest
internal class InMemoryExtendedHttpClientIntegrationTest(private val mockWebServer: MockWebServer) :
  ExtendedHttpClientContract {

  private val defaultClock = Clock.systemUTC()
  private val delegate = HttpClient.newHttpClient()

  private lateinit var client: ExtendedHttpClient
  private lateinit var cache: Cache

  @BeforeEach
  fun setUp() {
    cache = Cache.newInMemoryCacheBuilder()
      .requestFilter { true }
      .responseFilter { true }
      .build()
    client = client(defaultClock)
  }

  override fun client() = client

  override fun cache() = cache

  override fun client(clock: Clock) = ExtendedHttpClient(delegate, cache, clock)

  override fun mockWebServer() = mockWebServer

  @Test
  fun shouldRespondWithCachedWhenNotModified() {
    //given
    val clock = of(defaultClock, Duration.ofSeconds(2))
    client = client(clock)
    val date = Headers.toRFC1123(clock.instant().minusSeconds(2))

    mockWebServer.enqueue(
      MockResponse()
        .ok()
        .addHeader(Headers.HEADER_DATE, date)
        .addHeader(Headers.HEADER_CACHE_CONTROL, "public,max-age=1")
        .setBody("Hello world!")
    )
    mockWebServer.enqueue(
      MockResponse()
        .notModified()
        .addHeader(Headers.HEADER_CACHE_CONTROL, "private, max-age=1")
    )

    val request = requestBuilder().build()

    //when
    val r1 = send(request)
    val r2 = await().until({ send(request) }, isCached())

    //then
    assertThat(r1)
      .hasHeader(Headers.HEADER_CACHE_CONTROL, "public,max-age=1")
      .isNetwork
    assertThat(r2)
      .hasHeader(Headers.HEADER_CACHE_CONTROL, "private, max-age=1")
  }

  @Test
  fun shouldRespectMaxStaleRequests() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val clock = of(defaultClock, tickDuration)
    client = client(clock)
    val date = Headers.toRFC1123(clock.instant().minusMillis(tickDuration.toMillis()))
    val response = MockResponse().ok()
      .addHeader(Headers.HEADER_DATE, date)
      .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1")
      .setBody("abc")

    mockWebServer.enqueue(response, 2)

    //when
    val r1 = send(requestBuilder().build())
    val r2 = await().until(
      { send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "max-stale=5").build()) }, isCached()
    )
    val r3 = send(requestBuilder().header(Headers.HEADER_CACHE_CONTROL, "max-stale=4").build())

    //then
    assertThat(r1).isNetwork
    assertThat(r2).isCached
    assertThat(r3).isNetwork
  }

  @Test
  fun shouldUseResponseTimeWhenDateHeaderMissing() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val clock = of(defaultClock, tickDuration)
    client = client(clock)
    val expires = Headers.toRFC1123(defaultClock.instant().plusSeconds(6))
    val response = MockResponse()
      .ok()
      .addHeader(Headers.HEADER_EXPIRES, expires)
      .setBody("abc")

    mockWebServer.enqueue(response, 10)

    val request = requestBuilder().build()

    //when
    val r1 = send(request)
    await().until({ send(request) }, isCached())
    await().until({ send(request) }, not(isCached()))

    //then
    assertThat(r1).isNetwork
  }

  @Test
  @DisplayName("Should respond with 504 when server requires validation, but validation request fails.")
  fun shouldReturn504WhenMustRevalidate() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val clock = of(defaultClock, tickDuration)
    client = client(clock)
    val date = clock.instant().minusMillis(tickDuration.toMillis())

    mockWebServer.enqueue(
      MockResponse().ok()
        .addHeaderDate(date)
        .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1,must-revalidate")
        .setBody("abc")
    )

    mockWebServer.enqueue(
      MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
    )

    //when + then
    val r1 = send(requestBuilder().build())
    assertThat(r1).isNetwork.hasStatusCode(200).hasBody("abc")

    val r2 = send(requestBuilder().timeout(Duration.ofMillis(100)).build())
    assertThat(r2).isNotNetwork.isNotCached.hasStatusCode(504)
  }

  @Test
  fun shouldNotCacheWhenFiltered() {
    //given
    val cacheableUri = mockWebServer().url(path()).toUri()
    val notCacheableUri = cacheableUri.resolve("/no-cache")
    val cache = Cache.newInMemoryCacheBuilder()
      .requestFilter { it.uri() == cacheableUri }
      .build()
    client = ExtendedHttpClient(delegate, cache, defaultClock)

    mockWebServer.enqueue(
      MockResponse()
        .ok()
        .addHeader(Headers.HEADER_CACHE_CONTROL, "max-age=512")
        .setBody("abc"), 2
    )

    val request = HttpRequest.newBuilder().uri(notCacheableUri).build()

    //when
    val r1 = send(request)
    val r2 = send(request)

    //then
    assertThat(r1).isNetwork
    assertThat(r2).isNetwork
    cache.get(request).shouldBeNull()
  }
}