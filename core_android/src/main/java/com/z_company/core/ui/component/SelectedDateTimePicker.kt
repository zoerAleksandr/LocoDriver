package com.z_company.core.ui.component

import androidx.compose.runtime.Composable
import com.z_company.domain.entities.TypeDateTimePicker
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.datetime.LocalDateTime
import org.koin.compose.koinInject

@Composable
fun SelectableDateTimePicker(
    isShowPicker: Boolean,
    titleText: String,
    initDateTime: Long,
    onDismiss: () -> Unit,
    onDoneClick: (LocalDateTime) -> Unit,
    onSettingClick: () -> Unit
) {
    val preferences = koinInject<SharedPreferencesRepositories>()
    val token = preferences.tokenDateTimePickerType()

    val type = when(token){
        TypeDateTimePicker.ROUND.text -> {
            TypeDateTimePicker.ROUND
        }
        TypeDateTimePicker.INPUT.text -> {
            TypeDateTimePicker.INPUT
        }
        TypeDateTimePicker.WHEEL.text -> {
            TypeDateTimePicker.WHEEL
        }
        else -> {
            TypeDateTimePicker.WHEEL
        }

    }

    when (type) {
        TypeDateTimePicker.ROUND -> {
            RoundDateTimePicker(
                isShowPicker = isShowPicker,
                titleText = titleText,
                initDateTime = initDateTime,
                onDismiss = { onDismiss() },
                onDoneClick = onDoneClick,
                onSettingClick = onSettingClick
            )
        }

        TypeDateTimePicker.INPUT -> {
            InputDateTimePicker(
                isShowPicker = isShowPicker,
                titleText = titleText,
                initDateTime = initDateTime,
                onDismiss = { onDismiss() },
                onDoneClick = onDoneClick,
                onSettingClick = onSettingClick
            )
        }

        TypeDateTimePicker.WHEEL -> {
            WheelDateTimePicker(
                isShowPicker = isShowPicker,
                titleText = titleText,
                initDateTime = initDateTime,
                onDismiss = { onDismiss() },
                onDoneClick = onDoneClick,
                onSettingClick = onSettingClick
            )
        }
    }
}