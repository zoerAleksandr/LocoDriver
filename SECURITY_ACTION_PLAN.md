# Безопасность сервера — план действий

> Чек-лист уязвимостей, найденных в анализе кода `proxy-parser`
> 25 апреля 2026. Сервер и Android в релизе.
>
> Это **action plan**, не часть Project knowledge. Когда задачи выполнены —
> можно удалить или переместить в архив.

---

## ✅ Сделано на сессии 25 апреля 2026

| # | Задача | Статус |
|---|---|---|
| 1 | IDOR при `POST /v1/route/` (захват чужого рейса) | ✅ Закрыто |
| 3 | Race condition через `global` в `route.py` | ✅ Закрыто |
| - | `--reload` в проде (uvicorn перезапускается на правках) | ✅ Убрано |
| - | Мусорные правила в `pg_hba.conf` (`192.168.1.100`, `87.228.110.32`) | ✅ Удалены |
| - | PostgreSQL открыт миру на порту 5432 | ✅ Закрыто (`127.0.0.1:5432`) |
| - | Redis открыт миру на порту 6579 | ✅ Закрыто (`127.0.0.1:6579`) |

**Что в коде изменилось:**
- `src/db/clients/pg_client.py` — добавлен класс `RouteOwnershipError` и проверка владельца перед upsert в `PostgresRouteDbClient.process`.
- `src/api/v1/route.py` — добавлен импорт `RouteOwnershipError` и обработчик `except RouteOwnershipError → 404`. Удалены строки `global t, passenger_db` и `global passengers, trains, locomotives, basic_data`.
- `docker-compose.yml` — убрано `--reload`, порты db и redis привязаны к `127.0.0.1`.
- `backend/config/custom_pg_hba.conf` — удалены строки разрешения для `192.168.1.100` и `87.228.110.32`.

---

## 🔴 ОСТАЛОСЬ: критичное (требует координации)

### 2. Публичные эндпоинты выгружают данные всех пользователей

**Где:** `src/api/v1/route.py`, эндпоинты:
- `GET /stations/` (стр. 70)
- `GET /stations/{station_id}` (стр. 80)
- `GET /routes/` (стр. 90)
- `GET /routes/{route_id}` (стр. 107)
- `GET /routes/{route_id}/stations` (стр. 116)
- `GET /routes/{route_id}/schedule` (стр. 125)
- `GET /trains/` (стр. 135)
- `GET /trains/{train_id}` (стр. 158)
- `GET /trains/{train_id}/route` (стр. 167)
- `GET /search/routes` (стр. 177)

И в `src/db/clients/pg_client.py`:
- `PostgresRouteDbClient.get_all`, `get_by_id`, `get_routes_by_stations`,
  `get_by_id_with_stations` — без фильтра по `user_id`
- `PostgresStationDbClient.get_all`, `get_by_id`, `get_by_city` —
  без фильтра
- `PostgresTrainDbClient.get_all`, `get_by_id`, `get_by_number`,
  `get_trains_by_route` — без фильтра

**Что не так:** ни авторизации, ни фильтрации. Запрос
`curl https://<host>/v1/route/routes/?limit=10000` отдаст все рейсы
всех пользователей.

**Подтверждено: клиент использует эти эндпоинты.** Просто закрыть — сломает
существующие установки. Нужна координированная миграция.

**План фикса (3 этапа):**

**Этап 1 — релиз клиента (СРОЧНО, до фикса IDOR):**
- Android-клиент начинает слать `Authorization: Bearer <token>` на все
  эти эндпоинты. Сейчас он, скорее всего, использует общий HTTP-клиент,
  но не подключает interceptor авторизации к этим запросам.
- Релиз в Google Play.
- Подождать 1-2 недели, чтобы доля старых клиентов снизилась
  (по статистике Play Console).

**Этап 2 — релиз сервера (после Этапа 1):**
- В роутере `src/api/v1/route.py` ко всем 10 публичным эндпоинтам добавить
  `current_user: User = Depends(get_current_user)`.
- В `pg_client.py` методы `get_all`, `get_by_id`, `get_by_city`,
  `get_routes_by_stations`, `get_by_id_with_stations`, `get_by_number`,
  `get_trains_by_route` дополнить параметром `user_id: UUID` и фильтрами
  `where(... .user == user_id)` или join'ом через `Route.user`.
