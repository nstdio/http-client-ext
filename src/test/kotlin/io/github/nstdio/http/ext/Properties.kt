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

import java.time.Duration
import java.util.*

internal object Properties {
  private const val NAMESPACE = "io.github.nstdio.http.ext"

  @JvmStatic
  fun duration(propertyName: String): Optional<Duration> {
    return property(propertyName).map { text: String? -> Duration.parse(text) }
  }

  private fun property(propertyName: String): Optional<String> {
    val prop = withNamespace(propertyName)
    return Optional.ofNullable(System.getProperty(prop))
      .or { Optional.ofNullable(System.getenv(propertyToEnv(prop))) }
  }

  private fun withNamespace(propertyName: String): String {
    return if (propertyName.startsWith(NAMESPACE)) propertyName else "$NAMESPACE.$propertyName"
  }

  private fun propertyToEnv(prop: String): String {
    return prop.replace('.', '_').uppercase(Locale.getDefault())
  }
}