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

package com.github.b3er.kmapper.processor.mappings

import com.github.b3er.kmapper.processor.elements.ConstructorValuesEnumeration
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.elements.toMappingElement
import com.github.b3er.kmapper.processor.mappers.GeneratedMapper
import com.github.b3er.kmapper.processor.mappers.Mapper
import com.github.b3er.kmapper.processor.mappings.declared.EnumDeclaredMapping
import com.github.b3er.kmapper.processor.mappings.declared.IterableDeclaredMapping
import com.github.b3er.kmapper.processor.mappings.declared.SimpleDeclaredMapping
import com.github.b3er.kmapper.processor.mappings.generated.EnumGeneratedMapping
import com.github.b3er.kmapper.processor.mappings.generated.IterableGeneratedMapping
import com.github.b3er.kmapper.processor.mappings.generated.SimpleGeneratedMapping
import com.github.b3er.kmapper.processor.utils.*
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

object MappingFactory {
    fun createMapping(
        context: MappingContext,
        mapper: Mapper,
        ref: KSDeclaration?
    ): Mapping = when (ref) {
        is KSFunctionDeclaration -> {
            context.logger.check(ref.returnType != null, ref) {
                "Mapping function must return value!"
            }
            val target = ref.returnType!!.toMappingElement(enumeration = ConstructorValuesEnumeration)
            context.logger.check(target.type.declaration.isKotlin, mapper.declaration) {
                "Target $target must be a kotlin class, for mapper ${mapper.toFullString()}"
            }
            context.logger.check(!context.typeResolver.isUnit(target.type), ref) {
                "Mapping function must return value!"
            }
            when {
                context.typeResolver.isIterable(target.type) -> {
                    IterableDeclaredMapping(ref, target, mapper)
                }
                target.declaration.isEnumClass() -> {
                    EnumDeclaredMapping(ref, target, mapper)
                }
                else -> {
                    SimpleDeclaredMapping(ref, target, mapper)
                }
            }
        }
        else -> throw IllegalArgumentException("Can't crete mapping for $ref in ${mapper.toFullString()}")
    }

    fun createGeneratedMapping(
        mapper: GeneratedMapper,
        target: MappingElement,
        source: MappingElement
    ): Mapping {
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
                context.logger.check(target.type.declaration.isKotlin, mapper.declaration) {
                    "Target $target must be a kotlin class, for mapper ${mapper.toFullString()}"
                }
                SimpleGeneratedMapping(
                    generateName(mapper, "map${source.type.toClassName().simpleName}"),
                    mapper,
                    target.type.toMappingElement(target.node, enumeration = ConstructorValuesEnumeration),
                    listOf(source)
                )
            }
        }
    }

    private fun generateName(mapper: GeneratedMapper, template: String): String {
        var name = template
        var i = 1
        while (mapper.allMappings().any { it.name == name }) {
            name = "$template${i++}"
        }
        return name
    }
}
