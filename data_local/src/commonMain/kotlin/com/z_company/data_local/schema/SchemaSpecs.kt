package com.z_company.data_local.schema

/**
 * Описание столбцов таблиц (без первичных ключей) для самопроверки схемы.
 * Сгенерировано из .sq-файлов; при добавлении столбца в .sq/.sqm — добавлять его сюда же,
 * иначе самопроверка не сможет достроить его на «застрявшей» БД.
 */
object SchemaSpecs {
    val settings: List<TableSpec> = listOf(
        TableSpec(
            "LocomotiveSeries",
            listOf(
                "name" to "TEXT NOT NULL",
                "type" to "TEXT NOT NULL",
                "acceptanceDurationMin" to "INTEGER",
                "deliveryDurationMin" to "INTEGER",
                "acceptanceHandToHandMin" to "INTEGER",
                "deliveryHandToHandMin" to "INTEGER",
                "sectionNumberingType" to "TEXT NOT NULL DEFAULT 'NUMERIC'",
                "updatedAt" to "INTEGER NOT NULL",
            ),
        ),
        TableSpec(
            "LocomotiveSeriesTombstone",
            listOf(
                "name" to "TEXT NOT NULL",
                "type" to "TEXT NOT NULL",
                "deletedAt" to "INTEGER NOT NULL",
            ),
        ),
        TableSpec(
            "MonthOfYear",
            listOf(
                "year" to "INTEGER NOT NULL",
                "month" to "INTEGER NOT NULL",
                "days" to "TEXT NOT NULL",
                "tariffRate" to "REAL NOT NULL DEFAULT 0.0",
                "dateSetTariffRate" to "TEXT DEFAULT NULL",
            ),
        ),
        TableSpec(
            "Partner",
            listOf(
                "fullName" to "TEXT NOT NULL",
                "tabNumber" to "TEXT",
                "notes" to "TEXT",
                "updatedAt" to "INTEGER NOT NULL",
                "isPermanent" to "INTEGER NOT NULL DEFAULT 0",
            ),
        ),
        TableSpec(
            "ProductionCalendarDay",
            listOf(
                "country" to "TEXT NOT NULL",
                "year" to "INTEGER NOT NULL",
                "month" to "INTEGER NOT NULL",
                "dayOfMonth" to "INTEGER NOT NULL",
                "tag" to "TEXT NOT NULL",
            ),
        ),
        TableSpec(
            "RegionalHoliday",
            listOf(
                "region" to "TEXT NOT NULL",
                "year" to "INTEGER NOT NULL",
                "month" to "INTEGER NOT NULL",
                "dayOfMonth" to "INTEGER NOT NULL",
                "name" to "TEXT NOT NULL",
            ),
        ),
        TableSpec(
            "ReleaseDay",
            listOf(
                "year" to "INTEGER NOT NULL",
                "month" to "INTEGER NOT NULL",
                "dayOfMonth" to "INTEGER NOT NULL",
                "releaseType" to "TEXT NOT NULL",
                "hours" to "REAL",
            ),
        ),
        TableSpec(
            "StationNorm",
            listOf(
                "name" to "TEXT NOT NULL",
                "appearanceToStartMin" to "INTEGER",
                "endToBarrierMin" to "INTEGER",
                "barrierToStartMin" to "INTEGER",
                "endToWorkEndMin" to "INTEGER",
                "updatedAt" to "INTEGER NOT NULL",
                "latitude" to "REAL",
                "longitude" to "REAL",
                "esr" to "TEXT",
                "osmId" to "INTEGER",
                "kind" to "TEXT",
                "axisKm" to "INTEGER",
                "axisPicket" to "INTEGER",
            ),
        ),
        TableSpec(
            "StationTombstone",
            listOf(
                "name" to "TEXT NOT NULL",
                "deletedAt" to "INTEGER NOT NULL",
            ),
        ),
        TableSpec(
            "UserSettings",
            listOf(
                "minTimeRest" to "INTEGER NOT NULL",
                "minTimeRestSecond" to "INTEGER NOT NULL DEFAULT 14400000",
                "workStartOffsetMinutes" to "INTEGER NOT NULL DEFAULT 1",
                "minTimeHomeRest" to "INTEGER NOT NULL",
                "lastEnteredDieselCoefficient" to "REAL NOT NULL",
                "nightTime" to "TEXT NOT NULL",
                "updateAt" to "INTEGER NOT NULL",
                "defaultLocoType" to "TEXT NOT NULL",
                "defaultWorkTime" to "INTEGER NOT NULL",
                "usingDefaultWorkTime" to "INTEGER NOT NULL",
                "isConsiderFutureRoute" to "INTEGER NOT NULL DEFAULT 1",
                "monthOfYear" to "TEXT NOT NULL",
                "stationList" to "TEXT NOT NULL DEFAULT '[]'",
                "timeZone" to "INTEGER NOT NULL DEFAULT 0",
                "locomotiveSeriesList" to "TEXT NOT NULL DEFAULT '[]'",
                "servicePhases" to "TEXT NOT NULL DEFAULT '[]'",
                "standardTimesStartWork" to "TEXT NOT NULL DEFAULT '[28800000, 72000000]'",
                "subscriptionPeriod" to "INTEGER NOT NULL DEFAULT 0",
                "isDecimalTime" to "INTEGER NOT NULL DEFAULT 0",
                "isShowBreak" to "INTEGER NOT NULL DEFAULT 1",
                "isShowOnePersonSwitch" to "INTEGER NOT NULL DEFAULT 1",
                "isShowLocoHeating" to "INTEGER NOT NULL DEFAULT 1",
                "isShowLocoAuxiliary" to "INTEGER NOT NULL DEFAULT 1",
                "isConsiderLocoHeatingInTotal" to "INTEGER NOT NULL DEFAULT 1",
                "isConsiderLocoAuxiliaryInTotal" to "INTEGER NOT NULL DEFAULT 1",
                "isShowLocoStatistics" to "INTEGER NOT NULL DEFAULT 1",
                "isShowLocoNorma" to "INTEGER NOT NULL DEFAULT 1",
                "isShowOtherCurrent" to "INTEGER NOT NULL DEFAULT 0",
                "country" to "TEXT NOT NULL DEFAULT 'RU'",
                "crossMonthTimezone" to "TEXT NOT NULL DEFAULT 'LOCAL'",
                "useStandardTimePicker" to "INTEGER NOT NULL DEFAULT 0",
                "region" to "TEXT DEFAULT NULL",
                "isShowTrain" to "INTEGER NOT NULL DEFAULT 1",
                "isShowOtherWork" to "INTEGER NOT NULL DEFAULT 1",
                "otherWorkTypeList" to "TEXT NOT NULL DEFAULT '[]'",
                "isShowLocomotive" to "INTEGER NOT NULL DEFAULT 1",
                "isShowPassenger" to "INTEGER NOT NULL DEFAULT 1",
                "isShowPartner" to "INTEGER NOT NULL DEFAULT 1",
                "passengerWagonLengthMeters" to "REAL NOT NULL DEFAULT 24.5",
            ),
        ),
    )