- **Важно:** для `Station` и `Train` фильтрация по `user_id` идёт через
  связанный `Route`. Например, `select(Station).join(RouteStationLink).join(Route).where(Route.user == user_id)`.
- Если рейс/поезд/станция найдены, но принадлежат другому пользователю —
  возвращать **404, не 403** (не раскрывать сам факт существования).

**Этап 3 — проверка:**
- Логи: убедиться, что нет 401 от старых клиентов (если есть — увеличить
  паузу между этапами).

**Промежуточный временный фикс (если нет возможности быстро релизить
клиент):**

Можно закрыть IDOR (п. 1) **прямо сейчас**, не трогая эти эндпоинты.
IDOR — самостоятельная уязвимость, не зависит от публичности GET.
А публичные GET — это **меньшее зло** (read-only, в худшем случае утечка
графика рейсов), чем захват чужих данных через POST. Закрытие IDOR
устранит самое опасное, дав время на скоординированную миграцию GET.

---

### 3. Race condition в `/v1/route/`

**Где:** `src/api/v1/route.py`:
- `save_data`, строка 207: `global t, passenger_db`
- `get_sync_data`, строка 511: `global passengers, trains, locomotives, basic_data`

**Что не так:** глобальные переменные модуля общие на весь процесс.
В asyncio при concurrent-запросах двух пользователей значения могут
смешаться, и один пользователь может получить кусок данных другого.

**Фикс:**

```python
# save_data:
# Удалить строку: global t, passenger_db
# Использовать локальные переменные везде.

# get_sync_data:
# Удалить строку: global passengers, trains, locomotives, basic_data
# Локальные переменные внутри цикла for route in routes.
```

Простая механическая правка — удалить `global` строки, проверить, что
весь код в функции работает с локальными значениями.

---

### 4. Логирование PII в полном объёме

**Где:** все вызовы `LogManager.log_network_info(... data=data)` или
`message=f"... {data}"`. Особенно:
- `route.py` стр. 257-259 (полный SyncData)
- `route.py` стр. 682-684: `JSON response: {json.dumps(SyncDataResponse, default=str)}`
  — весь response в лог.
- `settings.py` — `data=settings_data`, `data=salary_setting_data`,
  `data=year_data` — настройки и календарь в логах.

**Что не так:** логи содержат рабочие данные пользователей, заметки, время
работы.

**Снижение срочности:** логи хранятся **локально на сервере**, не отгружаются
в облачные log-провайдеры. Утечка возможна только если кто-то получит доступ
к серверу — но если получит, у него и так есть доступ к БД. Поэтому это
не 🔴, а **🟡** на практике.

**Но!** Это становится 🔴 в момент:
- переезда логов в облачный провайдер (Sentry, CloudWatch, Logtail и т.п.)
- бэкапа сервера в стороннее место
- передачи логов разработчику для дебага (например, мне в чат)

**Фикс (когда дойдёт время):**

1. **На уровне `LogManager`:** добавить параметр `level`. По умолчанию
   `data` не логируется в info-сообщениях, только в DEBUG (который выключен
   в проде).

2. **Альтернатива:** логировать `len(data)` или хеш, не сами данные.

3. **На критичных эндпоинтах** (где есть PII): просто убрать `data=...`
   из log_network_info, оставить только `user_id` и краткое описание.

### 4a. Ротация логов и разбивка по группам

Связанная с п. 4 задача — **logrotate** и структура логов.

**Что обычно нужно:**
- Разбивка логов по уровням и компонентам: `error.log`, `info.log`,
  `network.log`, `share.log` — отдельные файлы.
- Ротация: `logrotate` с конфигом, который держит, например, 14 дней
  и сжимает старые в `.gz`. Удаление автоматически.
- Опционально — структурированное логирование (JSON-формат вместо
  свободного текста), чтобы можно было искать по полям через `jq` или
  отгружать в анализаторы.

**Как сделать (минимальный вариант):**

1. В `LogManager.setup_loggers()` (`src/core/log_manager.py`) проверить,
   что используется `RotatingFileHandler` или `TimedRotatingFileHandler`
   из `logging.handlers`. Если просто `FileHandler` — заменить.

   Пример с `TimedRotatingFileHandler`:
   ```python
   from logging.handlers import TimedRotatingFileHandler

   handler = TimedRotatingFileHandler(
       "/var/log/loco-driver/network.log",
       when="midnight",      # ротировать в полночь
       backupCount=14,       # хранить 14 дней
       encoding="utf-8",
   )
   ```

