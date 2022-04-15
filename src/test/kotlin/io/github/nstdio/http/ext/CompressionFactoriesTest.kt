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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CompressionFactoriesTest {
  @Test
  fun shouldMoveUsersUp() {
    //given
    val packages: MutableList<String> = ArrayList()
    packages.add("io.github.nstdio.http.ext.spi.JdkCompressionFactory")
    packages.add("io.github.nstdio.http.ext.spi.IdentityCompressionFactory")
    packages.add("io.github.nstdio.http.ext.spi.OptionalBrotli")
    packages.add("org.other.example.BrotliFactory")
    packages.add("com.example.GzipFactory")

    //when
    packages.sortWith(CompressionFactories.USERS_FIRST_COMPARATOR)

    //then
    assertThat(packages)
      .containsExactly(
        "org.other.example.BrotliFactory",
        "com.example.GzipFactory",
        "io.github.nstdio.http.ext.spi.JdkCompressionFactory",
        "io.github.nstdio.http.ext.spi.IdentityCompressionFactory",
        "io.github.nstdio.http.ext.spi.OptionalBrotli"
      )
  }
}