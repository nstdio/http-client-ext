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

import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import java.nio.ByteBuffer

fun Arb.Companion.byteArray(length: Gen<Int>): Arb<ByteArray> = Arb.byteArray(length, Arb.byte())

fun Arb.Companion.byteArray(length: Int): Arb<ByteArray> = Arb.byteArray(Arb.int(length, length))

fun Arb.Companion.byteBuffer(length: Gen<Int>): Arb<ByteBuffer> =
  Arb.byteArray(length).map { ByteBuffer.wrap(it) }

fun Arb.Companion.byteBuffer(length: Int): Arb<ByteBuffer> =
  Arb.byteArray(Arb.int(length, length)).map { ByteBuffer.wrap(it) }