    val salarySetting: List<TableSpec> = listOf(
        TableSpec(
            "SalarySetting",
            listOf(
                "nightTimePercent" to "REAL NOT NULL DEFAULT 40.0",
                "zonalSurcharge" to "REAL NOT NULL",
                "averagePaymentHour" to "REAL NOT NULL DEFAULT 0.0",
                "districtCoefficient" to "REAL NOT NULL DEFAULT 0.0",
                "nordicCoefficient" to "REAL NOT NULL DEFAULT 0.0",
                "onePersonOperationPercent" to "REAL NOT NULL DEFAULT 0.0",
                "onePersonOperationPassengerTrainPercent" to "REAL NOT NULL DEFAULT 50.0",
                "harmfulnessPercent" to "REAL NOT NULL DEFAULT 0.0",
                "surchargeQualificationClass" to "REAL NOT NULL",
                "surchargeExtendedServicePhaseList" to "TEXT NOT NULL",
                "surchargeHeavyTrainsList" to "TEXT NOT NULL DEFAULT '[]'",
                "surchargeLongTrainsList" to "TEXT NOT NULL DEFAULT '[]'",
                "surchargeHeavyLongDistanceTrains" to "REAL NOT NULL DEFAULT 5.0",
                "surchargeLongTrain" to "REAL NOT NULL DEFAULT 0.0",
                "lengthLongDistanceTrain" to "INTEGER NOT NULL DEFAULT 0",
                "otherSurcharge" to "REAL NOT NULL",
                "ndfl" to "REAL NOT NULL",
                "unionistsRetention" to "REAL NOT NULL",
                "otherRetention" to "REAL NOT NULL",
                "welfarePercent" to "REAL NOT NULL DEFAULT 0.0",
                "alimonyPercent" to "REAL NOT NULL DEFAULT 0.0",
                "showUnderworkPayments" to "INTEGER NOT NULL DEFAULT 1",
                "updatedAt" to "INTEGER NOT NULL DEFAULT 0",
            ),
        ),
    )

