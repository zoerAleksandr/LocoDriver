package com.z_company.route.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Route
import com.z_company.route.util.PdfGenerator
import com.z_company.route.util.PdfSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _shareEvent = MutableSharedFlow<Intent>()
    val shareEvent = _shareEvent.asSharedFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Salary state is set externally by SalaryCalculationScreen
    private var latestSalaryState: SalaryCalculationUIState? = null

    fun updateSalaryState(state: SalaryCalculationUIState) {
        latestSalaryState = state
    }

    fun generateAndShare(
        sections: PdfSections,
        routes: List<Route>,
        monthLabel: String
    ) {
        if (_isGenerating.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            _errorMessage.value = null
            try {
                val salaryState = if (sections.includeSalary) latestSalaryState else null
                val file = PdfGenerator(getApplication()).generatePdf(
                    routes = routes,
                    salaryState = salaryState,
                    monthLabel = monthLabel,
                    sections = sections
                )
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.provider",
                    file
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, "Поделиться PDF")
                _shareEvent.emit(chooser)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка формирования PDF: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
