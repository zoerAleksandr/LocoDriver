package com.z_company

import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.SaveCallback
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import ru.ok.tracer.crash.report.TracerCrashReport

object ParseHelper {
    /**
     * Saves a ParseObject to the database. If an object with the given unique key exists, it updates the existing object.
     *
     * @param className  The class name of the ParseObject.
     * @param uniqueKey  The key to query the existing object.
     * @param uniqueValue  The value of the unique key to identify the object.
     * @param fieldsToUpdate A hashmap with fields to be updated or added to the object.
     * @return A CompletableFuture that resolves with the saved or updated ParseObject, or an exception if an error occurs.
     */
    fun saveOrUpdateObjectAsync(
        className: String?,
        uniqueKey: String?,
        uniqueValue: String?,
        fieldsToUpdate: Map<String?, Any?>
    ): Flow<ResultState<String>> = channelFlow {
        trySend(ResultState.Loading)
        // Query for the object based on a unique attribute
        val query = ParseQuery.getQuery<ParseObject>(className!!)
        query.whereEqualTo(uniqueKey!!, uniqueValue)
        query.getFirstInBackground { parseObject: ParseObject?, e: ParseException? ->
            var parseObject = parseObject
            if (e != null && e.code != ParseException.OBJECT_NOT_FOUND) {
                // An error occurred that is not the object not being found
                trySend(ResultState.Error(ErrorEntity(e)))
                TracerCrashReport.log("Error get object fun ParseHelper.saveOrUpdateObjectAsync 43 $e")
                return@getFirstInBackground
            }

            // If object doesn't exist, create a new one
            if (parseObject == null) {
                parseObject = ParseObject(className)
//                parseObject.put(uniqueKey, uniqueValue!!)
                TracerCrashReport.log("Info fun ParseHelper.saveOrUpdateObjectAsync 50 Create New object")
            }

            // Update or add new fields
            for ((key, value) in fieldsToUpdate) {
                parseObject.put(key!!, value!!)
            }

            // Save the object
            parseObject.saveInBackground(SaveCallback { e2: ParseException? ->
                if (e2 == null) {
                    // Successfully saved or updated
                    TracerCrashReport.log("Info fun ParseHelper.saveOrUpdateObjectAsync 63 save Successfully ${parseObject.objectId}")
                    trySend(ResultState.Success(parseObject.objectId))
                } else {
                    // Save failed
                    TracerCrashReport.log("Error save object fun ParseHelper.saveOrUpdateObjectAsync 66 $e")
                    trySend(ResultState.Error(ErrorEntity(e)))
                }
            })
        }
        awaitClose()
    }
}