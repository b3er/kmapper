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

package com.github.b3er.kmapper.mapping.mappings

import com.github.b3er.kmapper.mapping.Mapper
import com.github.b3er.kmapper.mapping.api.MappingContext
import com.github.b3er.kmapper.mapping.api.MappingElement
import com.github.b3er.kmapper.mapping.common.ConstructorValuesEnumeration
import com.github.b3er.kmapper.mapping.common.toMappingElement
import com.github.b3er.kmapper.mapping.utils.check
import com.github.b3er.kmapper.mapping.utils.isEnumClass
import com.github.b3er.kmapper.mapping.utils.toClassName
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

object MappingFactory {
    fun createMapping(
        context: MappingContext,
        mapper: Mapper,
        ref: KSDeclaration?
    ): PureMapping = when (ref) {
        is KSFunctionDeclaration -> {
            context.logger.check(ref.returnType != null, ref) {
                "Mapping function must return value!"
            }
            val target = ref.returnType!!.toMappingElement(enumeration = ConstructorValuesEnumeration)
            when {
                target.declaration.isEnumClass() -> {
                    EnumMappingFunction(ref, target, mapper)
                }
                else -> {
                    SimpleMappingFunction(ref, target, mapper)
                }
            }
        }
        else -> throw IllegalArgumentException("Can't crete mapping for $ref in ${mapper.toFullString()}")
    }

    fun createGeneratedMapping(
        mapper: Mapper,
        target: MappingElement,
        source: MappingElement
    ): PureMapping {
        val context = mapper.context
        return when {
            context.typeResolver.isIterable(source.type) -> {
                context.logger.check(mapper.context.typeResolver.isIterable(target.type), mapper.declaration) {
                    "both source $source and target $target must be iterable!"
                }
                val src = source.type.arguments.first().type!!.resolve().toClassName()
                IterableGeneratedMapping(
                    generateName(mapper, "map${src.simpleName}${source.type.toClassName().simpleName}"),
                    mapper,
                    target,
                    listOf(source)
                )
            }
            target.declaration.isEnumClass() -> {
                EnumGeneratedMapping(
                    generateName(mapper, "map${source.type.toClassName().simpleName}"),
                    mapper,
                    target,
                    listOf(source)
                )
            }
            else -> {
                SimpleGeneratedMapping(
                    generateName(mapper, "map${source.type.toClassName().simpleName}"),
                    mapper,
                    target.type.toMappingElement(enumeration = ConstructorValuesEnumeration),
                    listOf(source)
                )
            }
        }
    }

    private fun generateName(mapper: Mapper, template: String): String {
        var name = template
        var i = 1
        while (mapper.allMappings().any { it.name == name }) {
            name = "template${i++}"
        }
        return name
    }
}
