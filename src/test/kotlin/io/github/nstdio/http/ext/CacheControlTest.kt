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
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package io.github.nstdio.http.ext

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.http.HttpHeaders
import java.util.Map

internal class CacheControlTest {
    @ParameterizedTest
    @ValueSource(strings = ["max-age=92233720368547758070,max-stale=92233720368547758070,min-fresh=92233720368547758070", "max-age= ,max-stale=,min-fresh=abc"])
    fun shouldNotFailWhenLongOverflow(value: String) {
        //given
        val httpHeaders =
            HttpHeaders.of(Map.of(Headers.HEADER_CACHE_CONTROL, listOf(value))) { _: String?, _: String? -> true }

        //when
        val actual = CacheControl.of(httpHeaders)

        //then
        Assertions.assertThat(actual).hasToString("")
    }

    @ParameterizedTest
    @ValueSource(strings = ["max-age=32,max-stale=64,min-fresh=128,stale-if-error=256,stale-while-revalidate=512", "max-age=\"32\" ,max-stale=\"64\",min-fresh=\"128\",stale-if-error=\"256\",stale-while-revalidate=\"512\""])
    fun shouldParseValues(value: String) {
        //given
        val httpHeaders =
            HttpHeaders.of(Map.of(Headers.HEADER_CACHE_CONTROL, listOf(value)), ) { _: String?, _: String? -> true }

        //when
        val actual = CacheControl.of(httpHeaders)

        //then
        Assertions.assertThat(actual.maxAge()).isEqualTo(32)
        Assertions.assertThat(actual.maxStale()).isEqualTo(64)
        Assertions.assertThat(actual.minFresh()).isEqualTo(128)
        Assertions.assertThat(actual.staleIfError()).isEqualTo(256)
        Assertions.assertThat(actual.staleWhileRevalidate()).isEqualTo(512)
        Assertions.assertThat(actual)
            .hasToString("max-age=32, max-stale=64, min-fresh=128, stale-if-error=256, stale-while-revalidate=512")
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
            .mustRevalidate()
            .immutable()
            .build()
        val expected = String.format(
            "no-cache, no-store, must-revalidate, no-transform, immutable, only-if-cached, max-age=%d, max-stale=%d, min-fresh=%d, stale-if-error=%d, stale-while-revalidate=%d",
            maxAge,
            maxStale,
            minFresh,
            staleIfError,
            staleWhileRevalidate
        )

        //when + then
        Assertions.assertThat(cc).hasToString(expected)
        Assertions.assertThat(CacheControl.parse(expected))
            .usingRecursiveComparison()
            .isEqualTo(cc)
    }
}