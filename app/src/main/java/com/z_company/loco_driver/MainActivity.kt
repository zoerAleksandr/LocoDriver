package com.z_company.loco_driver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.robokassa.library.helper.toParams
import com.robokassa.library.pay.RobokassaPayLauncher
import com.z_company.loco_driver.ui.LocoDriverApp
import com.z_company.loco_driver.ui.rememberLocoDriverAppState
import com.z_company.loco_driver.viewmodel.MainViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.pay.RuStorePayClient
import com.vk.id.VKID
import com.z_company.RouteSerializer
import com.z_company.domain.entities.route.Route
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity(), KoinComponent {

    private val mainViewModel: MainViewModel by viewModels()
//    private val payClient: RuStorePayClient by inject()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition { !mainViewModel.appInitialized.value }
        }
        checkIntent(intent)
//        if (savedInstanceState == null) {
//            payClient.getIntentInteractor().proceedIntent(intent)
//        }

        lifecycle.addObserver(mainViewModel)

        enableEdgeToEdge()

        setContent {
            val appState = rememberLocoDriverAppState()
            LocoDriverApp(
                appState = appState,
                isShowUpdatePresentation = mainViewModel.showUpdatePresentation
            )
        }
        VKID.logsEnabled = true
    }

    private fun checkIntent(i: Intent?) {
        val data = i?.data
        if (data?.scheme == "robokassa") {
            val prefs = getSharedPreferences("robokassa.pay.prefs", Context.MODE_PRIVATE)
            val paramStr = prefs.getString("pay", "")
            try {
                val params = paramStr?.toParams()
                mainViewModel.handlePaymentReturn(params)
            } catch (e: Exception) {
                // Показать ошибку
            }
        }

        // Изменено: Добавлена обработка Intent.ACTION_VIEW для импорта файла .routejson.
        // Для чего: Если Intent — просмотр файла, читаем содержимое, десериализуем JSON в Route и импортируем в базу.
        // Это интегрируется с существующими платежами, не нарушая их.
        if (i?.action == Intent.ACTION_VIEW && i.data != null) {
            val uri: Uri? = i.data
            uri?.let {
                try {
                    contentResolver.openInputStream(it)?.use { inputStream ->
                        val jsonString = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                        val route: Route = RouteSerializer.deserialize(jsonString)
                        // Импорт в ViewModel (добавьте метод importRoute в ваш VM, если нет)
//                        routeViewModel.importRoute(route)
                        // Показать уведомление (используйте Snackbar или Toast)
                        Log.d("ImportRoute", "Маршрут импортирован: ${route.basicData.timeStartWork}")  // Замените на реальное уведомление
                        // Например, в Compose: scope.launch { snackbarHostState.showSnackbar("Маршрут импортирован") }
                    }
                } catch (e: Exception) {
                    Log.e("ImportRoute", "Ошибка импорта: ${e.message}")
                    // Показать ошибку пользователю
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntent(intent)
//        payClient.getIntentInteractor().proceedIntent(intent)
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