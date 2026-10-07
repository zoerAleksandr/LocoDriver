# Серверная часть истории треков — дизайн (этап 3а)

Статус: дизайн, код/миграции не написаны. Репозиторий сервера: `/Users/zoer/Downloads/proxy-parser-sps` (worktree proxy-parser). Исходные требования — `docs/TRAIN_MAP_PLAN.md`, «Хранение треков и история на сервере» и «Решения по хранению (08.10.2026)». Контракт существующих эндпоинтов не меняется: только новые таблицы и маршруты.

## 1. Что уже есть и что переиспользуем

Есть (прочитано в коде):
- `models/advisor.py`: `AdvisorTrip` (`advisor_trip`: `user_id` FK, `points` JSONB `[tMs, lat, lon, speedKmh, altM, baroAltM|null, accM]`, `stops`, до `MAX_TRIP_POINTS=60_000`), `AdvisorSection` (геометрия `[km, lat, lon, alt]`, `profile_variants`, `version`).
- `POST /v1/advisor/trips` (`api/v1/advisor.py`) — выгрузка сырой поездки целиком, upsert по `tripId`, авто-привязка к участку. Это вклад для кабинета: админ видит список, точки и GPX (`admin_advisor.py`: `list_recent`, `/trips/{id}.gpx`, `/api/trips/{id}/altitude`). Он НЕ подходит для личной истории: точки в JSONB в открытом виде, привязаны к `user_id` и видимы администратору.
- `services/advisor_profiles.py`: `sensor_profile(points, length_km, offset, reverse, window)` — профиль уклонов из высот (медианный фильтр, регрессия, слияние); `AdvisorSectionDbClient.save_variant(..., "sensors", ...)` кладёт результат в `profile_variants`.
- `utils/rate_limit.py`: `SlidingWindowRateLimiter` (in-process, 1 worker). `services/route_tombstones.py`: образец фоновой чистки (`asyncio`-цикл каждые 6 ч, запуск в `main.py` через `add_event_handler("startup")`). `core/compression.SelectiveGZipMiddleware` (сжатие ответов). `get_current_user`; образец проверки владельца — `RouteOwnershipError` в `pg_client.py`/`route.py` (404 на чужое).
- Миграции — raw SQL `op.execute("... IF NOT EXISTS")`, FK на `"user"(id) ON DELETE CASCADE` (пример 005/006/030). Голова цепочки на момент чтения — `049_station_geo` (перед созданием проверить `alembic heads`).

Переиспользуем: рейт-лимитер, паттерн фоновой чистки, `get_current_user`, стиль миграций, `AdvisorSectionDbClient.save_variant`, ядро `sensor_profile` (медианный фильтр, `_local_grade`, `_merge_elements`).
Не переиспользуем: `advisor_trip` и `/v1/advisor/trips` для личной истории (см. выше) — отдельные таблицы. Админ-кабинет к новым таблицам доступа не получает.

## 2. Схема (миграция `050_user_tracks`, затем `051_altitude_contrib`)

Принцип: сырые координаты только в `BYTEA` сжатых и зашифрованных на уровне приложения чанков (не JSONB) — чтобы дамп БД, бэкапы и `adminer` (он есть в репозитории) не показывали координаты. Шифр: AES-256-GCM, ключ из env `TRACKS_ENC_KEY` (на проде в `.env`, вне БД), ключ трека = HKDF(master, `track_id`), nonce 12 байт в начале blob. Потеря ключа = потеря треков (бэкап ключа — отдельно).

**`track_consent`**: `user_id UUID PK FK "user"(id) CASCADE`, `storage_granted BOOLEAN NOT NULL DEFAULT false`, `storage_granted_at TIMESTAMPTZ`, `storage_revoked_at TIMESTAMPTZ`, `policy_version VARCHAR(16)`, `updated_at TIMESTAMPTZ`. Согласия №2 здесь нет (см. §4).

