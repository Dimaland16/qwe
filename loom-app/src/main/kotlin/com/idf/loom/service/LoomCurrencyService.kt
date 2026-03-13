package com.idf.loom.service

import com.idf.dto.CurrencyDeltaResponseDto
import com.idf.dto.CurrencyRateCreateDto
import com.idf.exeption.CurrencySyncException
import com.idf.parser.CurrencyParser
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class LoomCurrencyService(
    private val parsers: List<CurrencyParser>,
    private val rateSaver: CurrencyRateSaver,
    private val jdbcClient: JdbcClient
) {
    private val log = LoggerFactory.getLogger(LoomCurrencyService::class.java)

    fun syncAndCompare(currencyCode: String): CurrencyDeltaResponseDto {
        val parsedRates = fetchRatesConcurrently(currencyCode)

        if (parsedRates.isEmpty()) {
            throw CurrencySyncException("Оба парсера не смогли получить данные для валюты: $currencyCode")
        }

        parsedRates.forEach { dto ->
            rateSaver.saveRateWithRetry(dto)
        }

        return fetchDeltaFromView(currencyCode)
            ?: throw CurrencySyncException("Не удалось рассчитать дельту для $currencyCode (нет данных от одного из источников)")
    }

    private fun fetchRatesConcurrently(currencyCode: String): List<CurrencyRateCreateDto> {
        return Executors.newVirtualThreadPerTaskExecutor().use { executor ->

            val futures = parsers.map { parser ->
                executor.submit<CurrencyRateCreateDto> {
                    log.info("Парсинг ${parser.sourceId} для $currencyCode (Поток: ${Thread.currentThread()})")
                    parser.parse(currencyCode)
                }
            }

            futures.mapNotNull { future ->
                try {
                    future.get()
                } catch (e: Exception) {
                    log.error("Парсер упал, продолжаем без него: ${e.message}")
                    null
                }
            }
        }
    }

    private fun fetchDeltaFromView(currencyCode: String): CurrencyDeltaResponseDto? {
        return jdbcClient.sql("SELECT * FROM v_currency_delta WHERE currency_code = :code")
            .param("code", currencyCode.uppercase())
            .query(CurrencyDeltaResponseDto::class.java)
            .optional()
            .orElse(null)
    }
}