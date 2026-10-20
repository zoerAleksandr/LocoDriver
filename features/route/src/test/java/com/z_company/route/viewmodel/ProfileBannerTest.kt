package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.core.ErrorEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileBannerTest {
    private val ok = ResultState.Success(null)

    @Test fun loading_showsLoadingBanner() {
        assertEquals(ProfileDataBanner.LOADING, ProfileUiState(userDetailsState = ResultState.Loading("...")).dataBanner(loggedIn = true))
    }

    @Test fun success_noBanner() {
        assertNull(ProfileUiState(userDetailsState = ok).dataBanner(loggedIn = true))
    }

    @Test fun error_offlineBanner_screenStaysUsable() {
        val st = ProfileUiState(userDetailsState = ResultState.Error(ErrorEntity(message = "no network")), isProfileNetworkError = true, sessionChecked = true)
        assertEquals(ProfileDataBanner.OFFLINE, st.dataBanner(loggedIn = true))
        assertEquals("Нет сети. Показаны сохранённые данные, они могли устареть", ProfileDataBanner.OFFLINE.text)
    }

    @Test fun notLoggedIn_noBanner_andSessionGateIsLocal() {
        assertNull(ProfileUiState(userDetailsState = ResultState.Loading("...")).dataBanner(loggedIn = false))
        // до чтения локальной сессии - заглушка; сеть её не определяет
        assertEquals(false, ProfileUiState().sessionChecked)
        assertEquals("Загружаем данные. Текущая информация могла устареть", ProfileDataBanner.LOADING.text)
    }
}
