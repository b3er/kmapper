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

package com.github.b3er.kmapper.mapping.mappers

import com.github.b3er.kmapper.mapping.api.MappingContext
import com.github.b3er.kmapper.mapping.common.MapperAnnotation
import com.github.b3er.kmapper.mapping.common.MappingElement
import com.github.b3er.kmapper.mapping.mappings.GeneratedMapping
import com.github.b3er.kmapper.mapping.mappings.MappingFactory
import com.github.b3er.kmapper.mapping.mappings.PureMapping
import com.github.b3er.kmapper.mapping.utils.getAnnotation
import com.github.b3er.kmapper.mapping.utils.toClassName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import java.util.*

abstract class BaseMapper(override val declaration: KSClassDeclaration, override val context: MappingContext) :
    Mapper {
    override val className by lazy { declaration.toClassName() }
    override val implementationClassName by lazy { ClassName(className.packageName, className.simpleName + "Impl") }
    override val annotation by lazy {
        declaration.getAnnotation<com.github.b3er.kmapper.Mapper>()!!.let(::MapperAnnotation)
    }

    override fun allMappings(): Sequence<PureMapping> = declaredMappings.asSequence()
    override val logger = context.logger
    override val includes by lazy {
        annotation.includes?.asSequence()
            ?.map(context::findMapper)
            ?.associate {
                it to it.declaration.simpleName.getShortName()
                    .replaceFirstChar { c -> c.lowercase(Locale.ROOT) }
            } ?: emptyMap()
    }
    protected val declaredMappings: List<PureMapping> by lazy {
        declaration.getDeclaredFunctions().map { dec ->
            MappingFactory.createMapping(context, this, dec)
        }.toList()
    }

    override fun findMapping(
        target: MappingElement,
        source: MappingElement,
        parent: PureMapping,
        createIfNeeded: Boolean
    ): PureMapping? {
        return allMappings().find { mapping ->
            target.isAssignableFrom(mapping.target) && mapping.isSourceCompatibleWith(source, parent.sources)
        } ?: includes.mapNotNull { (include, _) -> include.findMapping(target, source, parent, false) }
            .firstOrNull { it !is GeneratedMapping }
        ?: if (createIfNeeded) createMapping(target, source, parent) else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeneratedMapper

        if (declaration != other.declaration) return false

        return true
    }

    override fun hashCode(): Int {
        return declaration.hashCode()
    }
}
