package com.z_company.data_local.route.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.route.dao.RouteDao
import com.z_company.data_local.route.entity.BasicData
import com.z_company.data_local.route.entity.Locomotive
import com.z_company.data_local.route.entity.Passenger
import com.z_company.data_local.route.entity.Photo
import com.z_company.data_local.route.entity.Train

/** version 2 add field distance in Train */
/** version 3 add field isHeavyLongDistance in Train */
/** version 4 add field schemaVersion in BasicData */
@Database(
    entities = [
        BasicData::class,
        Locomotive::class,
        Train::class,
        Passenger::class,
        Photo::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]
)
internal abstract class RouteDB : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}