2. Альтернатива — внешний `logrotate` (системный). Конфиг
   `/etc/logrotate.d/loco-driver`:
   ```
   /var/log/loco-driver/*.log {
       daily
       rotate 14
       compress
       delaycompress
       notifempty
       copytruncate
   }
   ```
   `copytruncate` нужен, потому что Python-приложение держит файл открытым
   и не реагирует на rename — `logrotate` копирует и обрезает оригинал.

3. Разбить логи по компонентам — если `LogManager` сейчас всё пишет в один
   файл, поделить:
   - `auth.log` — попытки логина, регистрации, смены паролей
   - `network.log` — запросы и ответы API
   - `error.log` — все ошибки уровня ERROR
   - `share.log` — операции шаринга

   Это делается через несколько Logger'ов с разными именами и handler'ами.

---

### 5. Утечка деталей реализации в 500-ответах

**Где:**
- `settings.py` `POST /salary_settings/` — особенно ярко: уходит имя
  проблемного поля БД, имя класса исключения, текст `str(e)`.
- `route.py` использует `_short_error` — частично фильтрует, но всё равно
  отдаёт сообщения вида `"column \"x\" does not exist"`.

**Что не так:** клиент может узнавать имена таблиц, колонок, фрагменты SQL —
помогает планировать атаки.

**Фикс:**

```python
# Универсальный handler:
except Exception as e:
    error_id = uuid.uuid4()
    await LogManager.log_network_error(
        endpoint="POST salary_settings/",
        error=f"[{error_id}] user_id={current_user.id}, exception={traceback.format_exc()}"
    )
    raise HTTPException(
        status_code=500,
        detail=f"Внутренняя ошибка ({error_id})"  # клиент может прислать ID в support
    )
```

Детали — в логи. Клиенту — ID, по которому ты найдёшь полную ошибку в логах.

---

## 🟡 Важное (после критичного)

### 6. CORS открыт всему миру

**Где:** `main.py`, `app.add_middleware(CORSMiddleware, allow_origins=["*"])`.

**Фикс:**

```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "https://<домен-сайта-шаринга>",
        "http://localhost:3000",  # dev
        "http://127.0.0.1:3000",
    ],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)
```

Учти: если есть админка или ещё какой-то фронт — добавить и его домен.

---

### 7. `forgot_password` раскрывает наличие email в БД

**Где:** `src/api/v1/page.py`, `POST /v1/page/forgot_password`.

**Что не так:** при несуществующем email — 404. При существующем — 200.
Можно перебирать список email и проверять регистрацию.

**Фикс:** всегда возвращать одинаковый ответ:
```python
# Если пользователь найден — отправить email.
# Если не найден — ничего не делать.
# В обоих случаях:
return templates.TemplateResponse(
    "forgot_password_sent.html",  # "Если такой email есть в системе — мы отправили письмо"
    {"request": request},
    status_code=200,
)
```

---

### 8. Reset-token в query string

**Где:** `GET /v1/page/reset_password_template?access_token=...`

**Что не так:** токен попадает в access-логи и Referer-заголовки.

**Фикс:** перевести на POST с токеном в форме или короткоживущую cookie.
Это требует изменений в email-шаблоне (URL формы). Не самое срочное —
токен живёт 10 минут, окно атаки маленькое.

---

### 9. `vk_id` в response — единственный snake_case

**Где:** `src/schemas/request.py`, `UserSafeResponse.vk_id`.

**Что не так:** ломает конвенцию. Менять — breaking change для клиента.

**Решение:** оставить как есть, задокументировано в reference. При
поднятии `/v2/` API — переименовать.

---

### 10. Rate-limit `/v1/share/route` не масштабируется

**Где:** `src/api/v1/share.py`.

**Что не так:** in-memory счётчик не шарится между worker'ами/инстансами.
Если у тебя один worker — работает. Если несколько — лимит фактически
умножается на их число.

**Фикс:** перевести на Redis-бэкенд (например, `slowapi`). Не срочно,
если воркер один.

---

## 🟢 Когда дойдут руки

### 11. Транзакция `POST /v1/route/` коммитится даже при ошибках
### 12. `electricSectionList` не вычищает удалённые секции
### 13. Notes накапливают мусор
### 14. `isHeavyLongDistance` всегда теряется
### 15. `int(accepted_energy)` теряет точность
### 16. `GET /v1/route/` возвращает 404 при пустом списке вместо `200 []`

