package com.example.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data_local.route.type_converters.SectionDieselToPrimitiveConverters
import com.example.data_local.route.type_converters.SectionElectricToPrimitiveConverter
import com.example.data_local.route.type_converters.TypeLocoConverter
import com.example.domain.entities.route.LocoType
import org.jetbrains.annotations.NotNull

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = BasicData::class,
            parentColumns = ["id"],
            childColumns = ["baseId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]
)
@TypeConverters(
    TypeLocoConverter::class,
    SectionElectricToPrimitiveConverter::class,
    SectionDieselToPrimitiveConverters::class
)
internal data class Locomotive(
    @NotNull
    @PrimaryKey
    val locoId: String,
    @NotNull
    @ColumnInfo(index = true)
    val baseId: String,
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: List<SectionElectric> = listOf(),
    var dieselSectionList: List<SectionDiesel> = listOf(),
    var timeStartOfAcceptance: Long? = null,
    var timeEndOfAcceptance: Long? = null,
    var timeStartOfDelivery: Long? = null,
    var timeEndOfDelivery: Long? = null
)

@Entity
@TypeConverters(TypeLocoConverter::class)
internal data class SectionElectric(
    @PrimaryKey
    var sectionId: String,
    @ColumnInfo(index = true)
    var locoId: String,
    val type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Double? = null,
    var deliveryEnergy: Double? = null,
    var acceptedRecovery: Double? = null,
    var deliveryRecovery: Double? = null
)


@Entity
@TypeConverters(TypeLocoConverter::class)
internal data class SectionDiesel(
    @PrimaryKey
    var sectionId: String,
    @ColumnInfo(index = true)
    var locoId: String,
    val type: LocoType = LocoType.ELECTRIC,
    var acceptedFuel: Double?,
    var deliveryFuel: Double?,
    var coefficient: Double?,
    var fuelSupply: Double?,
    var coefficientSupply: Double?
)

