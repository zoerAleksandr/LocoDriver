package com.z_company.route.component.servicephase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.z_company.domain.util.RailLookupResult
import com.z_company.domain.util.RailStationLookup
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import com.z_company.route.util.DeviceLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject

/**
 * Разовое определение километра и пикета по GPS для сообщений при вынужденной остановке («Закрепление поезда»).
 * Берётся тот же механизм, что на профиле пути: положение проецируется на ось ближайшего участка с километровыми отметками ([ProfileModel]).
 * Координата нигде не сохраняется и не логируется. Результат: [onResult] (километр, пикет, сообщение об ошибке).
 */
@Composable
internal fun HoldingPositionResolver(onResult: (km: String?, picket: String?, stretch: String?, message: String?) -> Unit) {
    val repo: SectionProfileRepository = koinInject()
    val railLookup: RailStationLookup = koinInject()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember { ProfileModel(repo, ProfileCalibrationStore(context), scope, railLookup) }
    LaunchedEffect(Unit) {
        // загрузка списка участков может зависнуть без сети - не ждём дольше 6 с (участки рядом подгрузятся ниже или из кэша)
        kotlinx.coroutines.withTimeoutOrNull(6_000) { model.load() }
        val fix = DeviceLocation.currentFix(context, timeoutMs = 8_000)
        if (fix == null) {
            onResult(null, null, null, "Положение по GPS не определено. Назовите километр и пикет по столбикам.")
            return@LaunchedEffect
        }
        repeat(24) {
            model.onFix(fix, null, System.currentTimeMillis())
            val p = model.position
            val m = p?.kmText?.let { Regex("""(\d+)\s*км\s*(\d+)\s*пк""").find(it) }
            if (m != null && !p.offRoute) {
                val stretch = withTimeoutOrNull(5_000) {
                    try {
                        (railLookup.nearestAlongRail(fix.latitude, fix.longitude, 5_000.0, emptyList()) as? RailLookupResult.Found)
                            ?.station?.name?.takeIf { it.isNotBlank() }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
                onResult(m.groupValues[1], m.groupValues[2], stretch, null)
                return@LaunchedEffect
            }
            delay(500)
        }
        onResult(
            null, null, null,
            if (model.needLogin) "Чтобы определить километр, войдите в аккаунт (участки пути отдаются только после входа)."
            else "Участок пути с километровыми отметками рядом не найден. Назовите километр и пикет по столбикам.",
        )
    }
}
