package com.idf.parser.impl

import com.idf.dto.CurrencyRateCreateDto
import com.idf.exeption.CurrencyNotFoundException
import com.idf.exeption.ParserCommunicationException
import com.idf.parser.CurrencyParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

@Component
class XeCurrencyParser(
    @Value("\${parser.xe.url-template}")
    private val urlTemplate: String
) : CurrencyParser {

    override val sourceId = "XE"
    companion object {
        // Паттерн: "1.00 USD = 491.15 KZT"
        // Группа 1 ([A-Z]{3}) — это код валюты
        // Группа 2 ([0-9.,]+) — это сам курс
        private val RATE_REGEX = Regex(
            "1(?:\\.00)?\\s+([A-Z]{3})\\s*=\\s*([0-9.,]+)\\s*KZT",
            RegexOption.IGNORE_CASE)
    }

    override fun parse(currencyCode: String): CurrencyRateCreateDto {
        val document = fetchHtml(currencyCode)
        return extractRate(document, currencyCode)
            ?: throw CurrencyNotFoundException("Курс $currencyCode не найден на $sourceId (Regex не совпал)")
    }

    private fun fetchHtml(currencyCode: String): Document {
        val url = String.format(urlTemplate, currencyCode.uppercase())
        return try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(5000)
                .get()
        } catch (e: Exception) {
            throw ParserCommunicationException("Ошибка подключения к $sourceId: $url", e)
        }
    }

    private fun extractRate(document: Document, targetCode: String): CurrencyRateCreateDto? {
        val pageText = document.text()

        val match = RATE_REGEX.findAll(pageText).firstOrNull { result ->
            result.groupValues[1].equals(targetCode, ignoreCase = true)
        } ?: return null

        val rateStr = match.groupValues[2].replace(",", "")

        val actualRate = rateStr.toBigDecimalOrNull()
            ?: throw ParserCommunicationException("Найдено совпадение на XE, но не удалось распарсить число: $rateStr")

        return CurrencyRateCreateDto(
            code = targetCode.uppercase(),
            sourceId = sourceId,
            rate = actualRate
        )
    }
}