См. описания в `31_API_REFERENCE.md` → раздел «Известные проблемы».

---

## Что осталось (roadmap по сессиям)

### 🔴 На следующей сессии безопасности

**Координированная миграция публичных GET-эндпоинтов** (п. 2 выше).
Это требует двух релизов: сначала Android-клиент должен начать слать
`Authorization: Bearer ...` к этим эндпоинтам, потом сервер начнёт требовать
авторизацию. Между релизами — пауза 1-2 недели на распространение
обновлённого клиента в Google Play.

### 🟡 На отдельных сессиях (приоритет средний)

| # | Задача | Срочность | Зависит от |
|---|---|---|---|
| - | TLS для API через Let's Encrypt + переезд клиента на HTTPS-URL | средняя | домен `api.locodriver.ru` или поддомен, релиз клиента |
| - | Email-эндпоинты на freemyip.com → переезд на свой домен | средняя | TLS поднят |
| - | Adminer через SSH-туннель (`127.0.0.1:8080:8080` + SSH `-L`) | средняя | — |
| 5 | Маскировка 500-ответов (не светить SQL и имена таблиц) | средняя | — |
| 6 | CORS — список доменов вместо `*` | средняя | известны домены клиентов |
| 7 | `forgot_password` — единый ответ независимо от наличия email | низкая | — |
| 8 | Reset-token из query string в form / cookie | низкая | — |

### 🟢 Когда дойдут руки (низкая срочность)

- PII в логах — отключить `data=` в `LogManager.log_network_info`
- Logrotate + разбивка по группам (auth/network/error/share)
- Каскады в БД через `ON DELETE CASCADE` (вместо ручного `clear_user.py`)
- `BasicData` без `extra = 'ignore'` — добавить
- `/v1/year/` schemaless — описать Pydantic-схемой
- `PATCH /vkId/add` молчаливо no-op → 409
- Унифицировать имена эндпоинтов (`/release_days` vs `/auth` vs `/forgot_password`)
- Удалить мёртвое поле `photos` из `SyncData` (после релиза клиента, который его не шлёт)
- **Куча багов поведения** — см. `31_API_REFERENCE.md`:
  - `isHeavyLongDistance` всегда теряется
  - `int(accepted_energy)` теряет точность
  - Notes накапливают мусор
  - `GET /v1/route/` возвращает 404 при пустом списке (должен `200 []`)
  - Расхождение `fuelSupplyKg` vs `fuelSupplyInKilo` в DieselSection
  - `surchargeLongTrainsList` есть на сервере, нет в Kotlin
  - Транзакция `POST /v1/route/` коммитится даже при ошибках

---

## Контекст (ответы на исходные вопросы — для следующих сессий)

- **Клиент использует** публичные эндпоинты `/v1/route/stations/`,
  `/routes/`, `/trains/`, `/search/routes`. Их нельзя просто закрыть —
  нужна координированная миграция (см. п. 2).
- **Логи хранятся локально на сервере**, никуда не отгружаются. Это
  снижает срочность фикса п. 4 (PII в логах) с 🔴 до 🟡.
- **Один worker** (`--workers 1`). Это значит:
  - Race condition (п. 3) проявляется только в asyncio одного процесса
    (окно меньше, но воспроизводимо). **Закрыто на сессии 25 апреля.**
  - In-memory rate limit на share работает корректно — фикс не срочный.
  - **Любая блокирующая операция в коде убивает весь сервер для всех
    пользователей.** Async-весь-стек критически важен.

---

## 🔴🔴 КРИТИЧНО: проблемы deployment

Найдено при анализе команды запуска:
```
uvicorn src.main:app --reload --workers 1 --host 0.0.0.0 --port 8000
```

Запущено от `root`, без обратного прокси (предположительно).

### D1. `--reload` в проде

**Что не так:** этот флаг — для разработки. В проде:
- Постоянное отслеживание ФС через watchfiles — лишняя нагрузка.
- Случайное изменение файла перезапускает сервер в момент работы пользователей.
- При `git pull` или `scp` сервер перезапускается сам, без контроля.
  Если код несовместим с БД-схемой — краш-цикл.
- Connection pool SQLAlchemy не закрывается gracefully при reload.

**Фикс:** убрать `--reload`. Это одно изменение в команде запуска.

