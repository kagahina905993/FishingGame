package com.example.fishinggame

import android.content.Context

class TargetLevelStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        "player_goals",
        Context.MODE_PRIVATE
    )

    fun loadTargetEikenLevel(): EikenLevel? = preferences
        .getString(KEY_TARGET_EIKEN_LEVEL, null)
        ?.let(EikenLevel::fromCode)

    fun saveTargetEikenLevel(level: EikenLevel) {
        preferences.edit()
            .putString(KEY_TARGET_EIKEN_LEVEL, level.code)
            .apply()
    }

    private companion object {
        const val KEY_TARGET_EIKEN_LEVEL = "target_eiken_level"
    }
}
