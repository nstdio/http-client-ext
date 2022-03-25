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

import gradle.kotlin.dsl.accessors._d03f5c1f48338c7d05c8fd3014919501.jacoco

plugins {
    jacoco
    id("org.sonarqube")
}



sonarqube {
    properties {
        property("sonar.projectKey", "nstdio_http-client-ext")
        property("sonar.organization", "nstdio")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.withType<JacocoReport> {
    reports {
        val isCI = System.getenv("CI").toBoolean()
        xml.required.set(isCI)
        html.required.set(!isCI)
    }

    executionData(tasks.withType<Test>())
}

tasks.withType<Test> {
    finalizedBy(tasks.named("jacocoTestReport"))
}