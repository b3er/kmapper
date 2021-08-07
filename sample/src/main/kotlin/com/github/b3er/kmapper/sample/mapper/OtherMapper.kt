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

import com.github.b3er.kmapper.Mapper
import com.github.b3er.kmapper.sample.data.OtherDto
import com.github.b3er.kmapper.sample.model.OtherModel
import javax.inject.Singleton

data class AggregatedModel(val some1: OtherModel, val some2: OtherModel.OtherNestedModel)

@Singleton
@Mapper(uses = [OtherNestedMapper::class], injectionType = Mapper.InjectionType.Jsr330)
interface OtherMapper {
    fun map(some1: OtherDto, some2: OtherDto.OtherNestedDto): AggregatedModel
}

@Mapper
interface OtherNestedMapper
