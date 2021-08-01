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

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Mapper(
    /**
     * Additional mappers to use
     */
    val uses: Array<KClass<*>> = [],
    /**
     * Additional file imports for expressions
     */
    val imports: Array<KClass<*>> = [],
    /**
     * Additional dependencies to add to constructor
     */
    val dependencies: Array<Dependency> = [],
    /**
     * Injection type for constructor annotations
     */
    val injectionType: InjectionType = InjectionType.None
) {
    enum class InjectionType {
        None, Jsr330
    }

    annotation class Dependency(
        val value: KClass<*>,
        val name: String
    )
}




