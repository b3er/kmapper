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

import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

interface GeneratesIterableMapping : Mapping, MappingGenerator {
    val logger: KSPLogger
    val sourceArgument: MappingElement
    val targetArgument: MappingElement
    val source: MappingElement
    override fun FunSpec.Builder.writeMapping() {
        ensureNullabilityComplies(sourceArgument, targetArgument, emptyList()) {
            "Cannot assign nullable source ${source.name}" +
                " to target ${target.name}"
        }
        CodeBlock.builder().apply {
            val targetType = ArrayList::class.asClassName().parameterizedBy(targetArgument.toTypeName())
            writeNullPreconditions()
            if (mapper.context.typeResolver.isCollection(source.type)) {
                val emptyFunction = when {
                    mapper.context.typeResolver.isImmutableList(target.type) -> {
                        MemberName("kotlinx.collections.immutable", "persistentListOf")
                    }

                    else -> {
                        MemberName("kotlin.collections", "emptyList")
                    }
                }
                addStatement("if (%N.isEmpty()) return %M()", source.name, emptyFunction)
                addStatement(
                    "val %N = %T(%N.size)",
                    "result",
                    targetType,
                    source.name
                )
            } else {
                addStatement(
                    "val %N = %T()",
                    "result",
                    targetType
                )
            }
            if (targetArgument.isAssignableFrom(sourceArgument)) {
                addStatement("%N.addAll(%N)", "result", source.name)
            } else {
                beginControlFlow("for(%N in %N) {", "item", source.name)
                val ref = findMapping(targetArgument, sourceArgument)
                val argumentsNames = sources.joinToString(", ") { "%N" }
                val argumentsValues = sources.asSequence().drop(1).map { it.name }.toList().toTypedArray()
                add("«")
                add("%N.add(", "result")
                if (ref.mapper != mapper) {
                    add("%N.", mapper.includes[ref.mapper])
                }
                add("%N(", ref.name)
                add(CodeBlock.of(argumentsNames, "item", *argumentsValues))
                add("))")
                add("»\n")
                endControlFlow()
            }
            when {
                mapper.context.typeResolver.isImmutableList(target.type) -> {
                    addStatement(
                        "return %N.%M()",
                        "result",
                        MemberName("kotlinx.collections.immutable", "toImmutableList")
                    )
                }

                mapper.context.typeResolver.isImmutableSet(target.type) -> {
                    addStatement(
                        "return %N.%M()",
                        "result",
                        MemberName("kotlinx.collections.immutable", "toImmutableSet")
                    )
                }

                else -> {
                    addStatement("return %N", "result")
                }
            }
            addCode(build())
        }
    }
}
