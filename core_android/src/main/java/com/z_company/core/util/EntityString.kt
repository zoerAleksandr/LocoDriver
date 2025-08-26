package com.z_company.core.util

import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train

class EntityString(setting: UserSettings) {
    private var dateAndTimeConverter: DateAndTimeConverter = DateAndTimeConverter(setting)

    fun basicDataStr(basicData: BasicData): String {
        val numberText = basicData.number?.let {
            "Маршрут №$it,"
        } ?: ""

        val startWorkText = basicData.timeStartWork?.let {
            "явка ${dateAndTimeConverter.getDateAndTime(it)},"
        } ?: ""
        val endWorkText = basicData.timeEndWork?.let {
            "сдача ${dateAndTimeConverter.getDateAndTime(it)},"
        } ?: ""

        return "$numberText $startWorkText $endWorkText"
    }

    fun locomotiveStr(locomotive: Locomotive): String {
        val text = StringBuilder()
        locomotive.type.let { type ->
            when (type) {
                LocoType.DIESEL -> {
                    text.append("Тепловоз")
                }

                LocoType.ELECTRIC -> {
                    text.append("Электровоз")
                }
            }
        }
        locomotive.series?.let {
            text.append(" $it")
        }
        locomotive.number?.let {
            text.append(" №$it")
        }
        if (locomotive.timeStartOfAcceptance != null || locomotive.timeEndOfAcceptance != null) {
            text.append("\nприемка")
        }
        locomotive.timeStartOfAcceptance?.let { time ->
            val dateAndTimeStartAccepted = dateAndTimeConverter.getDateAndTime(time)
            text.append("\n$dateAndTimeStartAccepted")
        }
        locomotive.timeEndOfAcceptance?.let { time ->
            val dateAndTimeEndAccepted = dateAndTimeConverter.getDateAndTime(time)
            text.append(" - $dateAndTimeEndAccepted")
        }
        if (locomotive.timeStartOfDelivery != null || locomotive.timeEndOfDelivery != null) {
            text.append("\nсдача")
        }
        locomotive.timeStartOfDelivery?.let { time ->
            val dateAndTimeStartDelivery = dateAndTimeConverter.getDateAndTime(time)
            text.append("\n$dateAndTimeStartDelivery")
        }
        locomotive.timeEndOfDelivery?.let { time ->
            val dateAndTimeEndDelivery = dateAndTimeConverter.getDateAndTime(time)
            text.append(" - $dateAndTimeEndDelivery")
        }
        locomotive.dieselSectionList.forEachIndexed { index, section ->
            text.append("\n • Cекция ${index + 1} ")
            text.append("\n${sectionDieselStr(section)}")
        }
        locomotive.electricSectionList.forEachIndexed { index, section ->
            text.append("\n • Cекция ${index + 1} ")
            text.append("\n${sectionElectricStr(section)}")
        }

        return text.toString()
    }

    fun sectionDieselStr(sectionDiesel: SectionDiesel): String {
        val text = StringBuilder()
        if (sectionDiesel.acceptedFuel != null || sectionDiesel.deliveryFuel != null) {
            text.append("Топливо ")
            sectionDiesel.acceptedFuel?.let {
                text.append("\n$it")
            }
            sectionDiesel.deliveryFuel?.let {
                text.append(" - $it ")
            }
            sectionDiesel.coefficient?.let {
                text.append("\n k = $it ")
            }
        }
        sectionDiesel.fuelSupply?.let { supply ->
            text.append("\nЭкипировка")
            text.append("\n$supply л")
            sectionDiesel.coefficientSupply?.let {
                text.append(" k = $it")
            }
        }
        return text.toString()
    }

    fun sectionElectricStr(sectionElectric: SectionElectric): String {
        val text = StringBuilder()
        if (sectionElectric.acceptedEnergy != null || sectionElectric.deliveryEnergy != null) {
            text.append("Расход электроэнергии")
            sectionElectric.acceptedEnergy?.let {
                text.append("\n$it")
            }
            sectionElectric.deliveryEnergy?.let {
                text.append(" - $it ")
            }
        }
        if (sectionElectric.acceptedRecovery != null || sectionElectric.deliveryRecovery != null) {
            text.append("\nРекуперация")
            sectionElectric.acceptedRecovery?.let {
                text.append("\n$it")
            }
            sectionElectric.deliveryRecovery?.let {
                text.append(" - $it ")
            }
        }

        return text.toString()
    }

    fun trainStr(train: Train): String {
        val text = StringBuilder("Поезд")
        train.number?.let {
            text.append(" № $it,")
        }
        train.weight?.let {
            text.append(" вес $it,")
        }
        train.axle?.let {
            text.append(" оси $it,")
        }
        train.conditionalLength?.let {
            text.append(" уд $it,")
        } ?: ""

        train.stations.forEach { station ->
            text.append(stationStr(station))
        }
        return text.toString()
    }

    fun stationStr(station: Station): String {
        val text = StringBuilder()
        station.stationName?.let { name ->
            val timeArrival = station.timeArrival?.let { time ->
                dateAndTimeConverter.getDateAndTime(time)
            } ?: ""

            val timeDeparture = station.timeDeparture?.let { time ->
                " - ${
                    dateAndTimeConverter.getDateAndTime(time)
                }"
            } ?: ""
            val stationText = "\n   • $name $timeArrival$timeDeparture"
            text.append(stationText)
        }
        return text.toString()
    }

    fun passengerStr(passenger: Passenger): String {
        val text = StringBuilder("Следование паccажиром.")
        passenger.trainNumber?.let {
            text.append("\nПоезд №$it ")
        }
        passenger.stationDeparture?.let { departure ->
            text.append("\n$departure")
            passenger.stationArrival?.let { arrival ->
                text.append(" - $arrival ")
            }
        }
        passenger.timeDeparture?.let { departure ->
            val departureDateAndTimeText = dateAndTimeConverter.getDateAndTime(departure)

            text.append("\n$departureDateAndTimeText")

            passenger.timeArrival?.let { arrival ->
                val arrivalDateAndTimeText = dateAndTimeConverter.getDateAndTime(arrival)

                text.append(" - $arrivalDateAndTimeText")
            }
        }
        passenger.notes?.let { notes ->
            text.append("\n$notes")
        }
        return text.toString()
    }
}