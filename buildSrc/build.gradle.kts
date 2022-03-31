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

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("de.jjohannes.gradle:extra-java-module-info:0.11")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("net.researchgate:gradle-release:2.8.1")
    implementation("com.github.dpaukov:combinatoricslib3:3.3.3")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.20-RC2")
}

kotlinDslPluginOptions {
    jvmTarget.set(provider { java.targetCompatibility.toString() })
}
