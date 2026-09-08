package com.example.fishinggame

import android.content.Context

class FishCollectionStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun loadFishCollection(): Map<String, FishCollectionRecord> =
        fishes.mapNotNull { fish ->
            val count = preferences.getInt(keyFor(fish.name), 0)
            if (count <= 0) return@mapNotNull null

            fish.name to FishCollectionRecord(
                caughtCount = count,
                largestSizeCm = preferences.getFloat(
                    largestSizeKeyFor(fish.name),
                    0f
                ),
                firstCaughtAtEpochMillis = preferences.getLong(
                    firstCaughtAtKeyFor(fish.name),
                    0L
                ),
                lastCaughtAtEpochMillis = preferences.getLong(
                    lastCaughtAtKeyFor(fish.name),
                    0L
                ),
                firstCaughtMapId = preferences.getString(
                    firstCaughtMapKeyFor(fish.name),
                    null
                ),
                firstCaughtPointId = preferences.getString(
                    firstCaughtPointKeyFor(fish.name),
                    null
                ),
                lastCaughtMapId = preferences.getString(
                    lastCaughtMapKeyFor(fish.name),
                    null
                ),
                lastCaughtPointId = preferences.getString(
                    lastCaughtPointKeyFor(fish.name),
                    null
                )
            )
        }.toMap()

    fun saveFishCollection(records: Map<String, FishCollectionRecord>) {
        preferences.edit().apply {
            fishes.forEach { fish ->
                val record = records[fish.name]
                putInt(keyFor(fish.name), record?.caughtCount ?: 0)
                putFloat(
                    largestSizeKeyFor(fish.name),
                    record?.largestSizeCm ?: 0f
                )
                putLong(
                    firstCaughtAtKeyFor(fish.name),
                    record?.firstCaughtAtEpochMillis ?: 0L
                )
                putLong(
                    lastCaughtAtKeyFor(fish.name),
                    record?.lastCaughtAtEpochMillis ?: 0L
                )
                putNullableString(
                    firstCaughtMapKeyFor(fish.name),
                    record?.firstCaughtMapId
                )
                putNullableString(
                    firstCaughtPointKeyFor(fish.name),
                    record?.firstCaughtPointId
                )
                putNullableString(
                    lastCaughtMapKeyFor(fish.name),
                    record?.lastCaughtMapId
                )
                putNullableString(
                    lastCaughtPointKeyFor(fish.name),
                    record?.lastCaughtPointId
                )
            }
        }.apply()
    }

    fun clearFishCollection() {
        preferences.edit().clear().apply()
    }

    private fun keyFor(fishName: String): String = "caught_$fishName"
    private fun largestSizeKeyFor(fishName: String): String =
        "largest_size_$fishName"
    private fun firstCaughtAtKeyFor(fishName: String): String =
        "first_caught_at_$fishName"
    private fun lastCaughtAtKeyFor(fishName: String): String =
        "last_caught_at_$fishName"
    private fun firstCaughtMapKeyFor(fishName: String): String =
        "first_caught_map_$fishName"
    private fun firstCaughtPointKeyFor(fishName: String): String =
        "first_caught_point_$fishName"
    private fun lastCaughtMapKeyFor(fishName: String): String =
        "last_caught_map_$fishName"
    private fun lastCaughtPointKeyFor(fishName: String): String =
        "last_caught_point_$fishName"

    private fun android.content.SharedPreferences.Editor.putNullableString(
        key: String,
        value: String?
    ) {
        if (value == null) remove(key) else putString(key, value)
    }

    private companion object {
        const val PREFERENCES_NAME = "fish_collection"
    }
}
