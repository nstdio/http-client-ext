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

plugins {
    signing
    `maven-publish`

    id("io.github.gradle-nexus.publish-plugin")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

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
