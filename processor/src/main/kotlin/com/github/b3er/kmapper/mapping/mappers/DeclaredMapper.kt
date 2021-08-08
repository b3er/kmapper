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
import com.github.b3er.kmapper.mapping.common.MappingElement
import com.github.b3er.kmapper.mapping.mappings.PureMapping
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec

class DeclaredMapper(declaration: KSClassDeclaration, context: MappingContext) : BaseMapper(declaration, context) {
    override fun createMapping(target: MappingElement, source: MappingElement, parent: PureMapping): PureMapping {
        TODO("not implemented")
    }

    override fun write(): FileSpec {
        TODO("not implemented")
    }
}
