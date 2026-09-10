# Интеграция платёжной системы CKassa

> Ветка: `feature/ckassa-payments` (и в клиенте, и в серверном репозитории
> `proxy-parser`). Статус: **в работе**. Документация провайдера:
> <https://docs.ckassa.ru/doc/open-api/#poryadok-integracii>

Цель — перевести приём оплаты подписки «Машинист Pro» на CKassa, единообразно
на всех платформах (Android, iOS, PWA), не ломая существующих пользователей
Robokassa (он остаётся включённым на переходный период).

---

## 1. Модель CKassa Open API (что удалось выяснить из доки)

Провайдер работает по **инвойсовой схеме с хостовой страницей оплаты**
(аналог PWA-пути Robokassa), а не через встраиваемый мобильный SDK.

| Что | Как |
|---|---|
| Авторизация | Заголовки `ApiLoginAuthorization` (логин) + `ApiAuthorization` (секрет). TLS ≥ 1.2. Секрет **только на сервере** |
| База | prod `https://api2.ckassa.ru/api-shop/rs/open`, test `https://demo-api2.ckassa.ru/api-shop/rs/open` |
| Создать оплату | `POST invoice/create2` → `servCode`, `amount` (**в копейках, целое**), `properties` (**массив только значений, без имён**), `invType`, `startPaySelect`, `bestBefore`, (demo: `tgInvPayer`). Ответ — **строка-URL** `https://bc.ckassa.ru/xxxx` (без regPayNum/orderId) |
| Отменить | `POST invoice/cancel` (`invoiceUrl`) |
| Статус (pull) | `GET payments/new` → `regPayNum`, `amount`, `state`, `payTools`, `properties`, `receipt` |
| Чек | `POST payment/receipt2` (`regPayNum`) |
| Callback (push) | S2S `POST`: `regPayNum`, `amount` (строка, копейки), `state` (**успех = `PAYED`**), `result{code,message,details}`, `property`/`map` (эхо реквизитов), `cardPan`, `rrn`, `irn`, `approvalCode`, `created`. Ретраи **12 раз, 1–180 мин**, ждёт HTTP 200 |
| Безопасность callback | **HMAC-подписи в доке нет.** Доверие по IP-allowlist `178.161.210.54`, `94.138.149.0/24` + (опц.) сверка через `GET payments/new` |

### Данные услуги (из письма CKassa)
- **servCode = `17233-20252-1`** («Подписка в приложении Машинист») — в `.env`
  (`CKASSA_SERV_CODE`), боевой; тестовый — из доки.
- **`properties` услуги = один реквизит `E_MAIL`** (строка 1–60, обязательный,
  ключевой). В запросе передаётся **только значение**: `properties: ["<email>"]`.
- В callback CKassa вернёт лишь `regPayNum` / `amount` / `state` + **эхо
  `E_MAIL`** в `property`/`map`. **Своих полей (`user_id`, `tariff_code`,
  `platform`) передать через `properties` НЕЛЬЗЯ** → см. связку в §4/§5.

### ⚠️ Что ещё подтвердить у менеджера CKassa
1. Есть ли у callback подпись, или только IP-allowlist (сейчас — только IP).
2. Точные значения `state` (принимаем `PAYED` + синонимы на всякий случай).
3. **Рекуррент** — см. §2.

---

## 2. Рекуррентные платежи — вывод

**В присланной документации Open API рекуррента нет** — ни привязки карты,
ни токена, ни автосписания. Раздел покрывает только разовые инвойсы.

Маркетинговый сайт CKassa (ckassa.com) заявляет поддержку регулярных платежей
и запоминания карты — значит, это, вероятно, **отдельный протокол/продукт через
менеджера**, а не документированный Open API.

**Решение по архитектуре:** продление НЕ зависит от рекуррента. Ручное
продление работает сразу. Автосписание — опциональная надстройка (колонки
`card_token`/`auto_renew`/`next_charge_at` + cron), заводится только после
подтверждения менеджером API привязки карты.

---

## 3. Цены — единый источник правды

Цены и скидки формируются в **кабинете администратора `api.locodriver.ru`**
(`/admin/tariffs` → таблица `subscription_tariff`, публичное чтение
`GET /v1/tariffs`). CKassa только списывает переданную сумму.

Клиент передаёт **только `tariff_code`**. Сервер сам берёт `effective_price`
и `period_days` из БД → подменить сумму/срок с клиента нельзя. `amount` в
CKassa = `round(effective_price * 100)` копеек.

---

## 4. Целевой поток оплаты (единый для всех платформ)

```
Клиент (Android / iOS / PWA)
   │  POST /v1/payment/ckassa/checkout { tariff_code, platform }   (JWT)
   ▼
Сервер: тариф из БД (со скидкой) → СОХРАНЯЕТ payment_intent
        {user_id, email, tariff_code, platform, amount, period_days}
        → invoice/create2 (amount = цена×100 коп., properties=[email])
   │  ← { payment_url }
   ▼
Клиент открывает payment_url:
   Android → Chrome Custom Tab
   iOS     → SFSafariViewController / ASWebAuthenticationSession
   PWA     → redirect
   ▼
Оплата на странице CKassa
   ├─► S2S callback → POST /v1/payment/ckassa/callback
   │        сервер: IP-allowlist → идемпотентность(regPayNum) →
   │        МАТЧ намерения по (email-эхо + amount) → user/tariff/platform/дни →
   │        продление subscriptionPeriod (в одной транзакции)
   └─► возврат в приложение (deeplink) / redirect на PWA success|fail
   ▼
Клиент поллит статус подписки (переиспользуем PurchasesViewModel.checkPaymentOnServer)
```

