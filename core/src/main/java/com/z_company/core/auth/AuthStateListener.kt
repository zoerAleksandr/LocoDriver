package com.z_company.core.auth

interface AuthStateListener {
    fun onAuthChanged(isLoggedIn: Boolean)
}
