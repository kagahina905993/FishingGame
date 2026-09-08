package com.example.fishinggame

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

fun loadWords(context: Context): Result<List<Word>> = runCatching {

    val jsonString = context.assets
        .open("words.json")
        .bufferedReader()
        .use { it.readText() }

    val jsonArray = JSONArray(jsonString)

    val words = mutableListOf<Word>()

    for (i in 0 until jsonArray.length()) {

        val obj = jsonArray.getJSONObject(i)

        words.add(
            Word(
                english = obj.getString("english"),
                japanese = obj.getString("japanese"),
                ngslRank = obj.nullableInt("ngslRank"),
                level = obj.getInt("level"),
                schoolGrade = obj.nullableSchoolGrade(),
                eikenLevel = obj.nullableEikenLevel(),
                partOfSpeech =
                    if (obj.isNull("partOfSpeech"))
                        null
                    else
                        obj.getString("partOfSpeech"),
                sfi = obj.nullableDouble("sfi"),
                frequencyPerMillion =
                    obj.nullableInt("frequencyPerMillion"),
                translationSource =
                    if (obj.isNull("translationSource"))
                        null
                    else
                        obj.getString("translationSource"),
                wordList = requireNotNull(
                    WordList.fromCode(obj.getString("wordList"))
                ) {
                    "未対応のwordListです: ${obj.getString("wordList")}"
                },
                sourceRank = obj.getInt("sourceRank"),
                estimatedCefrLevel =
                    obj.nullableString("estimatedCefrLevel"),
                eikenClassificationBasis =
                    obj.nullableString("eikenClassificationBasis"),
                quizMeaning = obj.nullableString("quizMeaning"),
                quizPartOfSpeech =
                    obj.nullableString("quizPartOfSpeech"),
                quizHint = obj.nullableString("quizHint"),
                quizSource = obj.nullableString("quizSource")
            )
        )
    }

    validateWordDataset(words)
}

private fun JSONObject.nullableString(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return getString(name).trim().takeIf { it.isNotEmpty() }
}

private fun JSONObject.nullableInt(name: String): Int? {
    if (!has(name) || isNull(name)) return null
    return getInt(name)
}

private fun JSONObject.nullableDouble(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return getDouble(name)
}

private fun JSONObject.nullableSchoolGrade(): SchoolGrade? {
    if (!has("schoolGrade") || isNull("schoolGrade")) return null
    val code = getInt("schoolGrade")
    return requireNotNull(SchoolGrade.fromCode(code)) {
        "未対応のschoolGradeです: $code"
    }
}

private fun JSONObject.nullableEikenLevel(): EikenLevel? {
    if (!has("eikenLevel") || isNull("eikenLevel")) return null
    val code = getString("eikenLevel")
    return requireNotNull(EikenLevel.fromCode(code)) {
        "未対応のeikenLevelです: $code"
    }
}
