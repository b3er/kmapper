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

package com.github.b3er.kmapper.sample.model

import com.github.b3er.kmapper.sample.data.SampleDto.Type
import java.math.BigDecimal
import java.time.LocalDate

data class SampleModel(
    val id: Long,
    val addedId: Long,
    val name: String,
    val hello: String,
    val nested: NestedModel,
    val nestedOptional: NestedModel?,
    val nullableSamples: List<NestedModel>?,
    val sourceNullable: String,
    val nonExistentElement: Long = 1231,
    val status: Status,
    val type: Type,
    val explicitStatus: Status,
    val amount: Money,
    val someDate: LocalDate,
    val nullableBoolean: Boolean,
    val uuid: String,
    val nullableLong: Long
) {
    enum class Status {
        OneSample, SecondSample, Unknown
    }

    enum class Type {
        TypeOne, TypeTwo
    }

    enum class Currency {
        RUB, EUR, USD
    }

    data class NestedModel(val nestedId: Long, val nestedName: String)
    data class Money(val currency: Currency, val amount: BigDecimal)
}

