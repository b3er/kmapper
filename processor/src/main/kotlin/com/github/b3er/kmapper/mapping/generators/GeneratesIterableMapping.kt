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

import com.github.b3er.kmapper.mapping.api.MappingElement
import com.github.b3er.kmapper.mapping.common.MappingAnnotation
import com.github.b3er.kmapper.mapping.mappings.PureMapping
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

interface GeneratesIterableMapping : PureMapping, MappingGenerator {
    val overrides: List<MappingAnnotation>
    val logger: KSPLogger
    val sourceArgument: MappingElement
    val targetArgument: MappingElement
    val source: MappingElement
    override fun FunSpec.Builder.writeMapping() {
        ensureNullabiliyComplies(sourceArgument, targetArgument) {
            "Cannot assign nullable source ${source.shortName}" +
                " to target ${target.shortName}"
        }
        CodeBlock.builder().apply {
            if (source.type.isMarkedNullable) {
                addStatement("if(%N == null) return null", source.shortName)
            }
            if (mapper.context.typeResolver.isList(source.type)) {
                addStatement("if(%N.size == 0) return emptyList()", source.shortName)
                addStatement(
                    "val %N = %T(%N.size)",
                    "result",
                    ArrayList::class.asClassName().parameterizedBy(targetArgument.toTypeName()),
                    source.shortName
                )
            } else {
                addStatement(
                    "val %N = %T()",
                    "result",
                    ArrayList::class.asClassName().parameterizedBy(targetArgument.toTypeName())
                )
            }
            if (targetArgument.isAssignableFrom(sourceArgument)) {
                addStatement("%N.addAll(%N)", "result", source.shortName)
            } else {
                beginControlFlow("for(%N in %N) {", "item", source.shortName)
                val ref = findMapping(targetArgument, sourceArgument)
                if (ref.mapper == mapper) {
                    addStatement("%N.add(%N(%N))", "result", ref.name, "item")
                } else {
                    addStatement("%N.add(%N.%N(%N))", "result", mapper.includes[ref.mapper], ref.name, "item")
                }
                endControlFlow()
            }
            addStatement("return %N", "result")
            addCode(build())
        }
        //mapper.findMapping(targetArgument, )
    }
}
