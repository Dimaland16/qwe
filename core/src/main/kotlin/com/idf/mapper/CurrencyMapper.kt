package com.idf.mapper

import com.idf.dto.CurrencyRateCreateDto
import com.idf.dto.CurrencyRateResponseDto
import com.idf.entity.CurrencyRate
import java.math.BigDecimal
import java.time.OffsetDateTime

fun CurrencyRateCreateDto.toEntity() = CurrencyRate(
    code = this.code,
    sourceId = this.sourceId,
    rate = this.rate
)

fun CurrencyRate.toResponseDto() = CurrencyRateResponseDto(
    code = this.code,
    sourceId = this.sourceId,
    rate = this.rate,
    updatedAt = this.updatedAt
)

fun CurrencyRate.updateWith(newRate: BigDecimal): CurrencyRate {
    return this.copy(
        rate = newRate,
        updatedAt = OffsetDateTime.now()
    )
}