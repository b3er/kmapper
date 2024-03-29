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

package com.github.b3er.kmapper.processor.mappers

import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.github.b3er.kmapper.processor.mappings.MappingFactory
import com.github.b3er.kmapper.processor.utils.MappingContext
import com.github.b3er.kmapper.processor.utils.addOriginatingKSFile
import com.github.b3er.kmapper.processor.utils.kModifiers
import com.github.b3er.kmapper.processor.utils.toAnnotationSpec
import com.github.b3er.kmapper.processor.utils.toClassName
import com.github.b3er.kmapper.processor.utils.toTypeName
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.github.b3er.kmapper.Mapper as MapperClassAnnotation

class GeneratedMapper(declaration: KSClassDeclaration, context: MappingContext) : BaseMapper(declaration, context) {
    private val createdMappings = mutableListOf<Mapping>()
    override fun allMappings(): Sequence<Mapping> = declaredMappings.asSequence() + createdMappings.asSequence()

    override fun createMapping(
        target: MappingElement,
        source: MappingElement,
        parent: Mapping
    ): Mapping {
        return MappingFactory.createGeneratedMapping(this, parent, target, source).also {
            createdMappings.add(it)
        }
    }

    override fun write(): FileSpec {
        val fileSpec = FileSpec.builder(implementationClassName.packageName, implementationClassName.simpleName)
            .addComment("Code generated by KMapper. Do not edit.")
        annotation.imports?.map { it.toClassName() }?.forEach { import ->
            fileSpec.addImport(import.packageName, import.simpleName)
        }
        val typeSpec = TypeSpec.classBuilder(implementationClassName)

        typeSpec.addOriginatingKSFile(declaration.containingFile!!)

        if (declaration.classKind == ClassKind.INTERFACE) {
            typeSpec.addSuperinterface(className)
        } else {
            typeSpec.superclass(className)
        }

        typeSpec.addModifiers(declaration.modifiers.kModifiers().filterNot { it == KModifier.ABSTRACT })

        typeSpec.addAnnotations(declaration.annotations.filter {
            it.annotationType != annotation.annotation.annotationType
        }.map { it.toAnnotationSpec(context.resolver) }.toList())

        val constSpec = FunSpec.constructorBuilder()
        if (annotation.injectionType == MapperClassAnnotation.InjectionType.Jsr330
            || annotation.injectionType == MapperClassAnnotation.InjectionType.Anvil
        ) {
            logger.info("Using JSR303 injection", declaration)
            constSpec.addAnnotation(AnnotationSpec.builder(ClassName.bestGuess("javax.inject.Inject")).build())
        }
        if (annotation.injectionType == MapperClassAnnotation.InjectionType.Anvil) {
            logger.info("Using Anvil/Dagger injection", declaration)
            annotation.injectionScope?.also { injectionScope ->
                if (injectionScope.toClassName().simpleName != "Unit") {
                    typeSpec.addAnnotation(
                        AnnotationSpec
                            .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesBinding"))
                            .addMember("scope = %T::class", injectionScope.toTypeName())
                            .build()
                    )
                }
            }
        }
        includes.forEach { (mapper, name) ->
            constSpec.addParameter(name, mapper.declaration.toClassName())
            typeSpec.addProperty(
                PropertySpec
                    .builder(name, mapper.declaration.toClassName())
                    .also {
                        if (declaration.getAllProperties().any { prop -> prop.simpleName.getShortName() == name }) {
                            it.addModifiers(KModifier.OVERRIDE)
                        } else {
                            it.addModifiers(KModifier.PRIVATE)
                        }
                    }.initializer(name).build()
            )
        }
        typeSpec.primaryConstructor(constSpec.build())
        declaredMappings.filter { !it.isImplemented }
            .forEach { mapper -> mapper.write().also { typeSpec.addFunction(it) } }
        val created = createdMappings.toList()
        created
            .forEach { mapper ->
                mapper.write().also { typeSpec.addFunction(it) }
            }
        typeSpec.writeCreatedMappings()
        fileSpec.addType(typeSpec.build())
        return fileSpec.build()
    }

    fun TypeSpec.Builder.writeCreatedMappings() {
        //TODO: Rewrite!
        while (createdMappings.any { !it.isImplemented }) {
            createdMappings.filterNot { it.isImplemented }.forEach { mapping ->
                mapping.write().also { addFunction(it) }
            }
        }
    }
}