**`user_track`**: `id UUID PK` (клиентский UUID), `user_id UUID NOT NULL FK CASCADE`, `route_id UUID NULL` (без FK: рейс может быть удалён; привязка для UI «история по рейсу»), `title VARCHAR(200)` (плечо/поезд — текст для карточки), `started_at BIGINT`, `ended_at BIGINT` (epoch ms, BigInteger явно — урок asyncpg int4), `distance_km`, `max_speed_kmh`, `avg_speed_kmh REAL`, `points_count INT`, `chunks_expected INT NULL`, `status VARCHAR(12)` (`uploading`|`complete`), `is_favorite BOOLEAN DEFAULT false`, `preview BYTEA NULL` (зашифрованная полилиния ≤100 точек для мини-карты карточки — чтобы список не читал чанки), `completed_at`, `expires_at TIMESTAMPTZ NULL` (NULL = избранное, не удаляется), `created_at`, `updated_at`, `bytes_total INT`.
Индексы: `(user_id, started_at DESC)` — список; partial `(expires_at) WHERE expires_at IS NOT NULL` — чистка; `(user_id, status)` — квота/незавершённые.

**`user_track_chunk`**: `track_id UUID FK user_track CASCADE`, `chunk_no INT`, `user_id UUID NOT NULL` (денормализация для быстрого «удалить всё» и проверки владельца без join), `points_count INT`, `t_from BIGINT`, `t_to BIGINT`, `payload BYTEA NOT NULL` (nonce+AES-GCM(zlib(binary v1))), `sha256 BYTEA(32)` (по открытому сжатому содержимому до шифрования — для идемпотентности), `size INT`, `created_at`. PK `(track_id, chunk_no)`; индекс `(user_id)`.

**TTL 180 дней**: константа `TRACK_RETENTION_DAYS=180` (config/env). `expires_at = completed_at + 180d` при `complete`. Включение звезды → `expires_at=NULL`; снятие → `expires_at = now + 30d` (не удалять мгновенно). Незавершённые (`uploading`) старше 7 дней удаляются. Чистка: `services/track_retention.py` по образцу `route_tombstones` — asyncio-цикл раз в 6 ч, запуск в `startup` рядом с `start_tombstone_cleanup_task`; батчи `DELETE FROM user_track WHERE id IN (SELECT id ... WHERE expires_at < now() AND is_favorite=false LIMIT 500)` (чанки уходят каскадом), цикл до пустого батча; `pg_try_advisory_lock(<const>)` на случай >1 процесса. Логируем только число удалённых. Внешний cron не нужен.

**`advisor_alt_contrib`** (обезличенный вклад, миграция 051): `id UUID PK` (случайный UUID, генерирует клиент на фрагмент, никак не связан с `tripId`/`user_id`), `section_id UUID NOT NULL FK advisor_section CASCADE`, `section_version INT`, `km_from_m INT` (начало фрагмента по оси участка), `step_m SMALLINT` (25), `alts BYTEA` (zlib дельт-массива высот в дм; `null`-маркер для пропусков), `source VARCHAR(8)` (`gnss`|`baro`), `mean_acc_m REAL`, `received_on DATE` (только день, не время), `n INT`. Нет `user_id`, времени поездки, координат. Индекс `(section_id, km_from_m)`. Хранение 365 дней (`received_on`), чистка в том же фоновом цикле.

## 3. Эндпоинты (все под `/v1`, JWT `get_current_user`, camelCase)

Формат чанка (`ld-track-v1`, до zlib): заголовок `LT|ver=1|flags|n` + колонки подряд (колонночная раскладка лучше сжимается): `dt` (мс от предыдущей точки, varint), `dLat`/`dLon` (1e-6° дельты, zigzag varint), `speed` (0,1 км/ч, varint), `alt` (дм, дельта zigzag; маркер «нет»), `baro` (то же + битовая маска наличия), `acc` (дм, varint). База (t0, lat0, lon0) — в заголовке. Затем zlib level 6. Оценка: ~12 Б/точку до zlib, ~6–9 Б после; смена 24 ч при 1 точке/5 с ≈ 17 тыс. точек ≈ 100–150 КБ (JSON — около 1,2 МБ, то есть ×8–10). 180 дней × смена через день ≈ 90 треков ≈ 10–13 МБ/пользователя; 1000 активных ≈ 10–13 ГБ — приемлемо для BYTEA (TOAST). Чанк ≤ 2000 точек (~15 КБ сжатого, 8–9 чанков на смену), лимит чанка 256 КБ.
Транспорт: JSON-обёртка `{"enc":"ld-track-v1","data":"<base64>"}` (проходит через существующий прокси без нюансов; +33% — допустимо). Сервер декодирует и валидирует (диапазоны lat/lon, монотонность t, число точек = заявленному), ошибки без значений в тексте.

