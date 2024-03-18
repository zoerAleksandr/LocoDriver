package com.z_company.loco_driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.z_company.data_remote.Appwrite
import com.z_company.loco_driver.ui.LocoDriverApp
import com.z_company.loco_driver.ui.rememberLocoDriverAppState
import com.z_company.loco_driver.viewmodel.MainViewModel
import kotlin.properties.Delegates

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var isLogin by Delegates.notNull<Boolean>()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition { mainViewModel.inProgress.value ?: false }

        Appwrite.init(applicationContext)
        lifecycle.addObserver(mainViewModel)

        mainViewModel.session.observe(this) {
            isLogin = it != null
            setContent {
                val appState = rememberLocoDriverAppState()
                LocoDriverApp(appState = appState, isLoggedIn = isLogin)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(mainViewModel)
    }
}