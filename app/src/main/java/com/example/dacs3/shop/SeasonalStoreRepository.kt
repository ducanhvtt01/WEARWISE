package com.example.dacs3.shop

import android.content.Context
import kotlinx.serialization.json.Json

object SeasonalStoreRepository {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    private var cachedStores: List<SeasonalStoreDto>? = null

    private fun loadJsonFromAssets(context: Context): String {
        return context.assets.open("generated.json")
            .bufferedReader()
            .use { it.readText() }
    }

    fun getAllStores(context: Context): List<SeasonalStoreDto> {
        cachedStores?.let {
            return it
        }

        val json = loadJsonFromAssets(context)
        val response = jsonParser.decodeFromString<SeasonalShopResponseDto>(json)

        cachedStores = response.shops

        return response.shops
    }

    fun getStoresBySeason(
        context: Context,
        season: String,
        provinceCode: String = "DN"
    ): List<SeasonalStoreDto> {
        val normalizedSeason = season.lowercase()

        return getAllStores(context).filter { store ->
            val matchProvince = store.provinceCode.equals(
                provinceCode,
                ignoreCase = true
            )

            val matchSeason =
                store.seasons.any { it.equals(normalizedSeason, ignoreCase = true) } ||
                        store.products.any {
                            it.season.equals(normalizedSeason, ignoreCase = true)
                        }

            matchProvince && matchSeason
        }
    }

    fun getProductsBySeason(
        store: SeasonalStoreDto,
        season: String
    ): List<SeasonalProductDto> {
        val normalizedSeason = season.lowercase()

        return store.products.filter {
            it.season.equals(normalizedSeason, ignoreCase = true)
        }
    }
}