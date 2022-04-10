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

import io.github.nstdio.http.ext.BinaryMetadataSerializer.ExternalizableHttpHeaders
import io.github.nstdio.http.ext.Headers.ALLOW_ALL
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream.nullOutputStream
import java.net.http.HttpHeaders

internal class ExternalizableHttpHeadersTest {

  @ParameterizedTest
  @MethodSource("httpHeaders")
  fun `Should round robin proper headers`(expected: HttpHeaders) {
    //given
    val out = ByteArrayOutputStream()

    //when
    ObjectOutputStream(out).use { it.writeObject(ExternalizableHttpHeaders(expected)) }

    val e = out.toObjectInput().readObject() as ExternalizableHttpHeaders

    //then
    e.headers.shouldBe(expected)
  }

  @Test
  fun `Should throw when large headers`() {
    //given
    val maxHeadersSize = 2048
    val headers = Arb.list(Arb.string(16..32), maxHeadersSize..3000)
      .map {
        val map = hashMapOf<String, List<String>>()
        map.putAll(it.zip(Arb.list(Arb.list(Arb.string(), 1..1), it.size..it.size).next()))
        map
      }
      .map { HttpHeaders.of(it, ALLOW_ALL) }
      .next()

    val headersSize = headers.map().size

    //when + then
    shouldThrowExactly<IOException> { nullOutput().writeObject(ExternalizableHttpHeaders(headers)) }
      .shouldHaveMessage(
        "The headers size exceeds max allowed number. Size: $headersSize, Max:1024"
      )

    val out = ByteArrayOutputStream()
    ObjectOutputStream(out).use { it.writeObject(ExternalizableHttpHeaders(headers, false)) }

    shouldThrowExactly<IOException> { out.toObjectInput().readObject() }
      .shouldHaveMessage(
        "The headers size exceeds max allowed number. Size: $headersSize, Max:1024"
      )
  }

  @Test
  fun `Should throw when large header values occures`() {
    //given
    val maxValuesSize = 256
    val headerName = "Content-Type"
    val headers = Arb.list(Arb.string(3..15), maxValuesSize..1000)
      .map { mapOf(headerName to it) }
      .map { HttpHeaders.of(it, ALLOW_ALL) }
      .next()
    val valuesSize = headers.allValues(headerName).size

    //when + then
    shouldThrowExactly<IOException> {
      nullOutput().writeObject(ExternalizableHttpHeaders(headers))
    }.shouldHaveMessage(
      "The values for header '$headerName' exceeds maximum allowed number. Size:$valuesSize, Max:256"
    )

    val out = ByteArrayOutputStream()
    ObjectOutputStream(out).use { it.writeObject(ExternalizableHttpHeaders(headers, false)) }

    shouldThrowExactly<IOException> { out.toObjectInput().readObject() }
      .shouldHaveMessage(
        "The values for header '$headerName' exceeds maximum allowed number. Size:$valuesSize, Max:256"
      )
  }

  @Test
  fun `Should throw if map size is negative`() {
    //given
    val bytes = byteArrayOf(
      // header
      -84, -19, 0, 5, 115, 114, 0, 76, 105, 111, 46, 103, 105, 116, 104, 117, 98, 46,
      110, 115, 116, 100, 105, 111, 46, 104, 116, 116, 112, 46, 101, 120, 116, 46, 66,
      105, 110, 97, 114, 121, 77, 101, 116, 97, 100, 97, 116, 97, 83, 101, 114, 105, 97,
      108, 105, 122, 101, 114, 36, 69, 120, 116, 101, 114, 110, 97, 108, 105, 122, 97,
      98, 108, 101, 72, 116, 116, 112, 72, 101, 97, 100, 101, 114, 115, 0, 0, 13, -80,
      -87, -115, -74, -90, 12, 0, 0, 120, 112, 119, 4,
      // map size
      -1, -1, -1, -42, // decimal: -42
      // block end
      120
    )

    //when
    shouldThrowExactly<IOException> { bytes.toObjectInput().readObject() }
      .shouldHaveMessage("Corrupted stream: map size cannot be negative")
  }

  @ParameterizedTest
  @ValueSource(ints = [0, -1, -100, Int.MIN_VALUE])
  fun `Should throw if list size is invalid`(listSize: Int) {
    //given
    val out = ByteArrayOutputStream()
    val objOut = ObjectOutputStream(out)

    objOut.use {
      it.writeInt(15) // map size
      it.writeUTF("content-type") // header value
      it.writeInt(listSize)
    }

    //when
    shouldThrowExactly<IOException> { ExternalizableHttpHeaders().readExternal(out.toObjectInput()) }
      .shouldHaveMessage("Corrupted stream: list size should be positive")
  }

  private fun ByteArrayOutputStream.toObjectInput(): ObjectInput =
    ObjectInputStream(ByteArrayInputStream(toByteArray()))

  private fun ByteArray.toObjectInput(): ObjectInput = ObjectInputStream(ByteArrayInputStream(this))

  private fun nullOutput() = ObjectOutputStream(nullOutputStream())

  companion object {
    @JvmStatic
    fun httpHeaders(): List<HttpHeaders> {
      return listOf(
        HttpHeaders.of(hashMapOf("Content-Type" to listOf("application/json")), Headers.ALLOW_ALL),
        HttpHeaders.of(
          hashMapOf(
            "Host" to listOf("www.random.org"),
            "User-Agent" to listOf("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:99.0) Gecko/20100101 Firefox/99.0"),
            "Accept" to listOf(
              "text/html",
              "application/xhtml+xml",
              "application/xml;q=0.9",
              "image/avif",
              "image/webp",
              "*/*;q=0.8"
            ),
            "Accept-Language" to listOf("en-US,en;q=0.5"),
            "Accept-Encoding" to listOf("gzip, deflate, br"),
            "Referer" to listOf("https://www.random.org/integers/"),
            "Connection" to listOf("keep-alive"),
            "Upgrade-Insecure-Requests" to listOf("1"),
            "Sec-Fetch-Dest" to listOf("document"),
            "Sec-Fetch-Mode" to listOf("navigate"),
            "Sec-Fetch-Site" to listOf("same-origin"),
            "Sec-Fetch-User" to listOf("?1"),
            "Cache-Control" to listOf("max-age=0"),
          ), Headers.ALLOW_ALL
        ),
        HttpHeaders.of(hashMapOf(), Headers.ALLOW_ALL)
      )
    }
  }
}