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

import com.jayway.jsonpath.matchers.JsonPathMatchers.isJson
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets

internal class InputStreamDecompressingBodyHandlerIntegrationTest {
    private val httpClient = HttpClient.newHttpClient()
    private val baseUri = URI.create("https://httpbin.org/")

    @ParameterizedTest
    @ValueSource(strings = ["gzip", "deflate"])
    @Throws(Exception::class)
    fun shouldCreate(compression: String) {
        //given
        val request = HttpRequest.newBuilder(baseUri.resolve(compression))
            .build()

        //when
        val body = httpClient.send(request, BodyHandlers.ofDecompressing()).body()
        val json = IOUtils.toString(body, StandardCharsets.UTF_8)

        //then
        assertThat(json, isJson())
    }
}