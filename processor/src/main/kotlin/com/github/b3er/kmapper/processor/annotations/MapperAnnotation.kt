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

import com.github.b3er.kmapper.CheckSeverity
import com.github.b3er.kmapper.EnumNaming
import com.github.b3er.kmapper.Mapper
import com.github.b3er.kmapper.processor.utils.get
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.asClassName

class MapperAnnotation(override val annotation: KSAnnotation) : AnnotationHolder {
    val includes: List<KSClassDeclaration>? by lazy {
        (annotation["uses"]?.value as List<*>)
            .asSequence()
            .filterIsInstance<KSType>()
            .map(KSType::declaration)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
    }
    val imports: List<KSClassDeclaration>? by lazy {
        (annotation["imports"]?.value as List<*>)
            .asSequence()
            .filterIsInstance<KSType>()
            .map(KSType::declaration)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
    }
    val injectionType: Mapper.InjectionType? by lazy {
        ((annotation["injectionType"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { Mapper.InjectionType.valueOf(it) }
    }
    val nullabilityStrategy: Mapper.NullabilityCheckStrategy? by lazy {
        ((annotation["nullabilityStrategy"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { Mapper.NullabilityCheckStrategy.valueOf(it) }
    }
    val enumComplianceCheck: CheckSeverity? by lazy {
        ((annotation["enumComplianceCheck"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { CheckSeverity.valueOf(it) }
    }
    val enumSourceNaming: EnumNaming? by lazy {
        ((annotation["enumSourceNaming"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { EnumNaming.valueOf(it) }
    }
    val enumTargetNaming: EnumNaming? by lazy {
        ((annotation["enumTargetNaming"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { EnumNaming.valueOf(it) }
    }

    val injectionScope: KSClassDeclaration? by lazy {
        (annotation["injectionScope"]?.value as? KSType)?.declaration as? KSClassDeclaration
    }

    override val matchedAnnotationTypes = listOf(Mapper::class.asClassName())
}