| Метод и путь | Тело / ответ | Коды |
|---|---|---|
| `GET /tracks/consent` | → `{storageGranted, grantedAt, policyVersion}` | 200 |
| `PUT /tracks/consent` | `{storageGranted: bool, policyVersion}`. Отзыв → в одной транзакции удаляются все `user_track` пользователя → `{storageGranted:false, deletedTracks:n}` | 200, 429 |
| `PUT /tracks/{id}` | мета: `{routeId?, title, startedAt, endedAt, distanceKm, maxSpeedKmh, avgSpeedKmh, pointsCount, chunksExpected, preview?}` — идемпотентный upsert | 200; 403 `{code:"consent_required"}`; 404 чужой `id`; 413/`quota_exceeded`; 422; 429 |
| `PUT /tracks/{id}/chunks/{n}` | `{enc, data, sha256}` → `{chunkNo, status:"stored"\|"duplicate"}` | 200; 403; 404 (нет трека/чужой); 409 `chunk_conflict` (тот же n, другой sha256); 413; 422; 429 |
| `POST /tracks/{id}/complete` | `{chunks:N}` → `{status:"complete", missing:[…]}`; при пропусках — `status:"uploading"` и список `missing` (клиент дошлёт) | 200; 403; 404; 429 |
| `GET /tracks?limit=50&cursor=&favorite=` | карточки: `id, title, routeId, startedAt, endedAt, distanceKm, durationS, avgSpeedKmh, maxSpeedKmh, isFavorite, expiresAt, preview` + `nextCursor` (keyset по `started_at,id`) | 200, 429 |
| `GET /tracks/{id}` | мета + `chunks:[{n,pointsCount,tFrom,tTo}]` | 200; 404 |
| `GET /tracks/{id}/chunks/{n}` | `{enc, data}` (клиент декодирует) | 200; 404 |
| `PATCH /tracks/{id}` | `{isFavorite?, title?}` | 200; 404 |
| `DELETE /tracks/{id}` | идемпотентно | 204 (и для отсутствующего, и для чужого — без перебора id), 429 |
| `DELETE /tracks` | `{confirm:true}` — «все мои треки» → `{deleted:n}` | 200; 400 без confirm; 429 |
| `POST /advisor/altitude-contrib` | `{contribId, sectionId, sectionVersion, kmFromM, stepM, source, meanAccM, data}` → `{accepted:true}` | 200 (повтор того же `contribId` — тоже 200 `duplicate`); 404 неизвестный/неопубликованный участок; 409 `section_version_mismatch`; 422; 429 |

Идемпотентность: трек — по `id` (PK, клиентский UUID), чанк — по `(track_id, chunk_no)`; повтор с тем же `sha256` → `duplicate`, 200. Чужой `id` трека ВСЕГДА 404 (как `RouteOwnershipError`→404): проверка `WHERE id=:id AND user_id=:uid` на каждом пути, включая `chunks` (поле `user_id` в чанке сверяется тоже).
Лимиты (per `user_id`, `SlidingWindowRateLimiter`; в памяти, 1 worker — оговорка как в самом классе): чанки 300/ч; мета и complete 60/ч; списки 120/мин; DELETE одиночный 120/ч; DELETE all и consent 5/ч; contrib 60/ч. Квоты: трек ≤ 100 000 точек и ≤ 60 чанков; пользователь ≤ 500 треков и ≤ 300 МБ (избранные считаются) → 413 `quota_exceeded`. Тело запроса ≤ 512 КБ (проверка до декодирования).