    val track: List<TableSpec> = listOf(
        TableSpec(
            "Track",
            listOf(
                "trainId" to "TEXT NOT NULL",
                "startedAt" to "INTEGER NOT NULL",
                "endedAt" to "INTEGER NOT NULL",
                "isFavorite" to "INTEGER NOT NULL DEFAULT 0",
                "uploadState" to "INTEGER NOT NULL DEFAULT 0",
                "uploadedPoints" to "INTEGER NOT NULL DEFAULT 0",
                "favoriteDirty" to "INTEGER NOT NULL DEFAULT 0",
            ),
        ),
        TableSpec(
            "TrackPendingDelete",
            listOf(
            ),
        ),
        TableSpec(
            "TrackPoint",
            listOf(
                "trackId" to "TEXT NOT NULL",
                "timeMs" to "INTEGER NOT NULL",
                "latitude" to "REAL NOT NULL",
                "longitude" to "REAL NOT NULL",
                "speedKmh" to "REAL",
                "accuracyM" to "REAL",
                "altitudeM" to "REAL",
                "baroAltitudeM" to "REAL",
            ),
        ),
        TableSpec(
            "RestrictionPass",
            listOf(
                "trainId" to "TEXT NOT NULL",
                "warningId" to "TEXT NOT NULL",
                "routeId" to "TEXT NOT NULL",
                "sectionId" to "TEXT NOT NULL",
                "kind" to "TEXT NOT NULL",
                "title" to "TEXT NOT NULL",
                "place" to "TEXT NOT NULL DEFAULT ''",
                "note" to "TEXT NOT NULL DEFAULT ''",
                "limitKmh" to "INTEGER",
                "startAbsKm" to "REAL",
                "endAbsKm" to "REAL",
                "startRelKm" to "REAL NOT NULL DEFAULT 0",
                "endRelKm" to "REAL NOT NULL DEFAULT 0",
                "enterAt" to "INTEGER",
                "enterSpeedKmh" to "REAL",
                "enterApprox" to "INTEGER NOT NULL DEFAULT 0",
                "exitAt" to "INTEGER",
                "exitSpeedKmh" to "REAL",
                "exitByHead" to "INTEGER NOT NULL DEFAULT 0",
                "trainLengthM" to "INTEGER",
                "dir" to "INTEGER NOT NULL DEFAULT 0",
                "maxSpeedKmh" to "REAL",
                "exceeded" to "INTEGER NOT NULL DEFAULT 0",
                "updatedAt" to "INTEGER NOT NULL",
                "isDeleted" to "INTEGER NOT NULL DEFAULT 0",
                "dirty" to "INTEGER NOT NULL DEFAULT 1",
            ),
        ),
    )

    val searchResponse: List<TableSpec> = listOf(
        TableSpec(
            "SearchResponse",
            listOf(
            ),
        ),
    )

}
