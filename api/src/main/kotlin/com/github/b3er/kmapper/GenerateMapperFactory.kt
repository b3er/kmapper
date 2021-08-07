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

package com.github.b3er.kmapper

/**
 * Generates  factory in this package with specified name
 * [Implementation.Simple] - just an object used to get mapper instance (new instance is returned each call)
 * [Implementation.Hilt] - Generates hilt Binds module, optional you can annotate mappers as Singleton
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateMapperFactory(
    val name: String,
    val implementation: Implementation
) {
    enum class Implementation {
        Simple, Hilt
    }
}
