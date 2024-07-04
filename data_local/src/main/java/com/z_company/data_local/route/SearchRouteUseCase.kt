package com.z_company.data_local.route

import com.z_company.core.ResultState
import com.z_company.core.util.str
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.RouteWithTag
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.SearchTag
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.inTimePeriod
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.addOrSkip
import com.z_company.domain.util.splitBySpaceAndComma
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SearchRouteUseCase(val repository: RouteRepository) {
    fun searchRoute(
        value: String,
        filter: FilterSearch,
        preliminarySearch: Boolean
    ): Flow<SearchStateScreen<List<RouteWithTag>>> =
        callbackFlow {
            repository.loadRoutes().collect { result ->
                when (result) {
                    is ResultState.Loading -> trySend(SearchStateScreen.Loading())

                    is ResultState.Success -> result.data.let { resultList ->
                        val sortedList = mutableListOf<RouteWithTag>()

                        resultList.forEach { route ->
                            if (route.inTimePeriod(filter.timePeriod)) {
                                if (filter.generalData.second) {
                                    sortedList.addAllOrSkip(searchInRouteData(route, value))
                                }
                                if (filter.locoData.second) {
                                    sortedList.addAllOrSkip(searchInLocoList(route, value))
                                }
                                if (filter.trainData.second) {
                                    sortedList.addAllOrSkip(searchInTrainList(route, value))
                                }
                                if (filter.passengerData.second) {
                                    sortedList.addAllOrSkip(searchInPassengerList(route, value))
                                }
                                if (filter.notesData.second) {
                                    sortedList.addAllOrSkip(searchInNotes(route, value))
                                }
                            }
                            if (preliminarySearch) {
                                val hintsList = mutableListOf<String>()

                                sortedList.forEach { routeWithTag: RouteWithTag ->
                                    val text = when (routeWithTag.tag) {
                                        SearchTag.BASIC_DATA -> {
                                            StringBuilder(routeWithTag.route.basicData.str())
                                        }

                                        SearchTag.LOCO -> {
                                            val text = StringBuilder()
                                            routeWithTag.route.locomotives.forEach { loco ->
                                                text.append("${loco.str()} ")
                                            }
                                            text
                                        }

                                        SearchTag.TRAIN -> {
                                            val text = StringBuilder()
                                            routeWithTag.route.trains.forEach { train ->
                                                text.append("${train.str()} ")
                                            }
                                            text
                                        }

                                        SearchTag.PASSENGER -> {
                                            val text = StringBuilder()
                                            routeWithTag.route.passengers.forEach { passenger ->
                                                text.append(passenger.str())
                                            }
                                            text
                                        }

                                        SearchTag.NOTES -> {
                                            StringBuilder(routeWithTag.route.basicData.notes.toString())
                                        }
                                    }

                                    val listStrings = text.toString().splitBySpaceAndComma()
                                    val valueString = value.splitBySpaceAndComma().last()

                                    listStrings.forEachIndexed { index, s ->
                                        if (s == valueString && index != listStrings.lastIndex) {
                                            hintsList.addOrSkip(listStrings[index + 1])
                                        } else if (s.contains(value)) {
                                            hintsList.addOrSkip(s)
                                        }
                                    }
                                }
                                trySend(
                                    SearchStateScreen.Input(hintsList)
                                )
                            } else {
                                trySend(SearchStateScreen.Success(sortedList))
                            }
                        }
                    }

                    is ResultState.Error -> trySend(SearchStateScreen.Failure(result.entity))
                }
            }
        }
}

private fun searchInRouteData(
    route: Route,
    value: String,
): MutableList<RouteWithTag> {
    val filteredList = mutableListOf<RouteWithTag>()

    val valueList = value.splitBySpaceAndComma()
    var respond = true

    valueList.forEach { s ->
        if (!route.basicData.str().contains(s, ignoreCase = true)) {
            respond = false
        }
    }

    if (respond) {
        filteredList.addOrReplace(RouteWithTag(SearchTag.BASIC_DATA, route))
    }
    return filteredList
}

private fun searchInLocoList(
    route: Route,
    value: String,
): MutableList<RouteWithTag> {
    val filteredList = mutableListOf<RouteWithTag>()
    route.locomotives.forEach { loco ->

        val valueList = value.splitBySpaceAndComma()
        var respond = true

        valueList.forEach { s ->
            if (!loco.str().contains(s, ignoreCase = true)) {
                respond = false
            }
        }

        if (respond) {
            filteredList.addOrReplace(RouteWithTag(SearchTag.LOCO, route))
        }
    }
    return filteredList
}

private fun searchInTrainList(
    route: Route,
    value: String
): MutableList<RouteWithTag> {
    val filteredList = mutableListOf<RouteWithTag>()

    route.trains.forEach { train ->
        val valueList = value.splitBySpaceAndComma()
        var respond = true

        valueList.forEach { s ->
            if (!train.str().contains(s, ignoreCase = true)) {
                respond = false
            }
        }

        if (respond) {
            filteredList.addOrReplace(RouteWithTag(SearchTag.TRAIN, route))
        }
    }
    return filteredList
}

private fun searchInPassengerList(
    route: Route,
    value: String
): MutableList<RouteWithTag> {
    val filteredList = mutableListOf<RouteWithTag>()

    route.passengers.forEach { passenger ->

        val valueList = value.splitBySpaceAndComma()
        var respond = true

        valueList.forEach { s ->
            if (!passenger.str().contains(s, ignoreCase = true)) {
                respond = false
            }
        }

        if (respond) {
            filteredList.addOrReplace(RouteWithTag(SearchTag.PASSENGER, route))
        }
    }
    return filteredList
}

private fun searchInNotes(
    route: Route,
    value: String
): MutableList<RouteWithTag> {
    val filteredList = mutableListOf<RouteWithTag>()

    route.basicData.notes?.let { note ->

        val valueList = value.splitBySpaceAndComma()
        var respond = true

        valueList.forEach { s ->
            if (!note.contains(s, ignoreCase = true)) {
                respond = false
            }
        }

        if (respond) {
            filteredList.addOrSkip(RouteWithTag(SearchTag.NOTES, route))
        }
    }
    return filteredList
}