import io.github.nstdio.http.ext.ReadmeUpdateTask

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
  id("net.researchgate.release")

  id("io.github.nstdio.http.ext.benchmark-conventions")
  id("io.github.nstdio.http.ext.library-conventions")
  id("io.github.nstdio.http.ext.test-conventions")
  id("io.github.nstdio.http.ext.quality-conventions")
  id("io.github.nstdio.http.ext.publish-conventions")
}

repositories {
  mavenCentral()
}

release {
  tagTemplate.set("v\${version}")
  git {
    requireBranch.set("main")
    pushToRemote.set("origin")
  }
}

tasks.register("updateReadme", ReadmeUpdateTask::class)

tasks.named("afterReleaseBuild") {
  dependsOn("publishToSonatype", "closeAndReleaseSonatypeStagingRepository", "updateReadme")
}