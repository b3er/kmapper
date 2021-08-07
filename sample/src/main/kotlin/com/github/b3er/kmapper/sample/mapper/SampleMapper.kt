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

package com.github.b3er.kmapper.sample.mapper

import com.github.b3er.kmapper.*
import com.github.b3er.kmapper.sample.data.SampleDto
import com.github.b3er.kmapper.sample.model.SampleModel
import com.github.b3er.kmapper.sample.model.SampleStatusPascalCase
import com.github.b3er.kmapper.sample.model.SampleStatusSnakeCase
import com.squareup.kotlinpoet.FunSpec

@Mapper(
    uses = [TestMapper::class],
    imports = [FunSpec::class],
    injectionType = Mapper.InjectionType.None
)
internal interface SampleMapper {
    fun map(dto: SampleDto, additional: Int, additionalForNested: Int, someId: Long): SampleModel

    @EnumMappings(
        EnumMapping(source = "ONE_SAMPLE", target = "OneSample"),
        EnumMapping(source = "SECOND_SAMPLE", target = "SecondSample"),
        EnumMapping(source = "THIRD_SAMPLE", target = "SecondSample")

    )
    fun map(status: SampleDto.SampleStatusSnake): SampleStatusPascalCase

    @EnumMapping(source = "THIRD_SAMPLE", target = "SECOND_SAMPLE")
    fun mapEnum3(status: SampleDto.SampleStatusSnake): SampleStatusSnakeCase

    @EnumMappings(
        EnumMapping(sourceName = EnumMapping.Naming.UpperUnderscore, targetName = EnumMapping.Naming.UpperCamel),
        EnumMapping(source = "THIRD_SAMPLE", target = "Unknown")
    )
    fun mapeEnum2(status: SampleDto.SampleStatusSnake): SampleStatusPascalCase
}

@Mapper
internal interface TestMapper {
    @Mappings(
        Mapping(target = "nestedId", source = "dto.nestedID"),
        Mapping(target = "additional", source = "additionalForNested")
    )
    fun map(dto: SampleDto.NestedDto, additionalForNested: Int): SampleModel.NestedModel
}
