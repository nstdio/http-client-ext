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

package io.github.nstdio.http.ext;

import static io.github.nstdio.http.ext.Helpers.responseInfo;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.nstdio.http.ext.InMemoryCache.InMemoryCacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class InMemoryCacheTest {
    public static final Clock SYSTEM_CLOCK = Clock.systemDefaultZone();
    private InMemoryCache cache;

    static InMemoryCacheEntry cacheEntry(Map<String, String> headers, HttpRequest r) {
        return cacheEntry(Long.MAX_VALUE, 0, headers, r);
    }

    static InMemoryCacheEntry cacheEntry(long maxAge, long responseTimeMs, Map<String, String> headers, HttpRequest r) {
        return new InMemoryCacheEntry(new byte[0], new CacheEntryMetadata(0, responseTimeMs, responseInfo(headers), r, Clock.systemDefaultZone()));
    }

    static List<URI> uris(int size) {
        var baseUri = URI.create("http://example.com/");
        return IntStream.rangeClosed(1, size)
                .mapToObj(String::valueOf)
                .map(baseUri::resolve)
                .collect(toList());
    }

    @BeforeEach
    void setUp() {
        cache = new InMemoryCache(512, -1);
    }

    @Test
    @DisplayName("Should be able to cache with Vary header")
    void simple() {
        //given
        var uris = uris(4);
        var acceptHeaders = List.of("text/plain", "application/json", "text/csv", "text/*");
        int maxItems = uris.size() * acceptHeaders.size() - 1;
        cache = new InMemoryCache(maxItems, -1);

        for (URI uri : uris) {
            for (String acceptHeader : acceptHeaders) {
                var request = HttpRequest.newBuilder(uri)
                        .headers("Accept", acceptHeader)
                        .build();
                var responseHeaders = Map.of(
                        "Content-Type", acceptHeader,
                        "Content-Length", "0",
                        "Vary", "Accept"
                );
                var e = cacheEntry(responseHeaders, request);

                //when
                cache.put(request, e);

                //then
                assertThat(cache.get(request)).isSameAs(e);
            }
        }

        assertThat(cache.mapSize()).isEqualTo(uris.size());
        assertThat(cache.multimapSize()).isEqualTo(maxItems);
    }

    @Test
    void shouldWorkAsLRU() {
        //given
        int maxItems = 2;
        cache = new InMemoryCache(maxItems, -1);

        var requests = uris(10)
                .stream()
                .map(uri -> HttpRequest.newBuilder(uri).build())
                .collect(toList());
        var expected = requests.subList(requests.size() - maxItems, requests.size());

        for (var request : requests) {
            var e = cacheEntry(Map.of(), request);

            cache.put(request, e);

            //then
            assertThat(cache.get(request)).isSameAs(e);
        }

        for (HttpRequest r : expected) {
            assertThat(cache.get(r)).isNotNull();
        }
    }

    @Test
    void shouldRespectSizeConstraints() {
        //given
        var maxBytes = 16;
        cache = new InMemoryCache(512, maxBytes);

        var bodySize = 4;
        var requests = uris(6)
                .stream()
                .map(uri -> HttpRequest.newBuilder(uri).build())
                .collect(toList());
        var expectedEntriesCount = maxBytes / bodySize;
        var notInCache = requests.subList(0, requests.size() - expectedEntriesCount);
        var inCache = requests.subList(requests.size() - expectedEntriesCount, requests.size());

        //when
        for (HttpRequest request : requests) {
            var metadata = metadata(request, responseInfo(Map.of()));
            var e = new InMemoryCacheEntry(new byte[bodySize], metadata);

            cache.put(request, e);
        }

        //then
        assertThat(cache.bytes()).isLessThanOrEqualTo(maxBytes);
        assertThat(cache.multimapSize())
                .isLessThanOrEqualTo(expectedEntriesCount);
        assertThat(notInCache).isNotEmpty().allMatch(r -> cache.get(r) == null);
        assertThat(inCache).isNotEmpty().allMatch(r -> cache.get(r) != null);
    }

    @Test
    void shouldEvictMultipleEldestToPutBigOne() {
        //given
        var maxBytes = 16;
        var bodySize = 8;
        cache = new InMemoryCache(512, maxBytes);
        var requests = uris(2)
                .stream()
                .map(uri -> HttpRequest.newBuilder(uri).build())
                .collect(toList());
        var r1 = HttpRequest.newBuilder(URI.create("https://testurl.com")).build();
        var e1 = new InMemoryCacheEntry(new byte[maxBytes], metadata(r1));

        //when
        for (HttpRequest r : requests) {
            cache.put(r, new InMemoryCacheEntry(new byte[bodySize], metadata(r)));
        }
        cache.put(r1, e1);

        //then
        assertThat(cache.bytes()).isEqualTo(maxBytes);
        assertThat(cache.multimapSize()).isEqualTo(1);
        assertThat(requests).allMatch(r -> cache.get(r) == null);
        assertThat(cache.get(r1)).isSameAs(e1);
    }

    @Test
    void shouldNotEvictWhenNewOneExceedsLimit() {
        //given
        var maxBytes = 16;
        var bodySize = 4;
        cache = new InMemoryCache(512, maxBytes);
        var requests = uris(4)
                .stream()
                .map(uri -> HttpRequest.newBuilder(uri).build())
                .collect(toList());
        var r1 = HttpRequest.newBuilder(URI.create("https://testurl.com")).build();
        var e1 = new InMemoryCacheEntry(new byte[maxBytes + 1], metadata(r1));

        //when
        for (HttpRequest r : requests) {
            cache.put(r, new InMemoryCacheEntry(new byte[bodySize], metadata(r)));
        }
        cache.put(r1, e1);

        //then
        assertThat(cache.bytes()).isEqualTo(maxBytes);
        assertThat(cache.multimapSize()).isEqualTo(4);
        assertThat(requests).allMatch(r -> cache.get(r) != null);
        assertThat(cache.get(r1)).isNull();

        cache.evictAll();
        assertThat(cache.bytes()).isZero();
        assertThat(cache.multimapSize()).isZero();
        assertThat(cache.mapSize()).isZero();
    }

    private CacheEntryMetadata metadata(HttpRequest request, HttpResponse.ResponseInfo info) {
        return new CacheEntryMetadata(0, 0, info, request, SYSTEM_CLOCK);
    }

    private CacheEntryMetadata metadata(HttpRequest request) {
        return metadata(request, responseInfo(Map.of()));
    }
}