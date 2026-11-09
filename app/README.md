# Resource Magnate

Backend-приложение на Spring Boot с использованием PostgreSQL, Redis, Flyway и Swagger.

Подробные правила игровой логики и интеграции фронтенда с API описаны в
[`docs/GAMEPLAY_BACKEND_CONTRACT.md`](docs/GAMEPLAY_BACKEND_CONTRACT.md).

## Требования

Перед запуском установи:

- **Java 21 (JDK)**
- Docker и Docker Compose
- Git (для клонирования репозитория)

## Технологии

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Redis 7
- Flyway — управление миграциями базы данных
- Spring Boot Actuator — проверка состояния приложения
- Swagger / OpenAPI — документация API

## Запуск инфраструктуры

Из корня репозитория перейди в каталог `app`, где находится `docker-compose.yml`:

```bash
cd app
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
application.yaml
application-local.yaml
application-dev.yaml
```

## Swagger / OpenAPI

После запуска приложения Swagger UI доступен по адресу:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON:

[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Swagger используется для просмотра API и отправки тестовых HTTP-запросов.

## API заданий

Прогресс заданий считается на сервере по подтверждённым игровым операциям.
Повторная отправка уже обработанного `clientBatchId` или `Idempotency-Key`
не увеличивает счётчики повторно.

Получить три текущих задания и общий прогресс:

```http
GET /api/v1/challenges
X-User-Id: <uuid>
```

Получить награду за выполненное задание:

```http
POST /api/v1/challenges/{challengeIndex}/claim
X-User-Id: <uuid>
```

`challengeIndex` совпадает с нулевым индексом задания во фронтенд-каталоге.
Ответ `GET` содержит `items`, глобальные `stats`, список `claimed` и общее
число заданий. После успешного `claim` сервер возвращает новый баланс и
обновлённую тройку заданий. Награду нельзя получить повторно или забрать у
задания, которое ещё не входит в доступную тройку.

## API инструментов

Купленный инструмент можно экипировать и снять:

```http
POST /api/v1/inventory/tools/{toolCode}/equip
DELETE /api/v1/inventory/tools/equipped
X-User-Id: <uuid>
```

`GET /api/v1/inventory` помечает активный инструмент полем `equipped`.
Эффективная сила удара равна базовой силе клика, умноженной на коэффициент
экипированного инструмента.

Redis используется для распределённого rate limit. Если Redis временно
недоступен, клики продолжают обрабатываться с локальным ограничителем внутри
экземпляра приложения.

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

com.resourcemagnate.app/
├── auth/          — гостевые и зарегистрированные пользователи
├── challenge/     — каталог, прогресс и награды заданий
├── common/        — общий формат ошибок
├── config/        — Security, Redis, OpenAPI, Clock
├── economy/       — магазин, инвентарь, инструменты и машины
├── game/          — блоки, клики, прогрессия и rate limit
├── progress/      — агрегированный прогресс игрока
└── user/          — пользовательская сущность и репозиторий

| Пакет                   | Назначение|
|-------------------------|---------------------------------------------------------------------------------|
| com.resourcemagnate.app | Корневой пакет приложения. Содержит главный класс запуска Spring Boot.|
| config/ | Конфигурация приложения: подключение Redis, настройка OpenAPI/Swagger и другие конфигурационные классы Spring. |
|common/| Общие компоненты, используемые в разных модулях приложения.|
|common/exception/| Обработка ошибок: глобальный обработчик исключений, единый формат API-ошибок и бизнес-исключения.|
|user/| Работа с пользователями: профили, управление пользовательскими данными и связанные бизнес-правила.|
|auth/| Создание гостя, регистрация и вход.|
|challenge/| Серверный прогресс и получение наград за задания.|
|economy/| Магазин, баланс, инвентарь, инструменты и машины.|
|game/| Клики, блоки, редкости, награды и ограничение частоты запросов.|
|progress/| Чтение текущего прогресса игрока.|





## Примечание

Учётные данные PostgreSQL в конфигурации предназначены только для локальной разработки. Не используй эти значения в production-окружении.
