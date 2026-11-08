@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.salary.isFederalLaw144Effective
import com.z_company.domain.salary.isUnderworkPeriodClosed
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.iosapp.pdf.PdfLayoutBuilder
import com.z_company.iosapp.pdf.PdfLayoutDocument
import com.z_company.iosapp.pdf.PdfSectionsModel
import com.z_company.iosapp.pdf.collectSalaryStatement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Итог генерации вёрстки PDF (§21). [document] == null при ошибке — тогда
 * [errorMessage] содержит сообщение для snackbar («Ошибка формирования PDF: …»).
 */
class PdfGenerationResult(
    val document: PdfLayoutDocument?,
    val errorMessage: String?,
)

/**
 * Экспорт PDF (SCREEN_SPECS §21): сборка модели вёрстки из данных месяца.
 *
 * Android (`PdfViewModel`) отдаёт готовый файл (Canvas/PdfDocument); iOS рендерит
 * PDF нативным UIKit по этой модели (`PdfRenderer.swift`), поэтому VM выдаёт
 * `PdfLayoutDocument`, а не файл. Разделы «Поездки/График/Расчётный листок».
 *
 * Контракт §21: повторный вызов во время генерации игнорируется (`isGenerating`);
 * данные расчёта ждутся до 8 с, иначе листок заменяется текстом «Данные расчёта
 * недоступны…» (не ошибка). Маршруты и дни месяца VM берёт из use case'ов
 * (эквивалент `updateRoutes`/`updateCalendarDays` экранами на Android).
 */
class PdfExportIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val normaUseCase: NormaUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val sharedPreferences: SharedPreferencesRepositories,
) : ViewModel() {

    /**
     * Выбранные разделы шторки «Содержание PDF». Значения переживают пересоздание
     * шторки/поворот (§21 сценарий п.1) — VM-singleton в Koin.
     */
    var sections: PdfSectionsModel = PdfSectionsModel()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /** Однократная подписка на готовность/ошибку (шаблон watchX для Swift). */
    fun watchIsGenerating(callback: (Boolean) -> Unit) {
        viewModelScope.launch { _isGenerating.collect { callback(it) } }
    }

    /**
     * Сформировать модель вёрстки. [callback] вызывается один раз на Main:
     * документ либо сообщение об ошибке (§21 сценарий п.5).
     */
    fun generate(sections: PdfSectionsModel, callback: (PdfGenerationResult) -> Unit) {
        if (_isGenerating.value) return // повторный вызов игнорируется (§21 п.2)
        this.sections = sections
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val result = withContext(Dispatchers.Default) { buildDocument(sections) }
                callback(result)
            } catch (e: Exception) {
                callback(
                    PdfGenerationResult(
                        document = null,
                        errorMessage = "Ошибка формирования PDF: ${e.message ?: "неизвестная ошибка"}",
                    ),
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun buildDocument(sections: PdfSectionsModel): PdfGenerationResult {
        if (!sections.includeRouteDetails && !sections.includeSchedule && !sections.includeSalary) {
            // §21 п.5 (❓ не проверено на Android): документ без разделов — ошибка.
            return PdfGenerationResult(
                document = null,
                errorMessage = "Ошибка формирования PDF: не выбрано ни одного раздела",
            )
        }
        val settings = settingsUseCase.getUserSettingFlow().first()
        val moy = settings.selectMonthOfYear
        val monthLabel = PdfLayoutBuilder.monthLabelFor(moy.month, moy.year)
        val routes = routeUseCase.routeListByMonthFlow(moy, settings.timeZone).first()
        val workScheduleProfile = sharedPreferences.getWorkScheduleProfile()

        val salary = if (sections.includeSalary) {
            // §21 п.3: ждём данные расчёта до 8 с; не дождались — листок с текстом.
            withTimeoutOrNull(8_000L) {
                buildSalaryData(settings, moy, routes, workScheduleProfile)
            }
        } else null

        val document = PdfLayoutBuilder.build(
            sections = sections,
            routes = routes,
            monthLabel = monthLabel,
            calendarDays = moy.days,
            workScheduleProfile = workScheduleProfile,
            salary = salary,
        )
        return PdfGenerationResult(document = document, errorMessage = null)
    }

    private suspend fun buildSalaryData(
        settings: com.z_company.domain.entities.setting.UserSettings,
        moy: MonthOfYear,
        allRoutes: List<Route>,
        workScheduleProfile: com.z_company.domain.entities.WorkScheduleProfile,
    ) = run {
        val now = Clock.System.now().toEpochMilliseconds()
        val routeList = if (settings.isConsiderFutureRoute) {
            allRoutes
        } else {
            allRoutes.filter { (it.basicData.timeStartWork ?: 0L) < now }
        }
        val adjacentAll = routeUseCase.adjacentRoutesOfMonthFlow(
            moy,
            TimeCalculationContext.from(settings),
        ).first()
        val adjacentList = if (settings.isConsiderFutureRoute) {
            adjacentAll
        } else {
            adjacentAll.filter { (it.basicData.timeStartWork ?: 0L) < now }
        }
        val normaHours = normaUseCase.normaHoursFlow(moy.year, moy.month).first()
        val effectiveNormaHours = run {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            if (isUnderworkPeriodClosed(moy.year, moy.month, today)) normaHours else 0
        }
        collectSalaryStatement(
            userSettings = settings,
            salarySetting = salarySettingUseCase.getSalarySetting(),
            routes = routeList,
            adjacentRoutes = adjacentList,
            effectiveNormaHoursForUnderwork = effectiveNormaHours,
            annualOvertimeBeforePeriod = annualOvertimeBeforeMonth(settings, moy),
            workScheduleProfile = workScheduleProfile,
            monthLabel = PdfLayoutBuilder.monthLabelFor(moy.month, moy.year),
            normaHours = normaHours,
        )
    }

    /** Сумма переработки предыдущих месяцев года (порог ФЗ-144 с 09.2026) — порт Android. */
    private suspend fun annualOvertimeBeforeMonth(
        settings: com.z_company.domain.entities.setting.UserSettings,
        selectedMonth: MonthOfYear,
    ): Long {
        if (!isFederalLaw144Effective(selectedMonth.year, selectedMonth.month)) return 0L
        val previousMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
            .filter { it.year == selectedMonth.year && it.month < selectedMonth.month }
            .distinctBy { it.month }
            .sortedBy { it.month }
        var annualOvertime = 0L
        previousMonths.forEach { month ->
            val routesState = routeUseCase.listRoutesByMonth(
                month,
                TimeCalculationContext.from(settings),
            ).first { it is ResultState.Success || it is ResultState.Error }
            val routes = (routesState as? ResultState.Success)?.data.orEmpty()
            val helper = SalaryCalculationHelper(
                userSettings = settings.copy(selectMonthOfYear = month),
                salarySetting = salarySettingUseCase.getSalarySetting(),
                allRoutes = routes,
                workScheduleProfile = sharedPreferences.getWorkScheduleProfile(),
            )
            annualOvertime += helper.getTimeOvertimeFlow().first()
        }
        return annualOvertime
    }
}
