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

import io.github.nstdio.http.ext.jupiter.DisabledIfOnClasspath
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DiskCacheBuilderSpiTest {
  @Nested
  @DisabledIfOnClasspath(JACKSON, GSON)
  internal inner class WithoutJacksonTest {
    @Test
    fun shouldDescriptiveException() {
      Assertions.assertThatIllegalStateException()
        .isThrownBy { Cache.newDiskCacheBuilder() }
        .withMessage("In order to use disk cache please add either Jackson or Gson to your dependencies")
    }
  }
}