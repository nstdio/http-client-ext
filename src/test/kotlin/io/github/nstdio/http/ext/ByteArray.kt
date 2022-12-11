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
import java.nio.ByteBuffer

fun ByteArray.toBuffer(): ByteBuffer = ByteBuffer.wrap(this)

fun ByteArray.toBuffer(fromIndex: Int, toIndex: Int): ByteBuffer = ByteBuffer.wrap(copyOfRange(fromIndex, toIndex))

fun ByteArray.toChunkedBuffers(checkResult: Boolean = false): MutableList<ByteBuffer> {
  val ret = mutableListOf<ByteBuffer>()
  var start = 0
  val stop = size
  var end = 1.coerceAtLeast(Arb.int(start, stop).next())

  while (end != stop) {
    ret.add(toBuffer(start, end))
    start = end
    end = Arb.int(start + 1, stop).next()
  }
  if (start < end) {
    ret.add(toBuffer(start, end))
  }

  if (checkResult) {
    var i = 0
    ret.forEach {
      while (it.hasRemaining()) {
        this[i++].shouldBe(it.get())
      }
      it.flip()
    }
    i.shouldBe(size)
  }

  return ret
}
