package com.z_company.shared.platform

actual class PlatformActions {
    actual fun shareText(text: String) {
        // TODO: implement with Android Intent
    }
    actual fun openUrl(url: String) {
        // TODO: implement with Android Intent
    }
    actual fun sendEmail(to: String, subject: String, body: String) {
        // TODO: implement with Android Intent
    }
    actual fun getAppVersion(): String {
        return "Android" // TODO: read from PackageManager
    }
}

actual fun createPlatformActions(): PlatformActions = PlatformActions()
