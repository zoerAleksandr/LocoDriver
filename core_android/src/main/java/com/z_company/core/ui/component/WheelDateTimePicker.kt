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
import kotlinx.datetime.LocalDateTime
import network.chaintech.kmp_date_time_picker.ui.datetimepicker.WheelDateTimePickerView
import network.chaintech.kmp_date_time_picker.utils.DateTimePickerView

@Composable
fun WheelDateTimePicker(
    isShowPicker: Boolean,
    titleText: String,
    onDismiss: () -> Unit,
    onDoneClick: (LocalDateTime) -> Unit
) {
    val dataTextStyle = AppTypography.getType().titleLarge.copy(
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.tertiary
    )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.tertiary
        )


    WheelDateTimePickerView(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
        showDatePicker = isShowPicker,
        title = titleText,
        titleStyle = dataTextStyle,
        doneLabel = "Готово",
        doneLabelStyle = hintStyle,
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