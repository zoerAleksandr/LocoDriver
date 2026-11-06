package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.salary.PayrollCodeReference
import com.z_company.domain.entities.salary.PayrollCodeReferenceCatalog
import com.z_company.domain.entities.salary.PayrollPaymentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Строка справочника кодов для Swift (§11.8). `source` наружу не экспортируется. */
data class PayrollCodeItemUi(
    val code: String,
    val isAccrual: Boolean,
    val shortName: String,
    val description: String,
)

/**
 * Справочник кодов начислений/удержаний (§11.8).
 * Поиск — `PayrollCodeReferenceCatalog.search` (общий KMP-код, 286 записей):
 * все слова запроса должны встретиться, регистр не учитывается, кириллические
 * буквы, визуально совпадающие с латинскими, нормализуются.
 */
class PayrollCodeSearchIosViewModel : ViewModel() {

    companion object {
        /**
         * Текст запроса сохраняется между открытиями экрана (§11.8):
         * один экземпляр VM на «сессию поиска» + снимок последнего запроса.
         */
        var lastQuery: String = ""
    }

    private val _query = MutableStateFlow(lastQuery)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(runSearch(lastQuery))
    val results: StateFlow<List<PayrollCodeItemUi>> = _results.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
        lastQuery = value
        _results.value = runSearch(value)
    }

    fun clearQuery() = setQuery("")

    private fun runSearch(query: String): List<PayrollCodeItemUi> =
        PayrollCodeReferenceCatalog.search(query).map { it.toUi() }

    private fun PayrollCodeReference.toUi() = PayrollCodeItemUi(
        code = code,
        isAccrual = type == PayrollPaymentType.ACCRUAL,
        shortName = shortName,
        description = description,
    )

    // ── Watch callbacks (Swift) ──────────────────────────────────────────────

    fun watchQuery(callback: (String) -> Unit) {
        viewModelScope.launch { query.collect { callback(it) } }
    }

    fun watchResults(callback: (List<PayrollCodeItemUi>) -> Unit) {
        viewModelScope.launch { results.collect { callback(it) } }
    }
}
