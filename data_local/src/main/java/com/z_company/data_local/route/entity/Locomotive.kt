package com.z_company.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.route.type_converters.SectionDieselToPrimitiveConverters
import com.z_company.data_local.route.type_converters.SectionElectricToPrimitiveConverter
import com.z_company.data_local.route.type_converters.TypeLocoConverter
import com.z_company.domain.entities.route.LocoType
import java.math.BigDecimal

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = BasicData::class,
            parentColumns = ["id"],
            childColumns = ["basicId"],
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
    @PrimaryKey
    val locoId: String,
    @ColumnInfo(index = true)
    var basicId: String,
    var removeObjectId: String,
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: List<SectionElectric> = emptyList(),
    var dieselSectionList: List<SectionDiesel> = emptyList(),
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
    var acceptedEnergy: BigDecimal? = null,
    var deliveryEnergy: BigDecimal? = null,
    var acceptedRecovery: BigDecimal? = null,
    var deliveryRecovery: BigDecimal? = null
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

