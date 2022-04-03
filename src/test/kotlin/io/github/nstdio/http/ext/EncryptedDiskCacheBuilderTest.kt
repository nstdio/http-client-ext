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
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.Assertions.assertThatNullPointerException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import java.nio.file.Path
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

internal class EncryptedDiskCacheBuilderTest {
  @ParameterizedTest
  @ValueSource(
    strings = [
      "AES/CBC/NoPadding",
      "AES/ECB/NoPadding",
      "AES/GCM/NoPadding",
      "DES/CBC/NoPadding"
    ]
  )
  fun `Should not allow algorithms with no padding`(algo: String) {
    //given
    val builder = Cache.newDiskCacheBuilder().encrypted()

    //when + then
    assertThatIllegalArgumentException()
      .isThrownBy { builder.cipherAlgorithm(algo) }
      .withMessage("NoPadding transformations are not supported")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "AES",
      "DES",
      "AES/CBC/PKCS5Padding"
    ]
  )
  fun `Should allow algorithms with padding explicit or implicit`(algo: String) {
    //given
    val builder = Cache.newDiskCacheBuilder().encrypted()

    //when
    builder.cipherAlgorithm(algo)
  }

  @Test
  fun `Should not allow nulls as keys`() {
    //given
    val builder = Cache.newDiskCacheBuilder().encrypted()

    //when + then
    assertThatNullPointerException()
      .isThrownBy { builder.publicKey(null) }
      .withMessage("publicKey cannot be null")

    assertThatNullPointerException()
      .isThrownBy { builder.privateKey(null) }
      .withMessage("privateKey cannot be null")
  }

  @Test
  fun `Should report not existing algorithms`() {
    //given
    val builder = Cache.newDiskCacheBuilder().encrypted()

    //when + then
    assertThatExceptionOfType(NoSuchAlgorithmException::class.java)
      .isThrownBy { builder.cipherAlgorithm("rubbish") }
  }

  @Test
  fun `Should throw when no key is specified`() {
    //given
    val mockPublicKey = Mockito.mock(PublicKey::class.java)
    val mockPrivateKey = Mockito.mock(PrivateKey::class.java)
    val dir = Path.of("abc")

    //when + then
    assertThatIllegalStateException()
      .isThrownBy { Cache.newDiskCacheBuilder().dir(dir).encrypted().build() }
      .withMessage("specify keypair or secret")

    assertThatIllegalStateException()
      .isThrownBy { Cache.newDiskCacheBuilder().dir(dir).encrypted().publicKey(mockPublicKey).build() }
      .withMessage("specify keypair or secret")

    assertThatIllegalStateException()
      .isThrownBy { Cache.newDiskCacheBuilder().dir(dir).encrypted().privateKey(mockPrivateKey).build() }
      .withMessage("specify keypair or secret")
  }

  @Test
  fun `Should return self when encrypted() method invoked`() {
    //given
    val builder = Cache.newDiskCacheBuilder().encrypted()

    //when
    val actual = builder.encrypted()

    //then
    assertThat(actual).isSameAs(builder)
  }

  @Test
  fun `Should throw when algo is null`() {
    //given
    val mockKey = Mockito.mock(SecretKey::class.java)

    //when
    assertThatIllegalArgumentException()
      .isThrownBy { Cache.newDiskCacheBuilder().encrypted().cipherAlgorithm(null) }
      .withMessage("algorithm cannot be null")

    assertThatIllegalStateException()
      .isThrownBy {
        Cache.newDiskCacheBuilder()
          .dir(Path.of("abc"))
          .encrypted()
          .key(mockKey)
          .build()
      }
      .withMessage("algorithm cannot be null")
  }

  @Test
  fun `Should check provider`() {
    //given
    val builder = Cache.newDiskCacheBuilder().encrypted()

    assertThatIllegalArgumentException()
      .isThrownBy { builder.provider(null) }
      .withMessage("provider cannot be null")

    builder.provider("BC")
  }
}