### D2. Запуск от `root`

**Что не так:** при любой RCE-уязвимости в FastAPI / любой Python-зависимости
атакующий получает **root** на сервере.

**Фикс:**
```bash
# Создать выделенного пользователя
useradd -r -s /usr/sbin/nologin -d /opt/locodriver locodriver

# Передать ему права на код и логи
chown -R locodriver:locodriver /opt/locodriver /var/log/locodriver

# Запускать от него (см. systemd unit ниже)
```

Минус: для биндинга на порт ниже 1024 нужны привилегии. Но 8000 ≥ 1024,
так что для текущей конфигурации проблем нет.

### D3. Открытый порт 8000 наружу (если нет обратного прокси)

**Проверка:**
```bash
# Публичный IP сервера
curl -s ifconfig.me

# С другой машины:
curl -s -o /dev/null -w "%{http_code}\n" http://<этот-IP>:8000/v1/ping
```
Если возвращает `200` — порт виден из интернета напрямую.

```bash
# Что слушает порты:
ss -tlnp | grep -E ":80|:443|:8000"
```
Если на 80 или 443 ничего не висит — нет TLS, всё ходит **открытым текстом**.

**Если обратного прокси нет и API доступен на 8000:**
🔴 **JWT, пароли, данные пользователей передаются без шифрования.**
Любой между клиентом и сервером (Wi-Fi провайдер кафе, мобильный оператор,
владелец промежуточного маршрутизатора) видит всё в plain.

**Фикс:** поставить nginx или Caddy перед FastAPI:
1. Бесплатный TLS-сертификат через Let's Encrypt (`certbot` или встроенный
   в Caddy ACME).
2. nginx/Caddy слушает 80/443, проксирует на `127.0.0.1:8000`.
3. uvicorn биндится только на `--host 127.0.0.1`, не на `0.0.0.0`.

Минимальная конфигурация Caddy (`/etc/caddy/Caddyfile`):
```
api.locodriver.ru {
    reverse_proxy 127.0.0.1:8000
}
```
Caddy сам получит сертификат при первом запуске.

После настройки прокси:
- В Android-клиенте поменять URL API на `https://api.locodriver.ru` (или
  как назовёшь домен) — это **breaking change**, нужен релиз. До релиза
  старая версия клиента продолжит ходить на HTTP и продолжит сливать
  данные. Решение — **поставить редирект** на серверной стороне с HTTP на
  HTTPS, тогда старые клиенты перестанут работать (получат редирект,
  который OkHttp по умолчанию следует, но JWT и body могут потеряться при
  редиректе POST).
- **Лучший вариант**: сначала релиз клиента с новым URL → ждать → выключить
  HTTP на сервере. Аналогично сценарию с публичными эндпоинтами.

### D4. Нет supervisor / systemd unit

**Что не так:** при краше процесса сервер не поднимется. При перезагрузке
сервера — тоже.

**Фикс:** systemd unit. Создать `/etc/systemd/system/locodriver.service`:
```ini
[Unit]
Description=LocoDriver API
After=network.target postgresql.service

[Service]
Type=simple
User=locodriver
Group=locodriver
WorkingDirectory=/opt/locodriver
ExecStart=/usr/local/bin/uvicorn src.main:app --workers 1 --host 127.0.0.1 --port 8000
Restart=always
RestartSec=5
# Лимиты для безопасности
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=/var/log/locodriver
ProtectHome=true

[Install]
WantedBy=multi-user.target
```
Активация:
```bash
systemctl daemon-reload
systemctl enable --now locodriver
```
Управление:
```bash
systemctl status locodriver
systemctl restart locodriver
journalctl -u locodriver -f   # смотреть логи
```

### Порядок фиксов deployment

**Сегодня (можно сразу, ничего не ломает):**
- [ ] Убрать `--reload` (D1) — просто перезапуск с новой командой.

**На неделе (нужна подготовка):**
- [ ] Поставить Caddy/nginx + TLS (D3). Изначально оставить HTTP открытым
      на 8000 для совместимости со старыми клиентами. Поднять HTTPS на 443.
- [ ] Релизнуть клиента с новым URL `https://...`. Подождать.
- [ ] Закрыть порт 8000 наружу (firewall: `ufw deny 8000`), uvicorn
      перевести на `--host 127.0.0.1`.
- [ ] systemd unit (D4).
- [ ] Выделенный пользователь (D2).

---


