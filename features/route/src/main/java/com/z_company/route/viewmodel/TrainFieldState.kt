package com.z_company.route.viewmodel


enum class StationDataType {
    NAME, ARRIVAL, DEPARTURE
}

data class StationField(
    val data: String? = null,
    val type: StationDataType
)

data class StationFieldDate(
    val data: Long? = null,
    val type: StationDataType
)

data class StationIsValidField(
    val data: Boolean = true,
    val type: StationDataType? = null
)

data class StationFormState(
    val id: String,
    val station: StationField = StationField(type = StationDataType.NAME),
    val arrival: StationFieldDate = StationFieldDate(type = StationDataType.ARRIVAL),
    val departure: StationFieldDate = StationFieldDate(type = StationDataType.DEPARTURE),
//    val formValid: StationIsValidField = StationIsValidField(),
    val errorMessage: String = ""
)

sealed class StationEvent{
    data class EnteredStationName(val index: Int, val data: String?): StationEvent()
    data class EnteredArrivalTime(val index: Int, val data: Long?): StationEvent()
    data class EnteredDepartureTime(val index: Int, val data: Long?): StationEvent()
    data class FocusChange(val index: Int, val fieldName: StationDataType): StationEvent()
}