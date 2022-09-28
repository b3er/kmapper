package com.github.b3er.kmapper.sample.mapper

import com.github.b3er.kmapper.EnumMapping
import com.github.b3er.kmapper.EnumMappings
import com.github.b3er.kmapper.EnumNaming.UpperCamel
import com.github.b3er.kmapper.EnumNaming.UpperUnderscore
import com.github.b3er.kmapper.Mapper
import com.github.b3er.kmapper.Mapper.InjectionType.Anvil
import com.github.b3er.kmapper.sample.data.SampleDto
import com.github.b3er.kmapper.sample.di.SingleIn
import com.github.b3er.kmapper.sample.di.scope.AppScope
import com.github.b3er.kmapper.sample.model.SampleModel

@Mapper(injectionType = Anvil, injectionScope = AppScope::class)
@SingleIn(AppScope::class)
interface AnvilMapper {
    @EnumMappings(
        EnumMapping(sourceName = UpperUnderscore, targetName = UpperCamel),
        EnumMapping(source = "THIRD_SAMPLE", target = "Unknown")
    )
    abstract fun map(status: SampleDto.Status): SampleModel.Status
}
