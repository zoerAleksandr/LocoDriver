package com.z_company.core.util

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import java.text.SimpleDateFormat
import java.util.Locale

fun BasicData.str(): String {
    val numberText = this.number?.let {
        "Маршрут №$it,"
    } ?: ""

    val startWorkText = this.timeStartWork?.let{
        "явка ${DateAndTimeConverter.getDateAndTime(it)},"
    } ?: ""
    val endWorkText = this.timeEndWork?.let{
        "сдача ${DateAndTimeConverter.getDateAndTime(it)},"
    } ?: ""

    return "$numberText $startWorkText $endWorkText"
}

fun Locomotive.str(): String {
    val text = StringBuilder()
    this.type.let { type ->
        when (type) {
            LocoType.DIESEL -> {
                text.append("Тепловоз")
            }
            LocoType.ELECTRIC -> {
                text.append("Электровоз")
            }
        }
    }
    this.series?.let {
        text.append(" $it")
    }
    this.number?.let {
        text.append(" №$it")
    }
    if (timeStartOfAcceptance != null || timeEndOfAcceptance != null) {
        text.append("\nприемка")
    }
    this.timeStartOfAcceptance?.let { time ->
        val timeStartAccepted = SimpleDateFormat(
            DateAndTimeFormat.TIME_FORMAT,
            Locale.getDefault()
        ).format(time)
        val dateStartAccepted = SimpleDateFormat(
            DateAndTimeFormat.DATE_FORMAT,
            Locale.getDefault()
        ).format(time)
        text.append("\n$dateStartAccepted $timeStartAccepted")
    }
    this.timeEndOfAcceptance?.let { time ->
        val timeEndAccepted = SimpleDateFormat(
            DateAndTimeFormat.TIME_FORMAT,
            Locale.getDefault()
        ).format(time)
        val dateEndAccepted = SimpleDateFormat(
            DateAndTimeFormat.DATE_FORMAT,
            Locale.getDefault()
        ).format(time)
        text.append(" - $dateEndAccepted $timeEndAccepted")
    }
    if (timeStartOfDelivery != null || timeEndOfDelivery != null) {
        text.append("\nсдача")
    }
    this.timeStartOfDelivery?.let { time ->
        val timeStartDelivery = SimpleDateFormat(
            DateAndTimeFormat.TIME_FORMAT,
            Locale.getDefault()
        ).format(time)
        val dateStartDelivery = SimpleDateFormat(
            DateAndTimeFormat.DATE_FORMAT,
            Locale.getDefault()
        ).format(time)
        text.append("\n$dateStartDelivery $timeStartDelivery")
    }
    this.timeEndOfDelivery?.let { time ->
        val timeEndDelivery = SimpleDateFormat(
            DateAndTimeFormat.TIME_FORMAT,
            Locale.getDefault()
        ).format(time)
        val dateEndDelivery = SimpleDateFormat(
            DateAndTimeFormat.DATE_FORMAT,
            Locale.getDefault()
        ).format(time)
        text.append(" - $dateEndDelivery $timeEndDelivery")
    }
    this.dieselSectionList.forEachIndexed { index, section ->
        text.append("\n • Cекция ${index + 1} ")
        text.append("\n${section.str()}")
    }
    this.electricSectionList.forEachIndexed { index, section ->
        text.append("\n • Cекция ${index + 1} ")
        text.append("\n${section.str()}")
    }

    return text.toString()
}

fun SectionDiesel.str(): String {
    val text = StringBuilder()
    if (this.acceptedFuel != null || this.deliveryFuel != null) {
        text.append("Топливо ")
        this.acceptedFuel?.let {
            text.append("\n$it")
        }
        this.deliveryFuel?.let {
            text.append(" - $it ")
        }
        this.coefficient?.let {
            text.append("\n k = $it ")
        }
    }
    this.fuelSupply?.let { supply ->
        text.append("\nЭкипировка")
        text.append("\n$supply л")
        this.coefficientSupply?.let {
            text.append(" k = $it")
        }
    }
    return text.toString()
}
fun SectionElectric.str(): String {
    val text = StringBuilder()
    if (this.acceptedEnergy != null || this.deliveryEnergy != null) {
        text.append("Расход электроэнергии")
        this.acceptedEnergy?.let {
            text.append("\n$it")
        }
        this.deliveryEnergy?.let {
            text.append(" - $it ")
        }
    }
    if (this.acceptedRecovery != null || this.deliveryRecovery != null) {
        text.append("\nРекуперация")
        this.acceptedRecovery?.let {
            text.append("\n$it")
        }
        this.deliveryRecovery?.let {
            text.append(" - $it ")
        }
    }

    return text.toString()
}

fun Train.str(): String {
    val text = StringBuilder("Поезд")
    this.number?.let {
        text.append(" № $it,")
    }
    this.weight?.let {
        text.append(" вес $it,")
    }
    this.axle?.let {
        text.append(" оси $it,")
    }
    this.conditionalLength?.let {
        text.append(" уд $it,")
    } ?: ""

    this.stations.forEach { station ->
        text.append(station.str())
    }
    return text.toString()
}

fun Station.str(): String {
    val text = StringBuilder()
    this.stationName?.let { name ->
        val timeArrival = this.timeArrival?.let { time ->
            SimpleDateFormat(
                DateAndTimeFormat.TIME_FORMAT,
                Locale.getDefault()
            ).format(time)
        } ?: ""

        val timeDeparture = this.timeDeparture?.let { time ->
            " - ${
                SimpleDateFormat(
                    DateAndTimeFormat.TIME_FORMAT,
                    Locale.getDefault()
                ).format(time)
            }"
        } ?: ""
        val stationText = "\n   • $name $timeArrival$timeDeparture"
        text.append(stationText)
    }
    return text.toString()
}

fun Passenger.str(): String {
    val text = StringBuilder("Следование паccажиром.")
    this.trainNumber?.let {
        text.append("\nПоезд №$it ")
    }
    this.stationDeparture?.let { departure ->
        text.append("\n$departure")
        this.stationArrival?.let { arrival ->
            text.append(" - $arrival ")
        }
    }
    this.timeDeparture?.let { departure ->
        val departureTimeText = SimpleDateFormat(
            DateAndTimeFormat.TIME_FORMAT,
            Locale.getDefault()
        ).format(departure)

        val departureDateText = SimpleDateFormat(
            DateAndTimeFormat.DATE_FORMAT,
            Locale.getDefault()
        ).format(departure)

        text.append("\n$departureDateText $departureTimeText")

        this.timeArrival?.let { arrival ->
            val arrivalDateText = SimpleDateFormat(
                DateAndTimeFormat.DATE_FORMAT,
                Locale.getDefault()
            ).format(arrival)

            val arrivalTimeText = SimpleDateFormat(
                DateAndTimeFormat.TIME_FORMAT,
                Locale.getDefault()
            ).format(arrival)

            text.append(" - $arrivalDateText $arrivalTimeText")
        }
    }
    this.notes?.let { notes ->
        text.append("\n$notes")
    }
    return text.toString()
}