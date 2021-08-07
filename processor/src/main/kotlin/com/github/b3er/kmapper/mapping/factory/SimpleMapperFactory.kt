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

package com.github.b3er.kmapper.mapping.factory

import com.github.b3er.kmapper.MappersFactory
import com.github.b3er.kmapper.mapping.api.MappingContext
import com.github.b3er.kmapper.mapping.utils.addOriginatingKSFile
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass

class SimpleMapperFactory(
    override val context: MappingContext,
    override val declaration: KSClassDeclaration,
    private val annotation: GenerateMapperFactoryAnnotation
) : DeclaredMapperFactory() {
    override val factoryClassName by lazy { ClassName(className.packageName, annotation.name) }

    override fun FileSpec.Builder.writeClass() {
        TypeSpec.objectBuilder(factoryClassName).apply {
            addSuperinterface(MappersFactory::class)
            addOriginatingKSFile(declaration.containingFile!!)
            addAnnotation(
                AnnotationSpec.builder(Suppress::class.asClassName()).addMember("%S", "UNCHECKED_CAST").build()
            )
            writeFactory()
        }.also { addType(it.build()) }
    }

    private fun TypeSpec.Builder.writeFactory() = FunSpec.builder(FACTORY_FUNCTION_NAME).apply {
        addModifiers(KModifier.OVERRIDE)
        val type = TypeVariableName("T", Any::class)
        addTypeVariable(type)
        addParameter(ParameterSpec.builder("cls", KClass::class.asTypeName().parameterizedBy(type)).build())
        returns(type)
        val code = CodeBlock.builder()
        code.beginControlFlow("return when (%N) {", "cls")
        context.mappers().forEach { mapper ->
            if (mapper.includes.isEmpty()) {
                code.addStatement("%T::class -> %T() as %T", mapper.className, mapper.implementationClassName, type)

            } else {
                val includes = mapper.includes.keys.joinToString(", ") {
                    "getMapper(%T::class)"
                }
                val includesArgs = mapper.includes.keys.map { it.className }
                code.addStatement(
                    "%T::class -> %T($includes) as %T",
                    *(arrayOf(mapper.className, mapper.implementationClassName) + includesArgs), type
                )
            }
        }
        code.addStatement(
            "else -> throw %T(%P)",
            IllegalArgumentException::class.asClassName(),
            "Can't find mapper for class \${cls}"
        )
        code.endControlFlow()
        addCode(code.build())
    }.also { addFunction(it.build()) }

    companion object {
        const val FACTORY_FUNCTION_NAME = "getMapper"
    }
}

