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

package com.github.b3er.kmapper.mapping

import com.github.b3er.kmapper.mapping.api.MappingContext
import com.github.b3er.kmapper.mapping.api.MappingPropertyElement
import com.github.b3er.kmapper.mapping.common.MappingTarget
import com.github.b3er.kmapper.mapping.common.MappingTargetProperty
import com.github.b3er.kmapper.mapping.mappings.EnumMappingFunction
import com.github.b3er.kmapper.mapping.mappings.GenericMappingFunction
import com.github.b3er.kmapper.mapping.mappings.MappingFunction
import com.github.b3er.kmapper.mapping.utils.*
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import java.util.*
import com.github.b3er.kmapper.Mapper as MapperAnnotation

class Mapper(val declaration: KSClassDeclaration, val context: MappingContext) {
    private val className by lazy { declaration.toClassName() }
    private val implementationClassName by lazy { ClassName(className.packageName, className.simpleName + "Impl") }
    private val mappings: List<MappingFunction> by lazy {
        declaration.getDeclaredFunctions().map { dec ->
            context.logger.check(dec.returnType != null, declaration) {
                "Mapping function must return value!"
            }
            val target = MappingTarget(dec.returnType!!)
            if (target.declaration.isEnumClass()) {
                EnumMappingFunction(dec, target, this)
            } else {
                GenericMappingFunction(dec, target, this)
            }
        }.toList()
    }
    private val annotation by lazy { declaration.getAnnotation<MapperAnnotation>()!! }
    val logger = context.logger
    val includes by lazy {
        (annotation["uses"]?.value as List<*>)
            .asSequence()
            .filterIsInstance<KSType>()
            .map(KSType::declaration)
            .filterIsInstance<KSClassDeclaration>()
            .map(context::findMapper)
            .associate {
                it to it.declaration.simpleName.getShortName()
                    .replaceFirstChar { c -> c.lowercase(Locale.getDefault()) }
            }
    }
    private val injectionType by lazy {
        ((annotation["injectionType"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { MapperAnnotation.InjectionType.valueOf(it) }
    }

    fun findMapping(
        target: MappingTargetProperty,
        source: MappingPropertyElement
    ): MappingFunction? {
        return mappings.find { mapping ->
            mapping.target.matches(target) && mapping.isSourceCompatibleWith(source)
        } ?: includes.mapNotNull { (include, _) -> include.findMapping(target, source) }.firstOrNull()
    }

    fun createMapping(
        target: MappingTargetProperty,
        source: MappingPropertyElement,
        reference: GenericMappingFunction
    ) {

    }


    fun write(): FileSpec {
        val fileSpec = FileSpec.builder(implementationClassName.packageName, implementationClassName.simpleName)
        val typeSpec = TypeSpec.classBuilder(implementationClassName)

        typeSpec.addSuperinterface(className)

        typeSpec.addModifiers(declaration.modifiers.kModifiers())

        typeSpec.addAnnotations(declaration.annotations.filter {
            it.annotationType != annotation.annotationType
        }.map { it.toAnnotationSpec(context.resolver) }.toList())

        val constSpec = FunSpec.constructorBuilder()
        if (injectionType == MapperAnnotation.InjectionType.Jsr330) {
            logger.info("Using JSR303 injection", declaration)
            constSpec.addAnnotation(AnnotationSpec.builder(ClassName.bestGuess("javax.Inject")).build())
        }
        includes.forEach { (mapper, name) ->
            constSpec.addParameter(name, mapper.declaration.toClassName())
            typeSpec.addProperty(
                PropertySpec
                    .builder(name, mapper.declaration.toClassName(), KModifier.PRIVATE)
                    .initializer(name).build()
            )
        }
        typeSpec.primaryConstructor(constSpec.build())
        mappings.filter { !it.isImplemented }
            .forEach { mapper -> mapper.write().also { typeSpec.addFunction(it) } }
        fileSpec.addType(typeSpec.build())
        return fileSpec.build()
    }

    fun toFullString(): String {
        return className.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mapper

        if (declaration != other.declaration) return false

        return true
    }

    override fun hashCode(): Int {
        return declaration.hashCode()
    }
}

