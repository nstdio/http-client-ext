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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.UncheckedIOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.function.Supplier

internal class BodyHandlersTest {
    @Nested
    internal inner class OfJsonTest {
        private val client = HttpClient.newHttpClient()
        @Test
        fun shouldProperlyReadJson() {
            //given
            val request = HttpRequest.newBuilder(URI.create("https://httpbin.org/get")).build()

            //when
            val body1 = client.sendAsync(
                request, BodyHandlers.ofJson(
                    Any::class.java
                )
            )
                .thenApply { obj: HttpResponse<Supplier<Any>> -> obj.body() }
                .thenApply { obj: Supplier<Any> -> obj.get() }
                .join()

            //then
            Assertions.assertThat(body1).isNotNull
        }

        @Test
        fun shouldThrowUncheckedExceptionIfCannotRead() {
            //given
            val request = HttpRequest.newBuilder(URI.create("https://httpbin.org/html")).build()

            //when
            Assertions.assertThatExceptionOfType(UncheckedIOException::class.java)
                .isThrownBy {
                    client.send(
                        request, BodyHandlers.ofJson(
                            Any::class.java
                        )
                    ).body().get()
                }
                .havingRootCause()
        }
    }
}