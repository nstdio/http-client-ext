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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.invoke
import org.gradle.nativeplatform.OperatingSystemFamily.LINUX
import org.gradle.nativeplatform.OperatingSystemFamily.MACOS
import org.gradle.nativeplatform.OperatingSystemFamily.WINDOWS
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentArchitecture
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.paukov.combinatorics3.Generator
import java.lang.Boolean
import kotlin.Suppress
import kotlin.to

plugins {
  `java-library`
  idea
  id("org.gradlex.extra-java-module-info")
}

val isCI = System.getenv("CI").toBoolean()

java {
  sourceSets {
    create("spiTest") {
      val output = sourceSets.main.get().output
      compileClasspath += output
      runtimeClasspath += output
    }
  }
}
val sourceSetsSpiTest by sourceSets.named("spiTest")
val spiTestImplementation by configurations

idea.module {
  testSourceDirs.addAll(sourceSetsSpiTest.allSource.srcDirs)
}

mapOf(
  "spiTestImplementation" to "testImplementation",
  "spiTestRuntimeOnly" to "testRuntimeOnly",
).forEach { (t, u) ->
  configurations.getByName(t) { extendsFrom(configurations.getByName(u)) }
}

tasks.withType<Test> {
  useJUnitPlatform()
  reports {
    html.required.set(!isCI)
  }
  testLogging {
    events("skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}

val junitVersion = "5.9.1"
val assertJVersion = "3.23.1"
val kotestAssertionsVersion = "5.5.4"
val mockitoVersion = "4.8.1"
val jsonPathAssertVersion = "2.7.0"
val slf4jVersion = "1.7.36"
val jacksonVersion = "2.14.0-rc2"
val brotli4JVersion = "1.8.0"
val brotliOrgVersion = "0.1.2"
val gsonVersion = "2.10"
val equalsverifierVersion = "3.11.1"
val coroutinesVersion = "1.6.4"

val jsonLibs = mapOf(
  "jackson" to "com.fasterxml.jackson.core",
  "gson" to "gson-$gsonVersion"
)

val spiDeps = listOf(
  "org.brotli:dec:$brotliOrgVersion",
  "com.aayushatharva.brotli4j:brotli4j:$brotli4JVersion",
  "com.google.code.gson:gson:$gsonVersion",
  "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion"
)

dependencies {
  spiDeps.forEach { compileOnly(it) }

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

  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("com.squareup.okhttp3:mockwebserver3-junit5:5.0.0-alpha.10")

  testImplementation("nl.jqno.equalsverifier:equalsverifier:$equalsverifierVersion")
  testImplementation("com.tngtech.archunit:archunit-junit5:1.0.1")

  /** Kotlin Coroutines */
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  spiDeps.forEach { spiTestImplementation(it) }
  spiTestImplementation("com.aayushatharva.brotli4j:native-${arch()}:$brotli4JVersion")
}

Generator.subset(jsonLibs.keys)
  .simple()
  .stream()
  .forEach { it ->
    val postFix = it.joinToString("And") { it.capitalized() }
    val taskName = if (postFix.isEmpty()) "spiTest" else "spiTestWithout${postFix}"
    tasks.create<Test>(taskName) {
      description = "Run SPI tests"
      group = "verification"
      testClassesDirs = sourceSetsSpiTest.output.classesDirs
      classpath = sourceSetsSpiTest.runtimeClasspath

      doFirst {
        val toExclude = classpath.filter { file -> it?.any { file.absolutePath.contains(it) } ?: false }
        classpath -= toExclude
      }
    }
  }

tasks.create<Task>("spiMatrixTest") {
  description = "The aggregator task for all tests"
  group = "verification"
  dependsOn(tasks.filter { it.name.startsWith("spiTest") })
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    useK2 = true
    jvmTarget = JavaVersion.VERSION_11.toString()
  }
}

tasks.check {
  dependsOn("spiMatrixTest")
}

tasks.build {
  dependsOn("spiMatrixTest")
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
  module("brotli4j-${brotli4JVersion}.jar", "com.aayushatharva.brotli4j", brotli4JVersion) {
    exports("com.aayushatharva.brotli4j")
    exports("com.aayushatharva.brotli4j.common")
    exports("com.aayushatharva.brotli4j.decoder")
    exports("com.aayushatharva.brotli4j.encoder")
  }

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