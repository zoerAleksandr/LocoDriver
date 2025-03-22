package com.z_company.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.customDatePicker.DateTimePickerView
import com.z_company.core.ui.component.customDateTimePicker.MyWheelDateTimePickerView
import com.z_company.core.util.ConverterLongToTime
import kotlinx.datetime.LocalDateTime

@Composable
fun WheelDateTimePicker(
    isShowPicker: Boolean,
    titleText: String,
    initDateTime: Long,
    onDismiss: () -> Unit,
    onDoneClick: (LocalDateTime) -> Unit,
    onSettingClick: () -> Unit = {}
) {

    MyWheelDateTimePickerView(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
        showDatePicker = isShowPicker,
        startDate = ConverterLongToTime.timestampToDateTime(initDateTime),
        title = titleText,
        rowCount = 5,
        height = 158.dp,
        dateTimePickerView = DateTimePickerView.DIALOG_VIEW,
        showMonthAsNumber = false,
        onDoneClick = onDoneClick,
        onDismiss = onDismiss,
        onSettingClick = onSettingClick
    )
}