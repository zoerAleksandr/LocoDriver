package com.z_company.work_manager

import android.content.Context
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

object WorkManagerState {
    suspend fun stateTimeState(context: Context, workerId: UUID): Flow<ResultState<Long>> =
        flow {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workerId)
                .collect {
                    when (it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val currentTime = Calendar.getInstance().timeInMillis
                            emit(ResultState.Success(currentTime))
                        }

                        WorkInfo.State.RUNNING -> {
                            emit(ResultState.Loading)
                        }

                        WorkInfo.State.ENQUEUED -> {
                            emit(ResultState.Loading)
                        }

                        WorkInfo.State.BLOCKED -> {
                            emit(ResultState.Error(ErrorEntity(message = "Work $workerId State.BLOCKED")))
                        }

                        WorkInfo.State.FAILED -> {
                            emit(ResultState.Error(ErrorEntity(message = "Work $workerId State.FAILED")))
                        }

                        WorkInfo.State.CANCELLED -> {
                            emit(ResultState.Error(ErrorEntity(message = "Work $workerId State.CANCELLED")))
                        }
                    }
                }
        }

    suspend fun state(context: Context, workerId: UUID): Flow<ResultState<Data>> =
        flow {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workerId)
                .collect { workInfo ->
                    workInfo?.let {
                        when (it.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                emit(ResultState.Success(it.outputData))
                            }

                            WorkInfo.State.RUNNING -> {
                                emit(ResultState.Loading)
                            }

                            WorkInfo.State.ENQUEUED -> {
                                emit(ResultState.Loading)
                            }

                            WorkInfo.State.BLOCKED -> {
                                emit(ResultState.Error(ErrorEntity(message = "Work $workerId State.BLOCKED")))
                            }

                            WorkInfo.State.FAILED -> {
                                emit(ResultState.Error(ErrorEntity(message = "Work $workerId State.FAILED")))
                            }

                            WorkInfo.State.CANCELLED -> {
                                emit(ResultState.Error(ErrorEntity(message = "Work $workerId State.CANCELLED")))
                            }
                        }
                    }
                }
        }

    suspend fun listState(
        context: Context,
        worksId: List<UUID>,
        basicDataWorkId: UUID
    ): Flow<ResultState<String>> {
        val state: MutableStateFlow<ResultState<String>> = MutableStateFlow(ResultState.Loading)
        var basicDataObjectId = ""
        var countSuccess = 0

        worksId.forEach { uuid ->
            var job: Job? = null
            job = CoroutineScope(Dispatchers.IO).launch {
                state(context, uuid).collect { result ->
                    if (result is ResultState.Success) {
                        if (uuid == basicDataWorkId) {
                            result.data.getString(BASIC_DATA_OBJECT_ID_KEY)?.let { id ->
                                basicDataObjectId = id
                            }
                        }
                        countSuccess += 1
                        job?.cancel()
                    }
                }
            }
            job.join()
            if (countSuccess == worksId.size) {
                state.value = ResultState.Success(basicDataObjectId)
            }
        }
        return state
    }
}