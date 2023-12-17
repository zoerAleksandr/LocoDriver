package com.example.data_local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data_local.entity.Route
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(route: Route)

    @Delete
    suspend fun delete(vararg route: Route)

    @Query("SELECT * FROM Route WHERE id = :id")
    fun getRouteById(id: String): Flow<Route?>


    @Query("SELECT * FROM Route")
    fun getAllRoute(): Flow<List<Route>>

    @Query("SELECT * FROM Route")
    fun getListItineraryByMonth(): Flow<List<Route>>
}