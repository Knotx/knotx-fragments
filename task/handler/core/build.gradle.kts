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
    id("io.knotx.java-library")
    id("io.knotx.codegen")
    id("io.knotx.unit-test")
    id("io.knotx.jacoco")
    id("io.knotx.maven-publish")
    id("org.nosphere.apache.rat")
}

dependencies {
    annotationProcessor(platform("io.knotx:knotx-dependencies:${project.version}"))

    implementation(platform("io.knotx:knotx-dependencies:${project.version}"))

    api(project(":knotx-fragments-task-handler-log-api"))

    api(project(":knotx-fragments-task-factory-api"))
    implementation(project(":knotx-fragments-task-engine"))

    implementation("io.knotx:knotx-commons:${project.version}")
    implementation("io.knotx:knotx-server-http-common-placeholders:${project.version}")

    implementation(group = "io.vertx", name = "vertx-core")
    implementation(group = "io.vertx", name = "vertx-service-proxy")
    implementation(group = "io.vertx", name = "vertx-rx-java2")
    implementation(group = "io.vertx", name = "vertx-web-client")
    implementation(group = "org.apache.commons", name = "commons-lang3")
    implementation(group = "com.google.guava", name = "guava")

    testImplementation("io.knotx:knotx-junit5:${project.version}")
    testImplementation(group = "org.mockito", name = "mockito-core")
    testImplementation(group = "org.mockito", name = "mockito-junit-jupiter")
    testImplementation(group = "io.vertx", name = "vertx-web-client")
    testImplementation(group = "io.vertx", name = "vertx-rx-java2")
    testImplementation(group = "io.vertx", name = "vertx-config-hocon")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation(group = "com.github.tomakehurst", name = "wiremock")
}

tasks {
    named<RatTask>("rat") {
        excludes.addAll(listOf("*.yml", "*.md", "**/*.md", "**/build/*", "**/out/*", "**/generated/*", "**/*.adoc", "**/*.json", "**/*.conf"))
    }
    getByName("build").dependsOn("rat")
}

publishing {
    publications {
        withType(MavenPublication::class) {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}
