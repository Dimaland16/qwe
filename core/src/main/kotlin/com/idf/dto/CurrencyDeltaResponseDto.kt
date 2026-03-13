package com.idf.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class CurrencyDeltaResponseDto(
    val currencyCode: String,
    val nbkRate: BigDecimal,
    val xeRate: BigDecimal,
    val delta: BigDecimal,
    val nbkUpdatedAt: OffsetDateTime,
    val xeUpdatedAt: OffsetDateTime
)