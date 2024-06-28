package com.z_company.route.ui

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