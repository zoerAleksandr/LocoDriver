package com.z_company.login.ui

fun getMessageThrowable(throwable: Throwable?): String {
    return if (throwable == null) {
        return ""

    } else {
        when (throwable.message) {
            "Invalid username/password." -> "Неверный логин или пароль"
            "i/o failure" -> "Сбой связи. Проверьте интернет соединение"
            "Account already exists for this username." -> "Email зарегистрирован ранее"
            else -> throwable.message ?: "Ошибка"
        }
    }
}