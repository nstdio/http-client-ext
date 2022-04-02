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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.http.HttpHeaders
import java.time.Instant
import java.util.Map
import java.util.stream.Stream

internal class HeadersTest {
  @ParameterizedTest
  @MethodSource("varyAllPositiveData")
  fun varyAllPositive(headers: HttpHeaders) {
    //when
    val actual = Headers.isVaryAll(headers)

    //then
    assertThat(actual).isTrue
  }

  @ParameterizedTest
  @MethodSource("varyAllNegativeData")
  fun varyAllNegative(headers: HttpHeaders) {
    //when
    val actual = Headers.isVaryAll(headers)

    //then
    assertThat(actual).isFalse
  }

  @ParameterizedTest
  @MethodSource("parseDateData")
  fun parseDate(date: String?, expected: Instant?) {
    //when
    val actual = Headers.parseInstant(date)

    //then
    assertThat(actual).isEqualTo(expected)
  }

  @ParameterizedTest
  @MethodSource("effectiveUriHeadersData")
  fun effectiveUriHeaders(headers: HttpHeaders?, headerName: String?, responseUri: URI?, expected: List<URI?>?) {
    //when
    val uris = Headers.effectiveUri(headers, headerName, responseUri)

    //then
    assertThat(uris).isEqualTo(expected)
  }

  @ParameterizedTest
  @MethodSource("effectiveUriData")
  fun effectiveUri(s: String?, responseUri: URI?, expected: URI?) {
    //when
    val actual = Headers.effectiveUri(s, responseUri)

    //then
    assertThat(actual).isEqualTo(expected)
  }

