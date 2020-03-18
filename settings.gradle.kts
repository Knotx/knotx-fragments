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

// API
include("knotx-fragments-api")
project(":knotx-fragments-api").projectDir = file("api")

// Supplier
include("knotx-fragments-supplier-api")
include("knotx-fragments-supplier-single-fragment")
include("knotx-fragments-supplier-html-splitter")
project(":knotx-fragments-supplier-api").projectDir = file("supplier/api")
project(":knotx-fragments-supplier-single-fragment").projectDir = file("supplier/single-fragment")
project(":knotx-fragments-supplier-html-splitter").projectDir = file("supplier/html-splitter")

// Actions
include("knotx-fragments-action-api")
include("knotx-fragments-action-core")
project(":knotx-fragments-action-api").projectDir = file("actions/api")
project(":knotx-fragments-action-core").projectDir = file("actions/core")

// Handler
include("knotx-fragments-handler-api")
include("knotx-fragments-handler-core")
include("knotx-fragments-handler-consumer-html")
project(":knotx-fragments-handler-api").projectDir = file("handler/api")
project(":knotx-fragments-handler-core").projectDir = file("handler/core")
project(":knotx-fragments-handler-consumer-html").projectDir = file("handler/consumer/html")

// Engine
include("knotx-fragments-engine-api")
include("knotx-fragments-engine-core")
project(":knotx-fragments-engine-api").projectDir = file("engine/api")
project(":knotx-fragments-engine-core").projectDir = file("engine/core")

// Assembler
include("knotx-fragments-assembler")
project(":knotx-fragments-assembler").projectDir = file("assembler")