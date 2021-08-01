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
package com.github.b3er.kmapper.mapping.utils

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal fun KSType.toClassName(): ClassName {
    val decl = declaration
    check(decl is KSClassDeclaration)
    return decl.toClassName()
}

internal fun KSClassDeclaration.toTypeName(argumentList: List<TypeName> = emptyList()): TypeName {
    val className = toClassName()
    return if (argumentList.isNotEmpty()) {
        className.parameterizedBy(argumentList)
    } else {
        className
    }
}

internal fun KSClassDeclaration.toClassName(): ClassName {
    require(!isLocal()) {
        "Local/anonymous classes are not supported!"
    }
    val pkgName = packageName.asString()
    val typesString = qualifiedName!!.asString().removePrefix("$pkgName.")

    val simpleNames = typesString
        .split(".")
    return ClassName(pkgName, simpleNames)
}

fun Modifier.kModifier(): KModifier? = when (this) {
    Modifier.PUBLIC -> KModifier.PUBLIC
    Modifier.PRIVATE -> KModifier.PRIVATE
    Modifier.INTERNAL -> KModifier.INTERNAL
    Modifier.PROTECTED -> KModifier.PROTECTED
    Modifier.IN -> KModifier.IN
    Modifier.OUT -> KModifier.OUT
    Modifier.OVERRIDE -> KModifier.OVERRIDE
    Modifier.LATEINIT -> KModifier.LATEINIT
    Modifier.ENUM -> KModifier.ENUM
    Modifier.SEALED -> KModifier.SEALED
    Modifier.ANNOTATION -> KModifier.ANNOTATION
    Modifier.DATA -> KModifier.DATA
    Modifier.INNER -> KModifier.INNER
    Modifier.FUN -> KModifier.FUN
    Modifier.VALUE -> KModifier.VALUE
    Modifier.SUSPEND -> KModifier.SUSPEND
    Modifier.TAILREC -> KModifier.TAILREC
    Modifier.OPERATOR -> KModifier.OPERATOR
    Modifier.INFIX -> KModifier.INFIX
    Modifier.INLINE -> KModifier.INLINE
    Modifier.EXTERNAL -> KModifier.EXTERNAL
    Modifier.ABSTRACT -> KModifier.ABSTRACT
    Modifier.FINAL -> KModifier.FINAL
    Modifier.OPEN -> KModifier.OPEN
    Modifier.VARARG -> KModifier.VARARG
    Modifier.NOINLINE -> KModifier.NOINLINE
    Modifier.CROSSINLINE -> KModifier.CROSSINLINE
    Modifier.REIFIED -> KModifier.REIFIED
    Modifier.EXPECT -> KModifier.EXPECT
    Modifier.ACTUAL -> KModifier.ACTUAL
    else -> null
}

fun Collection<Modifier>.kModifiers(): List<KModifier> = mapNotNull { it.kModifier() }

fun KSValueParameter.kModifiers(): Sequence<KModifier> = sequence {
    if (isCrossInline) {
        yield(KModifier.CROSSINLINE)
    }
    if (isNoInline) {
        yield(KModifier.NOINLINE)
    }
    if (isVararg) {
        yield(KModifier.VARARG)
    }
}

internal fun FileSpec.writeTo(codeGenerator: CodeGenerator) {
    val dependencies = Dependencies(false, *originatingKSFiles().toTypedArray())
    val file = codeGenerator.createNewFile(dependencies, packageName, name)
    // Don't use writeTo(file) because that tries to handle directories under the hood
    OutputStreamWriter(file, StandardCharsets.UTF_8)
        .use(::writeTo)
}
