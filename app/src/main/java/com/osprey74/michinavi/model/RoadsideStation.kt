package com.osprey74.michinavi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoadsideStation(
    val id: String,
    val name: String,
    val prefecture: String? = null,
    val municipality: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("road_name")
    val roadName: String? = null,
    val features: List<String> = emptyList(),
    val url: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
)
