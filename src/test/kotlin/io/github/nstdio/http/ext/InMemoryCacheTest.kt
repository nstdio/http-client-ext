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

import io.github.nstdio.http.ext.InMemoryCache.InMemoryCacheEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.ResponseInfo
import java.time.Clock
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

internal class InMemoryCacheTest {
    private var cache: InMemoryCache? = null

    @BeforeEach
    fun setUp() {
        cache = InMemoryCache(512, -1)
    }

    @Test
    @DisplayName("Should be able to cache with Vary header")
    fun simple() {
        //given
        val uris = uris(4)
        val acceptHeaders = listOf("text/plain", "application/json", "text/csv", "text/*")
        val maxItems = uris.size * acceptHeaders.size - 1
        cache = InMemoryCache(maxItems, -1)
        for (uri in uris) {
            for (acceptHeader in acceptHeaders) {
                val request = HttpRequest.newBuilder(uri)
                    .headers("Accept", acceptHeader)
                    .build()
                val responseHeaders = java.util.Map.of(
                    "Content-Type", acceptHeader,
                    "Content-Length", "0",
                    "Vary", "Accept"
                )
                val e = cacheEntry(responseHeaders, request)

                //when
                cache!!.put(request, e)

                //then
                assertThat(cache!![request]).isSameAs(e)
            }
        }
        assertThat(cache!!.mapSize()).isEqualTo(uris.size)
        assertThat(cache!!.multimapSize()).isEqualTo(maxItems)
    }

    @Test
    fun shouldWorkAsLRU() {
        //given
        val maxItems = 2
        cache = InMemoryCache(maxItems, -1)
        val requests = uris(10)
            .stream()
            .map { uri: URI? -> HttpRequest.newBuilder(uri).build() }
            .collect(toList())
        val expected = requests.subList(requests.size - maxItems, requests.size)
        for (request in requests) {
            val e = cacheEntry(java.util.Map.of(), request)
            cache!!.put(request, e)

            //then
            assertThat(cache!![request]).isSameAs(e)
        }
        for (r in expected) {
            assertThat(cache!![r]).isNotNull
        }
    }

    @Test
    fun shouldRespectSizeConstraints() {
        //given
        val maxBytes = 16
        cache = InMemoryCache(512, maxBytes.toLong())
        val bodySize = 4
        val requests = uris(6)
            .stream()
            .map { uri: URI? -> HttpRequest.newBuilder(uri).build() }
            .collect(toList())
        val expectedEntriesCount = maxBytes / bodySize
        val notInCache = requests.subList(0, requests.size - expectedEntriesCount)
        val inCache = requests.subList(requests.size - expectedEntriesCount, requests.size)

        //when
        for (request in requests) {
            val metadata = metadata(request, Helpers.responseInfo(java.util.Map.of()))
            val e = InMemoryCacheEntry(ByteArray(bodySize), metadata)
            cache!!.put(request, e)
        }

        //then
        assertThat(cache!!.bytes()).isLessThanOrEqualTo(maxBytes.toLong())
        assertThat(cache!!.multimapSize())
            .isLessThanOrEqualTo(expectedEntriesCount)
        assertThat(notInCache).isNotEmpty.allMatch { r: HttpRequest? -> cache!![r] == null }
        assertThat(inCache).isNotEmpty.allMatch { r: HttpRequest? -> cache!![r] != null }
    }

    @Test
    fun shouldEvictMultipleEldestToPutBigOne() {
        //given
        val maxBytes = 16
        val bodySize = 8
        cache = InMemoryCache(512, maxBytes.toLong())
        val requests = uris(2)
            .stream()
            .map { uri: URI? -> HttpRequest.newBuilder(uri).build() }
            .collect(toList())
        val r1 = HttpRequest.newBuilder(URI.create("https://testurl.com")).build()
        val e1 = InMemoryCacheEntry(ByteArray(maxBytes), metadata(r1))

        //when
        for (r in requests) {
            cache!!.put(r, InMemoryCacheEntry(ByteArray(bodySize), metadata(r)))
        }
        cache!!.put(r1, e1)

        //then
        assertThat(cache!!.bytes()).isEqualTo(maxBytes.toLong())
        assertThat(cache!!.multimapSize()).isEqualTo(1)
        assertThat(requests).allMatch { r: HttpRequest? -> cache!![r] == null }
        assertThat(cache!![r1]).isSameAs(e1)
    }

    @Test
    fun shouldNotEvictWhenNewOneExceedsLimit() {
        //given
        val maxBytes = 16
        val bodySize = 4
        cache = InMemoryCache(512, maxBytes.toLong())
        val requests = uris(4)
            .stream()
            .map { uri: URI? -> HttpRequest.newBuilder(uri).build() }
            .collect(toList())
        val r1 = HttpRequest.newBuilder(URI.create("https://testurl.com")).build()
        val e1 = InMemoryCacheEntry(ByteArray(maxBytes + 1), metadata(r1))

        //when
        for (r in requests) {
            cache!!.put(r, InMemoryCacheEntry(ByteArray(bodySize), metadata(r)))
        }
        cache!!.put(r1, e1)

        //then
        assertThat(cache!!.bytes()).isEqualTo(maxBytes.toLong())
        assertThat(cache!!.multimapSize()).isEqualTo(4)
        assertThat(requests).allMatch { r: HttpRequest? -> cache!![r] != null }
        assertThat(cache!![r1]).isNull()
        cache!!.evictAll()
        assertThat(cache!!.bytes()).isZero
        assertThat(cache!!.multimapSize()).isZero
        assertThat(cache!!.mapSize()).isZero
    }

    private fun metadata(
        request: HttpRequest,
        info: ResponseInfo = Helpers.responseInfo(java.util.Map.of())
    ): CacheEntryMetadata {
        return CacheEntryMetadata(0, 0, info, request, SYSTEM_CLOCK)
    }

    companion object {
        private val SYSTEM_CLOCK = Clock.systemDefaultZone()
        fun cacheEntry(headers: Map<String, String>?, r: HttpRequest?): InMemoryCacheEntry {
            return cacheEntry(0, headers, r)
        }

        private fun cacheEntry(
            responseTimeMs: Long,
            headers: Map<String, String>?,
            r: HttpRequest?
        ): InMemoryCacheEntry {
            return InMemoryCacheEntry(
                ByteArray(0),
                CacheEntryMetadata(0, responseTimeMs, Helpers.responseInfo(headers), r, Clock.systemDefaultZone())
            )
        }

        fun uris(size: Int): List<URI> {
            val baseUri = URI.create("http://example.com/")
            return IntStream.rangeClosed(1, size)
                .mapToObj { it.toString() }
                .map { str: String? -> baseUri.resolve(str) }
                .collect(toList())
        }
    }
}