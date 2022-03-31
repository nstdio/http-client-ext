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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.util.function.Function
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

internal object Compression {
    fun gzip(input: String): ByteArray {
        return compress(input) { out: OutputStream ->
            try {
                return@compress GZIPOutputStream(out, true)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
    }

    fun deflate(input: String): ByteArray {
        return compress(input) { out: OutputStream -> DeflaterOutputStream(out, true) }
    }

    private fun compress(input: String, compressorCreator: Function<OutputStream, DeflaterOutputStream>): ByteArray {
        try {
            ByteArrayOutputStream().use { out ->
                compressorCreator.apply(out).use { compressor ->
                    compressor.write(input.toByteArray(StandardCharsets.UTF_8))
                    compressor.finish()
                    return out.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}