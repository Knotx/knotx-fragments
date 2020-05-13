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
    id("io.knotx.unit-test")
    id("io.knotx.jacoco")
    id("org.nosphere.apache.rat") version "0.6.0"
}

dependencies {
    implementation(platform("io.knotx:knotx-dependencies:${project.version}"))

    testImplementation(group = "org.mockito", name = "mockito-core")
    testImplementation(group = "org.mockito", name = "mockito-junit-jupiter")
    testImplementation(group = "io.vertx", name = "vertx-web-client")
    testImplementation(group = "io.vertx", name = "vertx-rx-java2")
    testImplementation(group = "io.vertx", name = "vertx-config-hocon")
    testImplementation(group = "com.github.tomakehurst", name = "wiremock")

    // logging
    testRuntimeOnly("io.knotx:knotx-launcher:${project.version}")
    // handler dependencies
    testImplementation(project(":knotx-fragments-task-handler"))
    testImplementation(project(":knotx-fragments-task-factory-default"))
    testRuntimeOnly(project(":knotx-fragments-task-handler-log-html"))
    testRuntimeOnly(project(":knotx-fragments-task-handler-log-json"))
}

tasks {
    named<RatTask>("rat") {
        excludes.addAll(listOf("**/build/*", "**/*.conf"))
    }
    getByName("build").dependsOn("rat")
}