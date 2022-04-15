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
import java.util.Map
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class CacheEntryMetadataTest {
  @Test
  fun varyHeaders() {
    val headers = Map.of(
      "Vary", "Accept, Accept-Encoding, User-Agent"
    )
    val r = HttpRequest.newBuilder(URI.create("https://example.com"))
      .headers(
        "Accept", "text/plain",
        "Accept-Encoding", "gzip",
        "User-Agent", "Java/11",
        "Accept-Language", "en-EN"
      )
      .build()
    val m = CacheEntryMetadata(0, 0, Helpers.responseInfo(headers), r, Clock.systemDefaultZone())

    //when
    val actual = m.varyHeaders()

    //then
    Assertions.assertThat(actual)
      .hasHeaderWithValues("Accept", "text/plain")
      .hasHeaderWithValues("Accept-Encoding", "gzip")
      .hasHeaderWithValues("User-Agent", "Java/11")
  }

  @Test
  @Throws(Exception::class)
  fun shouldGenerateHeuristicExpirationWarning() {
    //given
    val tickDuration = Duration.ofSeconds(1)
    val baseInstant = Instant.ofEpochSecond(0)
    val clock = FixedRateTickClock(baseInstant, ZoneOffset.UTC, tickDuration)
    // Creating Last-Modified just about to exceed 24 hour limit
    val lastModified = baseInstant.minus(241, ChronoUnit.HOURS)
    val expectedExpirationTime = baseInstant.plus(2, ChronoUnit.DAYS)
    val info = Helpers.responseInfo(
      Map.of(
        "Last-Modified", Headers.toRFC1123(lastModified),
        "Date", Headers.toRFC1123(Instant.ofEpochSecond(0))
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
    org.junit.jupiter.api.Assertions.assertTrue(metadata2.maxAge() < 0)
    assertFalse(metadata2.isApplicable)
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
    val info = Helpers.responseInfo(
      Map.of(
        "Cache-Control", "max-age=1,must-revalidate",
        "Date", Headers.toRFC1123(dateHeader)
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
      val info = Helpers.responseInfo(
        Map.of(
          "Cache-Control", "max-age=1",
          "Date", Headers.toRFC1123(Instant.ofEpochMilli(serverDate))
        )
      )
      val request = HttpRequest.newBuilder().uri(uri).build()
      val e = CacheEntryMetadata(requestTimeMs, responseTimeMs, info, request, clock)
      //when + then
      org.junit.jupiter.api.Assertions.assertTrue(e.isFresh(CacheControl.builder().build()))
    }
  }
}