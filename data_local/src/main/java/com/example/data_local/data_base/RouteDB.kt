package com.example.data_local.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_local.dao.RouteDao
import com.example.data_local.entity.Route

@Database(
    entities = [
        Route::class
    ],
    version = 1,
    exportSchema = false
)
internal abstract class RouteDB : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}