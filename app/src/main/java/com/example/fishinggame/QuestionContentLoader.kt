package com.example.fishinggame

import android.content.Context
import org.json.JSONArray

fun loadSentenceQuestions(
    context: Context,
    words: List<Word>
): Result<List<SentenceQuestion>> = runCatching {
    val json = context.assets
        .open("question_content.json")
        .bufferedReader()
        .use { it.readText() }
    val array = JSONArray(json)
    val questions = buildList {
        repeat(array.length()) { index ->
            val item = array.getJSONObject(index)
            val distractors = item.getJSONArray("distractorWordIds")
            add(
                SentenceQuestion(
                    questionId = item.getString("questionId"),
                    wordId = item.getString("wordId"),
                    sentence = item.getString("sentence"),
                    japaneseSentence = item.getString("japaneseSentence"),
                    answer = item.getString("answer"),
                    distractorWordIds = buildList {
                        repeat(distractors.length()) { distractorIndex ->
                            add(distractors.getString(distractorIndex))
                        }
                    },
                    explanation = item.getString("explanation"),
                    source = item.getString("source"),
                    license = item.getString("license"),
                    sourceUrl = if (
                        item.has("sourceUrl") && !item.isNull("sourceUrl")
                    ) {
                        item.getString("sourceUrl")
                    } else {
                        null
                    }
                )
            )
        }
    }
    validateSentenceQuestions(questions, words)
}
