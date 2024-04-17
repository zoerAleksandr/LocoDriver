package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

object WorkManagerState {
    suspend fun state(context: Context, workerId: UUID): Flow<ResultState<Data>> =
        flow {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workerId)
                .collect {
                    when (it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            it.outputData
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