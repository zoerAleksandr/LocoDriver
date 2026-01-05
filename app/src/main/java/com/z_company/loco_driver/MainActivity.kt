package com.z_company.loco_driver

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.z_company.SessionManager
import com.z_company.loco_driver.ui.LocoDriverApp
import com.z_company.loco_driver.ui.rememberLocoDriverAppState
import com.z_company.loco_driver.viewmodel.MainViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.pay.RuStorePayClient
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity(), KoinComponent {

    private val mainViewModel: MainViewModel by viewModels()

    private val payClient: RuStorePayClient by inject()

    private val sessionManager: SessionManager by inject()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition { !mainViewModel.appInitialized.value }
        }
        if (savedInstanceState == null) {
            payClient.getIntentInteractor().proceedIntent(intent)
        }

        lifecycle.addObserver(mainViewModel)

        enableEdgeToEdge()

        setContent {
            val isLoggedIn by sessionManager.isLoggedInFlow.collectAsState(initial = false)
            val appState = rememberLocoDriverAppState()
            LocoDriverApp(
                appState = appState,
                isLoggedIn = isLoggedIn,
                isShowFirstPresentation = mainViewModel.showFirstPresentation,
                isShowUpdatePresentation = mainViewModel.showUpdatePresentation
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        payClient.getIntentInteractor().proceedIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(mainViewModel)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus?.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }
}