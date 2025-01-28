/*
 * Copyright (C) 2023-2025 the original author or authors.
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

import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.net.http.HttpClient
import java.net.http.HttpRequest
import javax.net.ssl.SSLSession

class StaticHttpResponseTest {
  @Test
  fun `Should create proper response`() {
    //given
    val body = "abc"
    val uri = "https://example.com".toUri()
    val request = HttpRequest.newBuilder(uri).build()
    val statusCode = 200
    val headers = HttpHeadersBuilder().add("A", "1").build()
    val sslSession = Mockito.mock(SSLSession::class.java)
    val version = HttpClient.Version.HTTP_2

    //when
    val response = StaticHttpResponse.builder<String>()
      .body(body)
      .request(request)
      .uri(uri)
      .statusCode(statusCode)
      .headers(headers)
      .sslSession(sslSession)
      .version(version)
      .build()
    
    //then
    response.should {
      it.body() shouldBe body
      it.uri() shouldBe uri
      it.request() shouldBe request
      it.statusCode() shouldBe statusCode
      it.headers() shouldBe headers
      it.sslSession().shouldBePresent { shouldBe(sslSession) }
      it.version() shouldBe version
      it.previousResponse().shouldBeEmpty()
    }
  }
}