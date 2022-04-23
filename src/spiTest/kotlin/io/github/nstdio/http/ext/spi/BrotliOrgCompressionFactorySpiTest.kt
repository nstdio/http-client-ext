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
package io.github.nstdio.http.ext.spi

import io.kotest.assertions.json.shouldBeValidJson
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

internal class BrotliOrgCompressionFactorySpiTest {
  @Test
  fun shouldDecompress() {
    //given
    val inputStream = Brotli4JCompressionFactorySpiTest::class.java.getResource("/__files/br")?.openStream()
    val factory = BrotliOrgCompressionFactory()

    //when
    val supported = factory.supported()
    val actual = factory.decompressing(inputStream, "br")
    val actualAsString = actual.readAllBytes().toString(StandardCharsets.UTF_8)

    //then
    actualAsString.shouldBeValidJson()
    supported.shouldContainExactly("br")
  }
}