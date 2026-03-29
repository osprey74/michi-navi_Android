package com.osprey74.michinavi.model

data class PoiItem(
    val id: Long,
    val category: PoiCategory,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
)
