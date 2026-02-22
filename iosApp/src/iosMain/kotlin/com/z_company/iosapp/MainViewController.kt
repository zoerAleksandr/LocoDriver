package com.z_company.iosapp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Точка входа Compose → UIKit.
 *
 * Swift вызывает MainViewController_iosKt.MainViewController()
 * из ContentView.swift и встраивает его в иерархию SwiftUI через
 * UIViewControllerRepresentable.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        App()
    }
