package com.idf.reactor.service

import com.idf.dto.CurrencyDeltaResponseDto
import com.idf.dto.CurrencyRateCreateDto
import com.idf.exeption.CurrencySyncException
import com.idf.parser.CurrencyParser
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Service
class ReactorCurrencyService(
    private val parsers: List<CurrencyParser>,
    private val rateSaver: ReactorCurrencyRateSaver,
    private val databaseClient: DatabaseClient
) {
    private val log = LoggerFactory.getLogger(ReactorCurrencyService::class.java)

    fun syncAndCompare(currencyCode: String): Mono<CurrencyDeltaResponseDto> {
        return fetchRatesConcurrently(currencyCode)
            .flatMap { parsedRates ->
                if (parsedRates.isEmpty()) {
                    return@flatMap Mono.error(CurrencySyncException("Оба парсера не смогли получить данные для $currencyCode"))
                }
                saveAllRates(parsedRates)
                    .then(fetchDeltaFromView(currencyCode))
            }
    }

    private fun fetchRatesConcurrently(currencyCode: String): Mono<List<CurrencyRateCreateDto>> {
        val parserMonos = parsers.map { parser ->
            Mono.fromCallable {
                log.info("Запуск парсинга ${parser.sourceId} в потоке: ${Thread.currentThread().name}")
                Optional.of(parser.parse(currencyCode))
            }
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume { e ->
                    log.error("Ошибка парсера ${parser.sourceId}: ${e.message}")
                    Mono.just(Optional.empty())
                }
        }

        return Flux.zip(parserMonos) { results ->
            @Suppress("UNCHECKED_CAST")
            results.mapNotNull { (it as Optional<CurrencyRateCreateDto>).orElse(null) }
        }.next()
    }

    private fun saveAllRates(rates: List<CurrencyRateCreateDto>): Mono<Void> {
        return Flux.fromIterable(rates)
            .flatMap { dto -> rateSaver.saveRateWithRetry(dto) }
            .then()
    }

    private fun fetchDeltaFromView(currencyCode: String): Mono<CurrencyDeltaResponseDto> {
        return databaseClient.sql("SELECT * FROM v_currency_delta WHERE currency_code = :code")
            .bind("code", currencyCode.uppercase())
            .map { row, _ ->
                CurrencyDeltaResponseDto(
                    currencyCode = row.get("currency_code", String::class.java)!!,
                    nbkRate = row.get("nbk_rate", BigDecimal::class.java)!!,
                    xeRate = row.get("xe_rate", BigDecimal::class.java)!!,
                    delta = row.get("delta", BigDecimal::class.java)!!,
                    nbkUpdatedAt = row.get("nbk_updated_at", OffsetDateTime::class.java)!!,
                    xeUpdatedAt = row.get("xe_updated_at", OffsetDateTime::class.java)!!
                )
            }
            .one()
            .switchIfEmpty(
                Mono.error(CurrencySyncException("Не удалось рассчитать дельту (возможно, нет данных от одного из источников)"))
            )
    }
}