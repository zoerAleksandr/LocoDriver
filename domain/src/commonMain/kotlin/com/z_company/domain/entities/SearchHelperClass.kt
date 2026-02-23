package com.z_company.domain.entities

import com.z_company.core.ErrorEntity
import com.z_company.domain.entities.route.Route

data class FilterSearch(
    val generalData: Pair<String, Boolean> = Pair(FilterNames.GENERAL_DATA.value, true),
    val locoData: Pair<String, Boolean> = Pair(FilterNames.LOCO_DATA.value, true),
    val trainData: Pair<String, Boolean> = Pair(FilterNames.TRAIN_DATA.value, true),
    val passengerData: Pair<String, Boolean> = Pair(FilterNames.PASSENGER_DATA.value, true),
    val notesData: Pair<String, Boolean> = Pair(FilterNames.NOTES_DATA.value, true),
    val timePeriod: TimePeriod = TimePeriod(null, null)
)

enum class FilterNames(val value: String){
    GENERAL_DATA("основные данные"),
    LOCO_DATA("локомотив"),
    TRAIN_DATA("поезд"),
    PASSENGER_DATA("следование пассажиром"),
    NOTES_DATA("примечания")
}

data class TimePeriod(
    val startDate: Long?,
    val endDate: Long?
)

enum class SearchTag {
    BASIC_DATA, LOCO, TRAIN, PASSENGER, NOTES
}

data class RouteWithTag(val tag: SearchTag, val route: Route)

sealed class SearchStateScreen<out T> {
    data class Input(val hints: List<String>): SearchStateScreen<Nothing>()
    data class Loading(val msg: String? = null) : SearchStateScreen<Nothing>()
    data class Success<out R>(val data: R?) : SearchStateScreen<R>()
    data class Failure(val entity: ErrorEntity) : SearchStateScreen<Nothing>()
}