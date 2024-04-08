package com.z_company.work_manager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.ParseUser
import com.z_company.converter.BasicDataConverter
import com.z_company.data_remote.BASIC_DATA_INPUT

private const val basicDataNameClassRemote = "BasicData"
private const val numberFieldName = "number"
private const val timeStartWorkFieldName = "timeStartWork"
private const val timeEndWorkFieldName = "timeEndWork"
private const val restFieldName = "restPointOfTurnover"
private const val notesFieldName = "notes"
private const val userFieldName = "user"

class SaveBasicDataWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val value = inputData.getString(BASIC_DATA_INPUT)
        val basicData = BasicDataConverter.fromString(value!!)

        val basicDataObject = ParseObject(basicDataNameClassRemote)
        try {
            val userId = ParseUser.getCurrentUser()
            basicData.number?.let { number ->
                basicDataObject.put(numberFieldName, number)
            }
            basicData.timeStartWork?.let { time ->
                basicDataObject.put(timeStartWorkFieldName, time)
            }
            basicData.timeEndWork?.let { time ->
                basicDataObject.put(timeEndWorkFieldName, time)
            }
            basicDataObject.put(restFieldName, basicData.restPointOfTurnover)
            basicData.notes?.let { notes ->
                basicDataObject.put(notesFieldName, notes)
            }
            val relation: ParseRelation<ParseUser> = basicDataObject.getRelation(userFieldName)
            relation.add(userId)

            basicDataObject.saveInBackground()
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }
}