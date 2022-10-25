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

import com.github.b3er.kmapper.EnumMapping
import com.github.b3er.kmapper.EnumMappings
import com.github.b3er.kmapper.EnumNaming
import com.github.b3er.kmapper.Mapper
import com.github.b3er.kmapper.Mapping
import com.github.b3er.kmapper.sample.data.SampleDto
import com.github.b3er.kmapper.sample.model.SampleModel
import java.time.LocalDate
import java.util.UUID


@Mapper(
    uses = [NestedSampleMapper::class],
    injectionType = Mapper.InjectionType.None,
    nullabilityStrategy = Mapper.NullabilityCheckStrategy.RuntimeException,
    enumTargetNaming = EnumNaming.UpperCamel,
    enumSourceNaming = EnumNaming.UpperUnderscore
)
internal interface SampleMapper {
    val nestedSampleMapper: NestedSampleMapper
    //    fun map(sampleNonTypedDto: SampleNonTypedDto): SampleTypedModel<SampleModel>

    @EnumMappings(
        EnumMapping(sourceName = EnumNaming.UpperUnderscore, targetName = EnumNaming.UpperCamel),
        EnumMapping(source = "THIRD_SAMPLE", target = "Unknown")
    )
    fun map(status: SampleDto.Status): SampleModel.Status

    @Mapping(
        options = [Mapping.Option.NullableBooleanToFalse, Mapping.Option.NullableStringToEmpty],
        nullabilityStrategy = Mapping.NullabilityCheckStrategy.RuntimeException
    )
    fun map(dto: SampleDto, addedId: Long): SampleModel

    @EnumMapping(target = "Unknown")
    fun mapDtoEnum(dto: DtoEnum): ModelEnum
    fun mapStringToLocalDate(value: String): LocalDate = LocalDate.parse(value)

    fun mapUUIDToString(value: UUID): String = value.toString()

//    @EnumMapping(target = "Three", targetComplianceCheck = Warning)
//    fun map(dto: DtoEnum?): ModelEnum
    //
    //    fun mapList(dto: List<SampleDto>, addedId: Long): List<SampleDto>
    //    fun mapIterable(dto: Iterable<SampleDto>, addedId: Long): List<SampleDto>
    //    fun mapCollection(dto: Collection<SampleDto>, addedId: Long): List<SampleDto>
    //
    //    fun mapListToModel(dto: List<SampleDto>, addedId: Long): List<SampleModel>
    //    fun mapIterableToModel(dto: Iterable<SampleDto>, addedId: Long): List<SampleModel>
    //    fun mapCollectionToModel(dto: Collection<SampleDto>, addedId: Long): List<SampleModel>
}

enum class DtoEnum {
    One, Two, Three, Four
}

enum class ModelEnum {
    One, Two, Unknown
}
