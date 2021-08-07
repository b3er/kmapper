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

internal data class SampleModel(
    val id: Long,
    val name: String,
    val hello: String = "",
    val nested: NestedModel,
    val someId: Long,
    val status: SampleStatusPascalCase
) {
    internal data class NestedModel(
        val nestedId: Long,
        val nestedName: String,
        val additional: Int
    )
}

enum class SampleStatusCamelCase {
    oneSample,
    secondSample
}

enum class SampleStatusPascalCase {
    OneSample,
    SecondSample,
    Unknown
}

enum class SampleStatusSnakeCase {
    ONE_SAMPLE,
    SECOND_SAMPLE
}

