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

import org.assertj.core.api.Assertions.assertThatIOException
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.http.HttpResponse
import java.util.function.UnaryOperator

internal class FutureHandlerTest {
    @Test
    fun shouldThrowIfThrowableNotNull() {
        //given
        val op = UnaryOperator.identity<HttpResponse<Any?>>()
        val handler = FutureHandler.of(op)

        //when + then
        assertThatIOException()
            .isThrownBy { handler.apply(null, IOException()) }
    }

    @Test
    fun shouldProperlyChain() {
        //given
        val handler = FutureHandler { r: HttpResponse<HttpResponse<Any?>?>?, _: Throwable? -> r }
        val other = FutureHandler.of(UnaryOperator.identity<HttpResponse<HttpResponse<Any?>?>>())

        //when + then
        assertThatIOException()
            .isThrownBy { handler.andThen(other).apply(null, IOException()) }
    }
}