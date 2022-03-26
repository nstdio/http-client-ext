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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.lang.Boolean as JavaBoolean

plugins {
    `java-library`
    jacoco
    id("de.jjohannes.extra-java-module-info")
}

val isCI = System.getenv("CI").toBoolean();

group = "io.github.nstdio"

java {
    withJavadocJar()
    withSourcesJar()

    sourceSets {
        create("spiTest") {
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        }
    }
}

configurations.getByName("spiTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}

mapOf(
    "spiTestImplementation" to "testImplementation",
    "spiTestRuntimeOnly" to "testRuntimeOnly",
).forEach { (t, u) ->
    configurations.getByName(t) { extendsFrom(configurations.getByName(u)) }
}

configurations.names
    .filter { !setOf("compileClasspath", "runtimeClasspath").contains(it) }
    .map { configurations.getByName(it) }
    .forEach {
        configure(listOf(it)) {
            attributes {
                @Suppress("UNCHECKED_CAST")
                val forName = Class.forName("java.lang.Boolean") as Class<JavaBoolean>
                val value: JavaBoolean = JavaBoolean.valueOf("false") as JavaBoolean

                attribute(Attribute.of("javaModule", forName), value)
            }
        }
    }

val junitVersion = "5.8.2"
val commonIoVersion = "1.3.2"
val assertJVersion = "3.22.0"
val jsonPathAssertVersion = "2.7.0"
val slf4jVersion = "1.7.36"
val jacksonVersion = "2.13.2"
val brotli4JVersion = "1.6.0"
val brotliOrgVersion = "0.1.2"

val spiDeps = listOf("org.brotli:dec:$brotliOrgVersion", "com.aayushatharva.brotli4j:brotli4j:$brotli4JVersion")

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    spiDeps.forEach { compileOnly(it) }

    /** AssertJ & Friends */
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("com.jayway.jsonpath:json-path-assert:$jsonPathAssertVersion")

    testImplementation("org.apache.commons:commons-io:$commonIoVersion")

    /** Jupiter */
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.mockito:mockito-core:4.4.0")

    testImplementation("com.github.tomakehurst:wiremock-jre8:2.32.0")
    testImplementation("com.tngtech.archunit:archunit-junit5:0.23.1")

    val spiTestImplementation = configurations.getByName("spiTestImplementation")
    spiDeps.forEach { spiTestImplementation(it) }
    spiTestImplementation("com.aayushatharva.brotli4j:native-${getArch()}:$brotli4JVersion")
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

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()

    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-Xlint:all", "-Xlint:-deprecation")
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        html.required.set(!isCI)
    }
    testLogging {
        events("skipped", "failed")
        exceptionFormat = FULL
    }
}

tasks.create<Test>("spiTest") {
    description = "Run SPI tests"
    group = "verification"
    testClassesDirs = sourceSets.getByName("spiTest").output.classesDirs
    classpath = sourceSets.getByName("spiTest").runtimeClasspath
}

tasks.check {
    dependsOn("spiTest")
}

tasks.build {
    dependsOn("spiTest")
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
