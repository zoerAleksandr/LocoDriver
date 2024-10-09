package com.z_company.domain.navigation

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train

interface Router {
    fun showSignIn()
    fun showLogIn()
    fun showRecoveryPassword()
    fun showHome()
    fun showRouteForm(basicId: String? = null)
    fun showRouteDetails(basicData: BasicData)
    fun showSettings()
    fun showSearch()
    fun back()
    fun navigationUp(): Boolean
    fun showChangedLocoForm(locomotive: Locomotive)
    fun showEmptyLocoForm(basicId: String)
    fun showChangeTrainForm(train: Train)
    fun showEmptyTrainForm(basicId: String)
    fun showChangePassengerForm(passenger: Passenger)
    fun showEmptyPassengerForm(basicId: String)
    fun showCameraScreen(basicId: String)
    fun showPreviewPhotoScreen(photo: String, basicId: String)
    fun showViewingImageScreen(imageId: String)
    fun showSelectReleaseDayScreen()
    fun showPurchasesScreen()
    fun showMoreInfo(monthOfYearId: String)
    fun showSalaryCalculation()
    fun showSettingSalary()
}
