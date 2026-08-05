package com.z_company.loco_driver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.robokassa.library.helper.toParams
import com.robokassa.library.pay.RobokassaPayLauncher
import com.z_company.RouteSerializer
import com.z_company.loco_driver.ui.LocoDriverApp
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.loco_driver.ui.rememberLocoDriverAppState
import com.z_company.loco_driver.viewmodel.MainViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.vk.id.VKID

class MainActivity : ComponentActivity(), KoinComponent {

    private val mainViewModel: MainViewModel by viewModels()
//    private val payClient: RuStorePayClient by inject()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Системный сплэш убираем сразу (первый кадр) — единственный видимый сплэш
        // это брендовый Compose-overlay ниже (M + линия + МАШИНИСТ + слоган).
        installSplashScreen()
        checkIntent(intent)
//        if (savedInstanceState == null) {
//            payClient.getIntentInteractor().proceedIntent(intent)
//        }

        lifecycle.addObserver(mainViewModel)

        enableEdgeToEdge()

        setContent {
            val appState = rememberLocoDriverAppState()
            val pendingImportRoute by mainViewModel.pendingImportRoute.collectAsState()
            val pendingFormOpen by mainViewModel.pendingFormOpen.collectAsState()
            val pendingNavigateHome by mainViewModel.pendingNavigateHome.collectAsState()
            val pendingNavigateProfile by mainViewModel.pendingNavigateProfile.collectAsState()
            val pendingOpenFormWithId by mainViewModel.pendingOpenFormWithId.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true }
            ) {
            LocoDriverApp(
                appState = appState,
                isShowUpdatePresentation = mainViewModel.showUpdatePresentation,
                pendingImportRoute = pendingImportRoute,
                onConfirmImport = mainViewModel::confirmImportRoute,
                onDismissImport = mainViewModel::dismissImportRoute,
                pendingFormOpen = pendingFormOpen,
                onFormOpened = mainViewModel::clearOpenForm,
                pendingNavigateHome = pendingNavigateHome,
                onNavigatedHome = mainViewModel::clearNavigateHome,
                pendingNavigateProfile = pendingNavigateProfile,
                onNavigatedProfile = mainViewModel::clearNavigateProfile,
                pendingOpenFormWithId = pendingOpenFormWithId,
                onFormOpenedWithId = mainViewModel::clearOpenFormWithId
            )

            // Единственный сплэш — брендовый лок-ап поверх приложения с самого старта.
            // Держим пока идёт инициализация И минимальное время (чтобы слоган прочитался).
            val appInitialized by mainViewModel.appInitialized.collectAsState()
            var minTimePassed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(1500); minTimePassed = true }
            androidx.compose.animation.AnimatedVisibility(
                visible = !appInitialized || !minTimePassed,
                enter = androidx.compose.animation.EnterTransition.None,
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400)),
            ) {
                com.z_company.loco_driver.ui.BrandedSplash()
            }

            // Полноэкранное сообщение-«новость при запуске» поверх приложения.
            // announcement != null только после инициализации (см. MainViewModel),
            // поэтому появляется уже после того, как сплэш ушёл.
            val announcement by mainViewModel.announcement.collectAsState()
            announcement?.let { ann ->
                com.z_company.loco_driver.ui.AnnouncementScreen(
                    announcement = ann,
                    onDismiss = mainViewModel::dismissAnnouncement,
                )
            }
            } // Box
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

        // Deep link: переход на экран Профиль с сайта — locodriver://profile
        if (i?.action == Intent.ACTION_VIEW && data != null &&
            data.scheme == ShareRouteManager.SHARE_SCHEME && data.host == "profile"
        ) {
            mainViewModel.requestNavigateProfile()
            return
        }

        // Deep link: публичная ссылка на маршрут.
        //  - https://locodriver.ru/r/{id}      (App Link, кликабелен в Telegram и т.п.)
        //  - locodriver://share/{id}            (fallback / iOS, кастомная схема)
        if (i?.action == Intent.ACTION_VIEW && data != null && isShareDeepLink(data)) {
            val shareId = ShareRouteManager.parseShareId(data.toString())
            if (!shareId.isNullOrBlank()) {
                mainViewModel.handleShareDeepLink(shareId)
                return
            }
        }

        // Импорт .zroute файла
        if (i?.action == Intent.ACTION_VIEW && data != null) {
            val mimeType = i.type ?: contentResolver.getType(data)
            val isCustomMime = mimeType == "application/vnd.com.z_company.loco_driver.route"
            val isOctetStream = mimeType == "application/octet-stream"
            if (isCustomMime || (isOctetStream && isZrouteFile(data))) {
                importRouteFrom(data)
            }
        }

        // Навигация из виджета → добавить маршрут
        if (i?.getBooleanExtra("widget_add_route", false) == true) {
            mainViewModel.requestOpenForm()
        }

        // Навигация из виджета → HomeScreen
        if (i?.getBooleanExtra("widget_open_home", false) == true) {
            mainViewModel.requestNavigateHome()
        }
    }

    private fun isShareDeepLink(uri: Uri): Boolean {
        // Кастомная схема: locodriver://share/...
        if (uri.scheme == ShareRouteManager.SHARE_SCHEME &&
            uri.host == ShareRouteManager.SHARE_HOST
        ) return true
        // App Link: https://locodriver.ru/r/...
        val isHttps = uri.scheme == "https" || uri.scheme == "http"
        val isOurHost = uri.host == ShareRouteManager.SHARE_HTTPS_HOST
        val firstSegment = uri.pathSegments.firstOrNull()
        val isSharePath =
            firstSegment == ShareRouteManager.SHARE_HTTPS_PATH ||
                firstSegment == ShareRouteManager.SHARE_HOST
        return isHttps && isOurHost && isSharePath
    }

    private fun isZrouteFile(uri: Uri): Boolean {
        return try {
            val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) it.getString(0).endsWith(".zroute", ignoreCase = true)
                else false
            } ?: uri.lastPathSegment?.endsWith(".zroute", ignoreCase = true) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun importRouteFrom(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
            val route = RouteSerializer.deserialize(json)
            mainViewModel.setPendingImportRoute(route)
        } catch (e: Exception) {
            Log.e("ImportRoute", "Ошибка чтения .zroute файла", e)
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

    // Скрытие клавиатуры реализовано через Compose (clearFocusOnTap)
    // а не через dispatchTouchEvent, чтобы не скрывать клавиатуру
    // при нажатии на само поле ввода
}