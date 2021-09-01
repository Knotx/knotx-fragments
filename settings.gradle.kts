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
pluginManagement {
    val version: String by settings
    plugins {
        id("io.knotx.java-library") version version
        id("io.knotx.codegen") version version
        id("io.knotx.unit-test") version version
        id("io.knotx.jacoco") version version
        id("io.knotx.maven-publish") version version
        id("io.knotx.composite-build-support") version version
        id("io.knotx.release-java") version version
        id("org.nosphere.apache.rat") version "0.7.0"
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "knotx-fragments"

// Fragment

include("knotx-fragments-api")
project(":knotx-fragments-api").projectDir = file("api")

include("knotx-fragments-action-api")
include("knotx-fragments-action-core")
include("knotx-fragments-action-library")
project(":knotx-fragments-action-api").projectDir = file("action/api")
project(":knotx-fragments-action-core").projectDir = file("action/core")
project(":knotx-fragments-action-library").projectDir = file("action/library")

include("knotx-fragments-supplier-api")
include("knotx-fragments-supplier-single-fragment")
include("knotx-fragments-supplier-html-splitter")
project(":knotx-fragments-supplier-api").projectDir = file("supplier/api")
project(":knotx-fragments-supplier-single-fragment").projectDir = file("supplier/single-fragment")
project(":knotx-fragments-supplier-html-splitter").projectDir = file("supplier/html-splitter")

include("knotx-fragments-assembler")
project(":knotx-fragments-assembler").projectDir = file("assembler")

// Task

include("knotx-fragments-task-api")
project(":knotx-fragments-task-api").projectDir = file("task/api")

include("knotx-fragments-task-factory-api")
include("knotx-fragments-task-factory-default")
project(":knotx-fragments-task-factory-api").projectDir = file("task/factory/api")
project(":knotx-fragments-task-factory-default").projectDir = file("task/factory/default")

include("knotx-fragments-task-engine")
project(":knotx-fragments-task-engine").projectDir = file("task/engine")

include("knotx-fragments-task-handler")
include("knotx-fragments-task-handler-log-api")
include("knotx-fragments-task-handler-log-html")
include("knotx-fragments-task-handler-log-json")
project(":knotx-fragments-task-handler").projectDir = file("task/handler/core")
project(":knotx-fragments-task-handler-log-api").projectDir = file("task/handler/log/api")
project(":knotx-fragments-task-handler-log-html").projectDir = file("task/handler/log/html")
project(":knotx-fragments-task-handler-log-json").projectDir = file("task/handler/log/json")

include("knotx-fragments-task-functional-test")
project(":knotx-fragments-task-functional-test").projectDir = file("task/functional")