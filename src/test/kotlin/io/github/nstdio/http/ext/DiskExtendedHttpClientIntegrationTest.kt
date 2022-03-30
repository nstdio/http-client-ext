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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.io.IOException
import java.net.http.HttpClient
import java.nio.file.Files
import java.time.Clock

internal class DiskExtendedHttpClientIntegrationTest : ExtendedHttpClientContract {
    @RegisterExtension
    val wm: WireMockExtension = WireMockExtension.newInstance()
        .configureStaticDsl(true)
        .failOnUnmatchedRequests(true)
        .options(WireMockConfiguration.wireMockConfig().dynamicPort())
        .build()

    private val delegate = HttpClient.newHttpClient()
    private var cacheDir: File? = null
    private var client: ExtendedHttpClient? = null
    private var cache: DiskCache? = null

    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        val dir = Files.createTempDirectory("diskcache").toFile()
        dir.deleteOnExit()
        cache = DiskCache(dir.toPath())
        cacheDir = dir
        client = ExtendedHttpClient(delegate, cache, Clock.systemUTC())
    }

    @AfterEach
    fun tearDown() {
        cache!!.evictAll()
        cacheDir?.listFiles()?.forEach { it.delete() }
    }

    override fun client(): ExtendedHttpClient {
        return client!!
    }

    override fun cache(): Cache {
        return cache!!
    }

    override fun wiremockRuntimeInfo(): WireMockRuntimeInfo {
        return wm.runtimeInfo
    }

    override fun client(clock: Clock): ExtendedHttpClient {
        return ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock)
    }
}