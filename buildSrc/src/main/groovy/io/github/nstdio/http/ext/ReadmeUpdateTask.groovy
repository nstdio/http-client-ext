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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class ReadmeUpdateTask extends DefaultTask {
    @TaskAction
    def update() {

        def v = project.version as String

        def file = project.file("README.md")
        def text = new StringBuilder(file.text as String)

        replaceGradle(text, v)
        replaceMaven(text, v)

        file.setText(text.toString())
    }

    static StringBuilder replaceMaven(StringBuilder text, String version) {
        def pattern = """
<dependency>
\\s+<groupId>io\\.github\\.nstdio</groupId>
\\s+<artifactId>http-client-ext</artifactId>
\\s+<version>(.+)</version>
</dependency>
"""
        def mvnPattern = Pattern.compile(pattern, Pattern.MULTILINE)
        return replacePattern(mvnPattern, text, version)
    }

    static void replaceGradle(StringBuilder text, String version) {
        def gradlePattern = Pattern.compile("implementation 'io\\.github\\.nstdio:http-client-ext:(.+)'");
        replacePattern(gradlePattern, text, version)
    }

    static void replacePattern(Pattern pattern, StringBuilder text, String version) {
        Matcher matcher = pattern.matcher(text)
        if (matcher.find()) {
            int start = matcher.start(1)
            int end = matcher.end(1)

            text.replace(start, end, version)
        }
    }
}

