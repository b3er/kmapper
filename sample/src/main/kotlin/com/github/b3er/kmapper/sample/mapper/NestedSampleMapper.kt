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
import com.github.b3er.kmapper.Mapper
import com.github.b3er.kmapper.sample.data.SampleDto
import com.github.b3er.kmapper.sample.model.SampleModel

@Mapper(injectionType = Mapper.InjectionType.None)
abstract class NestedSampleMapper {
    abstract fun map(dto: SampleDto.NestedDto): SampleModel.NestedModel

    @EnumMappings(
        EnumMapping(sourceName = EnumMapping.Naming.UpperUnderscore, targetName = EnumMapping.Naming.UpperCamel),
        EnumMapping(source = "THIRD_SAMPLE", target = "Unknown")
    )
    abstract fun map(status: SampleDto.Status): SampleModel.Status
}
