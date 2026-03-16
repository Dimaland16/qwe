# 🛡️ Rates Sentinel: Project Loom vs Spring WebFlux

Сервис для параллельного парсинга, синхронизации и сравнения курсов валют из нескольких независимых источников (Национальный Банк и XE.com).

Проект реализован в рамках микросервисной архитектуры (Multi-module Gradle) и демонстрирует два фундаментально разных подхода к высокой конкурентности в Java 24+:
1. **Императивный подход:** Project Loom (Virtual Threads) + JDBC.
2. **Реактивный подход:** Spring WebFlux (Project Reactor) + R2DBC.

## 🚀 Live Demo (Развернуто на Render.com)

Сервисы подключены к единой базе данных PostgreSQL. Вы можете запросить синхронизацию и расчет дельты по следующим ссылкам:

* **Loom API (Virtual Threads):**[https://loom-rates-sentinel.onrender.com/api/v1/currencies/USD/sync-and-compare](https://loom-rates-sentinel.onrender.com/api/v1/currencies/USD/sync-and-compare)
* **Reactor API (WebFlux):**[https://reactor-rates-sentinel.onrender.com/api/v1/currencies/USD/sync-and-compare](https://reactor-rates-sentinel.onrender.com/api/v1/currencies/USD/sync-and-compare)

*(Примечание: Сервисы развернуты на Free Tier. При первом запросе возможна задержка "Cold Start" до 50 секунд).*

## 🏗 Архитектура и Стек технологий

* **Язык:** Kotlin + Java 24
* **Фреймворк:** Spring Boot 4.0.2
* **БД:** PostgreSQL
* **Миграции:** Liquibase
* **Парсинг:** Jsoup + Regex
* **Сборка:** Gradle Kotlin DSL (`.kts`)
* **Нагрузочное тестирование:** k6

### Структура проекта (Multi-module)
* `:core` — Ядро бизнес-логики. Содержит DTO, Entity, интерфейсы парсеров, Liquibase миграции и общие обработчики (без зависимости от Web-слоя).
* `:loom-app` — Приложение на базе Spring Web MVC, использующее `Executors.newVirtualThreadPerTaskExecutor()` и Spring Data JDBC.
* `:reactor-app` — Приложение на базе Spring WebFlux, использующее неблокирующий ввод-вывод (`Flux`/`Mono`) и Spring Data R2DBC.

## 🧠 Инженерные вызовы и их решения

### 1. Защита от Lost Update (Оптимистичная блокировка)
Оба приложения работают с одной БД одновременно. Для защиты от перезаписи данных (Lost Update) реализован механизм **Optimistic Locking** через аннотацию `@Version`. В случае конфликта (когда оба сервиса пытаются обновить курс одной валюты одновременно), реализован механизм **Silent Retry** (3 попытки с задержкой), который скрывает конфликт от клиента и успешно завершает транзакцию.

### 2. Защита пула соединений (Loom Challenge)
Виртуальные потоки дешевые, но JDBC-коннекты — нет. Использование Jsoup (блокирующий I/O) внутри транзакции привело бы к исчерпанию пула HikariCP.
**Решение:** Парсинг вынесен за пределы `@Transactional`. Транзакция открывается только в выделенном компоненте `CurrencyRateSaver` на миллисекунды (исключительно для выполнения `UPDATE`), что позволяет держать `maximum-pool-size` минимальным (20 коннектов) даже при тысячах виртуальных потоков.

### 3. Толстая БД (DBA View)
Расчет разницы (дельты) между курсами делегирован базе данных через `CREATE OR REPLACE VIEW v_currency_delta`. Приложения читают готовый результат через `JdbcClient` / `DatabaseClient`, что снижает нагрузку на сеть и Heap памяти Java.

## 🧪 Нагрузочное тестирование (k6)

В проекте предусмотрен скрипт для тестирования на излом, который бьет по обоим сервисам одновременно, вызывая искусственные коллизии версий в БД.

Для запуска теста локально (требуется Docker):
```bash
docker compose run --rm k6 run /scripts/load_test.js