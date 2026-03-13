package com.idf.loom.controller

import com.idf.dto.CurrencyDeltaResponseDto
import com.idf.loom.service.LoomCurrencyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/currencies")
class CurrencyController(
    private val loomCurrencyService: LoomCurrencyService
) {

    @GetMapping("/{code}/sync-and-compare")
    fun syncAndCompare(@PathVariable code: String): ResponseEntity<CurrencyDeltaResponseDto> =
         ResponseEntity.ok(loomCurrencyService.syncAndCompare(code))
}