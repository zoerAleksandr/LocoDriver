package com.z_company.repository.remote_rest.tracks

/**
 * Ключ устройства для участков советника: закрытая часть лежит в железе (Android Keystore, TEE/StrongBox) и не покидает его;
 * сервер оборачивает ключ шифрования участка открытой частью (конверт LD3, `docs/ADVISOR_SEAL_PROTOCOL.md`).
 * Google Key Attestation не используется как граница доступа: полезная защита здесь — закрытая часть,
 * которая остаётся в Android Keystore, плюс вход, лимит разных участков и водяной знак.
 * Свой ключ создаётся для каждого аккаунта.
 */
interface DeviceKeys {
    /**
     * Сертификат/цепочка (DER, лист первым) открытого ключа аккаунта [accountId]. Ключ создаётся при первом обращении;
     * [forceNew] - выпустить заново после потери/отказа токена. null - Android Keystore не смог создать ключ.
     */
    suspend fun certificateChain(accountId: String, forceNew: Boolean): List<ByteArray>?

    /** RSA-OAEP (SHA-256, MGF1-SHA-1) расшифровка [wrapped] закрытым ключом аккаунта; null - ключа нет или не открылось. */
    fun unwrap(accountId: String, wrapped: ByteArray): ByteArray?

    /** Токен устройства, выданный сервером после регистрации открытого ключа, и его срок в мс. */
    fun loadToken(accountId: String): Pair<String, Long>?
    fun saveToken(accountId: String, token: String, expiresAtMs: Long)
    fun clearToken(accountId: String)
}
