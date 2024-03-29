package com.example.domain.navigation

import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Train

interface Router {
    fun showLogin()
    fun showHome()
    fun showRouteForm(basicId: String? = null)
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
    fun showCameraScreen(basicId: String)
    fun showPreviewPhotoScreen(photo: String, basicId: String)
    fun showViewingImageScreen(imageId: String)
}
