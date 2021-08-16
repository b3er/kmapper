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

package com.github.b3er.kmapper.processor.annotations

import com.github.b3er.kmapper.Mapping
import com.github.b3er.kmapper.Mappings
import com.github.b3er.kmapper.processor.utils.get
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.asClassName

data class MappingAnnotation(override val annotation: KSAnnotation) : AnnotationHolder {
    val source: String by lazy { annotation["source"]!!.value as String }
    val expression: String by lazy { annotation["expression"]!!.value as String }
    val target: String by lazy { annotation["target"]!!.value as String }
    val nullabilityStrategy: Mapping.NullabilityCheckStrategy? by lazy {
        ((annotation["nullabilityStrategy"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { Mapping.NullabilityCheckStrategy.valueOf(it) }
    }
    val options: List<Mapping.Option> by lazy {
        ((annotation["options"]?.value) as? List<KSType>)
            ?.map {
                it.declaration.simpleName.getShortName()
                    .let { v -> Mapping.Option.valueOf(v) }
            } ?: emptyList()
    }

    override val matchedAnnotationTypes = listOf(Mapping::class.asClassName(), Mappings::class.asClassName())
}
