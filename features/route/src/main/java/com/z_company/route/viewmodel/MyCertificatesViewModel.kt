package com.z_company.route.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.brake.BrakeCertificateCalculator
import com.z_company.domain.brake.BrakeStatusResolver
import com.z_company.domain.brake.SpeedLimit
import com.z_company.domain.brake.StatusLevel
import com.z_company.domain.brake.storage.BrakeCertificateUseCase
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Карточка справки в списке «Мои справки ВУ-45». */
data class CertificateItem(
    val id: String,
    val routeId: String,
    val trainId: String?,
    val updatedAt: Long,
    val savedAt: String,
    /** Рейс: «дата начала работы» или «рейс не найден». */
    val routeLabel: String,
    val trainNumber: String,
    val loco: String,
    /** Скорость и вердикт («90 км/ч · нажатие достаточно»). */
    val verdict: String,
    val level: StatusLevel,
)

data class MyCertificatesUiState(
    val loading: Boolean = true,
    val items: List<CertificateItem> = emptyList(),
    val selectionMode: Boolean = false,
    val selected: Set<String> = emptySet(),
    /** Идёт подтверждение удаления: id справок. */
    val confirmDelete: Set<String>? = null,
)

class MyCertificatesViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val certificates: BrakeCertificateUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()

    private val _state = MutableStateFlow(MyCertificatesUiState())
    val state: StateFlow<MyCertificatesUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) { buildItems() }
            _state.update { it.copy(loading = false, items = items, selected = it.selected.filter { id -> items.any { i -> i.id == id } }.toSet()) }
        }
    }

    private fun buildItems(): List<CertificateItem> {
        val routes = runCatching { routeUseCase.getListRoutes() }.getOrDefault(emptyList()).associateBy { it.basicData.id }
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val fmtDay = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return certificates.listAll().mapNotNull { stored ->
            val data = certificates.decode(stored) ?: return@mapNotNull null
            val st = data.toUiState(BrakeCertificateUiState())
            val result = st.toInput()?.let(BrakeCertificateCalculator::calculate)
            val has = result != null && result.actualPressure > 0
            val status = BrakeStatusResolver.resolve(result, has)
            val speed = BrakeStatusResolver.speedKmh(result)
            val verdict = if (!has) "расчёт не выполнен" else (speed?.let { "$it км/ч · " } ?: "") + status.headline.replaceFirstChar { c -> c.lowercase() }
            val route = routes[stored.routeId]
            CertificateItem(
                id = stored.certificateId, routeId = stored.routeId, trainId = stored.trainId, updatedAt = stored.updatedAt,
                savedAt = fmt.format(Date(stored.updatedAt)),
                routeLabel = route?.basicData?.timeStartWork?.let { "рейс от " + fmtDay.format(Date(it)) } ?: "рейс не найден",
                trainNumber = data.trainNumber.trim().ifBlank { "—" },
                loco = (data.locoSeries.trim() + (if (data.locoNumber.isNotBlank()) "-" + data.locoNumber.trim() else "")).ifBlank { "—" },
                verdict = verdict, level = status.level,
            )
        }
    }

    fun toggleSelectionMode() = _state.update { it.copy(selectionMode = !it.selectionMode, selected = emptySet()) }
    fun toggleSelected(id: String) = _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }
    fun selectAll() = _state.update { it.copy(selected = it.items.map { i -> i.id }.toSet()) }
    fun askDelete(ids: Set<String>) = _state.update { it.copy(confirmDelete = ids) }
    fun dismissDelete() = _state.update { it.copy(confirmDelete = null) }

    /** Удаление пачкой: надгробия + пометка рейсов несинхронизированными (сервер удалит справки при следующей синхронизации). */
    fun confirmDelete(ids: Set<String>) {
        if (ids.isEmpty()) return
        _state.update { it.copy(confirmDelete = null, selectionMode = false, selected = emptySet()) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val routes = certificates.deleteByIds(ids)
                if (com.z_company.repository.remote_rest.RemoteFeatureFlags.remoteCertificatesEnabled) {
                    routes.forEach { runCatching { routeUseCase.markUnsynchronized(it).first { r -> r !is ResultState.Loading } } }
                }
            }
            val n = ids.size
            android.widget.Toast.makeText(getApplication(), if (n == 1) "Справка удалена" else "Справок удалено: $n", android.widget.Toast.LENGTH_SHORT).show()
            reload()
        }
    }
}
