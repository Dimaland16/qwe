package com.idf.loom.repository

import com.idf.entity.CurrencyRate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CurrencyRateRepository : CrudRepository<CurrencyRate, Long> {
    fun findByCodeAndSourceId(code: String, sourceId: String): CurrencyRate?
}