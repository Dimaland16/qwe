package com.idf.loom.service

import com.idf.dto.CurrencyRateCreateDto
import com.idf.entity.CurrencyRate
import com.idf.exeption.CurrencySyncException
import com.idf.loom.repository.CurrencyRateRepository
import com.idf.mapper.updateWith
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CurrencyRateSaver(
    private val repository: CurrencyRateRepository
) {
    fun saveRateWithRetry(dto: CurrencyRateCreateDto) {
        val maxRetries = 15
        var attempt = 0

        while (attempt < maxRetries) {
            try {
                val existingEntity = repository.findByCodeAndSourceId(dto.code, dto.sourceId)

                if (existingEntity == null) {
                    val newEntity = CurrencyRate(
                        code = dto.code,
                        sourceId = dto.sourceId,
                        rate = dto.rate
                    )
                    repository.save(newEntity)
                } else {
                    val updatedEntity = existingEntity.updateWith(dto.rate)
                    repository.save(updatedEntity)
                }
                return
            } catch (e: OptimisticLockingFailureException) {
                attempt++
                if (attempt == maxRetries) {
                    throw CurrencySyncException("Не удалось обновить курс ${dto.sourceId} после $maxRetries попыток", e)
                }
                Thread.sleep((50..250).random().toLong())
            }
        }
    }
}