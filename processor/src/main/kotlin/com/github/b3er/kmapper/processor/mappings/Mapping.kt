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

import com.github.b3er.kmapper.processor.annotations.AnnotationHolder
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.mappers.Mapper
import com.github.b3er.kmapper.processor.mappings.declared.DeclaredMapping
import com.github.b3er.kmapper.processor.utils.check
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.FunSpec
import com.github.b3er.kmapper.Mapping as MappingAnnotation

interface Mapping {
    val isImplemented: Boolean
    val sources: List<MappingElement>
    val target: MappingElement
    val mapper: Mapper
    val name: String
    val declaration: KSNode
    val overrides: List<AnnotationHolder>

    fun write(): FunSpec

    fun isSourceCompatibleWith(property: MappingElement, refSources: List<MappingElement>): Boolean {
        return sources.any { my -> property.isAssignableFrom(my) }
            && sources.all { my ->
            my.isAssignableFrom(property) || refSources.any {
                my.isAssignableFrom(it) && my.matchesByName(
                    it
                )
            }
        }
    }

    fun findSource(target: MappingElement): List<MappingElement> {
        return sources.asSequence().mapNotNull { element ->
            element.properties.find { property -> property.matchesByName(target) }
                ?.let { listOf(element, it) } ?: element.takeIf { it.matchesByName(target) }?.let(::listOf)
        }.firstOrNull() ?: sources.find { element ->
            element.matchesByName(target)
        }?.let(::listOf) ?: emptyList()
    }

    fun isSourceCompatibleWith(property: MappingElement): Boolean {
        return sources.any { my -> my.isAssignableFrom(property) }
    }

    fun findSource(path: String): List<MappingElement> {
        val split = path.split('.')
        return split.fold(ArrayList(split.size.coerceAtLeast(2))) { elements, name ->
            if (elements.isEmpty()) {
                (sources.find { it.matchesByName(name) }
                    ?: sources.first().properties.find { it.matchesByName(name) }?.also {
                        elements.add(sources.first())
                    })
            } else {
                elements.last().properties.find { it.matchesByName(name) }
            }?.also(elements::add)
            elements
        }
    }

    fun toFullString(): String {
        return "${mapper.toFullString()}.${name}${
            sources.joinToString(
                ", ",
                prefix = "(",
                postfix = ")"
            ) { it.name }
        }"
    }

    fun findMapping(
        target: MappingElement,
        property: MappingElement,
        createIfNeeded: Boolean = true
    ): Mapping {
        val ref = mapper.findMapping(target, property, this@Mapping, createIfNeeded = createIfNeeded)
        mapper.context.logger.check(ref != null, declaration) {
            "can't find mapping for target.${property.name}"
        }
        return ref
    }

    fun peekMapping(
        target: MappingElement,
        property: MappingElement
    ): Mapping? {
        return mapper.findMapping(target, property, this@Mapping, createIfNeeded = false)
    }

    fun ensureNullabilityComplies(
        source: MappingElement,
        target: MappingElement,
        options: List<MappingAnnotation.Option>,
        message: () -> String
    ) {
        ensureNullabilityComplies(sequenceOf(source), target, options, message)
    }

    fun ensureNullabilityComplies(
        source: Sequence<MappingElement>,
        target: MappingElement,
        options: List<MappingAnnotation.Option>,
        message: () -> String
    ) {
        if (source.any { it.type.isMarkedNullable } && !target.type.isMarkedNullable) {
            if (mapper.context.typeResolver.isBoolean(target.type)
                && options.contains(MappingAnnotation.Option.NullableBooleanToFalse)
            ) {
                return
            }
            if (mapper.context.typeResolver.isString(target.type)
                && options.contains(MappingAnnotation.Option.NullableStringToEmpty)
            ) {
                return
            }
            mapper.context.logger.error(message(), declaration)
        }
    }
}
