package com.example.data_local.route.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data_local.route.entity.pre_save.PreLocomotive
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PreSaveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocomotive(preLocomotive: PreLocomotive)

    @Query("SELECT * FROM PreLocomotive WHERE locoId = :locoId")
    fun getPreLocomotive(locoId: String): Flow<PreLocomotive?>

    @Query("SELECT * FROM PreLocomotive")
    fun getAllLocomotives(): Flow<List<PreLocomotive>>
    @Delete
    suspend fun deleteLocomotives(preLocomotive: PreLocomotive)
    @Query("DELETE FROM PreLocomotive")
    suspend fun clearRepository()
}