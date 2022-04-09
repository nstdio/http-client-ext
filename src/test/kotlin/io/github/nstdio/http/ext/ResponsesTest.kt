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

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.noBody
import java.net.http.HttpResponse
import java.util.stream.IntStream
import java.util.stream.Stream

internal class ResponsesTest {
  @ParameterizedTest
  @MethodSource("successfulStatusCodes")
  fun `Should be successful`(response: HttpResponse<Any>) {
    Responses.isSuccessful(response).shouldBeTrue()
  }

  @ParameterizedTest
  @MethodSource("unsuccessfulStatusCodes")
  fun `Should not be successful`(response: HttpResponse<Any>) {
    Responses.isSuccessful(response).shouldBeFalse()
  }

  @ParameterizedTest
  @MethodSource("safeRequest")
  fun `Should be safe request`(response: HttpResponse<Any>) {
    Responses.isSafeRequest(response).shouldBeTrue()
  }

  @ParameterizedTest
  @MethodSource("unsafeRequest")
  fun `Should be unsafe request`(response: HttpResponse<Any>) {
    Responses.isSafeRequest(response).shouldBeFalse()
  }

  companion object {
    @JvmStatic
    fun safeRequest(): Stream<HttpResponse<Any>> {
      return Stream.of("GET", "get", "HEAD", "head", "gEt", "HeAd")
        .map { HttpRequest.newBuilder(URI.create("http://example.com")).method(it, noBody()).build() }
        .map { StaticHttpResponse.builder<Any>().request(it).build() }
    }

    @JvmStatic
    fun unsafeRequest(): Stream<HttpResponse<Any>> {
      return Stream.of("POST", "DELETE", "PUT", "PATCH")
        .flatMap { Stream.of(it, it.lowercase()) }
        .map { HttpRequest.newBuilder(URI.create("http://example.com")).method(it, noBody()).build() }
        .map { StaticHttpResponse.builder<Any>().request(it).build() }
    }

    @JvmStatic
    fun successfulStatusCodes(): Stream<HttpResponse<Any>> {
      return IntStream.range(200, 400)
        .mapToObj { response(it) }
    }

    @JvmStatic
    fun unsuccessfulStatusCodes(): Stream<HttpResponse<Any>> {
      return IntStream.concat(IntStream.range(0, 200), IntStream.range(400, 1000))
        .mapToObj { response(it) }
    }

    private fun response(statusCode: Int) = StaticHttpResponse.builder<Any>().statusCode(statusCode).build()
  }
}