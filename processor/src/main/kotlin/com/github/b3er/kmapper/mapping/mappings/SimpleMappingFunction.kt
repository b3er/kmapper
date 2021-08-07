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

import com.github.b3er.kmapper.Mapping
import com.github.b3er.kmapper.mapping.Mapper
import com.github.b3er.kmapper.mapping.common.MappingAnnotation
import com.github.b3er.kmapper.mapping.common.MappingTarget
import com.github.b3er.kmapper.mapping.generators.GeneratesSimpleMapping
import com.github.b3er.kmapper.mapping.utils.getAnnotations
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class SimpleMappingFunction(
    override val declaration: KSFunctionDeclaration,
    override val target: MappingTarget,
    override val mapper: Mapper
) : MappingFunction(), GeneratesSimpleMapping {
    override val overrides by lazy {
        declaration.getAnnotations<Mapping>().map(::MappingAnnotation).toList()
    }
}