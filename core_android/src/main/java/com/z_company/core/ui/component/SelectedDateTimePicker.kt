package com.z_company.core.ui.component

import androidx.compose.runtime.Composable
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
) {
    val preferences = koinInject<SharedPreferencesRepositories>()
    val token = preferences.tokenDateTimePickerType()

    val type = when(token){
        TypeDateTimePicker.ROUND.name -> {
            TypeDateTimePicker.ROUND
        }
        TypeDateTimePicker.INPUT.name -> {
            TypeDateTimePicker.INPUT
        }
        TypeDateTimePicker.WHEEL.name -> {
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
                onSettingClick = {}
            )
        }

        TypeDateTimePicker.INPUT -> {
            InputDateTimePicker(
                isShowPicker = isShowPicker,
                titleText = titleText,
                initDateTime = initDateTime,
                onDismiss = { onDismiss() },
                onDoneClick = onDoneClick,
                onSettingClick = {}
            )
        }

        TypeDateTimePicker.WHEEL -> {
            WheelDateTimePicker(
                isShowPicker = isShowPicker,
                titleText = titleText,
                initDateTime = initDateTime,
                onDismiss = { onDismiss() },
                onDoneClick = onDoneClick,
                onSettingClick = {}
            )
        }
    }
}

enum class TypeDateTimePicker {
     ROUND, INPUT, WHEEL
}