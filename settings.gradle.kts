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
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2/") }
        mavenLocal()
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
include("knotx-fragments-task-factory-config")
project(":knotx-fragments-task-factory-api").projectDir = file("task/factory/api")
project(":knotx-fragments-task-factory-config").projectDir = file("task/factory/config")

include("knotx-fragments-engine-api")
include("knotx-fragments-engine-core")
project(":knotx-fragments-engine-api").projectDir = file("task/engine/api")
project(":knotx-fragments-engine-core").projectDir = file("task/engine/core")

include("knotx-fragments-handler-core")
include("knotx-fragments-execution-log-api")
include("knotx-fragments-execution-log-html")
include("knotx-fragments-execution-log-json")
project(":knotx-fragments-handler-core").projectDir = file("task/handler/core")
project(":knotx-fragments-execution-log-api").projectDir = file("task/handler/log-consumer/api")
project(":knotx-fragments-execution-log-html").projectDir = file("task/handler/log-consumer/html")
project(":knotx-fragments-execution-log-json").projectDir = file("task/handler/log-consumer/json")