package com.idf.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

@Table("currency_rates")
data class CurrencyRate(
    @Id
    @Column("id")
    val id: Long? = null,

    @Column("code")
    val code: String,

    @Column("source_id")
    val sourceId: String,

    @Column("rate")
    val rate: BigDecimal,

    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Version
    @Column("version")
    val version: Long = 0
)