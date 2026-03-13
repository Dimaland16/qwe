package com.idf.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class CurrencyRateResponseDto(
    val code: String,
    val sourceId: String,
    val rate: BigDecimal,
    val updatedAt: OffsetDateTime
)