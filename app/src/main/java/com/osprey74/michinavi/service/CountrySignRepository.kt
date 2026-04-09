package com.osprey74.michinavi.service

import android.content.Context
import com.osprey74.michinavi.model.CountrySign
import com.osprey74.michinavi.model.HokkaidoCountrySignRaw
import com.osprey74.michinavi.model.HokkaidoMunicipality
import kotlinx.serialization.json.Json

class CountrySignRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val allSigns: List<CountrySign> by lazy {
        val municipalitiesJson = context.assets.open("hokkaido_municipalities.json")
            .bufferedReader().use { it.readText() }
        val municipalities = json.decodeFromString<List<HokkaidoMunicipality>>(municipalitiesJson)

        val signsJson = context.assets.open("hokkaido_country_signs.json")
            .bufferedReader().use { it.readText() }
        val signs = json.decodeFromString<List<HokkaidoCountrySignRaw>>(signsJson)
        val signsByCode = signs.associateBy { it.municipalityCode }

        municipalities.mapNotNull { muni ->
            signsByCode[muni.code]?.let { sign ->
                CountrySign.merge(muni, sign)
            }
        }.sortedWith(
            compareBy<CountrySign> { subprefectureSortOrder(it.subprefectureOffice) }
                .thenBy { it.name },
        )
    }

    companion object {
        val SUBPREFECTURE_ORDER = listOf(
            "石狩振興局", "空知総合振興局", "後志総合振興局", "胆振総合振興局",
            "日高振興局", "渡島総合振興局", "檜山振興局", "上川総合振興局",
            "留萌振興局", "宗谷総合振興局", "オホーツク総合振興局",
            "十勝総合振興局", "釧路総合振興局", "根室振興局",
        )

        private val subprefectureIndexMap =
            SUBPREFECTURE_ORDER.withIndex().associate { (i, v) -> v to i }

        fun subprefectureSortOrder(subprefectureOffice: String): Int =
            subprefectureIndexMap[subprefectureOffice] ?: Int.MAX_VALUE
    }
}
