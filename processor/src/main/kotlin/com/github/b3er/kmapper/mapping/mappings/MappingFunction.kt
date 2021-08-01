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

import com.github.b3er.kmapper.mapping.api.MappingFunctionAnnotation
import com.github.b3er.kmapper.mapping.common.MappingSource
import com.github.b3er.kmapper.mapping.generators.MappingGenerator
import com.github.b3er.kmapper.mapping.utils.kModifiers
import com.github.b3er.kmapper.mapping.utils.toAnnotationSpec
import com.github.b3er.kmapper.mapping.utils.toClassName
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier

//
//class GeneratedMapping(
//    override val mapper: Mapper,
//    override val name: String,
//    override val target: MappingTarget,
//    override val sources: List<MappingSource>
//) : PureMapping {
//    protected fun FunSpec.Builder.writeFunctionDeclaration() {
//
//    }
//}

abstract class MappingFunction : PureMapping, MappingGenerator {
    abstract val declaration: KSFunctionDeclaration
    protected abstract val overrides: List<MappingFunctionAnnotation>
    override val name: String by lazy { declaration.simpleName.getShortName() }

    val isImplemented get() = !declaration.isAbstract
    val isDeclared = true

    val context by lazy(LazyThreadSafetyMode.NONE) { mapper.context }
    val logger by lazy(LazyThreadSafetyMode.NONE) { context.logger }

    override val sources by lazy { declaration.parameters.map(::MappingSource) }

    override fun write() = FunSpec.builder(name).apply {
        writeFunctionDeclaration()
        writeMapping()
    }.build()

    override fun FunSpec.Builder.writeFunctionDeclaration() {
        addModifiers(declaration.modifiers.kModifiers())

        addAnnotations(declaration.annotations.filter { ann ->
            overrides.none { it.annotation.annotationType == ann.annotationType }
        }.map { it.toAnnotationSpec(context.resolver) }.toList())

        if (isDeclared) {
            addModifiers(KModifier.OVERRIDE)
        }

        returns(target.type.toClassName())

        sources.forEach { source ->
            addParameter(
                source.shortName,
                source.type.toClassName(),
                source.declaration.kModifiers().toList()
            )
        }
    }

}
