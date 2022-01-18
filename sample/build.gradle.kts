/*
 * Copyright (C) 2021 Ilya Usanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(projects.api)
    implementation(libs.kotlinPoet.library)
    ksp(projects.processor)
    kspTest(projects.processor)

    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.engine)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
