package com.z_company.login.ui

import androidx.compose.runtime.Composable
import com.z_company.core.ui.component.OnBoardingItems
import com.z_company.core.ui.component.PresentationBlock

@Composable
fun FirstPresentationBlockScreen(onNextClick: () -> Unit) {
    val items = OnBoardingItems.getDataFirstPresentation()
    PresentationBlock(
        items = items,
        onHomeClick = onNextClick
    )
}