package com.example.fishinggame

import kotlin.random.Random

internal const val TARGET_WORDS_PER_MIX_BLOCK = 15
internal const val IMMEDIATE_LOWER_WORDS_PER_MIX_BLOCK = 3
internal const val FOUNDATION_WORDS_PER_MIX_BLOCK = 2

private val eikenLevelsLowToHigh = listOf(
    EikenLevel.GRADE_5,
    EikenLevel.GRADE_4,
    EikenLevel.GRADE_3,
    EikenLevel.GRADE_PRE_2,
    EikenLevel.GRADE_PRE_2_PLUS,
    EikenLevel.GRADE_2,
    EikenLevel.GRADE_PRE_1,
    EikenLevel.GRADE_1
)

internal fun buildTargetCourseWords(
    words: List<Word>,
    targetLevel: EikenLevel,
    urgentLearningItemIds: Set<String> = emptySet(),
    attemptedLearningItemIds: Set<String> = emptySet(),
    random: Random = Random.Default
): List<Word> {
    val targetIndex = eikenLevelsLowToHigh.indexOf(targetLevel)
    require(targetIndex >= 0) {
        "未対応の目標英検相当級です: " + targetLevel.code
    }

    val targetDeck = words
        .filter { it.eikenLevel == targetLevel }
        .shuffled(random)
    if (targetDeck.isEmpty() || targetIndex == 0) return targetDeck

    val immediateLowerLevel = eikenLevelsLowToHigh[targetIndex - 1]
    val immediateLowerCount = proportionalCount(
        targetCount = targetDeck.size,
        mixedCountPerBlock = IMMEDIATE_LOWER_WORDS_PER_MIX_BLOCK
    )
    val immediateLowerDeck = prioritizedDeck(
        words = words.filter { it.eikenLevel == immediateLowerLevel },
        urgentLearningItemIds = urgentLearningItemIds,
        attemptedLearningItemIds = attemptedLearningItemIds,
        random = random
    ).take(immediateLowerCount)

    val foundationCount = proportionalCount(
        targetCount = targetDeck.size,
        mixedCountPerBlock = FOUNDATION_WORDS_PER_MIX_BLOCK
    )
    val foundationDeck = takeBalancedFoundationWords(
        words = words,
        levels = eikenLevelsLowToHigh
            .subList(0, targetIndex - 1)
            .asReversed(),
        count = foundationCount,
        urgentLearningItemIds = urgentLearningItemIds,
        attemptedLearningItemIds = attemptedLearningItemIds,
        random = random
    )

    return interleaveCourseBlocks(
        targetWords = targetDeck,
        immediateLowerWords = immediateLowerDeck,
        foundationWords = foundationDeck,
        random = random
    )
}

private fun proportionalCount(
    targetCount: Int,
    mixedCountPerBlock: Int
): Int = (
    targetCount * mixedCountPerBlock +
        TARGET_WORDS_PER_MIX_BLOCK - 1
    ) / TARGET_WORDS_PER_MIX_BLOCK

private fun prioritizedDeck(
    words: List<Word>,
    urgentLearningItemIds: Set<String>,
    attemptedLearningItemIds: Set<String>,
    random: Random
): List<Word> = words
    .shuffled(random)
    .sortedBy { word ->
        learningPriority(
            word = word,
            urgentLearningItemIds = urgentLearningItemIds,
            attemptedLearningItemIds = attemptedLearningItemIds
        )
    }

private fun learningPriority(
    word: Word,
    urgentLearningItemIds: Set<String>,
    attemptedLearningItemIds: Set<String>
): Int = when (word.learningItemId()) {
    in urgentLearningItemIds -> 0
    !in attemptedLearningItemIds -> 1
    else -> 2
}

private fun takeBalancedFoundationWords(
    words: List<Word>,
    levels: List<EikenLevel>,
    count: Int,
    urgentLearningItemIds: Set<String>,
    attemptedLearningItemIds: Set<String>,
    random: Random
): List<Word> {
    if (count <= 0 || levels.isEmpty()) return emptyList()

    val selected = mutableListOf<Word>()

    for (priority in 0..2) {
        val decks = levels.map { level ->
            words.filter {
                it.eikenLevel == level &&
                    learningPriority(
                        word = it,
                        urgentLearningItemIds = urgentLearningItemIds,
                        attemptedLearningItemIds = attemptedLearningItemIds
                    ) == priority
            }.shuffled(random)
        }
        val positions = IntArray(decks.size)

        while (selected.size < count) {
            var addedInRound = false
            decks.indices.forEach { deckIndex ->
                if (selected.size >= count) return@forEach
                val position = positions[deckIndex]
                val word =
                    decks[deckIndex].getOrNull(position) ?: return@forEach
                selected += word
                positions[deckIndex] = position + 1
                addedInRound = true
            }
            if (!addedInRound) break
        }
        if (selected.size >= count) break
    }
    return selected
}

private fun interleaveCourseBlocks(
    targetWords: List<Word>,
    immediateLowerWords: List<Word>,
    foundationWords: List<Word>,
    random: Random
): List<Word> {
    var targetPosition = 0
    var immediateLowerPosition = 0
    var foundationPosition = 0
    val course = mutableListOf<Word>()

    while (
        targetPosition < targetWords.size ||
        immediateLowerPosition < immediateLowerWords.size ||
        foundationPosition < foundationWords.size
    ) {
        val block = mutableListOf<Word>()
        repeat(TARGET_WORDS_PER_MIX_BLOCK) {
            targetWords.getOrNull(targetPosition++)?.let(block::add)
        }
        repeat(IMMEDIATE_LOWER_WORDS_PER_MIX_BLOCK) {
            immediateLowerWords
                .getOrNull(immediateLowerPosition++)
                ?.let(block::add)
        }
        repeat(FOUNDATION_WORDS_PER_MIX_BLOCK) {
            foundationWords.getOrNull(foundationPosition++)?.let(block::add)
        }
        course += block.shuffled(random)
    }
    return course
}
