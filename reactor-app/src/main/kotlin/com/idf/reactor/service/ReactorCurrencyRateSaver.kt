package com.idf.reactor.service

import com.idf.dto.CurrencyRateCreateDto
import com.idf.entity.CurrencyRate
import com.idf.exeption.CurrencySyncException
import com.idf.mapper.updateWith
import com.idf.reactor.repository.CurrencyRateRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class ReactorCurrencyRateSaver(
    private val repository: CurrencyRateRepository
) {
    private val log = LoggerFactory.getLogger(ReactorCurrencyRateSaver::class.java)

    @Transactional
    fun saveRateWithRetry(dto: CurrencyRateCreateDto): Mono<CurrencyRate> {
        return repository.findByCodeAndSourceId(dto.code, dto.sourceId)
            .flatMap { existing ->
                repository.save(existing.updateWith(dto.rate))
            }
            .switchIfEmpty(Mono.defer {
                repository.save(
                    CurrencyRate(code = dto.code, sourceId = dto.sourceId, rate = dto.rate)
                )
            })
            .retryWhen(
                Retry.backoff(3, Duration.ofMillis(50))
                    .filter { it is OptimisticLockingFailureException }
                    .doBeforeRetry { signal ->
                        log.warn("Конфликт версий (${dto.sourceId}). Попытка: ${signal.totalRetries() + 1}")
                    }
                    .onRetryExhaustedThrow { _, retrySignal ->
                        CurrencySyncException(
                            "Не удалось обновить курс ${dto.sourceId} из-за высокой конкуренции",
                            retrySignal.failure()
                        )
                    }
            )
            .doOnSuccess { log.info("Успешно сохранен курс ${dto.sourceId}: ${dto.rate}") }
    }
}