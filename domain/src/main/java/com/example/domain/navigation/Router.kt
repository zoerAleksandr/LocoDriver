package com.example.domain.navigation

import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Locomotive
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
    fun showChangedLocoForm(locomotive: Locomotive)
    fun showEmptyLocoForm(basicId: String)
    fun showLocoDetails(locomotive: Locomotive)
    fun showChangeTrainForm(train: Train)
    fun showEmptyTrainForm(basicId: String)
    fun showTrainDetails(train: Train)
    fun showChangePassengerForm(passenger: Passenger)
    fun showEmptyPassengerForm(basicId: String)
    fun showPassengerDetails(passenger: Passenger)
    fun showNotesForm(basicId: String)
    fun showNotesDetails(basicId: String)
    fun showCameraScreen(notesId: String)
    fun showViewingPhotoScreen(photo: String)
}
