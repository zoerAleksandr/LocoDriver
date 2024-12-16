package com.z_company.data_local.route.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.z_company.data_local.route.entity.BasicData
import com.z_company.data_local.route.entity.Locomotive
import com.z_company.data_local.route.entity.Passenger
import com.z_company.data_local.route.entity.Photo
import com.z_company.data_local.route.entity.Route
import com.z_company.data_local.route.entity.Train
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
    @Query("SELECT * FROM BasicData WHERE isDeleted = 0")
    fun getAllRouteAsFlow(): Flow<List<Route>>
    @Transaction
    @Query("SELECT * FROM BasicData WHERE isDeleted = 0")
    fun getAllRoute(): List<Route>
    @Transaction
    @Query("SELECT * FROM BasicData")
    fun getAllRouteWithDeleting(): List<Route>
    @Transaction
    @Query("SELECT * FROM BasicData WHERE (timeStartWork <= :endPeriod AND timeEndWork >= :startPeriod AND isDeleted = 0) OR (timeStartWork BETWEEN :startPeriod AND :endPeriod AND isDeleted = 0)  ORDER BY timeStartWork desc")
    fun getAllRouteByPeriod(startPeriod: Long, endPeriod: Long): Flow<List<Route>>
    @Query("SELECT * FROM Locomotive WHERE locoId = :locoId")
    fun getLocoById(locoId: String): Flow<Locomotive?>
    @Query("SELECT * FROM Locomotive WHERE basicId = :basicId")
    fun getLocoListByBasicId(basicId: String): List<Locomotive>
    @Query("SELECT * FROM Train WHERE trainId = :trainId")
    fun getTrainById(trainId: String): Flow<Train?>
    @Query("SELECT * FROM Train WHERE basicId = :basicId")
    fun getTrainListByBasicId(basicId: String): List<Train>
    @Query("SELECT * FROM Passenger WHERE passengerId = :passengerId")
    fun getPassengerById(passengerId: String): Flow<Passenger?>
    @Query("SELECT * FROM Passenger WHERE basicId = :basicId")
    fun getPassengerListByBasicId(basicId: String): List<Passenger>
    @Query("SELECT * FROM Photo WHERE photoId = :photoId")
    fun getPhotoById(photoId: String): Flow<Photo?>
    @Query("SELECT * FROM Photo WHERE basicId = :basicId")
    fun getPhotosByRoute(basicId: String): Flow<List<Photo>>
    @Query("UPDATE BasicData SET remoteObjectId =:remoteObjectId WHERE id =:id")
    fun setRemoteObjectIdBasicData(id: String, remoteObjectId: String?)
    @Query("UPDATE Locomotive SET removeObjectId =:remoteObjectId WHERE locoId =:id")
    fun setRemoteObjectIdLocomotive(id: String, remoteObjectId: String)
    @Query("UPDATE Train SET remoteObjectId =:remoteObjectId WHERE trainId =:id")
    fun setRemoteObjectIdTrain(id: String, remoteObjectId: String)
    @Query("UPDATE Passenger SET remoteObjectId =:objectId WHERE passengerId =:passengerId")
    fun setRemoteObjectIdPassenger(passengerId: String, objectId: String)
    @Query("UPDATE Photo SET remoteObjectId =:objectId WHERE photoId =:photoId")
    fun setRemoteObjectIdPhoto(photoId: String, objectId: String)
    @Query("UPDATE BasicData SET isSynchronizedRoute = 1 WHERE id =:id")
    fun setSynchronizedRoute(id: String)
//
//    @Query("UPDATE BasicData SET schemaVersion =:version WHERE id =:id")
//    fun setSchemaVersion(version: Int, id: String)
    @Query("DELETE FROM BasicData")
    suspend fun clearRepository()
    @Query("UPDATE BasicData SET remoteRouteId =:remoteObjectId WHERE id =:basicId")
    fun setRemoteObjectIdRoute(basicId: String, remoteObjectId: String?)
}