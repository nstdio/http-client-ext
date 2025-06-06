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

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.http.HttpHeaders
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class CacheControlTest {
  @ParameterizedTest
  @ValueSource(strings = ["max-age=92233720368547758070,max-stale=92233720368547758070,min-fresh=92233720368547758070", "max-age= ,max-stale=,min-fresh=abc"])
  fun shouldNotFailWhenLongOverflow(value: String) {
    //given
    val httpHeaders = HttpHeaders.of(mapOf(Headers.HEADER_CACHE_CONTROL to listOf(value))) { _, _ -> true }

    //when
    val actual = CacheControl.of(httpHeaders)

    //then
    actual.toString().shouldBeEmpty()
  }

  @ParameterizedTest
  @ValueSource(strings = ["max-age=32,max-stale=64,min-fresh=128,stale-if-error=256,stale-while-revalidate=512", "max-age=\"32\" ,max-stale=\"64\",min-fresh=\"128\",stale-if-error=\"256\",stale-while-revalidate=\"512\""])
  fun shouldParseValues(value: String) {
    //given
    val httpHeaders = HttpHeaders.of(mapOf(Headers.HEADER_CACHE_CONTROL to listOf(value))) { _, _ -> true }

    //when
    val actual = CacheControl.of(httpHeaders)

    //then
    actual.maxAge() shouldBe 32
    actual.maxStale() shouldBe 64
    actual.minFresh() shouldBe 128
    actual.staleIfError() shouldBe 256
    actual.staleWhileRevalidate() shouldBe 512
    actual.staleWhileRevalidate(MILLISECONDS) shouldBe 512000
    
    actual.toString() shouldBe "max-age=32, max-stale=64, min-fresh=128, stale-if-error=256, stale-while-revalidate=512"
  }

  @Test
  fun shouldParseAndRoundRobin() {
    //given
    val minFresh = 5
    val maxStale = 6
    val maxAge = 7
    val staleIfError = 8
    val staleWhileRevalidate = 9
    val cc = CacheControl
      .builder()
      .minFresh(minFresh.toLong())
      .maxStale(maxStale.toLong())
      .maxAge(maxAge.toLong())
      .staleIfError(staleIfError.toLong())
      .staleWhileRevalidate(staleWhileRevalidate.toLong())
      .noCache()
      .noTransform()
      .onlyIfCached()
      .noStore()
      .mustUnderstand()
      .mustRevalidate()
      .immutable()
      .build()
    val expected = """
      no-cache, no-store, must-revalidate, no-transform, immutable, only-if-cached, must-understand,
      max-age=$maxAge, max-stale=$maxStale, min-fresh=$minFresh, stale-if-error=$staleIfError, stale-while-revalidate=$staleWhileRevalidate
    """.trimIndent().replace("\n", " ")

    //when + then
    cc.toString() shouldBe expected
    assertThat(CacheControl.parse(expected))
      .usingRecursiveComparison()
      .isEqualTo(cc)
  }

  @Test
  fun `Should support must-understand directive`() {
    //given
    val value = "must-understand, no-store, max-age=86400"

    //when
    val actual = CacheControl.parse(value)

    //then
    actual.noStore().shouldBeFalse()
    actual.mustUnderstand().shouldBeTrue()
    actual.maxAge().shouldBe(86400)
  }
}