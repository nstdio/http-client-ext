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

import org.apache.commons.lang3.RandomStringUtils
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {

  fun pbe(password: String = RandomStringUtils.random(16), algo: String = "AES", keyLen: Int = 128): SecretKey {
    val salt = ByteArray(16)
    SecureRandom.getInstanceStrong()
      .nextBytes(salt)

    val pbeKeySpec = PBEKeySpec(password.toCharArray(), salt, 1000, keyLen)
    val pbeKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec)

    return SecretKeySpec(pbeKey.encoded, algo)
  }

  fun rsaKeyPair(keySize: Int = 1024): KeyPair {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize)
    return keyGen.generateKeyPair()
  }
}