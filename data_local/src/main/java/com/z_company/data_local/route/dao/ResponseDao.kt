package com.z_company.data_local.route.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z_company.data_local.route.entity.SearchResponse
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ResponseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(response: SearchResponse)

    @Delete
    fun remove(response: SearchResponse)

    @Query("SELECT * FROM SearchResponse")
    fun getAllResponse(): Flow<List<SearchResponse>>
}