**Связка платежа (важно):** CKassa не даёт своего correlator между `create2`
и callback, а в `properties` разрешён только `E_MAIL`. Поэтому при checkout
пишем `payment_intent`, а в callback находим её по **`email` (эхо) + `amount`**
(самое свежее `pending`). Fallback без намерения: `email → пользователь`,
`amount → активный тариф`.

**Как приходит оплата и продлевается срок:**
- Со всех платформ одинаково: единственный источник истины о деньгах —
  **S2S-callback от CKassa** (+ pull `payments/new` как страховка). Клиент
  серверу об оплате не сообщает.
- Продление: `subscriptionPeriod = max(текущий, now) + period_days`
  (докупка до истечения продлевает поверх остатка).

---

## 5. Серверная часть (`proxy-parser`, ветка `feature/ckassa-payments`)

Новый пакет `backend/src/ckassa/` (по образцу `src/robokassa/`), Robokassa не трогаем.

- `client.py` — `CkassaClient` (httpx async): `create_invoice`, `cancel_invoice`,
  `get_payment` (payments/new по regPayNum), `receipt`. Заголовки-auth, base-url
  из конфига, `CKASSA_TEST_MODE`.
- `ckassa_routers.py` — роутер `prefix="/payment/ckassa"`:
  - `POST /checkout` (JWT) — цена/срок из БД, `invoice/create2`, вернуть `payment_url`.
  - `POST /callback` — IP-allowlist → идемпотентность → сверка → продление.
  - `GET /success`, `GET /fail` — redirect на PWA / deeplink.
- `repository.py` — `PostgresPaymentIntentDbClient.create` (намерение при
  checkout) + `PostgresPaymentTransactionDbClient.process_ckassa_callback`
  (claim + матч намерения + продление в одной транзакции). Платформа платежа
  (`android`/`ios`/`pwa`) сохраняется здесь.
- `models/payment_transaction.py` (журнал/идемпотентность, уникальность
  `(provider, reg_pay_num)`, поле `platform`) + `models/payment_intent.py`
  (связка + снапшот `period_days`) + миграция `036_ckassa_payment_transaction`
  (down_revision = `035_passenger_wagon_length`, обе таблицы).
  **Идемпотентность обязательна** — CKassa шлёт callback до 12 раз, повторно
  начислять срок нельзя (у текущего Robokassa-кода дедупликации нет).
- `core/config.py` — env: `CKASSA_API_LOGIN`, `CKASSA_API_SECRET`,
  `CKASSA_SERV_CODE`, `CKASSA_BASE_URL`, `CKASSA_TEST_MODE`, `CKASSA_ALLOWED_IPS`,
  `CKASSA_ENABLED`.
- Регистрация: `models/__init__.py`, импорт в `pg_client.py` (не нужен — свой
  repository), `api/routers.py` (`include_router`).

**Деплой (память проекта):** Alembic на CI не гоняется — миграцию применяем
руками ДО слияния в `main`, revision id ≤ 32 символов. БД/Redis на `127.0.0.1`.
Callback-логи не должны писать PII (email/токены/`data=...`).

---

## 6. Клиентская часть

- **Android**: заменить встроенный Robokassa SDK на серверный flow — метод
  `ckassaCheckout(tariffCode)` в `RemoteRestApi`, открытие `payment_url` в
  Chrome Custom Tab, возврат по deeplink `locodriver://`, затем существующий
  `checkPaymentOnServer()`-поллинг. Убрать зашитые креды из `PurchasesViewModel`.
  Модуль `robokassa_sdk` остаётся legacy до отказа от старых сборок.
- **iOS**: реализовать `PurchasesView` (сейчас заглушка `// TODO: initPayment`)
  по паттерну проекта: `PurchasesIosViewModel` (`watchX`) + Swift-wrapper,
  `SFSafariViewController`, поллинг. Deeplink — только кастомная схема
  (Universal Links нет).
- **PWA**: добавить провайдера CKassa в существующий `/payment`-поток.

⚠️ **Политика сторов:** Apple App Store для цифровых подписок обычно требует
свой IAP — внешняя оплата может не пройти ревью (уточнить до релиза iOS).
Для RuStore внешняя оплата допустима.

---

## 7. Этапы

1. [x] Ветки + изучение доки/кода.
2. [ ] **Сервер: модуль CKassa на test-контуре (`demo-api2`)** ← текущий шаг.
3. [ ] Уточнения у менеджера (§1) и подгонка схемы create2/callback.
4. [ ] Android на новый flow (фиче-флаг, Robokassa как fallback).
5. [ ] iOS-экран покупок.
6. [ ] PWA-провайдер.
7. [ ] (Опц.) Рекуррент, если подтверждён менеджером.

---

## 8. Контрактные гарантии (правила CLAUDE.md)

- `user_setting.subscriptionPeriod` не меняется — CKassa пишет в то же поле,
  старые клиенты не ломаются.
- Все новые эндпоинты аддитивны; `/v1/tariffs` формат не трогаем.
- Robokassa остаётся включённым на переходный период.
