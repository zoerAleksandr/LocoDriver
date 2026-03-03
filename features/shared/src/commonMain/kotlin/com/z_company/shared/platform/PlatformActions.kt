package com.z_company.shared.platform

/**
 * Platform-specific actions abstraction.
 * Android: wraps Intents. iOS: wraps UIKit/SwiftUI APIs.
 */
expect class PlatformActions {
    fun shareText(text: String)
    fun openUrl(url: String)
    fun sendEmail(to: String, subject: String, body: String)
    fun getAppVersion(): String
}

/**
 * Factory function to create a PlatformActions instance.
 * Each platform provides an actual implementation.
 */
expect fun createPlatformActions(): PlatformActions
