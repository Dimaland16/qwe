package com.idf.dto

import java.math.BigDecimal

data class CurrencyRateCreateDto(
    val code: String,
    val sourceId: String,
    val rate: BigDecimal
)