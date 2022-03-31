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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString

internal class DecompressingSubscriberSpiTest {
    private val client = HttpClient.newHttpClient()

    @ParameterizedTest
    @ValueSource(strings = ["brotli", "gzip", "deflate"])
    @Throws(
        IOException::class, InterruptedException::class
    )
    fun shouldDecompress(compressionType: String) {
        //given
        val uri = URI.create("https://httpbin.org/").resolve(compressionType)

        //when
        val response = client.send(
            HttpRequest.newBuilder(uri).build(),
            BodyHandlers.ofDecompressing(ofString())
        )
        val body = response.body()

        //then
        isJson().matches(body)
    }
}