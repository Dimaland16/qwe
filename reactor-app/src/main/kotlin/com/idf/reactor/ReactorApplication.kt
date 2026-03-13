package com.idf.reactor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.idf.reactor", "com.idf.parser"])
class ReactorApplication

fun main(args: Array<String>) {
	runApplication<ReactorApplication>(*args)
}