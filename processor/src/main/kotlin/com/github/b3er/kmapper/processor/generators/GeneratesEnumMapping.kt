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

package com.github.b3er.kmapper.processor.generators

import com.github.b3er.kmapper.EnumMapping
import com.github.b3er.kmapper.processor.annotations.EnumMappingAnnotation
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.github.b3er.kmapper.processor.utils.check
import com.github.b3er.kmapper.processor.utils.enumEntries
import com.github.b3er.kmapper.processor.utils.toClassName
import com.google.common.base.CaseFormat
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

interface GeneratesEnumMapping : Mapping, MappingGenerator {
    val overrides: List<EnumMappingAnnotation>
    val logger: KSPLogger

    override fun FunSpec.Builder.writeMapping() {
        val source = sources.firstOrNull()?.type?.declaration as? KSClassDeclaration

        logger.check(source != null, mapper.declaration) {
            "No sources found for ${toFullString()}"
        }

        val targetEnums = target.declaration.enumEntries()
        val sourceEnums = source.enumEntries()

        CodeBlock.builder().apply {
            writeNullPreconditions()
            beginControlFlow("return when (%N) {", sources.first().name)
            sourceEnums.forEach {
                indent().indent()
                writeEntryMapping(it, source, targetEnums)
                unindent().unindent()
            }
            endControlFlow()
        }.build().also { addCode(it) }
    }

    fun CodeBlock.Builder.writeEntryMapping(
        sourceEnum: KSClassDeclaration,
        sourceClass: KSClassDeclaration,
        targetEnums: Set<KSClassDeclaration>
    ) {
        val sourceName = sourceEnum.simpleName.getShortName()
        val targetOverride = overrides.find { it.source == sourceName } ?: overrides.find { it.source == "" }

        if (targetOverride?.source?.isNotEmpty() == true && targetOverride.target.isNotEmpty()) {
            addStatement(
                "%T.%L -> %T.%L",
                (sourceEnum.parentDeclaration as KSClassDeclaration).toClassName(),
                targetOverride.source,
                target.type.toClassName(),
                targetOverride.target
            )
            return
        }

        val targetEnum = if (targetOverride?.targetName != null && targetOverride.sourceName != null) {
            val decoratedName = findDecorator(targetOverride.sourceName!!, targetOverride.targetName!!, sourceName)
            targetEnums.find { it.simpleName.getShortName() == decoratedName }
        } else {
            targetEnums.find { it.simpleName.getShortName() == sourceEnum.simpleName.getShortName() }
        }

        logger.check(targetEnum != null, mapper.declaration) {
            "Can't find target enum value for ${sourceClass.simpleName.getShortName()}.${sourceName}"
        }

        addStatement("%T -> %T", sourceEnum.toClassName(), targetEnum.toClassName())
    }

    fun findDecorator(
        sourceName: EnumMapping.Naming,
        targetName: EnumMapping.Naming,
        value: String
    ): String? {
        val sourceFormat = sourceName.format()
        val targetFormat = targetName.format()
        logger.check(sourceFormat != null && targetFormat != null, mapper.declaration) {
            "Both sourceName and targetName should be specified for mapping"
        }
        return sourceFormat.to(targetFormat, value)
    }

    fun EnumMapping.Naming.format(): CaseFormat? = when (this) {
        EnumMapping.Naming.None -> null
        EnumMapping.Naming.UpperUnderscore -> CaseFormat.UPPER_UNDERSCORE
        EnumMapping.Naming.LowerUnderscore -> CaseFormat.LOWER_UNDERSCORE
        EnumMapping.Naming.LowerCamel -> CaseFormat.LOWER_CAMEL
        EnumMapping.Naming.UpperCamel -> CaseFormat.UPPER_CAMEL
    }
}
