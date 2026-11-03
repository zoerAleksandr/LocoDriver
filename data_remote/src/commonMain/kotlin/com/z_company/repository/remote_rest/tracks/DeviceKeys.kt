package com.z_company.repository.remote_rest.tracks

/**
 * Ключ устройства для участков советника: закрытая часть лежит в железе (Android Keystore, TEE/StrongBox) и не покидает его;
 * сервер оборачивает ключ шифрования участка открытой частью (конверт LD3, `docs/ADVISOR_SEAL_PROTOCOL.md`), а проверяет, что ключ
 * настоящий и создан нашим приложением, по цепочке аттестации. Свой ключ у каждого аккаунта: challenge сервера привязан к аккаунту.
 * Android - Keystore; iOS потом - App Attest/Secure Enclave по тому же протоколу.
 */
interface DeviceKeys {
    /**
     * Цепочка аттестации (DER, лист первым) ключа аккаунта [accountId] для [challenge]. Ключ создаётся при первом обращении;
     * [forceNew] - выпустить заново (сменился challenge). null - устройство аттестацию не умеет.
     */
    suspend fun attest(accountId: String, challenge: ByteArray, forceNew: Boolean): List<ByteArray>?

    /** RSA-OAEP (SHA-256, MGF1-SHA-1) расшифровка [wrapped] закрытым ключом аккаунта; null - ключа нет или не открылось. */
    fun unwrap(accountId: String, wrapped: ByteArray): ByteArray?

    /** Токен устройства, выданный сервером после аттестации (хранится на устройстве), и его срок в мс. */
    fun loadToken(accountId: String): Pair<String, Long>?
    fun saveToken(accountId: String, token: String, expiresAtMs: Long)
    fun clearToken(accountId: String)
}
