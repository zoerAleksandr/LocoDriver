package com.z_company.core

import io.sentry.kotlin.multiplatform.Sentry

fun initSentry(dsn: String) {
    Sentry.init { options ->
        options.dsn = dsn

        // Фильтруем системные ошибки Android 12+, которые не являются нашими багами:
        // SecurityException: CLOSE_SYSTEM_DIALOGS — бросается платформой при попытке
        // закрыть системные диалоги (например, при нажатии кнопки звонка на экране).
        // Нет смысла логировать эту ошибку — она вне нашего контроля.
        options.beforeSend = { event ->
            val isCloseSystemDialogs = event.exceptions?.any { exc ->
                exc.type?.contains("SecurityException") == true &&
                    exc.value?.contains("CLOSE_SYSTEM_DIALOGS") == true
            } == true

            // CancellationException / JobCancellationException — нормальное поведение
            // при закрытии экрана или отмене корутины. Не является ошибкой.
            val isCancellation = event.exceptions?.any { exc ->
                exc.type?.contains("CancellationException") == true
            } == true

            when {
                isCloseSystemDialogs -> null
                isCancellation -> null
                else -> event
            }
        }
    }
}
