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
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import com.squareup.kotlinpoet.STAR as KpStar

fun KSType.toClassName(): ClassName {
    val decl = declaration
    check(decl is KSClassDeclaration)
    return decl.toClassName()
}

fun KSType.toTypeName(typeParamResolver: TypeParameterResolver): TypeName {
    val type = when (val decl = declaration) {
        is KSClassDeclaration -> decl.toTypeName(arguments.map { it.toTypeName(typeParamResolver) })
        is KSTypeParameter -> typeParamResolver[decl.name.getShortName()]
        is KSTypeAlias -> decl.type.resolve().toTypeName(typeParamResolver)
        else -> error("Unsupported type: $declaration")
    }

    return type.copy(nullable = isMarkedNullable)
}

fun KSClassDeclaration.toTypeName(argumentList: List<TypeName> = emptyList()): TypeName {
    val className = toClassName()
    return if (argumentList.isNotEmpty()) {
        className.parameterizedBy(argumentList)
    } else {
        className
    }
}

interface TypeParameterResolver {
    val parametersMap: Map<String, TypeVariableName>
    operator fun get(index: String): TypeVariableName
}

fun List<KSTypeParameter>.toTypeParameterResolver(
    fallback: TypeParameterResolver? = null,
    sourceType: String? = null,
): TypeParameterResolver {
    val parametersMap = LinkedHashMap<String, TypeVariableName>()
    val typeParamResolver = { id: String ->
        parametersMap[id]
            ?: fallback?.get(id)
            ?: throw IllegalStateException("No type argument found for $id! Anaylzing $sourceType")
    }

    val resolver = object : TypeParameterResolver {
        override val parametersMap: Map<String, TypeVariableName> = parametersMap

        override operator fun get(index: String): TypeVariableName = typeParamResolver(index)
    }

    // Fill the parametersMap. Need to do sequentially and allow for referencing previously defined params
    for (typeVar in this) {
        // Put the simple typevar in first, then it can be referenced in the full toTypeVariable()
        // replacement later that may add bounds referencing this.
        val id = typeVar.name.getShortName()
        parametersMap[id] = TypeVariableName(id)
        // Now replace it with the full version.
        parametersMap[id] = typeVar.toTypeVariableName(resolver)
    }

    return resolver
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

internal fun KSTypeParameter.toTypeName(typeParamResolver: TypeParameterResolver): TypeName {
    if (variance == Variance.STAR) return KpStar
    return toTypeVariableName(typeParamResolver)
}

internal fun KSTypeParameter.toTypeVariableName(
    typeParamResolver: TypeParameterResolver,
): TypeVariableName {
    val typeVarName = name.getShortName()
    val typeVarBounds = bounds.map { it.toTypeName(typeParamResolver) }.toList()
    val typeVarVariance = when (variance) {
        Variance.COVARIANT -> KModifier.OUT
        Variance.CONTRAVARIANT -> KModifier.IN
        else -> null
    }
    return TypeVariableName(typeVarName, bounds = typeVarBounds, variance = typeVarVariance)
}

internal fun KSTypeArgument.toTypeName(typeParamResolver: TypeParameterResolver): TypeName {
    val typeName = type?.resolve()?.toTypeName(typeParamResolver) ?: return KpStar
    return when (variance) {
        Variance.COVARIANT -> WildcardTypeName.producerOf(typeName)
        Variance.CONTRAVARIANT -> WildcardTypeName.consumerOf(typeName)
        Variance.STAR -> KpStar
        Variance.INVARIANT -> typeName
    }
}

internal fun KSTypeReference.toTypeName(typeParamResolver: TypeParameterResolver): TypeName {
    val type = resolve()
    return type.toTypeName(typeParamResolver)
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
    OutputStreamWriter(file, StandardCharsets.UTF_8).use(::writeTo)
}
