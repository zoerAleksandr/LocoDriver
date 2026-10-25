# Протокол запечатанных участков советника (LD3, запасной LD2)

Решение 25.10.2026. Нужен, чтобы выкачанный с сервера участок без приложения был набором случайных байтов, а утёкшую копию можно было отнести к аккаунту. Работает так же на Android (готово), iOS и PWA (делать по этому описанию).

## Запрос
- `GET /v1/advisor/sections` — список (JSON, как раньше), **только с `Authorization: Bearer <JWT>`**. Без токена 401.
- `GET /v1/advisor/sections/{id}` — участок, **только с токеном**; ответ `application/octet-stream`, `Cache-Control: no-store`.
- Квоты на аккаунт: участок 120/час и 400/сутки, список 120/час; на IP: 60/мин (список), 120/мин и 600/час (участок). Превышение — `429` + `Retry-After`; клиент молча остаётся на кэше. `503 sealed_unavailable` — на сервере не настроены ключи (участки в открытом виде не отдаются никогда).
- Клиент без токена запрос не шлёт (Android: `AdvisorApiException(401, "login_required")`).

## Конверт
```
"LD2" (3 байта ASCII) | kid (1) | version (4, big-endian) | nonce (12) | ciphertext | tag (16)
```
- ключ: `HKDF-SHA256(ikm = ключ приложения[kid] (32 байта), salt = nonce, info = "locodriver/section/v2|<sectionId>|<version>", L = 32)`;
- шифр: ChaCha20-Poly1305 (RFC 8439), AAD = `"LD2" | kid | "<sectionId>|<version>"` (строка UTF-8);
- открытый текст: JSON участка (тот же `SectionResponse`, что отдавался раньше, UTF-8), без сжатия;
- `sectionId` — тот, что в URL (строчный UUID), `version` — из заголовка конверта (число).
Любая ошибка (формат, неизвестный kid, тег) — загрузка участка не удалась, берётся кэш; JSON из конверта не «чинить».

## Ключи
- Сервер: `ADVISOR_SEAL_KEYS` = `{"1": "<64 hex>"}`, активный — `ADVISOR_SEAL_ACTIVE_KID`; пусто — 503. Ключи только в `.env` прода и в клиентах, не в репозитории сервера.
- Клиент Android: `SectionSealKeys` (три доли XOR). Ротация: добавить kid 2 на сервер и в клиент, выпустить клиент, затем переключить `ACTIVE_KID`; kid 1 убрать, когда не останется старых клиентов. Утёкший ключ = ротация.
- iOS: тот же ключ хранить долями в коде/`Info`-ресурсе, читать общим Kotlin-кодом (`SectionSeal` лежит в `commonMain`, `data_remote` собирается и под iOS). PWA: реализовать ChaCha20-Poly1305/HKDF (WebCrypto не умеет ChaCha — нужна библиотека, согласовать), ключ вшивать при сборке и обфусцировать.

## Водяной знак
Сервер сдвигает каждую точку `geometry` на `{-1,0,+1}·1e-6°` по широте и долготе (≈0,11 м), последовательность — HMAC-SHA256 от `(userId, sectionId)` с ключом `ADVISOR_WATERMARK_KEY`. `advisor_seal.watermark_score(original, leaked, key, userId, sectionId)` даёт ≈1,0 для виновного аккаунта и ≈0,33 для остальных. Клиент знак не снимает и не использует: на расчёты (≤0,11 м) он не влияет.

## Честные пределы
Ключ приложения извлекаем реверсом (особенно в PWA). Защита рассчитана на то, чтобы (1) посторонний без аккаунта и приложения не получил ничего читаемого, (2) массовое выкачивание упиралось в квоты, (3) утёкшая копия указывала на аккаунт. Дальше — ротация ключей и бан аккаунта.


## LD3 и аттестация устройства (основной формат, 25.10.2026)
Статический ключ приложения (LD2) извлекается из APK, поэтому основной формат другой: ключ шифрования участка заворачивается открытым ключом **аттестованного устройства**. Закрытая часть лежит в железе (Android Keystore, TEE/StrongBox) и не извлекается; расшифровать конверт может только телефон, на котором ключ создан нашим приложением.

### Регистрация устройства (один раз на аккаунт и ключ)
1. `GET /v1/advisor/device/challenge` (JWT) → `{"challenge": base64}` (16 байт, `HMAC(ключ сервера, user_id)`: цепочка привязана к аккаунту).
2. Клиент создаёт RSA-2048 в Keystore (`PURPOSE_DECRYPT`, OAEP, digests SHA-256+SHA-1) с `setAttestationChallenge(challenge)`, берёт `getCertificateChain` (лист первым).
3. `POST /v1/advisor/device` (JWT) `{"chain": [base64 DER, ...]}` → `{"deviceToken", "expiresAt"}` либо `403` с кодом причины: `challenge_mismatch` (перевыпустить ключ), `software_only`, `root_untrusted`, `package`, `signer`, `boot_state`, `revoked`, `chain_signature`, `no_attestation`, …
4. Сервер проверяет: цепочка подписана до корня Google (закреплены SHA-256 открытых ключей корней: RSA `feb2ea75…fbae`, ECDSA `3ee44512…07ec`); в листе расширение `1.3.6.1.4.1.11129.2.1.17`: наш challenge, уровень не ниже TEE, origin=GENERATED, назначение DECRYPT, RSA; пакет и SHA-256 подписи приложения из белого списка (`ADVISOR_ATTEST_PACKAGES`, `ADVISOR_ATTEST_CERT_DIGESTS`); загрузчик Verified (`ADVISOR_ATTEST_BOOT_STATES`); сертификаты не в списке отзыва Google (`/attestation/status`, суточный кэш). Токен без БД: `LDT1.` + base64url{u: аккаунт, k: SPKI устройства, e: срок} + HMAC.
5. Токен не принят/просрочен → `403 device_required` на запросе участка → клиент проходит регистрацию заново (один раз).

### Запрос и конверт
`GET /v1/advisor/sections/{id}` с `Authorization` и `X-Device-Token`. Без токена `403 device_required` (LD2 отдаётся только при `ADVISOR_ALLOW_LD2=true`).
```
"LD3" | version (4, BE) | wrappedLen (2, BE) | wrapped | nonce (12) | ChaCha20-Poly1305(JSON) + tag (16)
```
- `wrapped` = RSA-OAEP(SHA-256, MGF1-**SHA-1**, пустая метка) от случайного 32-байтового ключа участка (так умеет Keystore);
- AAD = `"LD3" | version (4) | "<sectionId>"`.
iOS: тот же протокол на App Attest/Secure Enclave (ключ создаётся в Secure Enclave, проверка на сервере другая, отдельная доработка); PWA аппаратного ключа не имеет — для неё остаётся только LD2/квоты (или отдельное решение).

### Конфигурация прода
`ADVISOR_ATTEST_CERT_DIGESTS` = SHA-256 сертификата подписи релиза (через запятую, если несколько каналов дистрибуции: свой ключ, Google Play App Signing, RuStore); `ADVISOR_ATTEST_PACKAGES=com.z_company.loco_driver`; `ADVISOR_DEVICE_TOKEN_KEY` — отдельный секрет. Для локальной отладки (эмулятор, debug-подпись): `ADVISOR_ATTEST_DEV_MODE=true` и SHA-256 debug-сертификата в `ADVISOR_ATTEST_CERT_DIGESTS` — только не на проде.
