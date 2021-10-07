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

import com.github.b3er.kmapper.Mapping.NullabilityCheckStrategy
import com.github.b3er.kmapper.Mapping.Option
import com.github.b3er.kmapper.processor.annotations.MappingAnnotation
import com.github.b3er.kmapper.processor.annotations.isRuntime
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.github.b3er.kmapper.processor.utils.check
import com.github.b3er.kmapper.processor.utils.toClassName
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName

interface GeneratesSimpleMapping : Mapping, MappingGenerator {
    override val overrides: List<MappingAnnotation>
    val logger: KSPLogger

    override fun FunSpec.Builder.writeMapping() {
        CodeBlock.builder().apply {
            writeNullPreconditions()
            add("return %T(\n", target.type.toClassName()).indent()
            target.properties.forEach { property ->
                if (!writeOverrides(property)) {
                    writeMappingStatement(property, findSource(property))
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


    fun CodeBlock.Builder.writeOverrides(property: MappingElement): Boolean {
        val override = findOverride(property)
        if (override?.expression?.isNotEmpty() == true) {
            writeExpression(property, override.expression)
            return true
        }
        if (override?.source?.isNotEmpty() == true) {
            writeMappingStatement(property, findSource(override.source))
            return true
        }
        return false
    }

    fun CodeBlock.Builder.writeMappingStatement(
        target: MappingElement,
        sourcePath: List<MappingElement>,
    ) {
        if (target.hasDefault && sourcePath.isEmpty()) {
            // Skip this property bc target has default and no source is found
            return
        }

        logger.check(sourcePath.isNotEmpty(), declaration) {
            "Source for target.${target.name} not found in ${toFullString()}"
        }

        val source = sourcePath.asSequence().distinct()
        val sourceCount = source.count()
        val property = sourcePath.last()
        val nullabilityCheckStrategy = getNullabilityStrategy(target)
        val options = findOverride(property)?.options ?: emptyList()

        val nullableToNonNullable = !target.type.isMarkedNullable && property.type.isMarkedNullable

        val sourcePathStr = source.joinToString(".") { it.name }

        if (nullabilityCheckStrategy == NullabilityCheckStrategy.Source) {
            ensureNullabilityComplies(source.drop(1), target, options) {
                "Cannot assign nullable source $sourcePathStr" +
                    " to target ${target.name}"
            }
        }

        val sourcePathBlock = CodeBlock.of(
            source.take(sourceCount - 1)
                .joinToString(separator = "", postfix = "%N") { "%N." }, *(source.map { it.name }
                .toList().toTypedArray())
        )
        if (target.isAssignableFrom(property, ignoreNullability = true)) {
            add("«")
            add("%N = ", target.name)
            add(sourcePathBlock)
        } else {
            add("«")
            add("%N = ", target.name)
            val nullables = (property.type.isMarkedNullable && target.type.isMarkedNullable)
                || (nullableToNonNullable && nullabilityCheckStrategy.isRuntime)
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
        }

        if (nullableToNonNullable) {
            if (mapper.context.typeResolver.isBoolean(target.type) && options.contains(Option.NullableBooleanToFalse)) {
                add(" == true")
            } else if (mapper.context.typeResolver.isString(target.type) && options.contains(Option.NullableStringToEmpty)) {
                add(" ?: \"\"")
            } else if (nullabilityCheckStrategy == NullabilityCheckStrategy.Runtime) {
                add("!!")
            } else if (nullabilityCheckStrategy == NullabilityCheckStrategy.RuntimeException) {
                add(" ?: %M(%S, %S)", NULLABLE_ERROR_FUNCTION, sourcePathStr, target.name)
            }
        }
        add(",\n»")
    }

    private fun getNullabilityStrategy(property: MappingElement): NullabilityCheckStrategy {
        return findOverride(property)?.nullabilityStrategy
            ?: mapper.annotation.nullabilityStrategy?.let {
                NullabilityCheckStrategy.valueOf(it.name)
            } ?: NullabilityCheckStrategy.Source
    }


    private fun findOverride(property: MappingElement): MappingAnnotation? {
        return overrides
            .find { property.matchesByName(it.target) } ?: overrides.find { it.target.isEmpty() }
    }


    companion object {
        private  val NULLABLE_ERROR_FUNCTION = MemberName("com.github.b3er.kmapper","assignNullableError")
    }
}
