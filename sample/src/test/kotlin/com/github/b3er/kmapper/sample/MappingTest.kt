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

package com.github.b3er.kmapper.sample

import com.github.b3er.kmapper.getMapper
import com.github.b3er.kmapper.sample.data.SampleDto
import com.github.b3er.kmapper.sample.data.SampleDto.Type
import com.github.b3er.kmapper.sample.mapper.MyMappers
import com.github.b3er.kmapper.sample.mapper.SampleMapper
import com.github.b3er.kmapper.sample.model.SampleModel
import com.github.b3er.kmapper.sample.model.SampleModel.Type.TypeTwo
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class MappingTest {
    @Test
    fun testMapping() {
        val addedId = 456L
        val dto = SampleDto(
            id = 123,
            name = "testName",
            hello = "hi",
            nested = SampleDto.NestedDto(1234, "nested"),
            nestedOptional = SampleDto.NestedDto(4567, "optional"),
            nullableSamples = 10L.downTo(0).map { SampleDto.NestedDto(it, "nested_$it") },
            status = "SECOND_SAMPLE",
            sourceNullable = "sourceNullable",
            explicitStatus = SampleDto.Status.SECOND_SAMPLE,
            type = Type.TYPE_TWO,
            amount = SampleDto.Amount("EUR", 100.123.toBigDecimal()),
            someDate = "2021-02-12",
            nullableBoolean = null,
            uuid = UUID.randomUUID(),
            immutableSamples = 10L.downTo(0).map { SampleDto.NestedDto(it, "nested_$it") },
            nullableLong = 1L
        )

        val expected = SampleModel(
            id = dto.id,
            name = dto.name,
            hello = dto.hello,
            addedId = addedId,
            nested = SampleModel.NestedModel(dto.nested.nestedId, dto.nested.nestedName),
            nestedOptional = SampleModel.NestedModel(dto.nestedOptional!!.nestedId, dto.nestedOptional!!.nestedName),
            nullableSamples = dto.nullableSamples?.map { SampleModel.NestedModel(it.nestedId, it.nestedName) },
            status = SampleModel.Status.SecondSample,
            sourceNullable = dto.sourceNullable!!,
            nonExistentElement = 1231,
            type = TypeTwo,
            explicitStatus = SampleModel.Status.SecondSample,
            amount = SampleModel.Money(SampleModel.Currency.EUR, 100.123.toBigDecimal()),
            someDate = LocalDate.parse(dto.someDate),
            nullableBoolean = false,
            uuid = dto.uuid.toString(),
            immutableSamples = dto.immutableSamples.map { SampleModel.NestedModel(it.nestedId, it.nestedName) }
                .toImmutableList(),
            nullableLong = 1L
        )

        val result = MyMappers.getMapper<SampleMapper>().map(dto, addedId)

        assertEquals(expected, result)
    }
}
