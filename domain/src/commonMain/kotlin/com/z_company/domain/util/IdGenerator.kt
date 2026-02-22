package com.z_company.domain.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** KMP-совместимая генерация UUID v4. Заменяет java.util.UUID.randomUUID(). */
@OptIn(ExperimentalUuidApi::class)
fun generateId(): String = Uuid.random().toString()
