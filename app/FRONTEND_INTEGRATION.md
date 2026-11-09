# Backend Updates for Frontend — Stage 6: Leaderboard + Economy Fixes

## ✅ Что добавлено (Stage 6 — Leaderboard)

### Новые эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/v1/leaderboard?top=100` | Топ-N игроков + ваше место в рейтинге |
| `POST` | `/api/v1/leaderboard/refresh` | Принудительно обновить свой счёт |

### Пример запроса
```bash
curl "http://localhost:8080/api/v1/leaderboard?top=100" \
  -H "X-User-Id: <uuid>"
```

### Пример ответа
```json
{
  "top": [
    {
      "rank": 1,
      "userId": "d82038b7-3e31-4da6-8c30-d98fc4089e42",
      "displayName": "Player1",
      "score": 17,
      "credits": 1,
      "totalResourcesValue": 1,
      "clickPower": 1.0
    }
  ],
  "me": {
    "rank": 4,
    "userId": "198df77e-b1db-44e3-a7dd-86fb6b2ae80e",
    "displayName": null,
    "score": 15,
    "credits": 0,
    "totalResourcesValue": 0,
    "clickPower": 1.0
  },
  "totalPlayers": 4
}
```

### Как работает рейтинг
- **Формула счёта**: `credits + totalResourcesValue + clickPower×10 + toolMultiplier×5`
- **Кэш в Redis**: Sorted Set `leaderboard:scores` — обновляется в реальном времени при кликах/покупках
- **Фоновое пересчитывание**: каждые 5 минут (`@Scheduled`) полный пересчёт для всех игроков
- **Фолбэк**: если в Redis пусто — сервис сам делает `refreshFullLeaderboard()` и отдаёт данные

---

## ✅ Исправлены критические проблемы экономики

Все 4 основные проблемы решены:

### 1. Клики → credits (начисляются за каждый клик)
**Было**: credits давались только при уничтожении блока (+1).
**Стало**: в `click-batch` начисляются credits за **каждый клик**:
- Формула: `clicks × clickPower` (округляется вверх)
- Приходит в ответе: `rewards.credits` + обновлённый `balance.credits`
- Пример: 10 кликов при clickPower=1.0 → +10 credits сразу

```json
// Ответ click-batch (блок НЕ уничтожен)
{
  "destroyed": false,
  "block": {"type": "stone", "currentHp": 90, "maxHp": 100},
  "rewards": {"credits": 10, "resources": {}},
  "balance": {"credits": 110, "resources": {"stone": 5}}
}
```

### 2. Автодобыча → credits (машины дают валюту)
**Было**: машины давали только ресурс (stone/iron/gold).
**Стало**: у машин в каталоге появилось поле `creditsPerHour` (в credits/час, соответствует фронту в credits/сек):
- `basic_miner` — 10 stone/час + **36000 credits/час** (10/сек)
- `advanced_miner` — 50 iron/час + **180000 credits/час** (50/сек)
- `elite_miner` — 200 gold/час + **720000 credits/час** (200/сек)

При коллекте (`POST /api/v1/inventory/machines/collect`) приходят и ресурсы, и credits:
```json
{
  "collected": {"stone": 10, "credits": 36000},
  "balance": {"credits": 36115, "resources": {"stone": 15}},
  "collectedAt": "2026-11-09T15:00:00Z"
}
```

### 3. Задания — claim любого выполненного
**Было**: можно забрать награду только из окна из 3 видимых заданий.
**Стало**: можно забрать награду за **любое выполненное** задание (видимость — только для UI). Ограничение по окну снято.

### 4. Магазин — тиры 2-3 + gold_pickaxe
Добавлены новые продукты в каталог (`GET /api/v1/shop/catalog`):

| Тип | ID | Цена | Эффект |
|-----|-----|------|--------|
| CLICK_UPGRADE | `click_power_2` | 500 | +1.0 click power |
| CLICK_UPGRADE | `click_power_3` | 2000 | +2.0 click power |
| TOOL | `gold_pickaxe` | 3000 | x3.0 click damage |
| MACHINE | `advanced_miner` | 6000 | 50 iron + 50 credits/сек |
| MACHINE | `elite_miner` | 25000 | 200 gold + 200 credits/сек |

Также в БД добавлен `gold_pickaxe` (миграция V13).

---

## 📋 Чек-лист для интеграции

- [ ] Подключить `GET /api/v1/leaderboard` на экране рейтинга
- [ ] Показывать `me.rank` в профиле/шапке
- [ ] Обновлять баланс `credits` после `click-batch` и `collect` (теперь приходит в ответе)
- [ ] Показывать новые продукты в магазине (каталог расширен)
- [ ] В заданиях разрешить claim любого выполненного (не только видимых 3)

---

## 🔗 Все эндпоинты

### Auth & Progress
| Метод | URL | Для чего |
|-------|-----|----------|
| `POST` | `/api/v1/auth/guest` | Создать гостя |
| `POST` | `/api/v1/auth/register` | Регистрация (апгрейд гостя) |
| `POST` | `/api/v1/auth/login` | Логин |
| `GET` | `/api/v1/progress/me` | Текущий прогресс (credits, resources, clickPower) |

### Game Loop
| Метод | URL | Для чего |
|-------|-----|----------|
| `POST` | `/api/v1/game/click-batch` | Отправить пакет кликов (даёт credits за клики + при destroy) |

### Inventory & Machines
| Метод | URL | Для чего |
|-------|-----|----------|
| `GET` | `/api/v1/inventory` | Инвентарь (инструменты, машины) |
| `POST` | `/api/v1/inventory/tools/{toolCode}/equip` | Экипировать инструмент |
| `DELETE` | `/api/v1/inventory/tools/equipped` | Снять инструмент |
| `POST` | `/api/v1/inventory/machines/collect` | Собрать автодобычу (даёт ресурсы + credits) |
| `POST` | `/api/v1/inventory/machines/{machineCode}/install` | Установить машину |
| `POST` | `/api/v1/inventory/machines/{machineCode}/uninstall` | Снять машину |

### Shop
| Метод | URL | Для чего |
|-------|-----|----------|
| `GET` | `/api/v1/shop/catalog` | Каталог (теперь есть тиры 2-3 и gold_pickaxe) |
| `POST` | `/api/v1/shop/purchase` | Купить апгрейд/инструмент/машину |

### Challenges
| Метод | URL | Для чего |
|-------|-----|----------|
| `GET` | `/api/v1/challenges` | Список заданий (видимость 3, но claim любого) |
| `POST` | `/api/v1/challenges/{index}/claim` | Забрать награду за задание |

### Daily Rewards
| Метод | URL | Для чего |
|-------|-----|----------|
| `GET` | `/api/v1/daily-rewards` | Статус ежедневной награды |
| `POST` | `/api/v1/daily-rewards/claim` | Забрать ежедневную награду |

### Leaderboard
| Метод | URL | Для чего |
|-------|-----|----------|
| `GET` | `/api/v1/leaderboard?top=100` | Топ + моё место |
| `POST` | `/api/v1/leaderboard/refresh` | Принудительное обновление своего счёта |

---
