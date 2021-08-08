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

package com.github.b3er.kmapper.processor.generators

import com.github.b3er.kmapper.processor.annotations.MappingAnnotation
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.github.b3er.kmapper.processor.utils.check
import com.github.b3er.kmapper.processor.utils.toClassName
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

interface GeneratesSimpleMapping : Mapping, MappingGenerator {
    val overrides: List<MappingAnnotation>
    val logger: KSPLogger

    override fun FunSpec.Builder.writeMapping() {
        CodeBlock.builder().apply {
            writeNullPreconditions()
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

    fun CodeBlock.Builder.writeExpression(property: MappingElement, expression: String) {
        addStatement(
            "%N = %L,",
            property.name,
            expression
        )
    }

    fun CodeBlock.Builder.writeSourcePath(property: MappingElement, source: String) {
        val found = findSource(source)
        logger.check(found.isNotEmpty(), mapper.declaration) {
            "Source $source for target.${property.name} not found!"
        }
        writeMappingStatement(property, found.asSequence())
    }

    fun CodeBlock.Builder.writeOverrides(property: MappingElement): Boolean {
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

    fun CodeBlock.Builder.writeMappingStatement(property: MappingElement) {
        val found = findSource(property)
        logger.check(found.isNotEmpty(), mapper.declaration) {
            "Source for target.${property.name} not found." +
                " Available sources ${sources.map { it.name }}. Mapper: ${toFullString()}"
        }
        writeMappingStatement(property, found.asSequence())
    }

    fun CodeBlock.Builder.writeMappingStatement(
        target: MappingElement,
        sourcePath: Sequence<MappingElement>,
    ) {
        val source = sourcePath.distinct()
        val sourceCount = source.count()
        val property = sourcePath.last()

        val sourcePathStr = source.joinToString(".") { it.name }
        ensureNullabilityComplies(source.drop(1), target) {
            "Cannot assign nullable source $sourcePathStr" +
                " to target ${target.name}"
        }

        val sourcePathBlock = CodeBlock.of(
            source.take(sourceCount - 1).joinToString(separator = "", postfix = "%N") {
                if (it.type.isMarkedNullable) {
                    "%N?."
                } else {
                    "%N."
                }
            }, *(source.map { it.name }.toList().toTypedArray())
        )
        if (target.isAssignableFrom(property)) {
            add("«")
            add("%N = ", target.name)
            add(sourcePathBlock)
            add(",\n»")
        } else {
            add("«")
            add("%N = ", target.name)
            val nullables = property.type.isMarkedNullable && target.type.isMarkedNullable

            val ref = if (nullables) {
                peekMapping(target, property) ?: findMapping(target.makeNotNullable(), property.makeNotNullable())
            } else {
                findMapping(target, property)
            }

            val referenceBlock = if (nullables) {
                CodeBlock.of("%N", "it")
            } else {
                sourcePathBlock
            }

            if (nullables) {
                add(sourcePathBlock)
                add("?.let { ")
            }
            val refSources = ref.sources.drop(1)
            if (ref.mapper == mapper) {
                add("%N(", ref.name)
                add(referenceBlock)
                if (refSources.isNotEmpty()) {
                    add(", ${refSources.joinToString(", ") { "%N" }}", *refSources.map { it.name }.toTypedArray())
                }
                add(")")
            } else {
                add("%N.%N(", mapper.includes[ref.mapper], ref.name)
                add(referenceBlock)
                if (refSources.isNotEmpty()) {
                    add(", ${refSources.joinToString(", ") { "%N" }}", *refSources.map { it.name }.toTypedArray())
                }
                add(")")
            }
            if (nullables) {
                add(" }")
            }
            add(",\n»")
        }
    }
}
