/*
 * Copyright (C) 2019 Knot.x Project
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

import org.nosphere.apache.rat.RatTask

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.nosphere.apache.rat") version "0.4.0"
}

// -----------------------------------------------------------------------------
// Dependencies
// -----------------------------------------------------------------------------
dependencies {
    api("io.knotx:knotx-fragment-api")
    api("io.knotx:knotx-server-http-api")

    annotationProcessor(platform("io.knotx:knotx-dependencies:${project.version}"))
    annotationProcessor(group = "io.vertx", name = "vertx-codegen")
    annotationProcessor(group = "io.vertx", name = "vertx-service-proxy", classifier = "processor")
    annotationProcessor(group = "io.vertx", name = "vertx-rx-java2-gen")

    implementation(group = "io.vertx", name = "vertx-circuit-breaker")
    implementation(group = "org.apache.commons", name = "commons-lang3")
}

// -----------------------------------------------------------------------------
// Source sets
// -----------------------------------------------------------------------------
tasks.named<JavaCompile>("compileJava") {
    options.annotationProcessorGeneratedSourcesDirectory = file("src/main/generated")
}
tasks.named<Delete>("clean") {
    delete.add("src/main/generated")
}
sourceSets.named("main") {
    java.srcDir("src/main/generated")
}

// -----------------------------------------------------------------------------
// Tasks
// -----------------------------------------------------------------------------
tasks {
    named<RatTask>("rat") {
        excludes.addAll("**/*.md", "**/*.adoc", "**/build/*", "**/out/*", "**/generated/*")
    }
    getByName("build").dependsOn("rat")
}

// -----------------------------------------------------------------------------
// Publication
// -----------------------------------------------------------------------------
tasks.register<Jar>("sourcesJar") {
    from(sourceSets.named("main").get().allJava)
    classifier = "sources"
}
tasks.register<Jar>("javadocJar") {
    from(tasks.named<Javadoc>("javadoc"))
    classifier = "javadoc"
}
tasks.named<Javadoc>("javadoc") {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "knotx-knot-engine-api"
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set("Knot.x Knot Engine API")
                description.set("Knot Engine API module contains all Knot related interfaces.")
                url.set("http://knotx.io")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("marcinczeczko")
                        name.set("Marcin Czeczko")
                        email.set("https://github.com/marcinczeczko")
                    }
                    developer {
                        id.set("skejven")
                        name.set("Maciej Laskowski")
                        email.set("https://github.com/Skejven")
                    }
                    developer {
                        id.set("tomaszmichalak")
                        name.set("Tomasz Michalak")
                        email.set("https://github.com/tomaszmichalak")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Knotx/knotx-knot-engine.git")
                    developerConnection.set("scm:git:ssh://github.com:Knotx/knotx-knot-engine.git")
                    url.set("http://knotx.io")
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                credentials {
                    username = if (project.hasProperty("ossrhUsername")) project.property("ossrhUsername")?.toString() else "UNKNOWN"
                    password = if (project.hasProperty("ossrhPassword")) project.property("ossrhPassword")?.toString() else "UNKNOWN"
                    println("Connecting with user: ${username}")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

apply(from = "../gradle/common.deps.gradle.kts")