package com.z_company.data_local.route.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.route.dao.ResponseDao
import com.z_company.data_local.route.entity.SearchResponse

@Database(
    entities = [
        SearchResponse::class
    ],
    version = 1,
    exportSchema = false
)
internal abstract class SearchResponseDB: RoomDatabase() {
    abstract fun responseDao(): ResponseDao
}