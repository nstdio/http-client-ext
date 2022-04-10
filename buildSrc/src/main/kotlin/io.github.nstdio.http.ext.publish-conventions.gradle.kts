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

import se.bjurr.gitchangelog.api.model.Tag
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask
import se.bjurr.gitchangelog.plugin.gradle.HelperParam

plugins {
    signing
    `maven-publish`

    id("io.github.gradle-nexus.publish-plugin")
    id("se.bjurr.gitchangelog.git-changelog-gradle-plugin")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("JDK Http Client Extensions")
                description.set("The set of useful extensions for JDK HttpClient.")
                url.set("https://github.com/nstdio/http-client-ext")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("nstdio")
                        name.set("Edgar Asatryan")
                        email.set("nstdio@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git@github.com:nstdio/http-client-ext.git")
                    developerConnection.set("scm:git:git@github.com:nstdio/http-client-ext.git")
                    url.set("https://github.com/nstdio/http-client-ext")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            val baseUri = uri("https://s01.oss.sonatype.org")
            nexusUrl.set(baseUri.resolve("/service/local/"))
            snapshotRepositoryUrl.set(baseUri.resolve("/content/repositories/snapshots/"))
        }
    }
}

signing {
    isRequired = (version as String).endsWith("SNAPSHOT")

    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications["mavenJava"])
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.create("changelog", GitChangelogTask::class) {
    fromRepo = project.rootDir.path
    file = File("CHANGELOG.md")
    handlebarsHelpers = listOf(
        HelperParam("shortHash") { _: Any, options ->
            return@HelperParam options.get<String>("hash").substring(0, 7)
        },
        HelperParam("compare") { _, options ->
            val tagNames = options.get<List<Tag>>("tags").map { it.name }
            val name = options.get<String>("name")
            val prevTagIdx = tagNames.indexOf(name) + 1
            val compare = name.takeIf { it != "Unreleased" } ?: "HEAD"

            return@HelperParam if (prevTagIdx < tagNames.size) "compare/${tagNames[prevTagIdx]}...$compare"
            else "releases/tag/$name"
        }
    )

    doFirst {
        templateContent = file("changelog.mustache").readText()
    }
}
