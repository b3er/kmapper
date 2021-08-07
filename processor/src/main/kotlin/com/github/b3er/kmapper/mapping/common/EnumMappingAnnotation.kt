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

package com.github.b3er.kmapper.mapping.common

import com.github.b3er.kmapper.EnumMapping
import com.github.b3er.kmapper.EnumMappings
import com.github.b3er.kmapper.mapping.api.AnnotationHolder
import com.github.b3er.kmapper.mapping.utils.get
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.asClassName

data class EnumMappingAnnotation(override val annotation: KSAnnotation) : AnnotationHolder {
    val source: String by lazy { annotation["source"]!!.value as String }
    val target: String by lazy { annotation["target"]!!.value as String }
    val sourceName: EnumMapping.Naming? by lazy {
        ((annotation["sourceName"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { EnumMapping.Naming.valueOf(it) }
    }
    val targetName: EnumMapping.Naming? by lazy {
        ((annotation["targetName"]?.value) as? KSType)
            ?.declaration
            ?.simpleName
            ?.getShortName()
            ?.let { EnumMapping.Naming.valueOf(it) }
    }

    override val matchedAnnotationTypes = listOf(EnumMapping::class.asClassName(), EnumMappings::class.asClassName())
}
