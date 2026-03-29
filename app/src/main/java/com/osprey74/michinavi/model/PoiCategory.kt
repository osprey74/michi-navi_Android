package com.osprey74.michinavi.model

enum class PoiCategory(
    val key: String,
    val label: String,
    val osmTag: String,
    val osmValue: String,
    val color: String,
) {
    GAS_STATION(
        key = "gas_station",
        label = "GS",
        osmTag = "amenity",
        osmValue = "fuel",
        color = "#FF9800",
    ),
    CONVENIENCE_STORE(
        key = "convenience",
        label = "コンビニ",
        osmTag = "shop",
        osmValue = "convenience",
        color = "#4CAF50",
    ),
    RESTAURANT(
        key = "restaurant",
        label = "レストラン",
        osmTag = "amenity",
        osmValue = "restaurant",
        color = "#F44336",
    ),
    PARKING(
        key = "parking",
        label = "駐車場",
        osmTag = "amenity",
        osmValue = "parking",
        color = "#2196F3",
    ),
    RV_PARK(
        key = "rv_park",
        label = "RVパーク",
        osmTag = "tourism",
        osmValue = "caravan_site",
        color = "#9C27B0",
    );
}
