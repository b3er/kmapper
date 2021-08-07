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
import com.github.b3er.kmapper.sample.data.OtherDto
import com.github.b3er.kmapper.sample.data.SampleDto
import com.github.b3er.kmapper.sample.mapper.MyMappers
import com.github.b3er.kmapper.sample.mapper.OtherMapper
import com.github.b3er.kmapper.sample.mapper.SampleMapper
import com.github.b3er.kmapper.sample.model.OtherModel
import com.github.b3er.kmapper.sample.model.SampleModel
import com.github.b3er.kmapper.sample.model.SampleStatusPascalCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MappingTest {

    @Test
    fun testMapping() {
        val dto = SampleDto(
            id = 123,
            name = "testName",
            hello = "hi",
            nested = SampleDto.NestedDto(nestedID = 456, nestedName = "nested"),
            status = SampleDto.SampleStatusSnake.THIRD_SAMPLE
        )

        val expected = SampleModel(
            id = dto.id,
            name = dto.name,
            hello = dto.hello,
            nested = SampleModel.NestedModel(
                nestedName = dto.nested.nestedName,
                nestedId = dto.nested.nestedID,
                additional = 777
            ),
            status = SampleStatusPascalCase.SecondSample,
            someId = 312
        )

        val result = MyMappers.getMapper<SampleMapper>().map(
            dto, additional = 133,
            additionalForNested = 777,
            someId = 312
        )

        assertEquals(expected, result)
    }

    @Test
    fun testOtherMapping() {
        val dto = OtherDto(
            id = 123,
            name = "test",
            nested = OtherDto.OtherNestedDto(id = 456, nestedName = "nested"),
            status = OtherDto.Status.Success
        )

        val expected = OtherModel(
            id = dto.id,
            name = dto.name,
            nested = OtherModel.OtherNestedModel(id = dto.nested.id, nestedName = dto.nested.nestedName),
            status = OtherModel.Status.Success
        )

        val result = MyMappers.getMapper<OtherMapper>().map(dto)

        assertEquals(expected, result)
    }
}
