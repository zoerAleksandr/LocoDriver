package com.example.data_local.route.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data_local.route.entity.BasicData
import com.example.data_local.route.entity.Locomotive
import com.example.data_local.route.entity.Passenger
import com.example.data_local.route.entity.Photo
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
        route.trains.forEach { train ->
            saveTrain(train)
        }
        route.passengers.forEach { passenger ->
            savePassenger(passenger)
        }
        route.photos.forEach { photo ->
            savePhoto(photo)
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
    suspend fun savePhoto(photo: Photo)
    @Delete
    suspend fun deletePhoto(photo: Photo)
    @Transaction
    suspend fun delete(route: Route) {
        deleteBasicData(route.basicData)
    }
    @Delete
    suspend fun deleteBasicData(basicData: BasicData)
    @Delete
    suspend fun deleteLocomotives(locomotive: Locomotive)
    @Delete
    suspend fun deleteTrain(train: Train)
    @Delete
    suspend fun deletePassenger(passenger: Passenger)
    @Transaction
    @Query("SELECT * FROM BasicData WHERE id = :id")
    fun getRouteById(id: String): Flow<Route?>
    @Transaction
    @Query("SELECT * FROM BasicData")
    fun getAllRoute(): Flow<List<Route>>
    @Transaction
    @Query("SELECT * FROM BasicData")
    fun getListItineraryByMonth(): Flow<List<Route>>
    @Query("SELECT * FROM Locomotive WHERE locoId = :locoId")
    fun getLocoById(locoId: String): Flow<Locomotive?>
    @Query("SELECT * FROM Train WHERE trainId = :trainId")
    fun getTrainById(trainId: String): Flow<Train?>
    @Query("SELECT * FROM Passenger WHERE passengerId = :passengerId")
    fun getPassengerById(passengerId: String): Flow<Passenger?>
    @Query("SELECT * FROM Photo WHERE photoId = :photoId")
    fun getPhotoById(photoId: String): Flow<Photo?>
    @Query("SELECT * FROM Photo WHERE basicId = :basicId")
    fun getPhotosByRoute(basicId: String): Flow<List<Photo>>
}