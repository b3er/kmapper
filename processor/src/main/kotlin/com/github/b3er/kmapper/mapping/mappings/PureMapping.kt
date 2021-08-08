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

import com.github.b3er.kmapper.mapping.common.MappingElement
import com.github.b3er.kmapper.mapping.mappers.Mapper
import com.github.b3er.kmapper.mapping.utils.check
import com.squareup.kotlinpoet.FunSpec

interface PureMapping {
    val isImplemented: Boolean
    val sources: List<MappingElement>
    val target: MappingElement
    val mapper: Mapper
    val name: String

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
        return sources.find { element ->
            element.matchesByName(target)
        }?.let(::listOf) ?: sources.asSequence().mapNotNull { element ->
            if (element.matchesByName(target)) {
                listOf(element)
            } else {
                element.properties.find { property -> property.matchesByName(target) }
                    ?.let { listOf(element, it) }
            }
        }.firstOrNull() ?: emptyList()
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
    ): PureMapping {
        val ref = mapper.findMapping(target, property, this@PureMapping, createIfNeeded = createIfNeeded)
        mapper.context.logger.check(ref != null, mapper.declaration) {
            "can't find mapping for target.${property.name}"
        }
        return ref
    }

    fun peekMapping(
        target: MappingElement,
        property: MappingElement
    ): PureMapping? {
        return mapper.findMapping(target, property, this@PureMapping, createIfNeeded = false)
    }

    fun ensureNullabilityComplies(source: MappingElement, target: MappingElement, message: () -> String) {
        if (source.type.isMarkedNullable && !target.type.isMarkedNullable) {
            mapper.context.logger.error(message(), mapper.declaration)
        }
    }

    fun ensureNullabilityComplies(source: Sequence<MappingElement>, target: MappingElement, message: () -> String) {
        if (source.any { it.type.isMarkedNullable } && !target.type.isMarkedNullable) {
            mapper.context.logger.error(message(), mapper.declaration)
        }
    }
}
