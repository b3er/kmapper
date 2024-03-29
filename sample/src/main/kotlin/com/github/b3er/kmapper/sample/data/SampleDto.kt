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

package com.github.b3er.kmapper.sample.data

import java.math.BigDecimal
import java.util.*

data class SampleDto(
    val id: Long,
    val name: String,
    val hello: String,
    val nested: NestedDto,
    val nestedOptional: NestedDto?,
    val nullableSamples: List<NestedDto>?,
    val immutableSamples: List<NestedDto>,
    val sourceNullable: String?,
    val status: String,
    val type: Type,
    val explicitStatus: Status,
    val amount: Amount,
    val someDate: String,
    val nullableBoolean: Boolean?,
    val uuid: UUID?,
    val nullableLong: Long?
) {
    enum class Status {
        ONE_SAMPLE, SECOND_SAMPLE, THIRD_SAMPLE
    }

    enum class Type {
        TYPE_ONE, TYPE_TWO
    }

    data class NestedDto(val nestedId: Long, val nestedName: String)
    data class Amount(val currency: String, val amount: BigDecimal)
}

data class SampleNonTypedDto(val item: SampleDto)


