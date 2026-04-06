package com.z_company.route.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.route.Route
import com.z_company.route.util.PdfGenerator
import com.z_company.route.util.PdfSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PdfViewModel(application: Application) : AndroidViewModel(application), KoinComponent {

    private val salaryViewModel: SalaryCalculationViewModel by inject()

    private val _pdfReady = MutableSharedFlow<Uri>()
    val pdfReady = _pdfReady.asSharedFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // State set externally by screens
    private var latestSalaryState: SalaryCalculationUIState? = null
    private var latestRoutes: List<Route> = emptyList()
    private var latestMonthLabel: String = ""
    private var latestCalendarDays: List<Day> = emptyList()

    fun updateSalaryState(state: SalaryCalculationUIState) {
        latestSalaryState = state
    }

    fun updateRoutes(routes: List<Route>, monthLabel: String) {
        latestRoutes = routes
        latestMonthLabel = monthLabel
    }

    fun updateCalendarDays(days: List<Day>) {
        latestCalendarDays = days
    }

    fun generateAndShare(
        sections: PdfSections,
        routes: List<Route>,
        monthLabel: String,
        calendarDays: List<Day> = emptyList()
    ) {
        if (_isGenerating.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            _errorMessage.value = null
            try {
                // Используем latestSalaryState (если задан), иначе ждём завершения расчёта
                // в singleton SalaryCalculationViewModel (до 8 секунд)
                val salaryState = if (sections.includeSalary) {
                    latestSalaryState ?: withTimeoutOrNull(8_000L) {
                        salaryViewModel.uiState.first { it.screenState is ResultState.Success }
                    }
                } else null
                val effectiveRoutes = routes.ifEmpty { latestRoutes }
                val effectiveMonthLabel = monthLabel.ifEmpty { latestMonthLabel }
                val file = PdfGenerator(getApplication()).generatePdf(
                    routes = effectiveRoutes,
                    salaryState = salaryState,
                    monthLabel = effectiveMonthLabel,
                    sections = sections,
                    calendarDays = calendarDays.ifEmpty { latestCalendarDays }
                )
                // Authority must match the one declared in AndroidManifest.xml
                val authority = "${getApplication<Application>().packageName}.fileprovider"
                Log.d("PdfViewModel", "FileProvider authority: $authority, file: ${file.absolutePath}")
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    authority,
                    file
                )
                _pdfReady.emit(uri)
            } catch (e: Exception) {
                Log.e("PdfViewModel", "PDF generation failed", e)
                e.sendToSentry("PdfViewModel", "generateAndShare")
                _errorMessage.value = "Ошибка формирования PDF: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
