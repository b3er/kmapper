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


package com.github.b3er.kmapper

import com.github.b3er.kmapper.mapping.Mapper
import com.github.b3er.kmapper.mapping.api.MappingContext
import com.github.b3er.kmapper.mapping.factory.MapperModuleFactory
import com.github.b3er.kmapper.mapping.utils.check
import com.github.b3er.kmapper.mapping.utils.getAnnotation
import com.github.b3er.kmapper.mapping.utils.writeTo
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.Origin

class KMapperProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val mappers = mutableMapOf<KSDeclaration, Mapper>()
        val context = MappingContextImpl(resolver, logger, options, codeGenerator, mappers)
        resolver
            .getSymbolsWithAnnotation(MAPPER_ANNOTATION_NAME)
            .map { type ->
                logger.check(type is KSClassDeclaration && type.origin == Origin.KOTLIN && type.isOpen(), type) {
                    "@Mapper must be applied to open class or interface "
                }
                val annotation = type.getAnnotation<com.github.b3er.kmapper.Mapper>()
                logger.check(annotation != null, type) {
                    "Failed to get @Mapper annotation"
                }
                Mapper(type, context)
            }.associateByTo(mappers) { it.declaration }

        context.isResolved = true
        context.writeMappers()
        resolver.getSymbolsWithAnnotation(MAPPER_FACTORY_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { factory ->
                context.generateFactory(factory)
            }
        return emptyList()
    }

    class MappingContextImpl(
        override val resolver: Resolver,
        override val logger: KSPLogger,
        override val options: Map<String, String>,
        private val generator: CodeGenerator,
        protected val mappers: Map<KSDeclaration, Mapper>
    ) : MappingContext {
        private val generatedMappers = mutableSetOf<Mapper>()
        var isResolved = false

        fun writeMappers() {
            mappers.values.forEach(::writeMapper)
        }

        private fun writeMapper(mapper: Mapper) {
            if (generatedMappers.contains(mapper)) {
                return
            }
            if (mapper.includes.isNotEmpty()) {
                mapper.includes.keys.forEach(::writeMapper)
            }
            logger.info("Writing mapper ${mapper.declaration}", mapper.declaration)
            generatedMappers.add(mapper)
            mapper.write().writeTo(generator)
        }

        override fun findMapper(type: KSClassDeclaration): Mapper {
            require(isResolved) {
                "Context is not resolved, please try again later :)"
            }
            val mapper = mappers[type]
            require(mapper != null) {
                "Can't find mapper for $type"
            }
            return mapper
        }

        override fun mappers(): Sequence<Mapper> = mappers.values.asSequence()
        fun generateFactory(factory: KSClassDeclaration) {
            val factoryGenerator = MapperModuleFactory.createFactory(this, factory)
            factoryGenerator.write().writeTo(generator)
        }
    }

    companion object {
        const val MAPPER_ANNOTATION_NAME = "com.github.b3er.kmapper.Mapper"
        const val MAPPER_FACTORY_ANNOTATION_NAME = "com.github.b3er.kmapper.GenerateMapperFactory"
    }
}


