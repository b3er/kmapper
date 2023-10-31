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

import com.github.b3er.kmapper.Mapper.InjectionType
import com.github.b3er.kmapper.Mapper.NullabilityCheckStrategy
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Mapper(
    /**
     * Additional mappers to use
     */
    val uses: Array<KClass<*>> = [],
    /**
     * Additional file imports for expressions
     */
    val imports: Array<KClass<*>> = [],
    /**
     * Injection type for constructor annotations
     */
    val injectionType: InjectionType = InjectionType.None,
    /**
     *  Default [NullabilityCheckStrategy] in this mapper
     */
    val nullabilityStrategy: NullabilityCheckStrategy = NullabilityCheckStrategy.Source,
    /**
     * Default [CheckSeverity] for all enum mappers
     */
    val enumComplianceCheck: CheckSeverity = CheckSeverity.Error,
    /**
     * Default source [EnumNaming] for all enum mappers
     */
    val enumSourceNaming: EnumNaming = EnumNaming.None,
    /**
     * Default target [EnumNaming] for all enum mappers
     */
    val enumTargetNaming: EnumNaming = EnumNaming.None,
    /**
     * Injection scope for [InjectionType.Anvil]
     */
    val injectionScope: KClass<*> = Unit::class
) {
    public enum class InjectionType {
        None, Jsr330, Anvil
    }

    /**
     * Nullability checking strategy used in this mapper when target
     * and source nullability don't match and can't be assigned.
     */
    public enum class NullabilityCheckStrategy {
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
        RuntimeException
    }
}




