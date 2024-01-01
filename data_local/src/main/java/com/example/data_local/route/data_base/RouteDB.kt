package com.example.data_local.route.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_local.route.dao.RouteDao
import com.example.data_local.route.entity.BasicData
import com.example.data_local.route.entity.Locomotive
import com.example.data_local.route.entity.Notes
import com.example.data_local.route.entity.Passenger
import com.example.data_local.route.entity.Route
import com.example.data_local.route.entity.Train

@Database(
    entities = [
        BasicData::class,
        Locomotive::class,
        Train::class,
        Passenger::class,
        Notes::class
    ],
    version = 1,
    exportSchema = false
)
internal abstract class RouteDB : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}