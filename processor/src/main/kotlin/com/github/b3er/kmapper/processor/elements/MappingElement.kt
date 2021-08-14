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

package com.github.b3er.kmapper.processor.elements

import com.github.b3er.kmapper.processor.utils.TypeParameterResolver
import com.github.b3er.kmapper.processor.utils.kModifiers
import com.github.b3er.kmapper.processor.utils.toTypeParameterResolver
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.KModifier

data class MappingElement(
    val node: KSNode,
    override val type: KSType,
    override val name: String,
    val modifiers: List<KModifier>,
    val propertyEnumeration: (MappingElement) -> List<MappingElement>
) : NamedTypeElement {
    val properties by lazy { propertyEnumeration(this) }
    val declaration = type.declaration as KSClassDeclaration
    override val typeParameterResolver: TypeParameterResolver by lazy {
        type.declaration.typeParameters.toTypeParameterResolver()
    }
    val hasDefault: Boolean = (node as? KSValueParameter)?.hasDefault == true
    fun makeNotNullable(): MappingElement {
        return copy(type = type.makeNotNullable())
    }
}

fun KSPropertyDeclaration.toMappingElement() =
    type.resolve().toMappingElement(this, simpleName.getShortName(), emptyList())

fun KSValueParameter.toMappingElement() =
    type.resolve().toMappingElement(this, name!!.getShortName(), kModifiers().toList())

fun KSTypeReference.toMappingElement(
    name: String = "",
    modifiers: List<KModifier> = emptyList(),
    enumeration: (MappingElement) -> List<MappingElement> = DeclarationValuesEnumeration
) = resolve().toMappingElement(this, name, modifiers, enumeration)

fun KSType.toMappingElement(
    node: KSNode,
    name: String = "",
    modifiers: List<KModifier> = emptyList(),
    enumeration: (MappingElement) -> List<MappingElement> = DeclarationValuesEnumeration
) = MappingElement(node, this, name, modifiers, enumeration)

object ConstructorValuesEnumeration : (MappingElement) -> List<MappingElement> {
    override fun invoke(element: MappingElement): List<MappingElement> {
        return (element.type.declaration as KSClassDeclaration).primaryConstructor?.parameters?.map {
            it.toMappingElement()
        } ?: throw IllegalArgumentException("Can't find primary constructor for ${element.type}!")
    }
}

object DeclarationValuesEnumeration : (MappingElement) -> List<MappingElement> {
    override fun invoke(element: MappingElement): List<MappingElement> {
        return (element.type.declaration as KSClassDeclaration).getDeclaredProperties().map {
            it.toMappingElement()
        }.toList()
    }
}
