package com.osprey74.michinavi.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val favoriteStationIds: Set<String> = emptySet(),
    val visitedStationIds: Set<String> = emptySet(),
    val favoriteSignIds: Set<String> = emptySet(),
    val visitedSignIds: Set<String> = emptySet(),
)