## 4. Согласия и отзыв

- **Согласие №1 (хранение на сервере)**: `PUT /tracks/consent` пишет флаг+время+версию политики в `track_consent`. Без флага `PUT /tracks/{id}` и чанки → 403 `consent_required` (клиент показывает «Трек не будет сохранён: нет вашего согласия» и держит трек локально). Сервер проверяет флаг на каждой записи — клиентскому флагу не доверяем.
- **Отзыв** = `PUT consent {false}`: транзакция «флаг=false, `revoked_at`, `DELETE FROM user_track WHERE user_id`» (чанки каскадом); ответ с `deletedTracks`. Ретрай безопасен. Параллельно идущая загрузка после отзыва получает 403. Удаление аккаунта — каскад по FK.
- **Согласие №2 (обезличенная передача «км оси + высота»)**: независимо от №1 (можно включить №2 без №1 и наоборот). Принимается решение клиентом; сервер НЕ хранит флаг на пользователя (иначе появится связь «пользователь ↔ вклад» и нечего «отзывать» по смыслу). Тем самым после отправки вклад технически нельзя вернуть — это нужно прямо написать в тексте согласия и в политике. Включение №2 не завязано на №1 и не читает `user_track`.
- Политика конфиденциальности: обновить до выкладки клиента (геолокация, срок 180 дней, отзыв, обезличенный вклад необратим).

Приём вклада в профили высот: фрагмент (высоты на оси участка шаг 25 м, уже очищенные клиентом от `accuracy>20`) складывается в `advisor_alt_contrib`. В кабинете `/admin/advisor` — новая кнопка «профиль из вкладов» (отдельный от текущего `profile-sensors` по `tripId`): для каждой ячейки оси берётся медиана по вкладам (минимум 3 вклада, иначе ячейка пустая), результат — массив `(x_m, alt)` → `sensor_profile_from_heights()` (выносим хвост `sensor_profile` после получения `heights`, логика медианы/регрессии/слияния та же) → `save_variant(sid, "sensors", metadata={source:"crowd", contribCount})`. Админ видит только агрегат по участку, не отдельные вклады и не пользователей.

## 5. Безопасность и приватность

- Владелец: каждый запрос фильтруется по `user_id` из токена; чужое → 404; список/удаление — только свои. Проверку не выносить в клиентские параметры.
- Админ: у новых таблиц нет ни одного маршрута в `admin*.py`; сырые треки не отдаются кабинету, `list_recent`/GPX работают только по `advisor_trip` (его не трогаем). Поддержка по запросу пользователя: вариант — пользователь сам генерирует одноразовый код доступа, действующий 24 ч (в этот дизайн не входит, вопрос №4).
- Шифрование payload+preview на уровне приложения (см. §2): дамп БД/adminer/бэкап без `TRACKS_ENC_KEY` бесполезны. В `user_track` нет bbox и координат в открытом виде; `title` — только текст плеча.
- Логи: в `LogManager.log_*` — только `track_id`, `chunk_no`, число точек, размер, код результата. Никаких `lat/lon`, `points`, `data`, `preview`. Не отдавать `ValidationError` с входным значением (для этих роутеров свой обработчик 422 без `input`); тело не логировать; координаты никогда не в query/path; для `altitude-contrib` не логировать ни `user_id`, ни `contribId` с `sectionId` одновременно (достаточно счётчиков). Access-лог прокси (nginx/Caddy) — пути без координат; IP хранится по общему правилу сервера, к вкладу не привязывается.
- Обезличенность вклада: нет `user_id`, нет времени поездки, нет координат (только ось участка); `received_on` — дата без времени; JWT нужен только как защита от злоупотреблений и в БД не сохраняется. Риск отравления: медиана по ≥3 вкладам, ограничение уклона ±40‰ в `sensor_profile`, лимит 60/ч.
- Старые клиенты не затронуты: нет изменений в существующих маршрутах, моделях, `SyncData`; новые таблицы/пути; старый `POST /advisor/trips` остаётся.

