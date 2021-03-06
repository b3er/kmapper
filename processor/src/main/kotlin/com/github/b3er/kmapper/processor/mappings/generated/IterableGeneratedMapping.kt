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

package com.github.b3er.kmapper.processor.mappings.generated

import com.github.b3er.kmapper.processor.annotations.MappingAnnotation
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.elements.toMappingElement
import com.github.b3er.kmapper.processor.generators.GeneratesIterableMapping
import com.github.b3er.kmapper.processor.mappers.GeneratedMapper
import com.github.b3er.kmapper.processor.mappings.Mapping

class IterableGeneratedMapping(
    parent: Mapping,
    override val name: String,
    override val mapper: GeneratedMapper,
    override val target: MappingElement,
    override val sources: List<MappingElement>
) : GeneratedMapping(parent), GeneratesIterableMapping {
    override val source: MappingElement = sources.first()
    override val overrides: List<MappingAnnotation> by lazy {
        parent.overrides.asSequence()
            .filterIsInstance<MappingAnnotation>()
            .filter { it.inherit }
            .toList()
    }

    override val sourceArgument by lazy {
        sources.first().type.arguments.first().type!!.toMappingElement(name = "item")
    }
    override val targetArgument by lazy { target.type.arguments.first().type!!.toMappingElement() }
}
