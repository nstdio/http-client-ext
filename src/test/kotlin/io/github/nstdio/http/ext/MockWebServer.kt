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

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import java.time.Instant

fun MockWebServer.enqueue(response: MockResponse, count: Int) {
  (0..count).forEach { _ -> enqueue(response) }
}

fun MockResponse.addHeaderDate(date: Instant): MockResponse = addDateHeader(Headers.HEADER_DATE, date)

fun MockResponse.addDateHeader(name: String, date: Instant): MockResponse = addHeader(name, Headers.toRFC1123(date))

fun MockResponse.setBody(body: ByteArray): MockResponse = setBody(Buffer().also { it.write(body) })

fun MockResponse.ok(): MockResponse = setResponseCode(200)

fun MockResponse.notModified(): MockResponse = setResponseCode(304)