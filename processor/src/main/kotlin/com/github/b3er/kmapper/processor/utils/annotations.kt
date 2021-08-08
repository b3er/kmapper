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

package com.github.b3er.kmapper.processor.utils

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument

inline fun <reified T : Annotation> KSAnnotated.getAnnotation(): KSAnnotation? = getAnnotation(T::class.java)
inline fun <reified T : Annotation> KSAnnotated.getAnnotations(): Sequence<KSAnnotation> = getAnnotations(T::class.java)

fun KSAnnotated.getAnnotation(type: Class<out Annotation>): KSAnnotation? =
    annotations.getAnnotations(type).firstOrNull()

fun KSAnnotated.getAnnotations(type: Class<out Annotation>): Sequence<KSAnnotation> = annotations.getAnnotations(type)

inline fun <reified T : Annotation> Sequence<KSAnnotation>.getAnnotation(): KSAnnotation? =
    getAnnotations(T::class.java).firstOrNull()

inline fun <reified T : Annotation> Sequence<KSAnnotation>.getAnnotations(): Sequence<KSAnnotation> =
    getAnnotations(T::class.java)

fun Sequence<KSAnnotation>.getAnnotations(type: Class<out Annotation>): Sequence<KSAnnotation> =
    filter {
        it.shortName.asString() == type.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == type.name
    }

inline fun <reified T : Annotation> KSAnnotation.isAnnotation(): Boolean = isAnnotation(T::class.java)

fun KSAnnotation.isAnnotation(type: Class<out Annotation>): Boolean =
    shortName.asString() == type.simpleName &&
        annotationType.resolve().declaration.qualifiedName?.asString() == type.name

fun List<KSValueArgument>.asKeyValueMap(): Map<String, Any?> = associateBy({ it.name!!.asString() }, { it.value })

inline fun <reified V> KSAnnotation.getValue(name: String): V =
    arguments.first { it.name?.asString() == name }.value as V

inline fun <reified V : Enum<V>> KSAnnotation.getEnumValue(name: String, defaultValue: V): V =
    getValue<KSType?>(name)?.let { enumValueOf<V>(it.declaration.simpleName.getShortName()) } ?: defaultValue

operator fun KSAnnotation.get(name: String) = arguments.find { it.name?.getShortName()?.equals(name) == true }
