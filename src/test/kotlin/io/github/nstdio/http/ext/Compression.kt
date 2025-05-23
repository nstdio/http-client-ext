/*
 * Copyright (C) 2022-2025 the original author or authors.
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

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.function.Function
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

internal object Compression {
  fun gzip(input: String): ByteArray {
    return gzip(input.toByteArray(UTF_8))
  }

  fun gzip(input: ByteArray): ByteArray {
    return compress(input) { GZIPOutputStream(it, true) }
  }

  fun deflate(input: String): ByteArray {
    return deflate(input.toByteArray(UTF_8))
  }

  fun deflate(input: ByteArray): ByteArray {
    return compress(input) { DeflaterOutputStream(it, true) }
  }

  private fun compress(input: ByteArray, compressorCreator: Function<OutputStream, DeflaterOutputStream>): ByteArray {
    ByteArrayOutputStream().use { out ->
      compressorCreator.apply(out).use { compressor ->
        compressor.write(input)
        compressor.finish()
        return out.toByteArray()
      }
    }
  }
}