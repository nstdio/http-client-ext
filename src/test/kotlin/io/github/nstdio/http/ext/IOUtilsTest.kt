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

import io.github.nstdio.http.ext.IOUtils.createFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.Closeable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class IOUtilsTest {
    @Test
    fun shouldNotRethrowClosingException() {
        //given
        val c = Closeable { throw IOException() }

        //when + then
        IOUtils.closeQuietly(c)
    }

    @Test
    fun shouldReturnNegativeWhenFileNotExists() {
        //given
        val path = Path.of("abc")

        //when
        val size = IOUtils.size(path)

        //then
        assertEquals(-1, size)
    }

    @Test
    @Throws(IOException::class)
    fun shouldReturnTrueIfFileExists(@TempDir temp: Path) {
        //given
        val path = temp.resolve("abc")
        Files.createFile(path)

        //when
        val created = createFile(path)

        //then
        assertTrue(created)
    }
}