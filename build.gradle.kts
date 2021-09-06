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
plugins {
    java
    id("io.knotx.release-java")
    id("io.knotx.composite-build-support")
    id("org.nosphere.apache.rat")
    id("net.ossindex.audit")
}

allprojects {
    group = "io.knotx"
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    pluginManager.withPlugin("org.nosphere.apache.rat") {
        tasks {
            val rat = named<org.nosphere.apache.rat.RatTask>("rat") {
                verbose.set(true)
                excludes.addAll(listOf(
                    "**/*.md", // docs
                    "gradle/wrapper/**", "gradle*", "**/build/**", // Gradle
                    "*.iml", "*.ipr", "*.iws", "*.idea/**", // IDEs
                    "**/generated/*", "**/*.adoc", "**/resources/**", // assets
                    ".github/*"
                ))
            }
            getByName("check").dependsOn("rat")
            getByName("rat").dependsOn("compileJava")
        }
    }
}

subprojects {
    apply(plugin = "io.knotx.java-library")
    apply(plugin = "net.ossindex.audit")
    tasks {
        val audit = named("audit") {
            group = "verification"
        }
        named("check") {
            dependsOn(audit)
        }
        named("test") {
            mustRunAfter(audit)
        }
    }
}
