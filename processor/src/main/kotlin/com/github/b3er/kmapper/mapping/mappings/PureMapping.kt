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
import com.github.b3er.kmapper.mapping.api.MappingPropertyElement
import com.github.b3er.kmapper.mapping.common.MappingTarget
import com.squareup.kotlinpoet.FunSpec

interface PureMapping {
    val isImplemented: Boolean
    val sources: List<MappingPropertyElement>
    val target: MappingTarget
    val mapper: Mapper
    val name: String

    fun write(): FunSpec

    fun isSourceCompatibleWith(property: MappingPropertyElement, refSources: List<MappingPropertyElement>): Boolean {
        return sources.any { my -> property.isAssignableFrom(my) }
            && sources.all { my ->
            my.isAssignableFrom(property) || refSources.any {
                my.isAssignableFrom(it) && my.matchesByName(
                    it
                )
            }
        }
    }

    fun isSourceCompatibleWith(property: MappingPropertyElement): Boolean {
        return sources.any { my -> my.isAssignableFrom(property) }
    }

    fun findSource(targetName: String): Triple<Mapper, MappingPropertyElement, MappingPropertyElement>? {

        return sources.asSequence().mapNotNull { source ->
            source.findMatchingByName(targetName)?.let { Triple(mapper, source, it) }
        }.firstOrNull()
    }

    fun toFullString(): String {
        return "${mapper.toFullString()}.${name}${
            sources.joinToString(
                ", ",
                prefix = "(",
                postfix = ")"
            ) { it.shortName }
        }"
    }
}
