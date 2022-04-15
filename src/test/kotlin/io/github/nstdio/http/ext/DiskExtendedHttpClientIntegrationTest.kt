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

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import io.github.nstdio.http.ext.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofByteArray
import java.nio.file.Files
import java.time.Clock
import javax.crypto.SecretKey

internal class DiskExtendedHttpClientIntegrationTest : ExtendedHttpClientContract {

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
    stubFor(
      get(urlPathMatching("/any/[0-9]+"))
        .willReturn(
          ok()
            .withHeader("Cache-Control", "max-age=86400")
            .withBody("abc")
        )
    )

    val intRange = 0..64
    val requests = intRange
      .map { "${wm.baseUrl()}/any/$it".toUri() }
      .map { HttpRequest.newBuilder(it).build() }
      .toList()

    requests.map { client.send(it, ofByteArray()) }.forEach { it.body() }

    //when
    val newClient = ExtendedHttpClient(delegate, createCache(), Clock.systemUTC())
    val responses = requests.map { newClient.send(it, ofByteArray()) }.toList()

    //then
    responses.forEach { assertThat(it).isCached }
  }

  private fun createCache() = Cache.newDiskCacheBuilder()
    .dir(cacheDir.toPath())
    .encrypted()
    .key(secretKey)
    .cipherAlgorithm("AES")
    .build()

  override fun client(): ExtendedHttpClient {
    return client
  }

  override fun cache(): Cache {
    return cache
  }

  override fun wiremockRuntimeInfo(): WireMockRuntimeInfo {
    return wm.runtimeInfo
  }

  override fun client(clock: Clock): ExtendedHttpClient {
    return ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
  }

  companion object {
    @RegisterExtension
    @JvmStatic
    val wm: WireMockExtension = WireMockExtension.newInstance()
      .configureStaticDsl(true)
      .failOnUnmatchedRequests(true)
      .options(WireMockConfiguration.wireMockConfig().dynamicPort())
      .build()
  }
}