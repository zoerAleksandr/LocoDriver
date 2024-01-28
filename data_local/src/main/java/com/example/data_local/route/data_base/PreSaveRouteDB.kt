package com.example.data_local.route.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_local.route.dao.PreSaveDao
import com.example.data_local.route.entity.pre_save.PreLocomotive

@Database(
    entities = [
        PreLocomotive::class,
    ],
    version = 1,
    exportSchema = false
)
internal abstract class PreSaveRouteDB: RoomDatabase() {
    abstract fun preSaveDao() : PreSaveDao
}