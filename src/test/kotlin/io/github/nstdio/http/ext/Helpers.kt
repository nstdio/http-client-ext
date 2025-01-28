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

import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpResponse.ResponseInfo

internal object Helpers {
  fun responseInfo(headers: Map<String, String>) = responseInfo0(headers.mapValues { listOf(it.value) }.toMutableMap())

  fun responseInfo0(headers: Map<String, List<String>>): ResponseInfo = object : ResponseInfo {
    override fun statusCode() = 200

    override fun headers() = HttpHeaders.of(headers) { _, _ -> true }

    override fun version() = HttpClient.Version.HTTP_1_1
  }
}