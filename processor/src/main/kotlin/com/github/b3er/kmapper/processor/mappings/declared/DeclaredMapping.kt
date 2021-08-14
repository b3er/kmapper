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

package com.github.b3er.kmapper.processor.mappings.declared

import com.github.b3er.kmapper.processor.annotations.AnnotationHolder
import com.github.b3er.kmapper.processor.elements.toMappingElement
import com.github.b3er.kmapper.processor.generators.MappingGenerator
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.github.b3er.kmapper.processor.utils.kModifiers
import com.github.b3er.kmapper.processor.utils.toAnnotationSpec
import com.github.b3er.kmapper.processor.utils.toClassName
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier

abstract class DeclaredMapping : Mapping, MappingGenerator {
    abstract override val declaration: KSFunctionDeclaration
    protected abstract val overrides: List<AnnotationHolder>
    override val name: String by lazy { declaration.simpleName.getShortName() }
    override val isImplemented get() = !declaration.isAbstract
    val isDeclared = true

    val context by lazy(LazyThreadSafetyMode.NONE) { mapper.context }
    val logger by lazy(LazyThreadSafetyMode.NONE) { context.logger }

    override val sources by lazy {
        declaration.parameters.map { value ->
            value.toMappingElement()
        }
    }

    override fun write() = FunSpec.builder(name).apply {
        writeFunctionDeclaration()
        writeMapping()
    }.build()

    override fun FunSpec.Builder.writeFunctionDeclaration() {
        addModifiers(declaration.modifiers.kModifiers().filterNot { it == KModifier.ABSTRACT })

        addAnnotations(declaration.annotations.filter { ann ->
            val type = ann.annotationType.resolve().toClassName()
            overrides.none { it.matchType(type) }
        }.map { it.toAnnotationSpec(context.resolver) }.toList())

        if (isDeclared) {
            addModifiers(KModifier.OVERRIDE)
        }

        returns(target.toTypeName())

        sources.forEach { source ->
            addParameter(
                source.name,
                source.toTypeName(),
                source.modifiers
            )
        }
    }

}
