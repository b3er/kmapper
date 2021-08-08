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

package com.github.b3er.kmapper.processor.factory

import com.github.b3er.kmapper.GenerateMapperFactory
import com.github.b3er.kmapper.processor.annotations.GenerateMapperFactoryAnnotation
import com.github.b3er.kmapper.processor.factory.hilt.HiltMapperFactory
import com.github.b3er.kmapper.processor.factory.simple.SimpleMapperFactory
import com.github.b3er.kmapper.processor.utils.MappingContext
import com.github.b3er.kmapper.processor.utils.check
import com.github.b3er.kmapper.processor.utils.getAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration

object MapperModuleFactory {
    fun createFactory(context: MappingContext, declaration: KSClassDeclaration): MapperFactory {
        val annotation = declaration
            .getAnnotation(GenerateMapperFactory::class.java)
            ?.let(::GenerateMapperFactoryAnnotation)

        context.logger.check(annotation != null, declaration) {
            "Can't find GenerateMapperFactory for $declaration"
        }
        context.logger.check(!annotation.mappers.isNullOrEmpty(), declaration) {
            "Mappers must be set in annotation $declaration"
        }
        return when (annotation.implementation) {
            GenerateMapperFactory.Implementation.Hilt -> HiltMapperFactory(context, declaration, annotation)
            // simple by default
            else -> SimpleMapperFactory(context, declaration, annotation)
        }
    }
}
