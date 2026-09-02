package com.z_company.repository.remote_rest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Разбор ответов VK-входа (3.0.4): именно здесь ломается сценарий
 * «аккаунта с этим VK нет» — раньше он показывался как «Неверная почта или
 * пароль» и заводил пользователя в тупик.
 */
class VkAuthErrorMappingTest {

    @Test
    fun `401 vk_token_invalid просит повторить вход`() {
        val error = vkAuthError(401, VK_DETAIL_TOKEN_INVALID)
        assertEquals(VkAuthError.TokenInvalid, error.vkError)
    }

    @Test
    fun `401 vk_access_token_required просит обновить приложение`() {
        val error = vkAuthError(401, VK_DETAIL_TOKEN_REQUIRED)
        assertEquals(VkAuthError.ClientOutdated, error.vkError)
        assertTrue(error.errorMessage.contains("Обновите приложение"))
    }

    @Test
    fun `404 vk_user_not_found ведёт в регистрацию`() {
        val error = vkAuthError(404, VK_DETAIL_USER_NOT_FOUND)
        assertEquals(VkAuthError.UserNotFound, error.vkError)
    }

    @Test
    fun `404 без detail тоже считается отсутствующим аккаунтом`() {
        assertEquals(VkAuthError.UserNotFound, vkAuthError(404, null).vkError)
    }

    /**
     * Бэкенд с проверкой токена деплоится уже после публикации 3.0.4, поэтому
     * какое-то время 3.0.4 работает со старым продом: там «нет аккаунта» — это
     * 401 без detail. Такой ответ обязан вести в регистрацию, а не в «неверный
     * пароль».
     */
    @Test
    fun `401 без detail на старом сервере ведёт в регистрацию`() {
        val error = vkAuthError(401, null)
        assertEquals(VkAuthError.UserNotFound, error.vkError)
        assertTrue(error.errorMessage.contains("Зарегистрируйтесь"))
    }

    @Test
    fun `503 отдаётся как временная недоступность VK`() {
        assertEquals(VkAuthError.VkUnavailable, vkAuthError(503, VK_DETAIL_UNAVAILABLE).vkError)
        assertEquals(VkAuthError.VkUnavailable, vkAuthError(503, null).vkError)
    }

    @Test
    fun `неизвестный код не притворяется ошибкой VK`() {
        val error = vkAuthError(500, null)
        assertNull(error.vkError)
    }

    @Test
    fun `409 при привязке объясняет что VK занят другим аккаунтом`() {
        val byDetail = attachErrorMessage(409, VK_DETAIL_ALREADY_LINKED)
        val byCode = attachErrorMessage(409, null)
        assertTrue(byDetail.contains("уже привязан"))
        assertEquals(byDetail, byCode)
    }

    @Test
    fun `прочие ошибки привязки содержат код ответа`() {
        assertTrue(attachErrorMessage(500, null).contains("500"))
    }
}
