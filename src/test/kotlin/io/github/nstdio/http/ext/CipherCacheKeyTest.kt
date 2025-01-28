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

import io.github.nstdio.http.ext.EncryptedStreamFactory.CipherCacheKey
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.jupiter.api.Test

internal class CipherCacheKeyTest {
  @Test
  fun `Should respect equals and hashCode contract`() {
    EqualsVerifier.forClass(CipherCacheKey::class.java)
      .withNonnullFields("transformation")
      .verify()
  }
}