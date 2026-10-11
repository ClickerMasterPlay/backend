# Resource Magnate

Backend-приложение на Spring Boot с использованием PostgreSQL, Redis, Flyway и Swagger.

## Требования

Перед запуском установи:

- **Java 21 (JDK)**
- Docker и Docker Compose
- Git (для клонирования репозитория)

## Технологии

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Redis 7
- Flyway — управление миграциями базы данных
- Spring Boot Actuator — проверка состояния приложения
- Swagger / OpenAPI — документация API

## Запуск инфраструктуры

В корне проекта выполни:

```bash
docker-compose up -d
```

Если используется современный Docker Compose, можно выполнить:

```bash
docker compose up -d
```

Команда запустит PostgreSQL, Redis и Adminer.

Проверить состояние контейнеров:

```bash
docker compose ps
```

Посмотреть логи инфраструктуры:

```bash
docker compose logs
```

### PostgreSQL

| Параметр | Значение |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `resource_magnate` |
| Username | `postgres` |
| Password | `postgres` |

### Redis

| Параметр | Значение |
|---|---|
| Host | `localhost` |
| Port | `6379` |
| Password | Не требуется |

### Adminer

Веб-интерфейс для просмотра базы данных:

[http://localhost:8081](http://localhost:8081)

Для подключения выбери PostgreSQL и укажи параметры из таблицы выше. В поле сервера используй `postgres`, если подключаешься через Adminer.

## Запуск приложения локально

### 1. Проверь Java

```bash
java -version
```

Для сборки проекта требуется Java 21.

### 2. Запусти инфраструктуру

```bash
docker compose up -d
```

Убедись, что PostgreSQL и Redis запущены.

### 3. Запусти приложение

Для Linux и macOS:

```bash
./gradlew bootRun
```

Для Windows:

```powershell
.\gradlew.bat bootRun
```

Если Java 21 не является версией по умолчанию в системе, укажи JDK 21 в настройках Gradle или IntelliJ IDEA.

## Профили Spring Boot

В проекте используются следующие профили:

- `local` — локальная разработка с PostgreSQL и Redis из Docker Compose.
- `dev` — окружение разработки с настройками, которые можно переопределять через переменные окружения.

Профиль `local` используется по умолчанию.

Выбор конкретного профиля
Запуск с профилем `local`:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Запуск с профилем `dev`:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Конфигурационные файлы находятся в `src/main/resources/`:

```text
application.yml
application-local.yml
application-dev.yml
```

## Swagger / OpenAPI

После запуска приложения Swagger UI доступен по адресу:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON:

[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Swagger используется для просмотра API и отправки тестовых HTTP-запросов.

## Health-check

Состояние приложения и доступность зависимостей можно проверить через Spring Boot Actuator:

[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

При настроенных health-индикаторах endpoint показывает состояние приложения, PostgreSQL и Redis.

## Миграции базы данных

Миграции Flyway находятся в каталоге:

```text
src/main/resources/db/migration/
```

Пример имени миграции:

```text
V1__init.sql
V2__create_resource_table.sql
```

При запуске приложения Flyway применяет доступные миграции, а Hibernate проверяет соответствие схемы сущностям с помощью `ddl-auto: validate`.

## Остановка инфраструктуры

Остановить контейнеры:

```bash
docker compose down
```

Данные PostgreSQL и Redis сохраняются в Docker volumes.

**Внимание:** команда ниже удаляет также volumes и сохранённые данные:

```bash
docker compose down -v
```

## Структура проекта

Основной Java-код находится в пакете com.resourcemagnate.app

com.resourcemagnate\
├── ResourceMagnateApplication.java\
├── config/\
│   ├── RedisConfig.java\
│   ├── OpenApiConfig.java\
│   └── ...\
├── common/\
│   ├── exception/\
│   │   ├── GlobalExceptionHandler.java\
│   │   ├── ApiError.java\
│   │   └── BusinessException.java\
│   ├── dto/\
│   └── util/\
├── user/\
├── economy/\
├── game/\
├── inventory/\
└── ranking/

| Пакет                   | Назначение|
|-------------------------|---------------------------------------------------------------------------------|
| com.resourcemagnate.app | Корневой пакет приложения. Содержит главный класс запуска Spring Boot.|
| config/ | Конфигурация приложения: подключение Redis, настройка OpenAPI/Swagger и другие конфигурационные классы Spring. |
|common/| Общие компоненты, используемые в разных модулях приложения.|
|common/exception/| Обработка ошибок: глобальный обработчик исключений, единый формат API-ошибок и бизнес-исключения.|
|common/dto/| Общие DTO (Data Transfer Objects) для передачи данных между компонентами и через API.|
|common/util/| Вспомогательные классы и утилиты общего назначения.|
|user/| Работа с пользователями: профили, управление пользовательскими данными и связанные бизнес-правила.|
|economy/| Игровая экономика.|
|game/| Основная игровая логика, игровые механики и управление игровыми процессами.|
|inventory/| Инвентарь пользователя.|
|ranking/| Рейтинги, таблицы лидеров.|





## Примечание

Учётные данные PostgreSQL в конфигурации предназначены только для локальной разработки. Не используй эти значения в production-окружении.
