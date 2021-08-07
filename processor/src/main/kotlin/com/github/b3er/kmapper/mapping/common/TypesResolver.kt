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

package com.github.b3er.kmapper.mapping.common

import com.github.b3er.kmapper.mapping.Mapper
import com.github.b3er.kmapper.mapping.utils.getClassDeclarationByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

interface MappingContext {
    val resolver: Resolver
    val logger: KSPLogger
    val options: Map<String, String>
    val typeResolver: TypesResolver
    fun mappers(): Sequence<Mapper>
    fun findMapper(type: KSClassDeclaration): Mapper
}

class TypesResolver(resolver: Resolver) {
    private val iterableType by lazy { resolver.getClassDeclarationByName<Iterable<*>>() }

    fun isIterable(type: KSType): Boolean {
        return iterableType.asStarProjectedType().isAssignableFrom(type)
    }
}