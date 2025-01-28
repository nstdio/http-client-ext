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

import io.github.nstdio.http.ext.Assertions.assertThat
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.discarding
import java.net.http.HttpResponse.BodyHandlers.ofByteArray
import java.nio.file.Files
import java.time.Clock
import java.util.concurrent.TimeUnit.SECONDS
import javax.crypto.SecretKey

@MockWebServerTest
internal class DiskExtendedHttpClientIntegrationTest(private val mockWebServer: MockWebServer) :
  ExtendedHttpClientContract {

  private val delegate = HttpClient.newHttpClient()
  private lateinit var cacheDir: File
  private lateinit var client: ExtendedHttpClient
  private lateinit var cache: Cache

  private lateinit var secretKey: SecretKey

  @BeforeEach
  fun setUp() {
    val dir = Files.createTempDirectory("diskcache").toFile()
    dir.deleteOnExit()
    cacheDir = dir

    secretKey = Crypto.pbe()
    cache = createCache()
    client = ExtendedHttpClient(delegate, cache, Clock.systemUTC())
  }

  @AfterEach
  fun tearDown() {
    cache.evictAll()
    cacheDir.listFiles()?.forEach { it.delete() }
  }

  @Test
  fun `Should restore cache`() {
    //given
    val range = 0..64
    stubNumericCached(range)

    val requests = httpRequests(range).toList()
    requests.map { client.send(it, ofByteArray()) }.forEach { it.body() }

    //when
    val newClient = ExtendedHttpClient(delegate, createCache(), Clock.systemUTC())
    val responses = requests.map { newClient.send(it, ofByteArray()) }

    //then
    responses.forEach { assertThat(it).isCached }
  }

  @Test
  fun `Should close cache`() {
    //given
    stubNumericCached(0..8)

    //when
    cache.use {
      httpRequests()
        .forEach { client.send(it, discarding()).body() }
    }

    //then
    await.atMost(1, SECONDS).until { cacheDir.listFiles()?.isEmpty() }
  }

  private fun stubNumericCached(range: IntRange) {
    range.forEach { _ ->
      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .addHeader("Cache-Control", "max-age=86400")
          .setBody("abc")
      )
    }
  }

  private fun httpRequests(range: IntRange = 0..8) = range
    .map { mockWebServer.url(it.toString()).toUri() }
    .map { HttpRequest.newBuilder(it).build() }

  private fun createCache() = Cache.newDiskCacheBuilder()
    .dir(cacheDir.toPath())
    .encrypted()
    .key(secretKey)
    .cipherAlgorithm("AES")
    .build()

  override fun client() = client

  override fun mockWebServer() = mockWebServer

  override fun cache() = cache

  override fun client(clock: Clock) = ExtendedHttpClient(delegate, cache, clock)
}