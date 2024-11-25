package com.z_company.work_manager

object RouteFieldName {
    const val ROUTE_CLASS_NAME_REMOTE = "Route"
    const val USER_FIELD_NAME = "user"
    const val DATA_FIELD_NAME = "data"
}
object BasicDataFieldName {
    const val BASIC_DATA_CLASS_NAME_REMOTE = "BasicData"
    const val BASIC_DATA_UID_FIELD_NAME = "uid"
    const val NUMBER_FIELD_NAME = "number"
    const val TIME_START_WORK_FIELD_NAME = "timeStartWork"
    const val TIME_END_WORK_FIELD_NAME = "timeEndWork"
    const val REST_FIELD_NAME = "restPointOfTurnover"
    const val NOTES_FIELD_NAME = "notes"
    const val USER_FIELD_NAME = "user"
}

object LocomotiveFieldName {
    const val LOCOMOTIVE_CLASS_NAME_REMOTE = "Loco"
    const val LOCOMOTIVE_UID_FIELD_NAME = "uid"
    const val LOCOMOTIVE_BASIC_ID_FIELD_NAME = "basicId"
    const val SERIES_FIELD_NAME = "series"
    const val LOCOMOTIVE_NUMBER_FIELD_NAME = "number"
    const val TYPE_FIELD_NAME = "type"
    const val ELECTRIC_SECTIONS_FIELD_NAME = "electricSections"
    const val DIESEL_SECTIONS_FIELD_NAME = "dieselSections"
    const val TIME_START_ACCEPTED_FIELD_NAME = "timeStartOfAcceptance"
    const val TIME_END_ACCEPTED_FIELD_NAME = "timeEndOfAcceptance"
    const val TIME_START_DELIVERY_FIELD_NAME = "timeStartOfDelivery"
    const val TIME_END_DELIVERY_FIELD_NAME = "timeEndOfDelivery"
    const val BASIC_DATA_FIELD_NAME = "basicData"
}

object TrainFieldName {
    const val TRAIN_CLASS_NAME_REMOTE = "Train"
    const val TRAIN_ID_FIELD_NAME = "trainId"
    const val TRAIN_BASIC_ID_FIELD_NAME = "basicId"
    const val TRAIN_NUMBER_FIELD_NAME = "number"
    const val TRAIN_DISTANCE_FIELD_NAME = "distance"
    const val WEIGHT_FIELD_NAME = "weight"
    const val AXLE_FIELD_NAME = "axle"
    const val LENGTH_FIELD_NAME = "conditionalLength"
    const val STATIONS_FIELD_NAME = "stations"
    const val BASIC_DATA_FIELD_NAME = "basicData"
}

object PassengerFieldName {
    const val PASSENGER_CLASS_NAME_REMOTE = "Passenger"
    const val PASSENGER_ID_FIELD_NAME = "passengerId"
    const val PASSENGER_BASIC_ID_FIELD_NAME = "basicId"
    const val PASSENGER_TRAIN_NUMBER_FIELD_NAME = "trainNumber"
    const val STATION_DEPARTURE_FIELD_NAME = "stationDeparture"
    const val STATION_ARRIVAL_FIELD_NAME = "stationArrival"
    const val TIME_ARRIVAL_FIELD_NAME = "timeArrival"
    const val TIME_DEPARTURE_FIELD_NAME = "timeDeparture"
    const val NOTES_FIELD_NAME = "notes"
    const val BASIC_DATA_FIELD_NAME = "basicData"
}

object PhotoFieldName {
    const val PHOTO_CLASS_NAME_REMOTE = "Photo"
    const val PHOTO_ID_FIELD_NAME = "photoId"
    const val PHOTO_BASIC_ID_FIELD_NAME = "basicId"
    const val BASE_64_FIELD_NAME = "base64"
    const val DATE_OF_CREATE = "dateOfCreate"
    const val BASIC_DATA_FIELD_NAME = "basicData"
}

object UserFieldName {
    const val USER_CLASS_NAME_REMOTE = "_User"
    const val EMAIL_VERIFIED_FIELD_NAME_REMOTE = "emailVerified"
    const val USERNAME_FIELD_NAME_REMOTE = "username"
}