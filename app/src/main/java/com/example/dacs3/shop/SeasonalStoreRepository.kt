package com.example.dacs3.shop

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

object SeasonalStoreRepository {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
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

        return try {
            val json = loadJsonFromAssets(context)
            val response = jsonParser.decodeFromString<SeasonalShopResponseDto>(json)

            cachedStores = response.shops

            Log.d("SHOP_JSON", "Loaded shops: ${response.shops.size}")
            Log.d("SHOP_JSON", "Meta totalShops: ${response.meta.totalShops}")

            response.shops
        } catch (e: Exception) {
            Log.e("SHOP_JSON", "Failed to load generated.json: ${e.message}", e)
            emptyList()
        }
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