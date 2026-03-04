package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.Product
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.shared.platform.BillingProvider
import com.z_company.shared.util.DateAndTimeConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the purchases screen.
 *
 * Delegates product list and billing to [BillingProvider].
 * Provides date formatting via [DateAndTimeConverter].
 */
class PurchasesSharedViewModel(
    private val billingProvider: BillingProvider,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    val products: StateFlow<List<Product>> = billingProvider.products
    val purchasesEndTime: StateFlow<Long> = billingProvider.purchasesEndTime
    val isLoading: StateFlow<Boolean> = billingProvider.isLoading

    private val _dateAndTimeConverter = MutableStateFlow<DateAndTimeConverter?>(null)
    val dateAndTimeConverter: StateFlow<DateAndTimeConverter?> = _dateAndTimeConverter.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsUseCase.getUserSettingFlow().first()
            _dateAndTimeConverter.value = DateAndTimeConverter(settings)
        }
    }

    fun onProductClick(product: Product) = billingProvider.purchase(product)

    fun restorePurchases() = billingProvider.restorePurchases()
}
