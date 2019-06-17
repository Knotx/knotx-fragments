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

rootProject.name = "knotx-fragments-handler"

// API
include("knotx-fragments-api")

project(":knotx-fragments-api").projectDir = file("api")

// Supplier
include("knotx-fragments-supplier-api")
include("knotx-fragments-supplier-html-splitter")

project(":knotx-fragments-supplier-api").projectDir = file("supplier/api")
project(":knotx-fragments-supplier-html-splitter").projectDir = file("supplier/html-splitter")

// Handler
include("knotx-fragments-handler-api")
include("knotx-fragments-handler-core")
include("knotx-fragments-engine")

project(":knotx-fragments-handler-api").projectDir = file("handler/api")
project(":knotx-fragments-handler-core").projectDir = file("handler/core")
project(":knotx-fragments-engine").projectDir = file("handler/engine")

// Assembler
include("knotx-fragments-assembler")

project(":knotx-fragments-assembler").projectDir = file("assembler")