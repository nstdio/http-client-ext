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

import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
  jacoco
  id("com.github.spotbugs")
}

jacoco {
  toolVersion = "0.8.8"
}

spotbugs {
  toolVersion.set("4.7.2")
  effort.set(Effort.MAX_VALUE)
  excludeFilter.set(file("spotbugs.exclude.xml"))
}

dependencies {
  spotbugsPlugins("com.mebigfatguy.sb-contrib:sb-contrib:7.6.9")
  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}

tasks {
  withType<JacocoReport> {
    reports {
      val isCI = System.getenv("CI").toBoolean()
      xml.required.set(isCI)
      html.required.set(!isCI)
    }

    afterEvaluate {
      classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it).apply { exclude("io/**/NullCache.class", "io/**/Lazy.class") }
      }))
    }

    executionData(withType<Test>())
  }

  withType<Test> {
    finalizedBy(named("jacocoTestReport"))
  }

  withType<SpotBugsTask> {
    enabled = name == "spotbugsMain"
  }
}
