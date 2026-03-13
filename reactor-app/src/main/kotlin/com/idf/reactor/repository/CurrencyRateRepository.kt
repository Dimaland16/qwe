package com.idf.reactor.repository

import com.idf.entity.CurrencyRate
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface CurrencyRateRepository : ReactiveCrudRepository<CurrencyRate, Long> {
    fun findByCodeAndSourceId(code: String, sourceId: String): Mono<CurrencyRate>
}