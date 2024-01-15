package com.example.domain.navigation

import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train

interface Router {
    fun showLogin()
    fun showHome()
    fun showRouteForm(route: Route? = null)
    fun showRouteDetails(basicData: BasicData)
    fun showSettings()
    fun showSearch()
    fun back()
    fun navigationUp(): Boolean
    fun showLocoForm(locomotive: Locomotive?)
    fun showLocoDetails(locomotive: Locomotive)
    fun showTrainForm(train: Train?)
    fun showTrainDetails(train: Train)
    fun showPassengerForm(passenger: Passenger?)
    fun showPassengerDetails(passenger: Passenger)
    fun showNotesForm(notes: Notes?)
    fun showNotesDetails(notes: Notes)
}
