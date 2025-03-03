package com.z_company.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import kotlinx.datetime.LocalDateTime
import network.chaintech.kmp_date_time_picker.ui.datetimepicker.WheelDateTimePickerView
import network.chaintech.kmp_date_time_picker.utils.DateTimePickerView

@Composable
fun WheelDateTimePicker(
    isShowPicker: Boolean,
    titleText: String,
    initDateTime: Long,
    onDismiss: () -> Unit,
    onDoneClick: (LocalDateTime) -> Unit
) {
    val labelStyle = AppTypography.getType().titleLarge.copy(
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.tertiary
    )
    val doneStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )

    val startDate = ConverterLongToTime.timestampToDateTime(initDateTime)

    WheelDateTimePickerView(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
        showDatePicker = isShowPicker,
        startDate = startDate,
        title = titleText,
        titleStyle = labelStyle,
        doneLabel = "Готово",
        doneLabelStyle = doneStyle,
        rowCount = 5,
        height = 128.dp,
        dateTextColor = MaterialTheme.colorScheme.tertiary,
        dateTimePickerView = DateTimePickerView.DIALOG_VIEW,
        showMonthAsNumber = true,
        containerColor = Color.LightGray,
        onDoneClick = onDoneClick,
        onDismiss = onDismiss
    )
}