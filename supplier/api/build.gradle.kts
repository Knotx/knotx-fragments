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
    id("io.knotx.maven-publish")
    id("org.nosphere.apache.rat") version "0.6.0"
}

dependencies {
    implementation(platform("io.knotx:knotx-dependencies:${project.version}"))
    api(project(":knotx-fragments-api"))
    api("io.knotx:knotx-server-http-api:${project.version}")

    implementation(group = "io.vertx", name = "vertx-core")
    implementation(group = "io.vertx", name = "vertx-service-proxy")
    implementation(group = "io.vertx", name = "vertx-rx-java2")
    implementation(group = "org.apache.commons", name = "commons-lang3")
}

tasks {
    named<RatTask>("rat") {
        excludes.addAll(listOf("*.md", "**/*.md", "**/build/*", "**/out/*", "**/generated/*", "/src/test/resources/*"))
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
