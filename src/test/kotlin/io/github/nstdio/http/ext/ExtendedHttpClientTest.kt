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

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito
import org.mockito.Mockito
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.time.Clock

internal class ExtendedHttpClientTest {
    private var client: ExtendedHttpClient? = null
    private var mockHttpClient: HttpClient? = null

    @BeforeEach
    fun setUp() {
        mockHttpClient = Mockito.mock(HttpClient::class.java)
        client = ExtendedHttpClient(mockHttpClient, NullCache.INSTANCE, Clock.systemUTC())
    }

    @ParameterizedTest
    @ValueSource(classes = [IOException::class, InterruptedException::class, IllegalStateException::class, RuntimeException::class, OutOfMemoryError::class, SocketTimeoutException::class])
    @Throws(
        Exception::class
    )
    fun shouldPropagateExceptions(th: Class<Throwable>) {
        //given
        val request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).build()
        BDDMockito.given(mockHttpClient!!.send(Mockito.any(), Mockito.any<BodyHandler<Any>>())).willThrow(th)

        //when + then
        assertThatExceptionOfType(th)
            .isThrownBy { client!!.send(request, ofString()) }
        assertThrows(th) { client!!.send(request, ofString()) }
    }
}