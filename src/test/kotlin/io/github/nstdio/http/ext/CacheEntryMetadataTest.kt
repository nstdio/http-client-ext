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
package io.github.nstdio.http.ext

import io.github.nstdio.http.ext.Headers.HEADER_WARNING
import io.github.nstdio.http.ext.Helpers.responseInfo
import io.github.nstdio.http.ext.Helpers.responseInfo0
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.longs.shouldBeNegative
import io.kotest.matchers.should
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class CacheEntryMetadataTest {
  @Test
  fun varyHeaders() {
    val headers = mutableMapOf(
      "Vary" to "Accept, Accept-Encoding, User-Agent"
    )
    val r = HttpRequest.newBuilder(URI.create("https://example.com"))
      .headers(
        "Accept", "text/plain",
        "Accept-Encoding", "gzip",
        "User-Agent", "Java/11",
        "Accept-Language", "en-EN"
      )
      .build()
    val m = CacheEntryMetadata(0, 0, responseInfo(headers), r, Clock.systemDefaultZone())

    //when
    val actual = m.varyHeaders()

    //then
    Assertions.assertThat(actual)
      .hasHeaderWithValues("Accept", "text/plain")
      .hasHeaderWithValues("Accept-Encoding", "gzip")
      .hasHeaderWithValues("User-Agent", "Java/11")
  }

  @Test
  fun `Should remove warning header from response`() {
    //given
    val request = HttpRequest.newBuilder(URI.create("https://example.com")).build()
    val responseInfo = responseInfo0(
      mapOf(
        HEADER_WARNING to listOf(
          "112 - \"cache down\" \"Wed, 21 Oct 2015 07:28:00 GMT\"",
          "110 anderson/1.3.37 \"Response is stale\"",
          "299 - \"Deprecated\""
        )
      )
    )
    val metadata = CacheEntryMetadata(0, 0, responseInfo, request, Clock.systemDefaultZone())
    val responseHeaders = HttpHeadersBuilder()
      .add("X-A", "B")
      .build()

    //when
    metadata.update(responseHeaders, 0, 0)

    //then
    metadata.response().headers().should {
      it.allValues(HEADER_WARNING).shouldContainOnly("299 - \"Deprecated\"")
      it.allValues("X-A").shouldContainOnly("B")
    }
  }

  @Test
  fun shouldGenerateHeuristicExpirationWarning() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val baseInstant = Instant.ofEpochSecond(0)
    val clock = FixedRateTickClock(baseInstant, ZoneOffset.UTC, tickDuration)
    // Creating Last-Modified just about to exceed 24 hour limit
    val lastModified = baseInstant.minus(241, ChronoUnit.HOURS)
    val expectedExpirationTime = baseInstant.plus(2, ChronoUnit.DAYS)
    val info = responseInfo(
      mutableMapOf(
        "Last-Modified" to Headers.toRFC1123(lastModified),
        "Date" to Headers.toRFC1123(Instant.ofEpochSecond(0))
      )
    )
    val request1 = HttpRequest.newBuilder().uri(URI.create("https://www.example.com")).build()
    val request2 = HttpRequest.newBuilder().uri(URI.create("https://www.example.com/path?query")).build()
    val metadata1 = CacheEntryMetadata(0, 1000, info, request1, clock)
    val metadata2 = CacheEntryMetadata(0, 1000, info, request2, clock)
    tickUntil(clock, expectedExpirationTime)
    val executor = Executors.newFixedThreadPool(12)

    //when
    for (i in 0 until (1 shl 16)) {
      executor.submit { metadata1.updateWarnings() }
    }
    executor.shutdown()
    executor.awaitTermination(3, TimeUnit.SECONDS)

    //then
    metadata2.maxAge().shouldBeNegative()
    metadata2.isApplicable.shouldBeFalse()
    Assertions.assertThat(metadata1.response().headers())
      .hasHeaderWithOnlyValue("Warning", "113 - \"Heuristic Expiration\"")
  }

  private fun tickUntil(clock: FixedRateTickClock, expectedExpirationTime: Instant) {
    @Suppress("ControlFlowWithEmptyBody")
    while (clock.instant().isBefore(expectedExpirationTime)) {
    }
  }

  /**
   * https://httpwg.org/specs/rfc7234.html#rfc.section.5.2.2.1
   */
  @Test
  fun shouldRespectMustRevalidateResponseDirective() {
    //given
    val baseClock = Clock.systemUTC()
    val dateHeader = baseClock.instant()
    val clock = FixedRateTickClock.of(baseClock, Duration.ofSeconds(1))
    val info = responseInfo(
      mutableMapOf(
        "Cache-Control" to "max-age=1,must-revalidate",
        "Date" to Headers.toRFC1123(dateHeader)
      )
    )
    val request = HttpRequest.newBuilder().uri(URI.create("https://www.example.com")).build()
    val requestTimeMs = dateHeader.minusMillis(50).toEpochMilli()
    val responseTimeMs = dateHeader.plusMillis(50).toEpochMilli()
    val metadata = CacheEntryMetadata(requestTimeMs, responseTimeMs, info, request, clock)

    //when + then
    assertFalse(metadata.isFresh(CacheControl.parse("max-stale=1")))
  }

  @Test
  @Disabled("https://httpwg.org/specs/rfc7234.html#rfc.section.4.2.4")
  fun shouldAddStaleResponseWarning() {
  }

  @Nested
  internal inner class FreshnessTest {
    private val uri = URI.create("https://www.example.com")

    @Test
    fun shouldBeFresh() {
      //given
      val requestTimeMs: Long = 100
      val responseTimeMs: Long = 350
      val serverDate = responseTimeMs - 50
      val clock = Clock.fixed(Instant.ofEpochMilli(responseTimeMs + 5), ZoneId.systemDefault())
      val info = responseInfo(
        mutableMapOf(
          "Cache-Control" to "max-age=1",
          "Date" to Headers.toRFC1123(Instant.ofEpochMilli(serverDate))
        )
      )
      val request = HttpRequest.newBuilder().uri(uri).build()
      val e = CacheEntryMetadata(requestTimeMs, responseTimeMs, info, request, clock)
      //when + then
      e.isFresh(CacheControl.builder().build()).shouldBeTrue()
    }
  }
}