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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.invoke
import org.gradle.nativeplatform.OperatingSystemFamily.LINUX
import org.gradle.nativeplatform.OperatingSystemFamily.MACOS
import org.gradle.nativeplatform.OperatingSystemFamily.WINDOWS
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentArchitecture
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.Boolean
import kotlin.Suppress

plugins {
  `java-library`
  id("org.gradlex.extra-java-module-info")
}

val isCI = System.getenv("CI").toBoolean()

val junitVersion = "5.11.4"
val assertJVersion = "3.27.3"
val kotestAssertionsVersion = "5.9.1"
val mockitoVersion = "5.15.2"
val jsonPathAssertVersion = "2.7.0"
val slf4jVersion = "2.0.16"
val jacksonVersion = "2.18.2"
val brotli4JVersion = "1.18.0"
val brotliOrgVersion = "0.1.2"
val zstdJniVersion = "1.5.6-9"
val gsonVersion = "2.11.0"
val equalsverifierVersion = "3.18.1"
val coroutinesVersion = "1.10.1"

val optionalDeps = listOf(
  "org.brotli:dec:$brotliOrgVersion",
  "com.aayushatharva.brotli4j:brotli4j:$brotli4JVersion",
  "com.github.luben:zstd-jni:$zstdJniVersion",
  "com.google.code.gson:gson:$gsonVersion",
  "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion"
)

dependencies {
  optionalDeps.forEach { compileOnly(it) }

  testImplementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))

  /** AssertJ & Friends */
  testImplementation("org.assertj:assertj-core:$assertJVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestAssertionsVersion")
  testImplementation("io.kotest:kotest-property:$kotestAssertionsVersion")
  testImplementation("io.kotest:kotest-assertions-json:$kotestAssertionsVersion")

  /** Jupiter */
  testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
  testImplementation("org.mockito:mockito-core:$mockitoVersion")
  testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")

  testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("com.squareup.okhttp3:mockwebserver3-junit5:5.0.0-alpha.10")

  testImplementation("nl.jqno.equalsverifier:equalsverifier:$equalsverifierVersion")
  testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

  /** Kotlin Coroutines */
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  optionalDeps.forEach { testImplementation(it) }
  testImplementation("com.aayushatharva.brotli4j:native-${arch()}:$brotli4JVersion")
}

tasks {
  withType<Test> {
    useJUnitPlatform()
    reports {
      html.required.set(!isCI)
    }
    testLogging {
      events("skipped", "failed")
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  withType<KotlinCompile> {
    kotlinOptions {
      languageVersion = "2.0"
      jvmTarget = JavaVersion.VERSION_11.toString()
    }
  }
}

configurations.names
  .filter { !setOf("compileClasspath", "runtimeClasspath").contains(it) }
  .map { configurations.getByName(it) }
  .forEach {
    configure(listOf(it)) {
      attributes {
        attribute(Attribute.of("javaModule", Boolean::class.java), Boolean.valueOf("false") as Boolean)
      }
    }
  }

extraJavaModuleInfo {
  module("dec-${brotliOrgVersion}.jar", "org.brotli.dec", brotliOrgVersion) {
    exports("org.brotli.dec")
  }
}

fun arch() = when (getCurrentOperatingSystem().toFamilyName()) {
  LINUX -> if (getCurrentArchitecture().isArm) "linux-aarch64" else "linux-x86_64"
  WINDOWS -> "windows-x86_64"
  MACOS -> if (getCurrentArchitecture().isArm) "osx-aarch64" else "osx-x86_64"
  else -> "unknown"
}