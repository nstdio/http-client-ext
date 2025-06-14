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

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.gradlex:extra-java-module-info:1.4.1")
    implementation("io.github.gradle-nexus:publish-plugin:1.3.0")
    implementation("net.researchgate:gradle-release:3.0.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    implementation("me.champeau.jmh:jmh-gradle-plugin:0.7.2")
    implementation("se.bjurr.gitchangelog:git-changelog-gradle-plugin:2.1.0") {
        isTransitive = false
    }
    implementation("se.bjurr.gitchangelog:git-changelog-lib:2.1.0") {
        exclude("org.gitlab", "java-gitlab-api")
        exclude("org.ow2.asm", "asm")
    }
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:5.2.3")
    implementation("io.github.reyerizo.gradle:jcstress-gradle-plugin:0.8.15")
}
