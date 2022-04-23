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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodySubscriber
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Map
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

@ExtendWith(MockitoExtension::class)
internal class DecompressingBodyHandlerTest {
  private lateinit var handler: DecompressingBodyHandler<*>

  @Mock
  private lateinit var mockHandler: BodyHandler<Any>

  @Mock
  private lateinit var mockSubscriber: BodySubscriber<Any>

  @BeforeEach
  fun setUp() {
    handler = DecompressingBodyHandler(mockHandler, DecompressingBodyHandler.Options(false, false))
  }

  @AfterEach
  fun tearDown() {
    Mockito.verifyNoInteractions(mockSubscriber)
  }

  @Test
  fun shouldReturnOriginalSub() {
    //given
    val responseInfo = ImmutableResponseInfo.builder()
      .headers(HttpHeaders.of(Map.of(), Headers.ALLOW_ALL))
      .build()
    given(mockHandler.apply(responseInfo)).willReturn(mockSubscriber)

    //when
    val actual = handler.apply(responseInfo)

    //then
    assertThat(actual).isSameAs(mockSubscriber)
    verify(mockHandler).apply(responseInfo)
    verifyNoMoreInteractions(mockHandler)
  }

  @Test
  fun shouldReturnDirectSubscriptionWhenDirect() {
    val options = DecompressingBodyHandler.Options(false, false)
    val handler = DecompressingBodyHandler.ofDirect(options)
    val responseInfo = ImmutableResponseInfo.builder()
      .headers(HttpHeadersBuilder().add("Content-Encoding", "gzip").build())
      .build()

    //when
    val actual = handler.apply(responseInfo)

    //then
    assertThat(actual).isExactlyInstanceOf(AsyncMappingSubscriber::class.java)
  }

  @Test
  fun shouldReturnOriginalSubWhenDirectivesUnsupported() {
    //given
    val responseInfo = ImmutableResponseInfo.builder()
      .headers(HttpHeadersBuilder().add(Headers.HEADER_CONTENT_ENCODING, "compress,br,identity1,abc").build())
      .build()
    given(mockHandler.apply(responseInfo)).willReturn(mockSubscriber)

    //when
    val actual = handler.apply(responseInfo)

    //then
    assertThat(actual).isSameAs(mockSubscriber)
    verify(mockHandler).apply(responseInfo)
    verifyNoMoreInteractions(mockHandler)
  }

  @ParameterizedTest
  @ValueSource(strings = ["gzip", "x-gzip"])
  @Throws(
    IOException::class
  )
  fun shouldReturnGzipInputStream(directive: String?) {
    val gzipContent = ByteArrayInputStream(Compression.gzip("abc"))

    //when
    val fn = handler.decompressionFn(directive)
    val `in` = fn.apply(gzipContent)

    //then
    assertThat(`in`).isInstanceOf(GZIPInputStream::class.java)
    `in`.readAllBytes().toString(UTF_8).shouldBe("abc")
  }

  @Test
    fun shouldReturnDeflateInputStream() {
    val deflateContent = ByteArrayInputStream(Compression.deflate("abc"))

    //when
    val fn = handler.decompressionFn("deflate")
    val `in` = fn.apply(deflateContent)

    //then
    assertThat(`in`).isInstanceOf(InflaterInputStream::class.java)
    `in`.readAllBytes().toString(UTF_8).shouldBe("abc")
  }

  @Nested
  internal inner class FailureControlOptionsTest {
    @ParameterizedTest
    @ValueSource(strings = ["compress", "br"])
    fun shouldThrowUnsupportedOperationException(directive: String?) {
      //given
      val handler = BodyHandlers.decompressingBuilder()
        .failOnUnsupportedDirectives(true)
        .failOnUnknownDirectives(true)
        .build(mockHandler) as DecompressingBodyHandler

      //when + then
      assertThatExceptionOfType(UnsupportedOperationException::class.java)
        .isThrownBy { handler.decompressionFn(directive) }
        .withMessage("Compression directive '%s' is not supported", directive)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "abc", "gz", "a"])
    fun shouldThrowIllegalArgumentException(directive: String?) {
      val handler = BodyHandlers.decompressingBuilder()
        .failOnUnsupportedDirectives(true)
        .failOnUnknownDirectives(true)
        .build(mockHandler) as DecompressingBodyHandler
      //when + then
      assertThatIllegalArgumentException()
        .isThrownBy { handler.decompressionFn(directive) }
        .withMessage("Unknown compression directive '%s'", directive)
    }

    @ParameterizedTest
    @ValueSource(strings = ["compress", "br"])
    @DisplayName("Should not throw exception when 'failOnUnsupportedDirectives' is 'false'")
    fun shouldNotThrowUnsupportedOperationException(directive: String?) {
      //given
      val handler = BodyHandlers.decompressingBuilder()
        .failOnUnsupportedDirectives(false)
        .failOnUnknownDirectives(true)
        .build(mockHandler) as DecompressingBodyHandler
      val s = InputStream.nullInputStream()

      //when
      val fn = handler.decompressionFn(directive)
      val actual = fn.apply(s)

      //then
      actual shouldBeSameInstanceAs s
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "abc", "gz", "a"])
    @DisplayName("Should not throw exception when 'failOnUnknownDirectives' is 'false'")
    fun shouldNotIllegalArgumentException(directive: String?) {
      //given
      val handler = BodyHandlers.decompressingBuilder()
        .failOnUnsupportedDirectives(true)
        .failOnUnknownDirectives(false)
        .build(mockHandler) as DecompressingBodyHandler
      val s = InputStream.nullInputStream()

      //when
      val fn = handler.decompressionFn(directive)
      val actual = fn.apply(s)

      //then
      actual shouldBeSameInstanceAs s
    }
  }
}