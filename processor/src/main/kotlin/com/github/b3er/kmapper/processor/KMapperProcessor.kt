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


package com.github.b3er.kmapper.processor

import com.github.b3er.kmapper.processor.factory.MapperModuleFactory
import com.github.b3er.kmapper.processor.mappers.DeclaredMapper
import com.github.b3er.kmapper.processor.mappers.GeneratedMapper
import com.github.b3er.kmapper.processor.mappers.Mapper
import com.github.b3er.kmapper.processor.utils.*
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class KMapperProcessor(
    private val codeGenerator: CodeGenerator,
    override val logger: KSPLogger,
    override val options: Map<String, String>
) : SymbolProcessor, MappingContext {
    override val typeResolver: TypesResolver = TypesResolver(this)
    private val mappers = ConcurrentHashMap<KSDeclaration, Mapper>()
    private val generatedMappers = mutableSetOf<Mapper>()
    override lateinit var resolver: Resolver

    override fun process(resolver: Resolver): List<KSAnnotated> {
        this.resolver = resolver

        resolver
            .getSymbolsWithAnnotation(MAPPER_ANNOTATION_NAME)
            .map { type ->
                logger.check(type is KSClassDeclaration && type.isKotlinClass(resolver) && type.isOpen(), type) {
                    "@Mapper must be applied to open class or interface "
                }
                val annotation = type.getAnnotation<com.github.b3er.kmapper.Mapper>()
                logger.check(annotation != null, type) {
                    "Failed to get @Mapper annotation"
                }
                GeneratedMapper(type, this)
            }.associateByTo(mappers) { it.declaration }

        writeMappers()

        resolver.getSymbolsWithAnnotation(MAPPER_FACTORY_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { factory ->
                val factoryGenerator = MapperModuleFactory.createFactory(this, factory)
                factoryGenerator.write().writeTo(codeGenerator)
            }

        return emptyList()
    }

    private fun writeMappers() {
        mappers.values.forEach(::writeMapper)
    }

    private fun writeMapper(mapper: Mapper) {
        if (generatedMappers.contains(mapper)) {
            return
        }
        if (mapper.includes.isNotEmpty()) {
            mapper.includes.keys.forEach(::writeMapper)
        }
        if (mapper is GeneratedMapper) {
            logger.info("Writing mapper ${mapper.declaration}", mapper.declaration)
            generatedMappers.add(mapper)
            try {
                mapper.write().writeTo(codeGenerator)
            }catch (e: Throwable) {
                logger.error("Failed to write mapper ${mapper.declaration}", mapper.declaration)
                logger.exception(e)
            }
        } else {
            generatedMappers.add(mapper)
        }
    }

    override fun findMapper(type: KSClassDeclaration): Mapper {
        val mapper = mappers[type] ?: resolver.getClassDeclarationByName(type.qualifiedName!!)
            ?.let { DeclaredMapper(it, this) }
            ?.also { mappers[it.declaration] = it }

        require(mapper != null) {
            "Can't find mapper for $type"
        }
        return mapper
    }

    companion object {
        const val MAPPER_ANNOTATION_NAME = "com.github.b3er.kmapper.Mapper"
        const val MAPPER_FACTORY_ANNOTATION_NAME = "com.github.b3er.kmapper.GenerateMapperFactory"
    }
}


