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

import com.github.b3er.kmapper.GenerateMapperFactory
import com.github.b3er.kmapper.processor.utils.get
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.asClassName

data class GenerateMapperFactoryAnnotation(override val annotation: KSAnnotation) : AnnotationHolder {
    val name: String by lazy { annotation["name"]!!.value as String }
    val mappers: List<KSClassDeclaration>? by lazy {
        (annotation["mappers"]?.value as List<*>)
            .asSequence()
            .filterIsInstance<KSType>()
            .map(KSType::declaration)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
    }
    val implementation: GenerateMapperFactory.Implementation? by lazy {
        ((annotation["implementation"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { GenerateMapperFactory.Implementation.valueOf(it) }
    }
    override val matchedAnnotationTypes = listOf(GenerateMapperFactory::class.asClassName())
}
