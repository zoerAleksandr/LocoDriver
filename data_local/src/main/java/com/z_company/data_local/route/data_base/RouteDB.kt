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
@Database(
    entities = [
        BasicData::class,
        Locomotive::class,
        Train::class,
        Passenger::class,
        Photo::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ]
)
internal abstract class RouteDB : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}