package com.idf.reactor.controller

import com.idf.dto.CurrencyDeltaResponseDto
import com.idf.reactor.service.ReactorCurrencyService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/currencies")
class CurrencyController(
    private val reactorCurrencyService: ReactorCurrencyService
) {

    @GetMapping("/{code}/sync-and-compare")
    fun syncAndCompare(@PathVariable code: String): Mono<CurrencyDeltaResponseDto> =
        reactorCurrencyService.syncAndCompare(code)
}