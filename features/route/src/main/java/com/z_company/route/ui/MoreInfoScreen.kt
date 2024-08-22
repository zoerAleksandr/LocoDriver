package com.z_company.route.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getDayOffHours
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getStandardNormaHours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreInfoScreen(
    onBack: () -> Unit,
    currentMonthOfYearState: ResultState<MonthOfYear?>,
    totalWorkTimeState: ResultState<Long?>,
    nightTimeState: ResultState<Long?>,
    passengerTimeState: ResultState<Long?>
) {
    val subTitleStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val styleData = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)

    val styleTitle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Статистика",
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncData(
                resultState = currentMonthOfYearState,
                errorContent = {},
                loadingContent = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }) { monthOfYear ->
                monthOfYear?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = monthOfYear.month.getMonthFullText(),
                            style = styleData
                        )
                        Text(
                            text = monthOfYear.year.toString(),
                            style = styleData
                        )

                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Норма часов",
                    style = styleData
                )
                AsyncData(
                    resultState = currentMonthOfYearState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { monthOfYear ->
                    monthOfYear?.let {
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(
                                monthOfYear.getStandardNormaHours().toLong()
                                    .times(3_600_000)
                            ),
                            style = styleData
                        )
                    }
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Отвлечения",
                    style = styleData
                )
                AsyncData(
                    resultState = currentMonthOfYearState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { monthOfYear ->
                    monthOfYear?.let {
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(
                                monthOfYear.getDayOffHours().toLong()
                                    .times(3_600_000)
                            ),
                            style = styleData
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Индивидуальная норма",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                AsyncData(
                    resultState = currentMonthOfYearState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { monthOfYear ->
                    monthOfYear?.let {
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(
                                monthOfYear.getPersonalNormaHours().toLong()
                                    .times(3_600_000)
                            ),
                            style = styleData
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Норма на сегодня",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Отработано",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                AsyncData(
                    resultState = totalWorkTimeState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { workTime ->
                    workTime?.let {
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(workTime),
                            style = styleData
                        )
                    }
                }
            }

            Text(
                text = "из них: ",
                style = hintStyle
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "   ночных",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                AsyncData(
                    resultState = nightTimeState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { nightTime ->
                    nightTime?.let {
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(nightTime),
                            style = styleData
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "   праздничных",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "   пассажиром",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                AsyncData(
                    resultState = passengerTimeState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { passengerTime ->
                    passengerTime?.let {
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(passengerTime),
                            style = styleData
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "   ",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
}