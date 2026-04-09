package com.osprey74.michinavi.model

import kotlinx.serialization.Serializable

// --- JSON デシリアライズ用中間モデル ---

@Serializable
data class Coordinate(
    val lat: Double,
    val lng: Double,
    val approximate: Boolean = false,
)

@Serializable
data class OfficeCoordinate(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class FlowerInfo(
    val name: String,
    val description: String? = null,
    val imageName: String? = null,
    val imageUrl: String? = null,
    val imageCredit: String? = null,
    val colorHex: String? = null,
    val colorVibrantHex: String? = null,
)

@Serializable
data class HokkaidoMunicipality(
    val code: String,
    val name: String,
    val nameKana: String,
    val prefectureCode: String? = null,
    val subprefecture: String,
    val subprefectureOffice: String,
    val municipalityType: String,
    val population: Int? = null,
    val populationYear: Int? = null,
    val areaSqKm: Double? = null,
    val centroid: Coordinate,
    val flower: FlowerInfo? = null,
    val tourismUrl: String? = null,
    val tourismSiteName: String? = null,
    val officeCoordinate: OfficeCoordinate? = null,
)

@Serializable
data class HokkaidoCountrySignRaw(
    val municipalityCode: String,
    val coordinate: Coordinate,
    val imageName: String? = null,
    val imageUrl: String? = null,
    val imageCredit: String? = null,
    val originText: String? = null,
    val designDescription: String? = null,
)

// --- 結合済みモデル ---

data class CountrySign(
    val id: String,                      // 市町村コード (JIS 5桁)
    // hokkaido_municipalities.json
    val name: String,
    val nameKana: String,
    val subprefecture: String,           // "石狩"
    val subprefectureOffice: String,     // "石狩振興局"
    val municipalityType: String,        // "市" / "町" / "村"
    val population: Int?,
    val populationYear: Int?,
    val areaSqKm: Double?,
    val centroidLat: Double,
    val centroidLon: Double,
    val flower: FlowerInfo?,
    val tourismUrl: String?,
    val tourismSiteName: String?,
    val officeLat: Double?,
    val officeLon: Double?,
    // hokkaido_country_signs.json
    val signLat: Double,
    val signLon: Double,
    val imageName: String?,
    val imageUrl: String?,
    val imageCredit: String?,
    val originText: String?,
    val designDescription: String?,
) {
    companion object {
        fun merge(municipality: HokkaidoMunicipality, sign: HokkaidoCountrySignRaw): CountrySign {
            return CountrySign(
                id = municipality.code,
                name = municipality.name,
                nameKana = municipality.nameKana,
                subprefecture = municipality.subprefecture,
                subprefectureOffice = municipality.subprefectureOffice,
                municipalityType = municipality.municipalityType,
                population = municipality.population,
                populationYear = municipality.populationYear,
                areaSqKm = municipality.areaSqKm,
                centroidLat = municipality.centroid.lat,
                centroidLon = municipality.centroid.lng,
                flower = municipality.flower,
                tourismUrl = municipality.tourismUrl,
                tourismSiteName = municipality.tourismSiteName,
                officeLat = municipality.officeCoordinate?.lat,
                officeLon = municipality.officeCoordinate?.lng,
                signLat = sign.coordinate.lat,
                signLon = sign.coordinate.lng,
                imageName = sign.imageName,
                imageUrl = sign.imageUrl,
                imageCredit = sign.imageCredit,
                originText = sign.originText,
                designDescription = sign.designDescription,
            )
        }
    }
}
