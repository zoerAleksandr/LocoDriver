package com.example.core.auth

interface AuthStateListener {
    fun onAuthChanged(isLoggedIn: Boolean)
}
