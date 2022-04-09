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

plugins {
  `java-library`
  kotlin("jvm")
}

group = "io.github.nstdio"

java {
  targetCompatibility = JavaVersion.VERSION_11
  sourceCompatibility = JavaVersion.VERSION_11

  withJavadocJar()
  withSourcesJar()
}

configurations
  .filter { arrayOf("compileClasspath", "runtimeClasspath").contains(it.name) }
  .forEach {
    it.exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
  }

tasks.named("compileKotlin") {
  enabled = false
}

sourceSets {
  main {
    java.srcDirs -= file("src/main/kotlin")
  }
}

val lombokVersion = "1.18.22"
val lombokEnabled = false
dependencies {
  if (lombokEnabled) {
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.compilerArgs = listOf("-Xlint:all", "-Xlint:-deprecation")
}
