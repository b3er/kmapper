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

package com.github.b3er.kmapper.mapping.generators

import com.github.b3er.kmapper.mapping.api.MappingPropertyElement
import com.github.b3er.kmapper.mapping.common.MappingAnnotation
import com.github.b3er.kmapper.mapping.common.MappingTargetProperty
import com.github.b3er.kmapper.mapping.mappings.PureMapping
import com.github.b3er.kmapper.mapping.utils.check
import com.github.b3er.kmapper.mapping.utils.toClassName
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

interface GeneratesSimpleMapping : PureMapping, MappingGenerator {
    val overrides: List<MappingAnnotation>
    val logger: KSPLogger

    override fun FunSpec.Builder.writeMapping() {
        CodeBlock.builder().apply {
            add("return %T(\n", target.type.toClassName()).indent()
            target.properties.forEach { property ->
                if (!writeOverrides(property)) {
                    writeMappingStatement(property)
                }
            }
            unindent().add(")")
            addCode(build())
        }
    }

    fun CodeBlock.Builder.writeExpression(property: MappingTargetProperty, expression: String) {
        addStatement(
            "%N = %L,",
            property.shortName,
            expression
        )
    }

    fun CodeBlock.Builder.writeSourcePath(property: MappingTargetProperty, source: String) {
        val path = source.split('.')
        logger.check(path.size <= 2, mapper.declaration) {
            "Invalid source: '${source}' in ${toFullString()}. Only two level sources supported. "
        }
        if (path.size > 1) {
            val found = sources.find { it.matchesByName(path.first()) }
            logger.check(found != null, mapper.declaration) {
                "Source for target.${property.shortName} not found!"
            }
            val sourceProperty = found.properties.find { it.matchesByName(path.last()) }
            logger.check(sourceProperty != null, mapper.declaration) {
                "Source for target.${property.shortName} not found!"
            }
            writeMappingStatement(property, found, sourceProperty)
        } else {
            val found = findSource(path.first())
            logger.check(found != null, mapper.declaration) {
                "Source for target.${property.shortName} not found!"
            }
            writeMappingStatement(property, found.second, found.third)
        }
    }

    fun CodeBlock.Builder.writeOverrides(property: MappingTargetProperty): Boolean {
        val override = overrides.find { property.matchesByName(it.target) }
        if (override?.expression?.isNotEmpty() == true) {
            writeExpression(property, override.expression)
            return true
        }
        if (override?.source?.isNotEmpty() == true) {
            writeSourcePath(property, override.source)
            return true
        }
        return false
    }

    fun CodeBlock.Builder.writeMappingStatement(property: MappingTargetProperty) {
        val found = findSource(property.shortName)
        logger.check(found != null, mapper.declaration) {
            "Source for target.${property.shortName} not found!"
        }
        writeMappingStatement(property, found.second, found.third)
    }

    fun CodeBlock.Builder.writeMappingStatement(
        target: MappingTargetProperty,
        source: MappingPropertyElement,
        property: MappingPropertyElement
    ) {
        if (target.isAssignableFrom(property)) {
            if (source != property) {
                addStatement(
                    "%N = %N.%N,",
                    target.shortName,
                    source.shortName,
                    property.shortName
                )
            } else {
                addStatement(
                    "%N = %N,",
                    target.shortName,
                    source.shortName
                )
            }
        } else {
            val ref = mapper.findMapping(target, property, this@GeneratesSimpleMapping)
            logger.check(ref != null, mapper.declaration) {
                "can't find mapping for target.${property.shortName}"
            }
            if (ref.mapper == mapper) {
                addStatement(
                    "%N = %N(%N.%N),",
                    target.shortName,
                    ref.name,
                    source.shortName,
                    property.shortName
                )
            } else {
                val refSources = ref.sources.drop(1)
                addStatement(
                    "%N = %N.%N(%N.%N,${refSources.joinToString(", ") { "%N" }}),",
                    *(arrayOf(
                        target.shortName,
                        mapper.includes[ref.mapper],
                        ref.name,
                        source.shortName,
                        property.shortName
                    ) + refSources.map { it.shortName })
                )
            }
        }
    }
}
