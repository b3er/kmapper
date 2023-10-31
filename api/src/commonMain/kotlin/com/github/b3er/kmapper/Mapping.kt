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

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Mapping(
    /**
     * source property name
     */
    val source: String = "",
    /**
     * target property name
     */
    val target: String = "",
    /**
     * kotlin code expression
     */
    val expression: String = "",
    /**
     * [NullabilityCheckStrategy] used in this mapping
     */
    val nullabilityStrategy: NullabilityCheckStrategy = NullabilityCheckStrategy.Default,
    /**
     * Indicates that this annotation should be applied to nested generated (not declared) mappers
     */
    val inherit: Boolean = false,
    /**
     * Options for mapping
     */
    vararg val options: Option
) {
    /**
     * Nullability checking strategy used in this mapper when target
     * and source nullability don't match and can't be assigned.
     */
    public enum class NullabilityCheckStrategy {
        /**
         * Uses [Mapper.NullabilityCheckStrategy] defined in this mapper
         */
        Default,

        /**
         * Check nullability in stage of generation, fail if mismatch
         */
        Source,

        /**
         * Check nullability at runtime by using !!
         */
        Runtime,

        /**
         * Check nullability at runtime throwing [MappingException]
         */
        RuntimeException,
    }

    public enum class Option {
        /**
         * Assign false if source is null, e.g use target = source == true
         */
        NullableBooleanToFalse,

        /**
         * Assign '' if source is null
         */
        NullableStringToEmpty
    }
}
