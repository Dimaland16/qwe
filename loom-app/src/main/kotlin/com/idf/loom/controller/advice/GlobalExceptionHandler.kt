package com.idf.loom.controller.advice

import com.idf.dto.ErrorResponseDto
import com.idf.exeption.CurrencyNotFoundException
import com.idf.exeption.CurrencySyncException
import com.idf.exeption.ParserCommunicationException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CurrencyNotFoundException::class)
    fun handleCurrencyNotFound(
        ex: CurrencyNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponseDto> {
        log.warn("Валюта не найдена: ${ex.message}")
        return buildResponse(HttpStatus.NOT_FOUND, ex.message, request.requestURI)
    }

    @ExceptionHandler(ParserCommunicationException::class)
    fun handleParserCommunication(
        ex: ParserCommunicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponseDto> {
        log.error("Ошибка связи с внешним источником: ${ex.message}", ex)
        return buildResponse(HttpStatus.BAD_GATEWAY, "Внешний источник курсов недоступен", request.requestURI)
    }

    @ExceptionHandler(CurrencySyncException::class)
    fun handleCurrencySyncException(
        ex: CurrencySyncException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponseDto> {
        log.error("Ошибка синхронизации валют: ${ex.message}", ex)
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.message, request.requestURI)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponseDto> {
        log.error("Непредвиденная фатальная ошибка: ${ex.message}", ex)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера. Обратитесь в поддержку.", request.requestURI)
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String?,
        path: String
    ): ResponseEntity<ErrorResponseDto> {
        val errorDto = ErrorResponseDto(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = path
        )
        return ResponseEntity.status(status).body(errorDto)
    }
}