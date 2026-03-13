package com.idf.loom

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages =["com.idf.loom", "com.idf.parser"])
class LoomApplication

fun main(args: Array<String>) {
    runApplication<LoomApplication>(*args)
}