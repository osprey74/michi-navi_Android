package com.osprey74.michinavi.model

data class AppSettings(
    val zoomPosition: String = "right",
    val mapTileType: String = "gsi_pale",
    val showGasStations: Boolean = true,
    val showFoodMarkets: Boolean = false,
    val showRestaurants: Boolean = false,
    val showParking: Boolean = false,
    val showRvParks: Boolean = true,
)
