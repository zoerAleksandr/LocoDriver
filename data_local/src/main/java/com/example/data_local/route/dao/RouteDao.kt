package com.example.data_local.route.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data_local.route.entity.BasicData
import com.example.data_local.route.entity.Locomotive
import com.example.data_local.route.entity.Notes
import com.example.data_local.route.entity.Passenger
import com.example.data_local.route.entity.Route
import com.example.data_local.route.entity.Train
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RouteDao {
    @Transaction
    suspend fun save(route: Route) {
        saveBasicData(route.basicData)
        route.locomotives.forEach { locomotive ->
            saveLocomotive(locomotive)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBasicData(basicData: BasicData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocomotive(locomotive: Locomotive)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrain(train: Train)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePassenger(passenger: Passenger)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNotes(notes: Notes)


    @Transaction
    suspend fun delete(route: Route) {
        deleteBasicData(route.basicData)
        route.locomotives.forEach { locomotive ->
            deleteLocomotives(locomotive)
        }
    }

    @Delete
    suspend fun deleteBasicData(basicData: BasicData)

    @Delete
    suspend fun deleteLocomotives(locomotive: Locomotive)

    @Transaction
    @Query("SELECT * FROM BasicData WHERE id = :id")
    fun getRouteById(id: String): Flow<Route?>


    @Transaction
    @Query("SELECT * FROM BasicData")
    fun getAllRoute(): Flow<List<Route>>

    @Transaction
    @Query("SELECT * FROM BasicData")
    fun getListItineraryByMonth(): Flow<List<Route>>
}