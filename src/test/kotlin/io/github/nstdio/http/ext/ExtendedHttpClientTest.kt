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

import io.github.nstdio.http.ext.ExtendedHttpClient.Builder
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.throwable.shouldHaveCause
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.inOrder
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import java.io.IOException
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.SocketTimeoutException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Version
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.time.Clock
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

internal class ExtendedHttpClientTest {
  private lateinit var client: ExtendedHttpClient
  private lateinit var mockDelegate: HttpClient

  @BeforeEach
  fun setUp() {
    mockDelegate = mock(HttpClient::class.java)
    client = ExtendedHttpClient(mockDelegate, NullCache.INSTANCE, Clock.systemUTC())
  }

  @ParameterizedTest
  @ValueSource(classes = [IOException::class, InterruptedException::class, SocketTimeoutException::class])
  fun shouldPropagateExceptions(th: Class<Throwable>) {
    //given
    val request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).build()
    given(mockDelegate.send(any(), any<BodyHandler<Any>>())).willThrow(th)

    //when + then
    assertThatExceptionOfType(th)
      .isThrownBy { client.send(request, ofString()) }
  }

  @ParameterizedTest
  @MethodSource("notUnwrappedExceptions")
  fun `Should throw CompletionException with cause`(th: Throwable) {
    //given
    val request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).build()
    given(mockDelegate.send(any(), any<BodyHandler<Any>>())).willThrow(th)


    //when + then
    shouldThrowExactly<CompletionException> { client.send(request, ofString()) }
      .shouldHaveCause { it.shouldBeSameInstanceAs(th) }
  }

  @Test
  fun `Should forward calls to delegate`() {
    //when
    client.cookieHandler()
    client.connectTimeout()
    client.followRedirects()
    client.proxy()
    client.sslContext()
    client.sslParameters()
    client.authenticator()
    client.version()
    client.executor()
    client.newWebSocketBuilder()

    //then
    val inOrder = inOrder(mockDelegate)
    inOrder.verify(mockDelegate).cookieHandler()
    inOrder.verify(mockDelegate).connectTimeout()
    inOrder.verify(mockDelegate).followRedirects()
    inOrder.verify(mockDelegate).proxy()
    inOrder.verify(mockDelegate).sslContext()
    inOrder.verify(mockDelegate).sslParameters()
    inOrder.verify(mockDelegate).authenticator()
    inOrder.verify(mockDelegate).version()
    inOrder.verify(mockDelegate).executor()
    inOrder.verify(mockDelegate).newWebSocketBuilder()
  }

  @Test
  fun `Should not allow insecure requests`() {
    //given
    val mockBuilderDelegate = mock(HttpClient.Builder::class.java)
    given(mockBuilderDelegate.build()).willReturn(mockDelegate)

    val uri = "HTTP://abc.local"
    client = Builder(mockBuilderDelegate)
      .allowInsecure(false)
      .build()

    val request = HttpRequest.newBuilder(uri.toUri()).build()

    //when + then
    shouldThrowExactly<IllegalArgumentException> {
      client.send(request, ofString())
    }.should {
      it.message.shouldEndWith("URI: $uri")
    }
  }

  @Nested
  inner class BuilderTest {
    @Test
    fun `Should forward calls to delegate`() {
      //given
      val mockDelegate = mock(HttpClient.Builder::class.java)
      val mockCookieHandler = mock(CookieHandler::class.java)
      val mockSSLContext = mock(SSLContext::class.java)
      val mockSSLParameters = mock(SSLParameters::class.java)
      val mockProxySelector = mock(ProxySelector::class.java)
      val mockAuthenticator = mock(Authenticator::class.java)
      val mockExecutor = mock(Executor::class.java)
      val builder = Builder(mockDelegate)

      //when
      builder
        .cookieHandler(mockCookieHandler)
        .connectTimeout(Duration.ofSeconds(30))
        .sslContext(mockSSLContext)
        .sslParameters(mockSSLParameters)
        .version(Version.HTTP_2)
        .priority(500)
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .proxy(mockProxySelector)
        .authenticator(mockAuthenticator)
        .executor(mockExecutor)

      //then
      val inOrder = inOrder(mockDelegate)
      inOrder.verify(mockDelegate).cookieHandler(mockCookieHandler)
      inOrder.verify(mockDelegate).connectTimeout(Duration.ofSeconds(30))
      inOrder.verify(mockDelegate).sslContext(mockSSLContext)
      inOrder.verify(mockDelegate).sslParameters(mockSSLParameters)
      inOrder.verify(mockDelegate).version(Version.HTTP_2)
      inOrder.verify(mockDelegate).priority(500)
      inOrder.verify(mockDelegate).followRedirects(HttpClient.Redirect.ALWAYS)
      inOrder.verify(mockDelegate).proxy(mockProxySelector)
      inOrder.verify(mockDelegate).authenticator(mockAuthenticator)
      inOrder.verify(mockDelegate).executor(mockExecutor)
    }
  }

  companion object {
    @JvmStatic
    fun notUnwrappedExceptions(): List<Throwable> {
      return listOf(
        RuntimeException("abc"),
        IllegalStateException("abc"),
        OutOfMemoryError("abcd")
      )
    }
  }
}