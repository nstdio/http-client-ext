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

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpResponse.ResponseInfo
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

internal object Helpers {
  fun responseInfo(headers: Map<String, String>): ResponseInfo {
    return object : ResponseInfo {
      override fun statusCode(): Int {
        return 200
      }

      override fun headers(): HttpHeaders {
        return HttpHeaders.of(headers
          .mapValues { listOf(it.value) }
          .toMutableMap()) { _, _ -> true }
      }

      override fun version(): HttpClient.Version {
        return HttpClient.Version.HTTP_1_1
      }
    }
  }

  fun toBuffers(bytes: ByteArray, checkResult: Boolean = false): MutableList<ByteBuffer> {
    val ret: MutableList<ByteBuffer> = ArrayList()
    var start = 0
    val stop = bytes.size
    var end = 1.coerceAtLeast(Arb.int(start, stop).next())

    while (end != stop) {
      ret.add(bytes.toBuffer(start, end))
      start = end
      end = Arb.int(start + 1, stop).next()
    }
    if (start < end) {
      ret.add(bytes.toBuffer(start, end))
    }
    
    if (checkResult) {
      var i = 0
      for (b in ret) {
        while (b.hasRemaining()) {
          bytes[i++].shouldBe(b.get())
        }
        b.flip()
      }
      i.shouldBe(bytes.size)
    }

    return ret
  }

  fun toBuffers(`in`: String): MutableList<ByteBuffer> {
    return toBuffers(`in`.toByteArray(StandardCharsets.UTF_8), false)
  }
}