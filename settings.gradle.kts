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

rootProject.name = "knotx-knot-engine"

include("knotx-knot-engine-api")
include("knotx-knot-engine-core")
include("knotx-knot-engine-handler")

project(":knotx-knot-engine-api").projectDir = file("api")
project(":knotx-knot-engine-core").projectDir = file("core")
project(":knotx-knot-engine-handler").projectDir = file("handler")