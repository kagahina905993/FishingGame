package com.example.fishinggame

import org.junit.Assert.assertTrue
import org.junit.Test

class FishBalanceTest {
    private val representativeDamage = 16

    @Test
    fun fishSettings_haveValidRanges() {
        fishes.forEach { fish ->
            assertTrue("${fish.name}のHP範囲", fish.minHp in 1..fish.maxHp)
            assertTrue(
                "${fish.name}の距離範囲",
                fish.minDistance > 0f &&
                    fish.minDistance <= fish.maxDistance
            )
            assertTrue(
                "${fish.name}のサイズ範囲",
                fish.minSizeCm > 0f && fish.minSizeCm <= fish.maxSizeCm
            )
        }
    }

    @Test
    fun fishHp_matchesDesignedAnswerCounts() {
        val targetAnswerCountsByRarity = mapOf(
            FishRarity.COMMON to 3..5,
            FishRarity.UNCOMMON to 5..8,
            FishRarity.RARE to 7..11,
            FishRarity.EPIC to 11..17,
            FishRarity.LEGENDARY to 15..20
        )
        val targetAnswerCountsByFish = mapOf(
            "イワシ" to 2..3,
            "アジ" to 4..5,
            "マグロ" to 10..14
        )

        fishes.forEach { fish ->
            val target = targetAnswerCountsByFish[fish.name]
                ?: requireNotNull(targetAnswerCountsByRarity[fish.rarity])
            val minimumAnswers = answersToCatch(fish.minHp)
            val maximumAnswers = answersToCatch(fish.maxHp)

            assertTrue(
                "${fish.name}の最小想定問題数：$minimumAnswers",
                minimumAnswers in target
            )
            assertTrue(
                "${fish.name}の最大想定問題数：$maximumAnswers",
                maximumAnswers in target
            )
        }
    }

    @Test
    fun raritySpawnWeights_decreaseAsRarityIncreases() {
        val weights = FishRarity.entries.map { it.spawnWeight }
        assertTrue(weights.zipWithNext().all { (left, right) -> left > right })
    }

    private fun answersToCatch(hp: Int): Int =
        (hp + representativeDamage - 1) / representativeDamage
}
