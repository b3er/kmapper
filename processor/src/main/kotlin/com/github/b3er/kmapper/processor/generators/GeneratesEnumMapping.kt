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

import com.github.b3er.kmapper.CheckSeverity
import com.github.b3er.kmapper.CheckSeverity.Error
import com.github.b3er.kmapper.EnumNaming
import com.github.b3er.kmapper.processor.annotations.EnumMappingAnnotation
import com.github.b3er.kmapper.processor.elements.MappingElement
import com.github.b3er.kmapper.processor.mappings.Mapping
import com.github.b3er.kmapper.processor.utils.asType
import com.github.b3er.kmapper.processor.utils.check
import com.github.b3er.kmapper.processor.utils.enumEntries
import com.github.b3er.kmapper.processor.utils.isEnumClass
import com.github.b3er.kmapper.processor.utils.toClassName
import com.google.common.base.CaseFormat
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

interface GeneratesEnumMapping : Mapping, MappingGenerator {
    override val overrides: List<EnumMappingAnnotation>
    val logger: KSPLogger

    override fun FunSpec.Builder.writeMapping() {
        val source = sources.first()
        val sourceDeclaration = sources.firstOrNull()?.type?.declaration as? KSClassDeclaration
        logger.check(sourceDeclaration != null, declaration) {
            "No sources found for ${toFullString()}"
        }
        val targetEnums = target.declaration.enumEntries()
        val targetEnumConsumer = targetEnums.toMutableSet()
        CodeBlock.builder().apply {
            writeNullPreconditions()
            beginControlFlow("return when (%N) {", sources.first().name)
            if (sourceDeclaration.isEnumClass()) {
                val sourceEnums = sourceDeclaration.enumEntries()
                writeNullPreconditions()
                sourceEnums.forEach {
                    indent().indent()
                    writeEntryMapping(it, sourceDeclaration, targetEnums, targetEnumConsumer)
                    unindent().unindent()
                }
            } else if (mapper.context.typeResolver.isString(sourceDeclaration.asType())) {
                writeStringToEnumMappings(source, targetEnums, targetEnumConsumer)
            }
            endControlFlow()
        }.build().also { addCode(it) }
        targetEnumConsumer.forEach { notConsumed ->
            val severity = overrides.find {
                it.target == notConsumed.simpleName.getShortName()
            }?.targetComplianceCheck?.takeIf { it != CheckSeverity.Default }
                ?: overrides.find { it.target == "" }?.targetComplianceCheck?.takeIf { it != CheckSeverity.Default }
                ?: mapper.annotation.enumComplianceCheck
                ?: Error
            logger.check(severity, declaration) {
                "${target.declaration}.${notConsumed.simpleName.getShortName()} not mapped from ${source.declaration}"
            }
        }
    }

    fun CodeBlock.Builder.writeStringToEnumMappings(
        source: MappingElement,
        targetEnums: Set<KSClassDeclaration>,
        targetEnumConsumer: MutableSet<KSClassDeclaration>
    ) {
        val defaultOverride = overrides.find { it.source.isEmpty() }
        logger.check(defaultOverride != null, declaration) {
            "Default mapping source with empty string should be specified for String -> Enum mapping: ${toFullString()}"
        }
        targetEnums.forEach { targetEnum ->
            val targetEnumName = targetEnum.simpleName.getShortName()
            val targetOverride = overrides.find { it.source == targetEnumName }
            indent().indent()
            val targetName = targetOverride?.target?.takeIf { it.isNotEmpty() } ?: targetEnumName
            val sourceName = targetOverride?.source?.takeIf { it.isNotEmpty() }
                ?: mapEnumName(targetEnumName, targetOverride, defaultOverride, reverse = true)
            addStatement("%S -> %T.%L", sourceName, target.type.toClassName(), targetName)
            unindent().unindent()
            targetEnumConsumer.removeIf { it.simpleName.getShortName() == targetName }
        }
        indent().indent()
        addStatement("else -> %T.%L", target.type.toClassName(), defaultOverride.target)
        unindent().unindent()
    }

