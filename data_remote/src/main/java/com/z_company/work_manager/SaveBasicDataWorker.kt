package com.z_company.work_manager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.ParseUser
import com.z_company.converter.BasicDataConverter
import com.z_company.data_remote.BASIC_DATA_INPUT
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_NAME_CLASS_REMOTE
import com.z_company.work_manager.BasicDataFieldName.UID_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.NUMBER_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.TIME_START_WORK_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.TIME_END_WORK_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.REST_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.NOTES_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.USER_FIELD_NAME

class SaveBasicDataWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val value = inputData.getString(BASIC_DATA_INPUT)
        val basicData = BasicDataConverter.fromString(value!!)

        val basicDataObject = ParseObject(BASIC_DATA_NAME_CLASS_REMOTE)
        try {
            val userId = ParseUser.getCurrentUser()
            basicDataObject.put(UID_FIELD_NAME, basicData.id)
            basicData.number?.let { number ->
                basicDataObject.put(NUMBER_FIELD_NAME, number)
            }
            basicData.timeStartWork?.let { time ->
                basicDataObject.put(TIME_START_WORK_FIELD_NAME, time)
            }
            basicData.timeEndWork?.let { time ->
                basicDataObject.put(TIME_END_WORK_FIELD_NAME, time)
            }
            basicDataObject.put(REST_FIELD_NAME, basicData.restPointOfTurnover)
            basicData.notes?.let { notes ->
                basicDataObject.put(NOTES_FIELD_NAME, notes)
            }
            val relation: ParseRelation<ParseUser> = basicDataObject.getRelation(USER_FIELD_NAME)
            relation.add(userId)

            basicDataObject.saveInBackground()
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }
}