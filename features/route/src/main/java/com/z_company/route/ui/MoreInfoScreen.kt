package com.z_company.route.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreInfoScreen(
    onBack: () -> Unit,
    currentMonthOfYearState: ResultState<MonthOfYear?>,
    totalWorkTimeState: ResultState<Long?>,
    nightTimeState: ResultState<Long?>,
    passengerTimeState: ResultState<Long?>,
    holidayWorkTimeState: ResultState<Long?>,
    workTimeWithHoliday: ResultState<Long?>,
    todayNormaHours: ResultState<Int?>,
    timeBalanceState: ResultState<Long?>,
) {
    val styleDataLight = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)

    val styleDataMedium = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.SemiBold)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Статистика",
                        style = titleStyle
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
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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
                                style = styleDataLight
                            )
                            Text(
                                text = monthOfYear.year.toString(),
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Норма часов",
                        style = styleDataLight
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
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Отвлечения",
                        style = styleDataLight
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
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Индивидуальная норма",
                        style = styleDataLight,
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
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Норма на сегодня",
                        style = styleDataLight,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    AsyncData(
                        resultState = todayNormaHours,
                        errorContent = {},
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }) { todayNorma ->
                        todayNorma?.let {
                            Text(
                                text =
                                ConverterLongToTime.getTimeInStringFormat(
                                    todayNorma.toLong().times(3_600_000)
                                ),
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Отработано",
                        style = styleDataLight,
                    )
                    AsyncData(
                        resultState = workTimeWithHoliday,
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
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Праздничныx",
                        style = styleDataLight,
                    )
                    AsyncData(
                        resultState = holidayWorkTimeState,
                        errorContent = {},
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }) { holidayTime ->
                        holidayTime?.let {
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(holidayTime),
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Всего",
                        style = styleDataLight,
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
                                style = styleDataMedium
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "из них: ",
                    style = styleDataLight
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "   ночных",
                        style = styleDataLight,
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
                                style = styleDataLight
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "   пассажиром",
                        style = styleDataLight,
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
                                style = styleDataLight
                            )
                        }
                    }
                }
            }
            item {

                AsyncData(
                    resultState = timeBalanceState,
                    errorContent = {},
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }) { timeBalance ->
                    timeBalance?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val textBalance = if (timeBalance < 0) {
                                "Осталось до нормы"
                            } else {
                                "Переработка"
                            }
                            val colorBalance = if (timeBalance < 0) {
                                MaterialTheme.colorScheme.scrim
                            } else if (timeBalance < 72_000_000L) {
                                Color(0xFF3F920B)
                            } else {
                                Color.Red
                            }
                            Text(
                                text = textBalance,
                                style = styleDataLight,
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    timeBalance.absoluteValue
                                ),
                                style = styleDataMedium.copy(color = colorBalance)
                            )
                        }
                    }

                }
            }
        }
    }
}
