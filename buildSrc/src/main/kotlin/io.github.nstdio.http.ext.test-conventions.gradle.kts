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
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.paukov.combinatorics3.Generator
import java.lang.Boolean
import kotlin.String
import kotlin.Suppress
import kotlin.to

plugins {
  `java-library`
  idea
  id("de.jjohannes.extra-java-module-info")
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
val kotestAssertionsVersion = "5.4.2"
val mockitoVersion = "4.8.0"
val jsonPathAssertVersion = "2.7.0"
val slf4jVersion = "1.7.36"
val jacksonVersion = "2.13.4"
val brotli4JVersion = "1.8.0"
val brotliOrgVersion = "0.1.2"
val gsonVersion = "2.9.1"
val equalsverifierVersion = "3.10.1"
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
  testImplementation("com.tngtech.archunit:archunit-junit5:1.0.0-rc1")

  /** Kotlin Coroutines */
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  spiDeps.forEach { spiTestImplementation(it) }
  spiTestImplementation("com.aayushatharva.brotli4j:native-${getArch()}:$brotli4JVersion")
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
        @Suppress("UNCHECKED_CAST")
        val forName = Class.forName("java.lang.Boolean") as Class<Boolean>
        val value: Boolean = Boolean.valueOf("false") as Boolean

        attribute(Attribute.of("javaModule", forName), value)
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

fun getArch(): String {
  val operatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

  if (operatingSystem.isWindows) return "windows-x86_64"
  else if (operatingSystem.isMacOsX) return "osx-x86_64"
  else if (operatingSystem.isLinux)
    return if (DefaultNativePlatform.getCurrentArchitecture().isArm) "linux-aarch64"
    else "linux-x86_64"

  return ""
}