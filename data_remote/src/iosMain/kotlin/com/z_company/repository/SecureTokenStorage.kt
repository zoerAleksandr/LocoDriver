@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.z_company.repository

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS-реализация SecureTokenStorage через Keychain Services.
 *
 * Использует kSecClassGenericPassword с уникальным kSecAttrService
 * для хранения auth token, vkId, userId в системном Keychain iOS.
 */
actual class SecureTokenStorage {

    actual suspend fun saveAuthToken(token: String) = saveSecure(AUTH_TOKEN_KEY, token)
    actual fun getAuthBearerTokenFlow(): Flow<String?> = flow { emit(loadSecure(AUTH_TOKEN_KEY)) }

    actual suspend fun saveVkId(vkId: String) = saveSecure(VK_ID_KEY, vkId)
    actual fun getVkIdFlow(): Flow<String?> = flow { emit(loadSecure(VK_ID_KEY)) }

    actual suspend fun saveUserId(userId: String) = saveSecure(USER_ID_KEY, userId)
    actual fun getUserIdFlow(): Flow<String?> = flow { emit(loadSecure(USER_ID_KEY)) }

    // ----------------------------------------------------------------- helpers

    /** Сохраняет строку в Keychain. Удаляет старую запись перед добавлением. */
    private fun saveSecure(key: String, value: String) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        // Удаляем существующую запись
        NSMutableDictionary().apply {
            setKeychainClass()
            setService()
            setAccountKey(key)
            SecItemDelete(this as platform.CoreFoundation.CFDictionaryRef)
        }

        // Добавляем новую
        NSMutableDictionary().apply {
            setKeychainClass()
            setService()
            setAccountKey(key)
            setObject(data, forKey = kSecValueData as NSString)
            SecItemAdd(this as platform.CoreFoundation.CFDictionaryRef, null)
        }
    }

    /** Читает строку из Keychain. Возвращает null, если записи нет. */
    private fun loadSecure(key: String): String? = memScoped {
        val query = NSMutableDictionary().apply {
            setKeychainClass()
            setService()
            setAccountKey(key)
            setObject(NSNumber(bool = true), forKey = kSecReturnData as NSString)
            setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as NSString)
        }
        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(
            query as platform.CoreFoundation.CFDictionaryRef,
            resultRef.ptr
        )
        if (status == errSecSuccess) {
            (resultRef.value as? platform.Foundation.NSData)?.let { data ->
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            }
        } else null
    }

    // ----------------------------------------------------------------- dict helpers

    private fun NSMutableDictionary.setKeychainClass() =
        setObject(kSecClassGenericPassword, forKey = kSecClass as NSString)

    private fun NSMutableDictionary.setService() =
        setObject(SERVICE as NSString, forKey = kSecAttrService as NSString)

    private fun NSMutableDictionary.setAccountKey(key: String) =
        setObject(key as NSString, forKey = kSecAttrAccount as NSString)

    // ----------------------------------------------------------------- constants

    companion object {
        private const val SERVICE = "com.z_company.locodriver"
        private const val AUTH_TOKEN_KEY = "auth_token"
        private const val VK_ID_KEY = "vk_id"
        private const val USER_ID_KEY = "user_id"
    }
}
