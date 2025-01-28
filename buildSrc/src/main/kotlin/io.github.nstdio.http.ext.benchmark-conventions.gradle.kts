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
  id("me.champeau.jmh")
  id("io.github.reyerizo.gradle.jcstress")
}

val jmhVersion = "1.37"
val jmhArtifactIds = listOf("jmh-core", "jmh-generator-annprocess", "jmh-generator-bytecode")

dependencies {
  jmhArtifactIds.forEach { jmh("org.openjdk.jmh:$it:$jmhVersion") }
}

jmh {
  includeTests.set(false)
}