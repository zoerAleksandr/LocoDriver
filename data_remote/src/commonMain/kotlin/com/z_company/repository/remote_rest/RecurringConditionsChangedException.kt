package com.z_company.repository.remote_rest

/**
 * `POST /v1/payment/checkout` ответил 409: автопродление сейчас недоступно или
 * текст согласия на сервере обновился (версия не совпала). Клиент перечитывает
 * условия и просит пользователя подтвердить их заново.
 */
class RecurringConditionsChangedException(message: String? = null) : Exception(message)
