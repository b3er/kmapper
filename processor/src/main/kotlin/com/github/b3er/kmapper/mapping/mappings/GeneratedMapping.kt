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

import com.github.b3er.kmapper.mapping.api.AnnotationHolder
import com.github.b3er.kmapper.mapping.api.MappingSource
import com.github.b3er.kmapper.mapping.generators.MappingGenerator
import com.github.b3er.kmapper.mapping.utils.toClassName
import com.squareup.kotlinpoet.FunSpec

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

abstract class GeneratedMapping : PureMapping, MappingGenerator {
    protected abstract val overrides: List<AnnotationHolder>
    abstract override val sources: List<MappingSource>
    override val isImplemented get() = false
    val context by lazy(LazyThreadSafetyMode.NONE) { mapper.context }
    val logger by lazy(LazyThreadSafetyMode.NONE) { context.logger }

    override fun write() = FunSpec.builder(name).apply {
        writeFunctionDeclaration()
        writeMapping()
    }.build()

    override fun FunSpec.Builder.writeFunctionDeclaration() {
        returns(target.type.toClassName())

        sources.forEach { source ->
            addParameter(
                source.shortName,
                source.type.toClassName(),
                source.modifiers().toList()
            )
        }
    }
}
