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

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.throwable.shouldHaveCauseInstanceOf
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.BDDMockito.anyInt
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import java.io.BufferedWriter
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchProviderException
import java.security.ProviderException
import kotlin.io.path.readText

class EncryptedStreamFactoryTest {
  @TempDir
  private lateinit var dir: Path

  @AfterEach
  fun setup() {
    EncryptedStreamFactory.clear()
  }

  @ParameterizedTest
  @MethodSource("rwData")
  fun `Should rw`(algorithm: String, publicKey: Key, privateKey: Key) {
    //given
    val delegate = SimpleStreamFactory()

    val writeFactory = EncryptedStreamFactory(delegate, publicKey, privateKey, algorithm, null)
    val readFactory = EncryptedStreamFactory(delegate, publicKey, privateKey, algorithm, null)

    val expected = "abcdefjabcdefjabcdefjabcdefjasaea"
    val path = dir.resolve("sample")

    //when
    writeFactory.output(path).toWriter().use { it.write(expected) }
    val decrypted = readFactory.input(path).use { it.readText() }
    val plain = path.readText()

    //then
    decrypted.shouldBe(expected)
    plain.shouldNotBe(expected)
  }

  @Test
  fun `Should close input when exception occures`() {
    //given
    val delegate = mock(StreamFactory::class.java)
    val mockIs = mock(InputStream::class.java)
    val mockOut = mock(OutputStream::class.java)
    val expectedIO = IOException("expected")
    val key = Crypto.pbe()

    val factory = EncryptedStreamFactory(delegate, key, key, "AES/CBC/PKCS5Padding", null)

    given(mockIs.read()).willThrow(expectedIO)
    given(mockOut.write(anyInt())).willThrow(expectedIO)
    given(delegate.input(any(), any())).willReturn(mockIs)
    given(delegate.output(any(), any())).willReturn(mockOut)

    //when + then
    shouldThrowExactly<IOException> { factory.input(Path.of("abc")) }
      .shouldHaveMessage("expected")

    shouldThrowExactly<IOException> { factory.output(Path.of("abc")) }
      .shouldHaveMessage("expected")

    verify(mockIs, times(2)).close()
    verify(mockOut).close()
  }

  @Test
  fun `Should throw when provider does not exists`() {
    //given
    val delegate = mock(StreamFactory::class.java)
    val key = Crypto.pbe()

    val factory = EncryptedStreamFactory(delegate, key, key, "AES/CBC/PKCS5Padding", "plain-text-cryptography")

    //when + then
    shouldThrowExactly<IOException> { factory.input(Path.of("any")) }
      .shouldHaveCauseInstanceOf<NoSuchProviderException>()

    shouldThrowExactly<IOException> { factory.output(Path.of("any")) }
      .shouldHaveCauseInstanceOf<NoSuchProviderException>()
  }

  @Test
  fun `Should throw when key is invalid`() {
    //given
    val delegate = mock(StreamFactory::class.java).also {
      given(it.input(any())).willReturn(InputStream.nullInputStream())
    }

    val factory = Crypto.rsaKeyPair().let {
      EncryptedStreamFactory(delegate, it.public, it.private, "AES/CBC/PKCS5Padding", null)
    }

    //when + then
    assertSoftly {
      shouldThrowExactly<IOException> { factory.output(Path.of("any")) }
        .shouldHaveCauseInstanceOf<InvalidKeyException>()

      shouldThrowExactly<IOException> { factory.input(Path.of("any")) }
        .shouldHaveCauseInstanceOf<ProviderException>()
    }
  }

  @Test
  fun `Should throw eof when cannot read header length`() {
    //given
    val delegate = mock(StreamFactory::class.java)
    given(delegate.input(any())).willReturn(InputStream.nullInputStream())

    val key = Crypto.pbe()
    val factory = EncryptedStreamFactory(delegate, key, key, "AES/CBC/PKCS5Padding", null)

    //when + then
    shouldThrowExactly<EOFException> { factory.input(Path.of("any")) }
  }

  private fun InputStream.readText(): String {
    return String(readAllBytes(), UTF_8)
  }

  private fun OutputStream.toWriter() = BufferedWriter(OutputStreamWriter(this, UTF_8))

  companion object {
    @JvmStatic
    fun rwData(): List<Arguments> {
      val key1 = Crypto.rsaKeyPair()
      val key2 = Crypto.rsaKeyPair()

      return listOf(
        arguments("AES", Crypto.pbe()),
        arguments("AES/CBC/PKCS5Padding", Crypto.pbe()),
        arguments("AES/ECB/PKCS5Padding", Crypto.pbe()),
        arguments("AES/ECB/PKCS5Padding", Crypto.pbe()),
        arguments("DES/CBC/PKCS5Padding", Crypto.pbe(algo = "DES", keyLen = 64)),
        arguments("DES/ECB/PKCS5Padding", Crypto.pbe(algo = "DES", keyLen = 64)),
        arguments("DESede/CBC/PKCS5Padding", Crypto.pbe(algo = "DESede", keyLen = 192)),
        arguments("DESede/ECB/PKCS5Padding ", Crypto.pbe(algo = "DESede", keyLen = 192)),
        arguments("RSA/ECB/PKCS1Padding", key1.public, key1.private),
        arguments("RSA", key2.public, key2.private),
      )
        .map {
          val args = it.get()
          if (args.size == 2) arguments(args[0], args[1], args[1]) else it
        }
    }
  }
}