  @ParameterizedTest
  @MethodSource("varyHeadersData")
  fun varyHeaders(request: HttpHeaders?, response: HttpHeaders?, expected: HttpHeaders?) {
    //when
    val actual = Headers.varyHeaders(request, response)

    //then
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun toRFC1123() {
    //given
    val instant = Instant.ofEpochMilli(0)

    //when
    val actual = Headers.toRFC1123(instant)

    //then
    assertThat(actual).isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT")
  }

  @ParameterizedTest
  @MethodSource("splitCommaData")
  fun splitComma(input: String, expected: List<String>) {
    //when
    val actual = Headers.splitComma(input)

    //then
    assertThat(actual).isEqualTo(expected)
  }

  companion object {
    @JvmStatic
    fun parseDateData(): Array<Array<Any?>> {
      return arrayOf(
        arrayOf("Sun, 06 Nov 1994 08:49:37 GMT", Instant.parse("1994-11-06T08:49:37Z")),
        arrayOf("Sunday, 06-Nov-94 08:49:37 GMT", Instant.parse("1994-11-06T08:49:37Z")),
        arrayOf("Sun Nov  6 08:49:37 1994", Instant.parse("1994-11-06T08:49:37Z")),
        arrayOf("invalid text", null)
      )
    }

    @JvmStatic
    fun splitCommaData(): Array<Array<Any?>> {
      return arrayOf(
        arrayOf("  a, b, c   ", listOf("a", "b", "c")),
        arrayOf("a,b,c", listOf("a", "b", "c")),
        arrayOf(",,,,,,", listOf<String>()),
        arrayOf("abc", listOf("abc")),
      )
    }

    @JvmStatic
    fun varyHeadersData(): Array<Array<Any>> {
      return arrayOf(
        arrayOf(
          HttpHeaders.of(
            Map.of(
              "Accept",
              listOf("*/*"),
              "User-Agent",
              listOf("mobile")
            ), Headers.ALLOW_ALL
          ),
          HttpHeaders.of(Map.of("Vary", listOf("Accept, User-Agent")), Headers.ALLOW_ALL),
          HttpHeaders.of(
            Map.of(
              "Accept",
              listOf("*/*"),
              "User-Agent",
              listOf("mobile")
            ), Headers.ALLOW_ALL
          )
        ), arrayOf(
          HttpHeaders.of(
            Map.of(
              "Accept",
              listOf("*/*"),
              "User-Agent",
              listOf("mobile")
            ), Headers.ALLOW_ALL
          ),
          HttpHeaders.of(Map.of("Vary", listOf("Accept", "User-Agent")), Headers.ALLOW_ALL),
          HttpHeaders.of(
            Map.of(
              "Accept",
              listOf("*/*"),
              "User-Agent",
              listOf("mobile")
            ), Headers.ALLOW_ALL
          )
        ), arrayOf(
          HttpHeaders.of(
            Map.of(
              "Accept",
              listOf("*/*"),
              "User-Agent",
              listOf("mobile")
            ), Headers.ALLOW_ALL
          ),
          HttpHeaders.of(Map.of("Vary", listOf("")), Headers.ALLOW_ALL),
          HttpHeaders.of(Map.of(), Headers.ALLOW_ALL)
        ), arrayOf(
          HttpHeaders.of(Map.of("User-Agent", listOf("mobile")), Headers.ALLOW_ALL),
          HttpHeaders.of(Map.of("Vary", listOf("Accept")), Headers.ALLOW_ALL),
          HttpHeaders.of(Map.of(), Headers.ALLOW_ALL)
        ), arrayOf(
          HttpHeaders.of(Map.of("Accept", listOf("*/*")), Headers.ALLOW_ALL),
          HttpHeaders.of(Map.of(), Headers.ALLOW_ALL),
          HttpHeaders.of(Map.of(), Headers.ALLOW_ALL)
        )
      )
    }

    @JvmStatic
    fun effectiveUriHeadersData(): Stream<Arguments> {
      val responseUri = URI.create("https://example.com/index.html")
      return Stream.of(
        Arguments.arguments(
          HttpHeaders.of(Map.of("Location", listOf("/path")), Headers.ALLOW_ALL),
          "Location",
          responseUri,
          listOf(URI.create("https://example.com/path"))
        )
      )
    }

    @JvmStatic
    fun effectiveUriData(): Stream<Arguments> {
      return Stream.of(
        Arguments.arguments(
          "/path",
          URI.create("https://example.com/index.html"),
          URI.create("https://example.com/path")
        ),
        Arguments.arguments(
          "/path",
          URI.create("http://example.com/index.html"),
          URI.create("http://example.com/path")
        ),
        Arguments.arguments(
          "/path",
          URI.create("http://example.com/index?query=1"),
          URI.create("http://example.com/path")
        ),
        Arguments.arguments(
          "/path",
          URI.create("http://example.com:8080/index?query=1"),
          URI.create("http://example.com:8080/path")
        ),
        Arguments.arguments(
          "/path",
          URI.create("http://127.0.0.1:8090/index?query=1"),
          URI.create("http://127.0.0.1:8090/path")
        ),
        Arguments.arguments(
          "http://example.com/path",
          URI.create("https://example.com/index?query=1"),
          URI.create("http://example.com/path")
        ),
        Arguments.arguments(
          "http://other-domain.com/path",
          URI.create("https://example.com/index?query=1"),
          URI.create("http://other-domain.com/path")
        ),
        Arguments.arguments("adsbhj", URI.create("https://example.com"), null),
        Arguments.arguments("", URI.create("https://example.com"), null),
        Arguments.arguments("  ", URI.create("https://example.com"), null),
        Arguments.arguments(null, URI.create("https://example.com"), null)
      )
    }

    @JvmStatic
    fun varyAllPositiveData(): List<HttpHeaders> {
      return listOf(
        HttpHeaders.of(
          Map.of("Vary", listOf("*")),
          Headers.ALLOW_ALL
        ),
        HttpHeaders.of(
          Map.of("Vary", listOf("Accept-Encoding", "*")),
          Headers.ALLOW_ALL
        ),
        HttpHeaders.of(
          Map.of("Vary", listOf("*, Accept-Encoding")),
          Headers.ALLOW_ALL
        )
      )
    }

    @JvmStatic
    fun varyAllNegativeData(): List<HttpHeaders> {
      return listOf(
        HttpHeaders.of(
          Map.of("Vary", listOf("Accept-Encoding, User-Agent")),
          Headers.ALLOW_ALL
        ),
        HttpHeaders.of(
          Map.of("Vary", listOf("Accept-Encoding, User-Agent", "Accept")),
          Headers.ALLOW_ALL
        ),
        HttpHeaders.of(
          Map.of("Content-Type", listOf("*")),
          Headers.ALLOW_ALL
        ),
        HttpHeaders.of(
          Map.of("Vary", listOf("Accept-Encoding*")),
          Headers.ALLOW_ALL
        )
      )
    }
  }
}