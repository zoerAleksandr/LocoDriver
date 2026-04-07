@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.z_company.repository

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.CoreFoundation.*
import platform.Security.*

/**
 * iOS production Keychain implementation using CoreFoundation APIs only.
 * No ObjC↔CF bridging — stays in CF domain throughout.
 */
actual class SecureTokenStorage {

    actual suspend fun saveAuthToken(token: String) = saveSecure(AUTH_TOKEN_KEY, token)
    actual fun getAuthBearerTokenFlow(): Flow<String?> = flow { emit(loadSecure(AUTH_TOKEN_KEY)) }

    actual suspend fun saveVkId(vkId: String) = saveSecure(VK_ID_KEY, vkId)
    actual fun getVkIdFlow(): Flow<String?> = flow { emit(loadSecure(VK_ID_KEY)) }

    actual suspend fun saveUserId(userId: String) = saveSecure(USER_ID_KEY, userId)
    actual fun getUserIdFlow(): Flow<String?> = flow { emit(loadSecure(USER_ID_KEY)) }

    // -------------------------------------------------------------------------

    private fun saveSecure(key: String, value: String) {
        val cfKey     = key.toCFString()     ?: return
        val cfService = SERVICE.toCFString() ?: return
        val cfData    = value.toCFData()     ?: run { CFRelease(cfKey); CFRelease(cfService); return }

        try {
            // Delete existing item
            val delDict = newCFDict(3) ?: return
            CFDictionaryAddValue(delDict, kSecClass,       kSecClassGenericPassword)
            CFDictionaryAddValue(delDict, kSecAttrService, cfService)
            CFDictionaryAddValue(delDict, kSecAttrAccount, cfKey)
            SecItemDelete(delDict.reinterpret())
            CFRelease(delDict)

            // Add new item
            val addDict = newCFDict(5) ?: return
            CFDictionaryAddValue(addDict, kSecClass,       kSecClassGenericPassword)
            CFDictionaryAddValue(addDict, kSecAttrService, cfService)
            CFDictionaryAddValue(addDict, kSecAttrAccount, cfKey)
            CFDictionaryAddValue(addDict, kSecValueData,   cfData)
            SecItemAdd(addDict.reinterpret(), null)
            CFRelease(addDict)
        } finally {
            CFRelease(cfKey)
            CFRelease(cfService)
            CFRelease(cfData)
        }
    }

    private fun loadSecure(key: String): String? = memScoped {
        val cfKey     = key.toCFString()     ?: return null
        val cfService = SERVICE.toCFString() ?: return null

        val queryDict = newCFDict(5) ?: run { CFRelease(cfKey); CFRelease(cfService); return null }
        CFDictionaryAddValue(queryDict, kSecClass,       kSecClassGenericPassword)
        CFDictionaryAddValue(queryDict, kSecAttrService, cfService)
        CFDictionaryAddValue(queryDict, kSecAttrAccount, cfKey)
        CFDictionaryAddValue(queryDict, kSecReturnData,  kCFBooleanTrue)
        CFDictionaryAddValue(queryDict, kSecMatchLimit,  kSecMatchLimitOne)

        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(queryDict.reinterpret(), resultRef.ptr)

        CFRelease(queryDict)
        CFRelease(cfKey)
        CFRelease(cfService)

        if (status != errSecSuccess) return null

        // resultRef.value is CFTypeRef (COpaquePointer); reinterpret via type inference
        val cfData: CFDataRef? = resultRef.value?.reinterpret()
        if (cfData == null) return null
        val result = cfDataToString(cfData)
        CFRelease(cfData)
        result
    }

    // ---- helpers ------------------------------------------------------------

    private fun newCFDict(capacity: Int): CFMutableDictionaryRef? =
        CFDictionaryCreateMutable(null, capacity.convert(), null, null)

    private fun String.toCFString(): CFStringRef? =
        CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)

    /** Encodes a Kotlin String as UTF-8 and wraps it in a CFDataRef. */
    private fun String.toCFData(): CFDataRef? {
        val bytes = encodeToByteArray()
        if (bytes.isEmpty()) return CFDataCreate(null, null, 0)
        return bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.convert())
        }
    }

    /** Decodes a CFDataRef as a UTF-8 Kotlin String. */
    private fun cfDataToString(data: CFDataRef): String? {
        val length = CFDataGetLength(data)
        if (length <= 0L) return null
        val ptr = CFDataGetBytePtr(data) ?: return null
        return ptr.reinterpret<ByteVar>().toKString()
    }

    companion object {
        private const val SERVICE        = "com.z_company.locodriver"
        private const val AUTH_TOKEN_KEY = "auth_token"
        private const val VK_ID_KEY      = "vk_id"
        private const val USER_ID_KEY    = "user_id"
    }
}
