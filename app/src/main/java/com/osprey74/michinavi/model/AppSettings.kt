package com.osprey74.michinavi.model

data class AppSettings(
    val zoomPosition: String = "right",
    val mapTileType: String = "gsi_pale",
    val googleMapsApiKey: String = "",
)
