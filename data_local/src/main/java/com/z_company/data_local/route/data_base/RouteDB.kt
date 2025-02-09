package com.z_company.data_local.route.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.z_company.data_local.route.dao.RouteDao
import com.z_company.data_local.route.entity.BasicData
import com.z_company.data_local.route.entity.Locomotive
import com.z_company.data_local.route.entity.Passenger
import com.z_company.data_local.route.entity.Photo
import com.z_company.data_local.route.entity.Train

/** version 2 add field distance in Train */
/** version 3 add field isHeavyLongDistance in Train */
/** version 4 add field schemaVersion in BasicData */

/** version 5 remove field schemaVersion in BasicData
/*            add field isSynchronizedRoute in BasicData */
 *            add field remoteRouteId in BasicData */

/** version 6 add field isOnePersonOperation in BasicData */

/** version 6 add field isOnePersonOperation in BasicData */
/** version 7 add field servicePhase in Train*/

@Database(
    entities = [
        BasicData::class,
        Locomotive::class,
        Train::class,
        Passenger::class,
        Photo::class
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = DeleteOldColumn::class),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7)
    ]
)
internal abstract class RouteDB : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}
@DeleteColumn(tableName = "BasicData", columnName = "schemaVersion")
class DeleteOldColumn : AutoMigrationSpec