## 6. Порядок выкладки и миграции

1. Ветка в серверном репозитории (не от `feature/advisor-cabinet`, если та ещё не в main — проверить `alembic heads`), id ревизий `050_user_tracks`, `051_altitude_contrib` (≤32 символа), raw SQL с `IF NOT EXISTS`, `downgrade` с `DROP TABLE IF EXISTS`.
2. Миграции применить на проде ВРУЧНУЮ (CI Alembic не запускает) ДО слияния PR в `main`. Добавить `TRACKS_ENC_KEY` в прод-`.env` (и staging) до слияния; без ключа роуты треков отвечают 503, остальной сервер работает.
3. PR в `main` → автодеплой; проверка `curl` consent→PUT meta→chunk→complete→list→delete на staging/прод тестовым пользователем.
4. Только после этого — клиент (этап 3б). Флаг-страховка: env `TRACKS_ENABLED` (по умолчанию true), выключатель без отката миграции.
5. Откат: `TRACKS_ENABLED=false` → 503; таблицы не дропать при откате кода.

## 7. Тест-план

- Юнит: кодек `ld-track-v1` (round-trip, null-высоты, дельты на границах ±180°/0 точек/1 точка, повреждённый zlib/AEAD tag → 422); шифрование (разные ключи по `track_id`); расчёт `expires_at` (завершение, звезда, снятие звезды); квоты.
- API (pytest + async клиент): полный цикл; идемпотентность (повтор чанка — `duplicate`, тот же n с другим sha — 409, повтор `PUT meta`, повтор `complete`); докачка по `missing`; чужой пользователь получает 404 на meta/chunk/PATCH/DELETE/GET и не может записать чанк в чужой трек; DELETE чужого и несуществующего → 204 без побочных эффектов; consent: 403 без согласия, отзыв удаляет все треки и чанки (и только свои); `DELETE /tracks` без `confirm` → 400; 429 по лимитам; 413 по размеру/квоте.
- Чистка: просроченный не-избранный удаляется, избранный и непросроченный — нет; незавершённый >7 дн. удаляется; цикл идемпотентен, батчи.
- Приватность: grep-тест/ассерт по перехваченным логам — после загрузки в логах нет lat/lon/`data`; ответ 422 не содержит входных значений; в `advisor_alt_contrib` нет колонок user/time; админ-роуты не возвращают данные `user_track` (404/нет маршрута).
- Вклад: приём, дубль `contribId`, неизвестный/неопубликованный участок → 404, `sectionVersion` mismatch → 409, агрегация (медиана, порог 3), `sensor_profile_from_heights` даёт тот же результат, что `sensor_profile` на тех же высотах.
- Совместимость: существующие тесты контракта/sync и `POST /advisor/trips` без изменений; миграция upgrade/downgrade на копии БД.

## 8. Открытые вопросы к пользователю

1. **Шифрование на уровне приложения** (ключ `TRACKS_ENC_KEY`, потеря ключа = потеря треков, бэкап ключа отдельно): делаем? Рекомендую да — иначе админ с доступом к БД/adminer видит координаты.
2. **Доступ поддержки к треку** по просьбе пользователя: нужен механизм (одноразовый код 24 ч, который пользователь выдаёт из приложения) или в первой версии «никогда»?
3. **Обезличенный вклад**: подтверждаете, что сервер не хранит флаг согласия №2 и вклад нельзя отозвать (отправленное не удаляется, так как не привязано к аккаунту)? Нужен ли JWT на приёме (защита от спама, но связь по токену на момент запроса видна серверу) или анонимный эндпоинт только с лимитом по IP?
4. **Квоты** (500 треков/300 МБ на пользователя, лимиты запросов) и **срок после снятия звезды** (30 дней до удаления) — устраивают? Звёздочные треки без срока и без верхней границы в пределах квоты — ок?
5. **Привязка к рейсу**: при удалении рейса пользователем трек остаётся (история) или удаляется вместе с ним? Рекомендую «остаётся», удаление — только явное.
