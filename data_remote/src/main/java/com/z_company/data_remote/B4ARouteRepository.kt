package com.z_company.data_remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.z_company.converter.BasicDataConverter
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.repositories.RemoteRouteRepository
import com.z_company.work_manager.SaveBasicDataWorker

private const val SAVE_ROUTE_TAG = "SAVE_ROUTE_TAG"
const val BASIC_DATA_INPUT = "BASIC_DATA_INPUT"

class B4ARouteRepository(private val context: Context) : RemoteRouteRepository {
    override fun saveBasicData(basicData: BasicData) {
        val basicDataJSON = BasicDataConverter.toString(basicData)

        val inputData = Data.Builder()
            .putString(BASIC_DATA_INPUT, basicDataJSON)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<SaveBasicDataWorker>()
            .setInputData(inputData)
            .addTag(SAVE_ROUTE_TAG)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(worker)
    }
}