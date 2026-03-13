package com.idf.dto

import java.time.OffsetDateTime

data class ErrorResponseDto(
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String
)