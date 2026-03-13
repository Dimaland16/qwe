package com.idf.parser

import com.idf.dto.CurrencyRateCreateDto

interface CurrencyParser {
    val sourceId: String
    fun parse(currencyCode: String): CurrencyRateCreateDto
}