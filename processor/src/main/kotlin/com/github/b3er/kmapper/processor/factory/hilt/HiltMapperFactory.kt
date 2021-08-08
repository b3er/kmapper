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

package com.github.b3er.kmapper.processor.factory.hilt

import com.github.b3er.kmapper.processor.annotations.GenerateMapperFactoryAnnotation
import com.github.b3er.kmapper.processor.factory.DeclaredMapperFactory
import com.github.b3er.kmapper.processor.mappers.Mapper
import com.github.b3er.kmapper.processor.utils.MappingContext
import com.github.b3er.kmapper.processor.utils.addOriginatingKSFile
import com.github.b3er.kmapper.processor.utils.kModifiers
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.github.b3er.kmapper.Mapper as MapperAnnotation

class HiltMapperFactory(
    override val context: MappingContext,
    override val declaration: KSClassDeclaration,
    private val annotation: GenerateMapperFactoryAnnotation
) : DeclaredMapperFactory() {
    override val factoryClassName by lazy { ClassName(className.packageName, annotation.name) }

    override fun FileSpec.Builder.writeClass() {
        TypeSpec.classBuilder(factoryClassName).apply {
            addOriginatingKSFile(declaration.containingFile!!)
            addAnnotation(AnnotationSpec.builder(DAGGER_MODULE).build())
            addAnnotation(
                AnnotationSpec.builder(HILT_INSTALL_IN).addMember("%T::class", HILT_SINGLETON_COMPONENT).build()
            )
            addModifiers(declaration.modifiers.kModifiers())
            writeFactory()
        }.also { addType(it.build()) }
    }

    private fun TypeSpec.Builder.writeFactory() {
        annotation.mappers?.map { context.findMapper(it) }?.forEach { mapper ->
            if (mapper.annotation.injectionType == MapperAnnotation.InjectionType.Jsr330) {
                writeJsrProvide(mapper)
            } else {
                writeConstructorProvide(mapper)
            }
        }
    }

    //TODO: Possible function name clash, fix?
    private fun TypeSpec.Builder.writeJsrProvide(mapper: Mapper) = FunSpec
        .builder("provide${mapper.className.simpleName}").apply {
            addAnnotation(AnnotationSpec.builder(DAGGER_PROVIDES).build())
            addParameter(ParameterSpec("impl", mapper.implementationClassName))
            addCode(CodeBlock.of("return impl"))
            returns(mapper.className)
        }.build().also { addFunction(it) }

    private fun TypeSpec.Builder.writeConstructorProvide(mapper: Mapper) = FunSpec
        .builder("provide${mapper.className.simpleName}").apply {
            addAnnotation(AnnotationSpec.builder(DAGGER_PROVIDES).build())
            returns(mapper.className)
            mapper.includes.forEach { (include, name) ->
                addParameter(ParameterSpec(name, include.className))
            }
            val template = mapper.includes.values.joinToString(separator = ", ") { "%N" }
            val args = mapper.includes.values.toTypedArray()
            addCode(CodeBlock.of("return %T($template)", *arrayOf(mapper.implementationClassName, *args)))
        }.build().also { addFunction(it) }

    companion object {
        private val DAGGER_MODULE = ClassName.bestGuess("dagger.Module")
        private val DAGGER_BINDS = ClassName.bestGuess("dagger.Bind")
        private val DAGGER_PROVIDES = ClassName.bestGuess("dagger.Provides")
        private val HILT_INSTALL_IN = ClassName.bestGuess("dagger.hilt.InstallIn")
        private val HILT_SINGLETON_COMPONENT = ClassName.bestGuess("dagger.hilt.components.SingletonComponent")
    }
}
