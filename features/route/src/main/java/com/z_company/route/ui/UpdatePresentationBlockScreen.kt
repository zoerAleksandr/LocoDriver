package com.z_company.route.ui

import androidx.compose.runtime.Composable
import com.z_company.core.ui.component.OnBoardingItems
import com.z_company.core.ui.component.PresentationBlock

@Composable
fun UpdatePresentationBlockScreen(onHomeScreenClick: () -> Unit){
    val items = OnBoardingItems.getDataUpdatePresentation()
    PresentationBlock(items = items, onHomeClick = onHomeScreenClick)
}