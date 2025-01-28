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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.regex.Pattern

abstract class ReadmeUpdateTask : DefaultTask() {
  @TaskAction
  fun update() {

    val v = project.version as String

    val file = project.file("README.md")
    val text = StringBuilder(file.readText())

    replaceMaven(text, v)
    replaceGradle(text, v)

    file.writeText(text.toString())
  }

  private fun replaceMaven(text: StringBuilder, version: String) {
    val pattern =
      "<dependency>\\s+<groupId>io\\.github\\.nstdio</groupId>\\s+<artifactId>http-client-ext</artifactId>\\s+<version>(.+)</version>\\s+</dependency>"
    val mvnPattern = Pattern.compile(pattern, Pattern.MULTILINE)
    replacePattern(mvnPattern, text, version)
  }

  private fun replaceGradle(text: StringBuilder, version: String) {
    val gradlePattern = Pattern.compile("implementation 'io\\.github\\.nstdio:http-client-ext:(.+)'")
    replacePattern(gradlePattern, text, version)
  }

  private fun replacePattern(pattern: Pattern, text: StringBuilder, version: String) {
    val matcher = pattern.matcher(text)
    if (matcher.find()) {
      val start = matcher.start(1)
      val end = matcher.end(1)

      text.replace(start, end, version)
    }
  }
}

