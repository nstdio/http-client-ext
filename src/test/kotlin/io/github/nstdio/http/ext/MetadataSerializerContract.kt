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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.nio.file.Path
import java.time.Clock
import java.time.Duration

internal interface MetadataSerializerContract {

  /**
   * The metadata serializer under the test.
   */
  fun serializer(): MetadataSerializer

  /**
   * The metadata serializer under the test.
   */
  fun tempDir(): Path

  @Test
  fun `Should return null when cannot read`(@TempDir dir: Path) {
    //given
    val file = dir.resolve("abc")
    val ser = serializer()

    //when
    val metadata = ser.read(file)

    //then
    assertNull(metadata)
  }

  @ParameterizedTest
  @MethodSource("metadata")
  fun `Should write and read`(metadata: CacheEntryMetadata) {
    //given
    val dir = tempDir()
    val file = dir.resolve("abc")

    val ser = serializer()

    //when
    ser.write(metadata, file)
    val actual = ser.read(file)

    //then
    assertThat(actual.requestTime()).isEqualTo(metadata.requestTime())
    assertThat(actual.responseTime()).isEqualTo(metadata.responseTime())
    assertThat(actual.request()).isEqualTo(metadata.request())
    assertThat(actual.response().statusCode()).isEqualTo(metadata.response().statusCode())
    assertThat(actual.response().version()).isEqualTo(metadata.response().version())
    assertThat(actual.response().headers()).isEqualTo(metadata.response().headers())
  }

  companion object {
    @JvmStatic
    fun metadata(): List<CacheEntryMetadata> {
      val metadata1 = CacheEntryMetadata(
        10, 15, ImmutableResponseInfo.builder()
          .headers(
            HttpHeadersBuilder()
              .add("test", "1")
              .add("test", "2")
              .build()
          )
          .statusCode(200)
          .version(HttpClient.Version.HTTP_1_1)
          .build(), HttpRequest.newBuilder(URI.create("https://example.com"))
          .header("abc", "1")
          .header("abc", "2")
          .header("abcd", "11")
          .header("abcd", "22")
          .version(HTTP_2)
          .timeout(Duration.ofSeconds(30))
          .build(), Clock.systemUTC()
      )

      val metadata2 = CacheEntryMetadata(
        10, 15, ImmutableResponseInfo.builder()
          .headers(HttpHeadersBuilder().build())
          .statusCode(200)
          .version(HTTP_2)
          .build(), HttpRequest.newBuilder(URI.create("https://example.com"))
          .header("abc", "1")
          .header("abcd", "22")
          .build(), Clock.systemUTC()
      )

      return listOf(
        metadata1, metadata2
      )
    }
  }
}