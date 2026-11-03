package com.z_company.route.component.servicephase

import androidx.compose.runtime.Composable
import com.z_company.domain.onboarding.MapTourStep
import com.z_company.route.component.CoachAnchor
import com.z_company.route.component.CoachTour
import com.z_company.route.component.TourStep

/** Экскурсия по карте (ONBOARDING_MATERIALS §2): шаги [MapTourStep], привязанные к элементам экрана; нет элемента - нет шага. */
@Composable
internal fun MapHintsOverlay(
    anchors: Map<MapTourStep, CoachAnchor>,
    onDismiss: () -> Unit,
) {
    CoachTour(
        steps = MapTourStep.entries.map { TourStep(it.title, it.text, anchors.getValue(it)) },
        onDismiss = onDismiss,
    )
}
