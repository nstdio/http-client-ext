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

import static io.github.nstdio.http.ext.Headers.ALLOW_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class HeadersTest {
    static Object[][] parseDateData() {
        return new Object[][]{
                new Object[]{"Sun, 06 Nov 1994 08:49:37 GMT", Instant.parse("1994-11-06T08:49:37Z")},
                new Object[]{"Sunday, 06-Nov-94 08:49:37 GMT", Instant.parse("1994-11-06T08:49:37Z")},
                new Object[]{"Sun Nov  6 08:49:37 1994", Instant.parse("1994-11-06T08:49:37Z")},
                new Object[]{"invalid text", null}
        };
    }

    static Object[][] varyHeadersData() {
        return new Object[][]{
                new Object[]{
                        HttpHeaders.of(Map.of("Accept", List.of("*/*"), "User-Agent", List.of("mobile")), ALLOW_ALL),
                        HttpHeaders.of(Map.of("Vary", List.of("Accept, User-Agent")), ALLOW_ALL),
                        HttpHeaders.of(Map.of("Accept", List.of("*/*"), "User-Agent", List.of("mobile")), ALLOW_ALL),
                },
                new Object[]{
                        HttpHeaders.of(Map.of("Accept", List.of("*/*"), "User-Agent", List.of("mobile")), ALLOW_ALL),
                        HttpHeaders.of(Map.of("Vary", List.of("Accept", "User-Agent")), ALLOW_ALL),
                        HttpHeaders.of(Map.of("Accept", List.of("*/*"), "User-Agent", List.of("mobile")), ALLOW_ALL),
                },
                new Object[]{
                        HttpHeaders.of(Map.of("Accept", List.of("*/*"), "User-Agent", List.of("mobile")), ALLOW_ALL),
                        HttpHeaders.of(Map.of("Vary", List.of("")), ALLOW_ALL),
                        HttpHeaders.of(Map.of(), ALLOW_ALL),
                },
                new Object[]{
                        HttpHeaders.of(Map.of("User-Agent", List.of("mobile")), ALLOW_ALL),
                        HttpHeaders.of(Map.of("Vary", List.of("Accept")), ALLOW_ALL),
                        HttpHeaders.of(Map.of(), ALLOW_ALL),
                },
                new Object[]{
                        HttpHeaders.of(Map.of("Accept", List.of("*/*")), ALLOW_ALL),
                        HttpHeaders.of(Map.of(), ALLOW_ALL),
                        HttpHeaders.of(Map.of(), ALLOW_ALL),
                },
        };
    }

    static Stream<Arguments> effectiveUriHeadersData() {
        URI responseUri = URI.create("https://example.com/index.html");

        return Stream.of(
                arguments(HttpHeaders.of(Map.of("Location", List.of("/path")), ALLOW_ALL), "Location", responseUri,
                        List.of(URI.create("https://example.com/path")))
        );
    }

    static Stream<Arguments> effectiveUriData() {
        return Stream.of(
                arguments("/path", URI.create("https://example.com/index.html"), URI.create("https://example.com/path")),
                arguments("/path", URI.create("http://example.com/index.html"), URI.create("http://example.com/path")),
                arguments("/path", URI.create("http://example.com/index?query=1"), URI.create("http://example.com/path")),
                arguments("/path", URI.create("http://example.com:8080/index?query=1"), URI.create("http://example.com:8080/path")),
                arguments("/path", URI.create("http://127.0.0.1:8090/index?query=1"), URI.create("http://127.0.0.1:8090/path")),
                arguments("http://example.com/path", URI.create("https://example.com/index?query=1"), URI.create("http://example.com/path")),
                arguments("http://other-domain.com/path", URI.create("https://example.com/index?query=1"), URI.create("http://other-domain.com/path")),
                arguments("adsbhj", URI.create("https://example.com"), null),
                arguments("", URI.create("https://example.com"), null),
                arguments("  ", URI.create("https://example.com"), null),
                arguments(null, URI.create("https://example.com"), null)
        );
    }

    static List<HttpHeaders> varyAllPositiveData() {
        return List.of(
                HttpHeaders.of(
                        Map.of("Vary", List.of("*")),
                        ALLOW_ALL
                ),
                HttpHeaders.of(
                        Map.of("Vary", List.of("Accept-Encoding", "*")),
                        ALLOW_ALL
                ),
                HttpHeaders.of(
                        Map.of("Vary", List.of("*, Accept-Encoding")),
                        ALLOW_ALL
                )
        );
    }

    static List<HttpHeaders> varyAllNegativeData() {
        return List.of(
                HttpHeaders.of(
                        Map.of("Vary", List.of("Accept-Encoding, User-Agent")),
                        ALLOW_ALL
                ),
                HttpHeaders.of(
                        Map.of("Vary", List.of("Accept-Encoding, User-Agent", "Accept")),
                        ALLOW_ALL
                ),
                HttpHeaders.of(
                        Map.of("Content-Type", List.of("*")),
                        ALLOW_ALL
                ),
                HttpHeaders.of(
                        Map.of("Vary", List.of("Accept-Encoding*")),
                        ALLOW_ALL
                )
        );
    }

    @ParameterizedTest
    @MethodSource("varyAllPositiveData")
    void varyAllPositive(HttpHeaders headers) {
        //when
        var actual = Headers.isVaryAll(headers);

        //then
        assertThat(actual).isTrue();
    }

    @ParameterizedTest
    @MethodSource("varyAllNegativeData")
    void varyAllNegative(HttpHeaders headers) {
        //when
        var actual = Headers.isVaryAll(headers);

        //then
        assertThat(actual).isFalse();
    }

    @ParameterizedTest
    @MethodSource("parseDateData")
    void parseDate(String date, Instant expected) {
        //when
        var actual = Headers.parseInstant(date);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("effectiveUriHeadersData")
    void effectiveUriHeaders(HttpHeaders headers, String headerName, URI responseUri, List<URI> expected) {
        //when
        List<URI> uris = Headers.effectiveUri(headers, headerName, responseUri);

        //then
        assertThat(uris).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("effectiveUriData")
    void effectiveUri(String s, URI responseUri, URI expected) {
        //when
        URI actual = Headers.effectiveUri(s, responseUri);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("varyHeadersData")
    void varyHeaders(HttpHeaders request, HttpHeaders response, HttpHeaders expected) {
        //when
        HttpHeaders actual = Headers.varyHeaders(request, response);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void toRFC1123() {
        //given
        Instant instant = Instant.ofEpochMilli(0);

        //when
        String actual = Headers.toRFC1123(instant);

        //then
        assertThat(actual).isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT");
    }
}