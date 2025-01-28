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
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest

class PredicatesTest {
  @Test
  fun shouldMatchGivenUri() {
    //given
    val uri = URI.create("http://example.com")
    val r1 = HttpRequest.newBuilder(uri).build()
    val r2 = HttpRequest.newBuilder(uri.resolve("/path")).build()

    //when + then
    assertThat(Predicates.uri(uri))
      .accepts(r1)
      .rejects(r2)
  }

  @Test
  fun `Should accept or reject with header name and value`() {
    //given
    val r = StaticHttpResponse.builder<Any>()
      .statusCode(200)
      .headers(HttpHeadersBuilder().add("Content-Type", "text/plain").build())
      .build()

    //when + then
    assertThat(Predicates.hasHeader<Any>("Content-Type", "text/plain"))
      .accepts(r)

    assertThat(Predicates.hasHeader<Any>("Content-Type", "text/plain;charset=UTF-8"))
      .rejects(r)
    assertThat(Predicates.hasHeader<Any>("Content-Length", "12"))
      .rejects(r)
  }

  @Test
  fun `Should accept or reject with header name`() {
    //given
    val r = StaticHttpResponse.builder<Any>()
      .statusCode(200)
      .headers(HttpHeadersBuilder().add("Content-Type", "*/*").build())
      .build()

    //when + then
    assertThat(Predicates.hasHeader<Any>("Content-Type"))
      .accepts(r)

    assertThat(Predicates.hasHeader<Any>("Content-Length"))
      .rejects(r)
  }
}