    fun CodeBlock.Builder.writeEntryMapping(
        sourceEnum: KSClassDeclaration,
        sourceClass: KSClassDeclaration,
        targetEnums: Set<KSClassDeclaration>,
        targetEnumConsumer: MutableSet<KSClassDeclaration>
    ) {
        val sourceName = sourceEnum.simpleName.getShortName()
        val defaultOverride = overrides.find { it.source == "" && it.target == "" }
        val targetOverride = overrides.find { it.source == sourceName } ?: overrides.find { it.source == "" }

        if (targetOverride?.source?.isNotEmpty() == true && targetOverride.target.isNotEmpty()) {
            addStatement(
                "%T.%L -> %T.%L",
                (sourceEnum.parentDeclaration as KSClassDeclaration).toClassName(),
                targetOverride.source,
                target.type.toClassName(),
                targetOverride.target
            )
            targetEnumConsumer.removeIf { it.simpleName.getShortName() == targetOverride.target }
            return
        }
        val sourceNaming = findSourceEnumNaming(targetOverride, defaultOverride)
        val targetNaming = findTargetEnumNaming(targetOverride, defaultOverride)
        val targetEnum = if (sourceNaming != null && targetNaming != null) {
            val decoratedName = findDecorator(sourceNaming, targetNaming, sourceName)
            targetEnums.find { it.simpleName.getShortName() == decoratedName }
        } else {
            targetEnums.find { it.simpleName.getShortName() == sourceEnum.simpleName.getShortName() }
        }
        logger.check(targetEnum != null, declaration) {
            "Can't find target enum value for ${sourceClass.simpleName.getShortName()}.${sourceName}"
        }

        addStatement("%T -> %T", sourceEnum.toClassName(), targetEnum.toClassName())
        targetEnumConsumer.remove(targetEnum)
    }

    private fun findTargetEnumNaming(
        targetAnnotation: EnumMappingAnnotation?,
        defaultOverride: EnumMappingAnnotation?
    ): EnumNaming? {
        return (targetAnnotation?.targetName?.takeIf { it != EnumNaming.Default }
            ?: defaultOverride?.targetName?.takeIf { it != EnumNaming.Default }
            ?: mapper.annotation.enumTargetNaming)?.takeIf { it != EnumNaming.None && it != EnumNaming.Default }

    }

    private fun findSourceEnumNaming(
        targetAnnotation: EnumMappingAnnotation?,
        defaultOverride: EnumMappingAnnotation?
    ): EnumNaming? {
        return (targetAnnotation?.sourceName?.takeIf { it != EnumNaming.Default }
            ?: defaultOverride?.sourceName?.takeIf { it != EnumNaming.Default }
            ?: mapper.annotation.enumSourceNaming)?.takeIf { it != EnumNaming.None && it != EnumNaming.Default }

    }

    private fun mapEnumName(
        value: String,
        targetAnnotation: EnumMappingAnnotation?,
        defaultOverride: EnumMappingAnnotation,
        reverse: Boolean,
    ): String {
        val targetName = findTargetEnumNaming(targetAnnotation, defaultOverride)
        val sourceName = findSourceEnumNaming(targetAnnotation, defaultOverride)
        if (targetName != null && sourceName != null) {
            return if (reverse) {
                findDecorator(targetName, sourceName, value)
            } else {
                findDecorator(sourceName, targetName, value)
            }
        }
        return value
    }

    private fun findDecorator(
        sourceName: EnumNaming,
        targetName: EnumNaming,
        value: String
    ): String {
        val sourceFormat = sourceName.format()
        val targetFormat = targetName.format()
        logger.check(sourceFormat != null && targetFormat != null, declaration) {
            "Both sourceName and targetName should be specified for mapping"
        }
        return sourceFormat.to(targetFormat, value)
    }

    fun EnumNaming.format(): CaseFormat? = when (this) {
        EnumNaming.None, EnumNaming.Default -> null
        EnumNaming.UpperUnderscore -> CaseFormat.UPPER_UNDERSCORE
        EnumNaming.LowerUnderscore -> CaseFormat.LOWER_UNDERSCORE
        EnumNaming.LowerCamel -> CaseFormat.LOWER_CAMEL
        EnumNaming.UpperCamel -> CaseFormat.UPPER_CAMEL
    }
}
