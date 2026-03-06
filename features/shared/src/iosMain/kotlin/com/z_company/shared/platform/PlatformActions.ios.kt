package com.z_company.shared.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class PlatformActions {

    actual fun shareText(text: String) {
        val activityController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(
            activityController,
            animated = true,
            completion = null,
        )
    }

    actual fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    actual fun sendEmail(to: String, subject: String, body: String) {
        val encoded = "mailto:$to?subject=${urlEncode(subject)}&body=${urlEncode(body)}"
        val nsUrl = NSURL.URLWithString(encoded) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    actual fun getAppVersion(): String {
        val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val build = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
        return if (version != null && build != null) "$version ($build)"
        else version ?: build ?: "unknown"
    }

    private fun urlEncode(value: String): String {
        return value
            .replace(" ", "%20")
            .replace("&", "%26")
            .replace("=", "%3D")
            .replace("+", "%2B")
            .replace("\n", "%0A")
    }
}

actual fun createPlatformActions(): PlatformActions = PlatformActions()
