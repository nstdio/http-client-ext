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
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import io.github.nstdio.http.ext.Assertions.assertThat
import io.github.nstdio.http.ext.Assertions.await
import io.github.nstdio.http.ext.Assertions.awaitFor
import io.github.nstdio.http.ext.FixedRateTickClock.Companion.of
import io.github.nstdio.http.ext.Matchers.isCached
import org.awaitility.core.ThrowingRunnable
import org.hamcrest.CoreMatchers.not
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Clock
import java.time.Duration

internal class InMemoryExtendedHttpClientIntegrationTest : ExtendedHttpClientContract {
  @RegisterExtension
  val wm: WireMockExtension = WireMockExtension.newInstance()
    .configureStaticDsl(true)
    .failOnUnmatchedRequests(true)
    .options(WireMockConfiguration.wireMockConfig().dynamicPort())
    .build()

  private val defaultClock = Clock.systemUTC()
  private lateinit var client: ExtendedHttpClient
  private lateinit var path: String
  private lateinit var cache: Cache

  @BeforeEach
  fun setUp() {
    cache = Cache.newInMemoryCacheBuilder()
      .requestFilter { true }
      .responseFilter { true }
      .build()
    client = ExtendedHttpClient(HttpClient.newHttpClient(), cache, defaultClock)
    path = "/resource"
  }

  override fun path(): String {
    return path
  }

  override fun client(): ExtendedHttpClient {
    return client
  }

  override fun cache(): Cache {
    return cache
  }

  override fun wiremockRuntimeInfo(): WireMockRuntimeInfo {
    return wm.runtimeInfo
  }

  override fun client(clock: Clock): ExtendedHttpClient {
    return ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
  }

  @Test
  @Throws(IOException::class, InterruptedException::class)
  fun shouldRespondWithCachedWhenNotModified() {
    //given
    val urlPattern = urlEqualTo(path)
    val clock = of(defaultClock, Duration.ofSeconds(2))
    client = ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
    val date = Headers.toRFC1123(clock.instant().minusSeconds(2))
    stubFor(
      get(urlPattern)
        .willReturn(
          WireMock.ok()
            .withHeader(Headers.HEADER_DATE, date)
            .withHeader(Headers.HEADER_CACHE_CONTROL, "public,max-age=1")
            .withBody("Hello world!")
        )
    )
    stubFor(
      get(urlPattern)
        .withHeader(Headers.HEADER_IF_MODIFIED_SINCE, equalTo(date))
        .willReturn(status(304).withHeader(Headers.HEADER_CACHE_CONTROL, "private, max-age=1"))
    )
    val request = requestBuilder().build()

    //when
    val r1 = send(request)
    val r2 = await().until({ send(request) }, isCached())
    val r3 = await().until({ send(request) }, isCached())

    //then
    assertThat(r1)
      .hasHeader(Headers.HEADER_CACHE_CONTROL, "public,max-age=1")
      .isNetwork
    assertThat(r2)
      .hasHeader(Headers.HEADER_CACHE_CONTROL, "private, max-age=1")
      .isSemanticallyEqualTo(r3)
    verify(1, getRequestedFor(urlPattern).withoutHeader(Headers.HEADER_IF_MODIFIED_SINCE))
    verify(
      2,
      getRequestedFor(urlPattern).withHeader(Headers.HEADER_IF_MODIFIED_SINCE, equalTo(date))
    )
  }

  @Test
  @Throws(Exception::class)
  fun shouldRespectMaxStaleRequests() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val clock = of(defaultClock, tickDuration)
    client = ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
    val date = Headers.toRFC1123(clock.instant().minusMillis(tickDuration.toMillis()))
    stubFor(
      get(urlEqualTo(path()))
        .willReturn(
          WireMock.ok()
            .withHeader(Headers.HEADER_DATE, date)
            .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1")
            .withBody("abc")
        )
    )

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
  @Throws(Exception::class)
  fun shouldUseResponseTimeWhenDateHeaderMissing() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val clock = of(defaultClock, tickDuration)
    client = ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
    val expires = Headers.toRFC1123(defaultClock.instant().plusSeconds(6))
    stubFor(
      get(urlEqualTo(path()))
        .willReturn(
          WireMock.ok()
            .withHeader(Headers.HEADER_EXPIRES, expires)
            .withBody("abc")
        )
    )
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
  @Throws(
    Exception::class
  )
  fun shouldReturn504WhenMustRevalidate() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val clock = of(defaultClock, tickDuration)
    client = ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
    val date = Headers.toRFC1123(clock.instant().minusMillis(tickDuration.toMillis()))
    val urlPattern = urlEqualTo(path())
    stubFor(
      get(urlPattern)
        .willReturn(
          WireMock.ok()
            .withHeader(Headers.HEADER_DATE, date)
            .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=1,must-revalidate")
            .withBody("abc")
        )
    )
    stubFor(
      get(urlPattern)
        .withHeader(Headers.HEADER_IF_MODIFIED_SINCE, equalTo(date))
        .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
    )
    val request = requestBuilder().build()

    //when + then
    val r1 = send(request)
    assertThat(r1).isNetwork.hasStatusCode(200).hasBody("abc")
    awaitFor(ThrowingRunnable {
      val r2 = send(request)
      assertThat(r2).isNotNetwork.isNotCached.hasStatusCode(504)
    })
  }

  @Test
  @Throws(Exception::class)
  fun shouldNotCacheWhenFiltered() {
    //given
    val cacheableUri = resolve(path())
    val notCacheableUri = cacheableUri.resolve("/no-cache")
    val cache = Cache.newInMemoryCacheBuilder()
      .requestFilter { r: HttpRequest -> r.uri() == cacheableUri }
      .build()
    client = ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock())
    stubFor(
      get(urlEqualTo(notCacheableUri.path))
        .willReturn(
          WireMock.ok()
            .withHeader(Headers.HEADER_CACHE_CONTROL, "max-age=512")
            .withBody("abc")
        )
    )
    val request = HttpRequest.newBuilder().uri(notCacheableUri).build()

    //when
    val r1 = send(request)
    val r2 = send(request)

    //then
    assertThat(r1).isNetwork
    assertThat(r2).isNetwork
    assertNull(cache.get(request))
  }
}