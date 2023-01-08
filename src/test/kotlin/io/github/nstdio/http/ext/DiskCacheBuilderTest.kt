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

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.Assertions.assertThatNullPointerException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path

internal class DiskCacheBuilderTest {
  @ParameterizedTest
  @ValueSource(ints = [0, -1, -50, Int.MIN_VALUE])
  fun `Should throw when max items is not positive`(maxItems: Int) {
    //given
    val builder = Cache.newDiskCacheBuilder()

    //when + then
    assertThatIllegalArgumentException()
      .isThrownBy { builder.maxItems(maxItems) }
  }
  
  @ParameterizedTest
  @ValueSource(ints = [1, 2, 256])
  fun `Should not throw when max items is positive`(maxItems: Int) {
    //given
    val builder = Cache.newDiskCacheBuilder()
      .dir(Path.of("abc"))

    //when + then
    builder.maxItems(maxItems).build()
  }

  @Test
  fun shouldThrowWhenBuildWithoutDir() {
    //given
    val builder = Cache.newDiskCacheBuilder()

    //when + then
    assertThatIllegalStateException()
      .isThrownBy { builder.build() }
  }

  @Test
  fun shouldThrowWhenDirIsNull() {
    //given
    val builder = Cache.newDiskCacheBuilder()

    //when + then
    assertThatNullPointerException()
      .isThrownBy { builder.dir(null) }
  }
}