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

import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
  jacoco
  id("com.github.spotbugs")
}

jacoco {
  toolVersion = "0.8.7"
}

spotbugs {
  toolVersion.set("4.6.0")
  effort.set(Effort.MAX_VALUE)
  excludeFilter.set(file("spotbugs.exclude.xml"))
}

dependencies {
  spotbugsPlugins("com.mebigfatguy.sb-contrib:sb-contrib:7.4.7")
  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
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

tasks.withType<SpotBugsTask> {
  enabled = name == "spotbugsMain"
}