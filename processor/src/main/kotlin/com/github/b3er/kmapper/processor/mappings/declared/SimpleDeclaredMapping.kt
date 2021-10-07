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

import com.github.b3er.kmapper.Mapping
import com.github.b3er.kmapper.Mappings
import com.github.b3er.kmapper.processor.annotations.MappingAnnotation
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.generators.GeneratesSimpleMapping
import com.github.b3er.kmapper.processor.mappers.Mapper
import com.github.b3er.kmapper.processor.utils.get
import com.github.b3er.kmapper.processor.utils.getAnnotation
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class SimpleDeclaredMapping(
    override val declaration: KSFunctionDeclaration,
    override val target: MappingElement,
    override val mapper: Mapper,
) : DeclaredMapping(), GeneratesSimpleMapping {
    override val overrides by lazy {
        declaration.getAnnotation<Mapping>()?.let { listOf(MappingAnnotation(it)) }
            ?: (declaration.getAnnotation<Mappings>()?.get("mapping")?.value as? Collection<*>)
                ?.filterIsInstance<KSAnnotation>()?.map(::MappingAnnotation)?.toList()
            ?: emptyList()
    }
}
