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

import org.apache.tools.ant.filters.ReplaceTokens
import org.nosphere.apache.rat.RatTask

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.nosphere.apache.rat") version "0.4.0"
}

group = "io.knotx"

// -----------------------------------------------------------------------------
// Dependencies
// -----------------------------------------------------------------------------

val junitTestCompile = configurations.create("junitTestCompile")
tasks.named<JavaCompile>("compileJava") {
    options.annotationProcessorGeneratedSourcesDirectory = file("src/main/generated")
}

tasks.named<Delete>("clean") {
    delete.add("src/main/generated")
}

dependencies {
    annotationProcessor(platform("io.knotx:knotx-dependencies:${project.version}"))
    annotationProcessor(group = "io.vertx", name = "vertx-codegen")
    annotationProcessor(group = "io.vertx", name = "vertx-service-proxy", classifier = "processor")
    annotationProcessor(group = "io.vertx", name = "vertx-rx-java2-gen")
    
    api("io.knotx:knotx-fragment-api")
    api("io.knotx:knotx-server-http-api")
    api(group = "com.google.guava", name = "guava")
    api(group = "commons-io", name = "commons-io")
    api(group = "org.apache.commons", name = "commons-lang3")
    api(group = "com.typesafe", name = "config")
    api(group = "commons-collections", name = "commons-collections")

    implementation(platform("io.knotx:knotx-dependencies:${project.version}"))
    implementation(group = "io.vertx", name = "vertx-core")
    implementation(group = "io.vertx", name = "vertx-service-proxy")
    implementation(group = "io.vertx", name = "vertx-rx-java2")
    implementation(group = "io.vertx", name = "vertx-codegen")
    implementation(group = "io.vertx", name = "vertx-config")
    implementation(group = "io.vertx", name = "vertx-config-hocon")
    implementation(group = "io.vertx", name = "vertx-web")
    implementation(group = "io.vertx", name = "vertx-web-api-contract")
    implementation(group = "io.vertx", name = "vertx-web-client")
    implementation(group = "io.vertx", name = "vertx-service-discovery")
    implementation(group = "io.vertx", name = "vertx-circuit-breaker")
    implementation(group = "io.vertx", name = "vertx-hazelcast")

    implementation("io.vertx:vertx-dropwizard-metrics")


    testImplementation(group = "io.knotx", name = "knotx-junit5")
    testImplementation(group = "io.vertx", name = "vertx-junit5")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-migrationsupport")
    testImplementation(group = "io.vertx", name = "vertx-unit")
    testImplementation(group = "com.github.stefanbirkner", name = "system-rules") {
        exclude(module = "junit-dep")
    }
    testImplementation(group = "com.googlecode.zohhak", name = "zohhak")
    testImplementation(group = "uk.co.datumedge", name = "hamcrest-json")
    testImplementation(group = "org.hamcrest", name = "hamcrest-all")

    testImplementation(group = "io.vertx", name = "vertx-core")
    testImplementation(group = "io.vertx", name = "vertx-web")
    testImplementation(group = "io.vertx", name = "vertx-web-api-contract")
    testImplementation(group = "io.vertx", name = "vertx-web-client")
    testImplementation(group = "io.vertx", name = "vertx-rx-java2")
    testImplementation(group = "io.vertx", name = "vertx-service-proxy")
    testImplementation(group = "io.vertx", name = "vertx-config")
    testImplementation(group = "io.vertx", name = "vertx-config-hocon")
    testImplementation(group = "io.vertx", name = "vertx-hazelcast")
}

junitTestCompile.extendsFrom(configurations.named("testImplementation").get())

// -----------------------------------------------------------------------------
// Source sets
// -----------------------------------------------------------------------------

tasks.withType<JavaCompile>().configureEach {
    with(options) {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        encoding = "UTF-8"
    }
}

sourceSets.named("main") {
    java.srcDir("src/main/generated")
}
sourceSets.create("junitTest") {
    compileClasspath += sourceSets.named("main").get().output
}


// -----------------------------------------------------------------------------
// Tasks
// -----------------------------------------------------------------------------


tasks {
    named<RatTask>("rat") {
        excludes.addAll("**/*.json", "**/*.MD", "**/*.templ", "**/*.adoc", "**/build/*", "**/out/*", "**/generated/*", "/src/test/resources/*", "*.iml")
    }
    getByName("build").dependsOn("rat")

    named<Test>("test") {
        useJUnitPlatform()
        testLogging { showStandardStreams = true }
        testLogging { showExceptions = true }
        failFast = true
    }
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

tasks.register<Jar>("testJar") {
    from(sourceSets.named("junitTest").get().output)
    classifier = "tests"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "knotx-knot-engine-api"
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            artifact(tasks["testJar"])
            pom {
                name.set("Knot.x Knot Engine API")
                description.set("Knot Engine API")
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

tasks.named<Javadoc>("javadoc") {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
