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

package com.github.b3er.kmapper.processor.utils

import com.google.devtools.ksp.symbol.KSType

class TypesResolver(context: MappingContext) {
    private val iterableType by lazy { context.resolver.getClassDeclarationByName<Iterable<*>>() }
    private val listType by lazy { context.resolver.getClassDeclarationByName<List<*>>() }
    private val collectionType by lazy { context.resolver.getClassDeclarationByName<Collection<*>>() }
    private val unitType by lazy { context.resolver.getClassDeclarationByName<Unit>() }
    private val stringType by lazy { context.resolver.getClassDeclarationByName<String>() }

    fun isIterable(type: KSType): Boolean {
        return iterableType.asStarProjectedType().isAssignableFrom(type.makeNotNullable())
    }

    fun isList(type: KSType): Boolean {
        return listType.asStarProjectedType().isAssignableFrom(type.makeNotNullable())
    }

    fun isCollection(type: KSType): Boolean {
        return collectionType.asStarProjectedType().isAssignableFrom(type.makeNotNullable())
    }

    fun isUnit(type: KSType): Boolean {
        return unitType.asStarProjectedType().isAssignableFrom(type.makeNotNullable())
    }

    fun isString(source: KSType): Boolean {
        return stringType.asType().isAssignableFrom(source.makeNotNullable())
    }
}
