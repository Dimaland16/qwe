package com.idf.parser.impl

import com.idf.dto.CurrencyRateCreateDto
import com.idf.exeption.CurrencyNotFoundException
import com.idf.exeption.ParserCommunicationException
import com.idf.parser.CurrencyParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class NbkCurrencyParser(
    @Value("\${parser.nbk.url}")
    private val url: String
) : CurrencyParser {

    override val sourceId = "NBK"

    override fun parse(currencyCode: String): CurrencyRateCreateDto {
        val document = fetchHtml()
        return extractCurrencyRate(document, currencyCode)
            ?: throw CurrencyNotFoundException("Курс для валюты $currencyCode не найден на сайте $sourceId")
    }

    private fun fetchHtml(): Document {
        return try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(5000)
                .get()
        } catch (e: Exception) {
            throw ParserCommunicationException("Ошибка подключения к $sourceId: ${url}", e)
        }
    }

    private fun extractCurrencyRate(document: Document, targetCode: String): CurrencyRateCreateDto? {
        val rows = document.select("table tbody tr")
        return rows.firstNotNullOfOrNull { row -> parseRow(row, targetCode) }
    }

    private fun parseRow(row: Element, targetCode: String): CurrencyRateCreateDto? {
        val columns = row.select("td")
        if (columns.size < 4) return null

        val rawCode = columns[2].text().trim()
        val code = rawCode.substringBefore("/").trim()

        if (!code.equals(targetCode, ignoreCase = true)) {
            return null
        }

        val nominalStr = columns[1].text().trim().substringBefore(" ")
        val nominal = nominalStr.toBigDecimalOrNull() ?: BigDecimal.ONE

        val rateRaw = columns[3].text().trim().replace(",", ".").toBigDecimalOrNull()
            ?: throw ParserCommunicationException("Не удалось распарсить число из колонки курса")

        val actualRate = rateRaw.divide(nominal, 4, RoundingMode.HALF_UP)

        return CurrencyRateCreateDto(
            code = code.uppercase(),
            sourceId = sourceId,
            rate = actualRate
        )
    }
}