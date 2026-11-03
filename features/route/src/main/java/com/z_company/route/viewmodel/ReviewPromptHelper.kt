package com.z_company.route.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.review.RuStoreReviewManagerFactory
import ru.rustore.sdk.review.errors.RuStoreReviewExists

/**
 * Предложение оценить приложение в RuStore.
 *
 * Показываем только платным пользователям, начиная с [REVIEW_FIRST_ROUTES] маршрутов.
 * SDK не сообщает, поставил ли пользователь оценку, поэтому «отказ» = любой показ без оценки:
 * повторяем через каждые [REVIEW_REPEAT_ROUTES] новых маршрутов. Когда оценка уже есть,
 * requestReviewFlow() падает с RuStoreReviewExists — после этого не спрашиваем никогда.
 *
 * Вызывается после любого сохранения маршрута пользователем (форма, календарь, мастер графика).
 */
class ReviewPromptHelper(private val application: Application) : KoinComponent {

    private val routeUseCase: RouteUseCase by inject()
    private val routeHelper: RouteActionsHelper by inject()

    private val reviewManager by lazy { RuStoreReviewManagerFactory.create(application.applicationContext) }
    private val prefs by lazy { application.getSharedPreferences("review_prompt", Context.MODE_PRIVATE) }

    // Собственный scope: вызов не должен прерываться уходом с экрана после сохранения.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onRouteSaved() {
        scope.launch {
            try {
                if (prefs.getBoolean(KEY_FINISHED, false)) return@launch
                if (!routeHelper.hasActiveSubscription()) return@launch
                delay(300L)
                val count = routeUseCase.listRouteWithDeleting().count { !it.basicData.isDeleted }
                val lastPromptCount = prefs.getInt(KEY_LAST_COUNT, 0)
                if (count < REVIEW_FIRST_ROUTES || count - lastPromptCount < REVIEW_REPEAT_ROUTES) return@launch
                requestAndShow(count)
            } catch (e: Exception) {
                Log.w("ZZZ", "ReviewPromptHelper error = ${e.message}")
            }
        }
    }

    private fun requestAndShow(count: Int) {
        reviewManager.requestReviewFlow()
            .addOnSuccessListener { info ->
                reviewManager.launchReviewFlow(info)
                    .addOnSuccessListener {
                        prefs.edit().putInt(KEY_LAST_COUNT, count).apply()
                    }
                    .addOnFailureListener { throwable ->
                        Log.w("ZZZ", "showReviewDialog Failure = $throwable")
                    }
            }
            .addOnFailureListener { throwable ->
                if (throwable is RuStoreReviewExists) {
                    prefs.edit().putBoolean(KEY_FINISHED, true).apply()
                }
                // RuStoreRequestLimitReached и прочее — попробуем при следующем сохранении
                Log.w("ZZZ", "prepareReviewDialog throwable = ${throwable.message}")
            }
    }

    private companion object {
        const val KEY_FINISHED = "review_finished"
        const val KEY_LAST_COUNT = "review_last_prompt_count"
        const val REVIEW_FIRST_ROUTES = 10
        const val REVIEW_REPEAT_ROUTES = 10
    }
}
