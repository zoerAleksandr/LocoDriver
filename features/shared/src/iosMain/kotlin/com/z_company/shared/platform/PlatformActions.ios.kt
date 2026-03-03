package com.z_company.shared.platform

actual class PlatformActions {
    actual fun shareText(text: String) {
        // TODO: implement with UIActivityViewController
    }
    actual fun openUrl(url: String) {
        // TODO: implement with UIApplication.openURL
    }
    actual fun sendEmail(to: String, subject: String, body: String) {
        // TODO: implement
    }
    actual fun getAppVersion(): String {
        return "iOS" // TODO: read from NSBundle
    }
}

actual fun createPlatformActions(): PlatformActions = PlatformActions()
