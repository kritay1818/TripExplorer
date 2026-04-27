package com.example.tripexplorer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.data.remote.PlaceProperties

@Entity(tableName = "favorite_places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val xid: String,
    val name: String,
    val imageUrl: String?,
    val userNote: String?,
    val lat: Double,
    val lon: Double
)

fun PlaceEntity.toPlaceFeature() = PlaceFeature(
    PlaceProperties(
        xid = xid,
        name = name,
        kinds = "",
        rate = null
    )
)
