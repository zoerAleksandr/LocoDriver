package com.z_company.loco_driver

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.parse.ParseObject
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

        lifecycle.addObserver(mainViewModel)

        val firstObject = ParseObject("FirstClass")
        firstObject.put("message","Hey ! First message from android. Parse is now connected")
        firstObject.saveInBackground {
            if (it != null){
                it.localizedMessage?.let { message -> Log.d("ZZZ", message) }
            }else{
                Log.d("ZZZ","Object saved.")
            }
        }

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