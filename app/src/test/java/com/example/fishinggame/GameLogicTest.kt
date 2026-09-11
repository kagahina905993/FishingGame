package com.example.fishinggame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {
    private val word = Word(
        english = "fish",
        japanese = "魚",
        ngslRank = 1,
        level = 1,
        schoolGrade = null,
        eikenLevel = null,
        partOfSpeech = null,
        sfi = 1.0,
        frequencyPerMillion = 1,
        translationSource = "test",
        quizMeaning = "魚",
        quizPartOfSpeech = "名詞",
        quizSource = "test"
    )

    @Test
    fun schoolGrade_convertsSupportedCodesAndRejectsUnknownCodes() {
        assertEquals(
            SchoolGrade.ELEMENTARY_5,
            SchoolGrade.fromCode(5)
        )
        assertEquals(
            SchoolGrade.HIGH_SCHOOL_1,
            SchoolGrade.fromCode(10)
        )
        assertEquals(null, SchoolGrade.fromCode(4))
        assertEquals(null, SchoolGrade.fromCode(13))
    }

    @Test
    fun eikenLevel_convertsSupportedCodesAndRejectsUnknownCodes() {
        assertEquals(EikenLevel.GRADE_5, EikenLevel.fromCode("5"))
        assertEquals(
            EikenLevel.GRADE_PRE_2_PLUS,
            EikenLevel.fromCode("pre2plus")
        )
        assertEquals(null, EikenLevel.fromCode("unknown"))
    }

    @Test
    fun wordId_isStableAcrossDisplayTextChangesAndSourceQualified() {
        val ngsl = word.copy(sourceRank = 12)
        val renamedMeaning = ngsl.copy(
            japanese = "魚類",
            quizMeaning = "魚類"
        )
        val nawl = ngsl.copy(
            ngslRank = null,
            wordList = WordList.NAWL_1_2
        )
        val extended = ngsl.copy(
            ngslRank = null,
            wordList = WordList.OPEN_VOCAB_EXTENDED_V1
        )

        assertEquals("ngsl_1_2:0012", ngsl.wordId)
        assertEquals(ngsl.wordId, renamedMeaning.wordId)
        assertEquals("nawl_1_2:0012", nawl.wordId)
        assertEquals("open_vocab_extended_v1:0012", extended.wordId)
        assertEquals(
            "ngsl_1_2:0012:ja-en",
            ngsl.learningItemId()
        )
    }

    @Test
    fun validateWordDataset_acceptsValidSourcesAndRejectsDuplicates() {
        val ngslWord = word.copy(
            schoolGrade = SchoolGrade.ELEMENTARY_5,
            eikenLevel = EikenLevel.GRADE_5
        )
        val nawlWord = word.copy(
            english = "algorithm",
            ngslRank = null,
            schoolGrade = SchoolGrade.HIGH_SCHOOL_2,
            eikenLevel = EikenLevel.GRADE_PRE_2_PLUS,
            wordList = WordList.NAWL_1_2,
            sourceRank = 1,
            quizMeaning = "アルゴリズム"
        )
        val extendedWord = word.copy(
            english = "harbor",
            ngslRank = null,
            schoolGrade = SchoolGrade.HIGH_SCHOOL_3,
            eikenLevel = EikenLevel.GRADE_PRE_1,
            wordList = WordList.OPEN_VOCAB_EXTENDED_V1,
            sourceRank = 1,
            quizMeaning = "港"
        )

        assertEquals(
            listOf(ngslWord, nawlWord, extendedWord),
            validateWordDataset(listOf(ngslWord, nawlWord, extendedWord))
        )
        assertThrowsIllegalArgument {
            validateWordDataset(listOf(ngslWord, ngslWord))
        }
        assertThrowsIllegalArgument {
            validateWordDataset(
                listOf(
                    ngslWord,
                    nawlWord.copy(quizMeaning = ngslWord.quizMeaning)
                )
            )
        }
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("IllegalArgumentException was not thrown")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun rotationSteps_preservesClockwiseAlphabetRules() {
        assertEquals(26, rotationSteps('A', 'A'))
        assertEquals(1, rotationSteps('A', 'B'))
        assertEquals(1, rotationSteps('Z', 'A'))
    }

    @Test
    fun calculateRotation_startsAtAAndMovesFromPreviousLetter() {
        assertEquals(listOf(5, 3, 10, 15), calculateRotation("fish"))
    }

    @Test
    fun isCorrectAnswer_ignoresCaseAndWhitespace() {
        assertTrue(isCorrectAnswer(" F I S H ", word))
        assertFalse(isCorrectAnswer("dish", word))
    }

    @Test
    fun calculateTimeBonus_decreasesWithoutGoingBelowZero() {
        assertEquals(30, calculateTimeBonus(0))
        assertEquals(20, calculateTimeBonus(10))
        assertEquals(0, calculateTimeBonus(30))
        assertEquals(0, calculateTimeBonus(100))
    }

    @Test
    fun timeTension_startsAfterGraceAndIncreasesEveryFiveSeconds() {
        assertEquals(0, calculateTimeTensionSteps(10))
        assertEquals(0, calculateTimeTensionSteps(14))
        assertEquals(1, calculateTimeTensionSteps(15))
        assertEquals(2, calculateTimeTensionSteps(20))
    }

    @Test
    fun lineTension_isClampedAtMaximum() {
        assertEquals(100, increaseLineTension(95, 15))
        assertEquals(30, increaseLineTension(15, 15))
    }

    @Test
    fun registerWrongAnswer_detectsSameNormalizedAnswer() {
        val first = registerWrongAnswer(emptySet(), "Boat")
        val duplicate = registerWrongAnswer(first.answers, " B O A T ")

        assertFalse(first.isDuplicate)
        assertTrue(duplicate.isDuplicate)
        assertEquals(setOf("boat"), duplicate.answers)
    }

    @Test
    fun reelFish_preservesHpAndDistanceFormula() {
        val fish = Fish("テスト魚", 100, 100, 10f, 10f)
        val state = FishState(fish, 100, 100, 10f, 10f)

        val result = reelFish(state, totalRotation = 50)

        assertEquals(90, result.currentHp)
        assertEquals(9f, result.currentDistance, 0.0001f)
    }

    @Test
    fun reelFish_hasMinimumDamageAndNeverDropsBelowZero() {
        val fish = Fish("テスト魚", 2, 2, 10f, 10f)
        val state = FishState(fish, 2, 2, 10f, 10f)

        val first = reelFish(state, totalRotation = 0)
        val caught = reelFish(first, totalRotation = 100)

        assertEquals(1, first.currentHp)
        assertEquals(5f, first.currentDistance, 0.0001f)
        assertEquals(0, caught.currentHp)
        assertEquals(0f, caught.currentDistance, 0.0001f)
        assertEquals(0.5f, first.distanceProgress, 0.0001f)
        assertEquals(0f, caught.distanceProgress, 0.0001f)
    }

    @Test
    fun filterWordsByLevel_returnsOnlySelectedLevel() {
        val level2Word = word.copy(
            english = "boat",
            level = 2
        )
        val level3Word = word.copy(
            english = "water",
            level = 3
        )

        val result = filterWordsByLevel(
            words = listOf(word, level2Word, level3Word),
            selectedLevel = 2
        )

        assertEquals(listOf(level2Word), result)
    }

    @Test
    fun filterWordsBySchoolGrade_returnsOnlySelectedGrade() {
        val elementaryWord = word.copy(
            schoolGrade = SchoolGrade.ELEMENTARY_5
        )
        val juniorHighWord = word.copy(
            english = "boat",
            schoolGrade = SchoolGrade.JUNIOR_HIGH_1
        )

        val result = filterWordsBySchoolGrade(
            words = listOf(elementaryWord, juniorHighWord),
            schoolGrade = SchoolGrade.JUNIOR_HIGH_1
        )

        assertEquals(listOf(juniorHighWord), result)
    }

    @Test
    fun filterWordsByEikenLevel_returnsOnlySelectedLevel() {
        val grade5Word = word.copy(eikenLevel = EikenLevel.GRADE_5)
        val grade4Word = word.copy(
            english = "boat",
            eikenLevel = EikenLevel.GRADE_4
        )

        val result = filterWordsByEikenLevel(
            words = listOf(grade5Word, grade4Word),
            eikenLevel = EikenLevel.GRADE_4
        )

        assertEquals(listOf(grade4Word), result)
    }

    @Test
    fun advanceQuestion_movesToNextWordWithoutChangingOrder() {
        val words = listOf(
            word,
            word.copy(english = "boat"),
            word.copy(english = "water")
        )

        val result = advanceQuestion(
            words = words,
            currentIndex = 0
        )

        assertEquals(words, result.words)
        assertEquals(1, result.currentIndex)
    }

    @Test
    fun advanceQuestion_reshufflesAfterRoundWithoutImmediateDuplicate() {
        val words = listOf(
            word,
            word.copy(english = "boat"),
            word.copy(english = "water")
        )
        val previousWord = words.last()

        val result = advanceQuestion(
            words = words,
            currentIndex = words.lastIndex,
            random = Random(0)
        )

        assertEquals(0, result.currentIndex)
        assertEquals(words.toSet(), result.words.toSet())
        assertEquals(words.size, result.words.size)
        assertFalse(result.words.first() == previousWord)
    }

    @Test
    fun recordFishCatch_updatesCountLargestSizeAndDates() {
        val fish = Fish("アジ", 100, 100, 10f, 10f)
        val previous = FishCollectionRecord(
            caughtCount = 2,
            largestSizeCm = 30f,
            firstCaughtAtEpochMillis = 1000L,
            lastCaughtAtEpochMillis = 2000L
        )
        val caughtFish = FishState(
            fish = fish,
            startHp = 100,
            currentHp = 0,
            startDistance = 10f,
            currentDistance = 0f,
            sizeCm = 35.5f
        )

        val result = recordFishCatch(
            records = mapOf("アジ" to previous),
            fishState = caughtFish,
            caughtAtEpochMillis = 3000L,
            mapId = FOOTHILL_STREAM_MAP_ID,
            pointId = SUNLIT_SHALLOWS_POINT_ID
        )

        assertEquals(3, result.records["アジ"]?.caughtCount)
        assertEquals(
            35.5f,
            result.records["アジ"]?.largestSizeCm ?: 0f,
            0.001f
        )
        assertEquals(
            1000L,
            result.records["アジ"]?.firstCaughtAtEpochMillis
        )
        assertEquals(
            3000L,
            result.records["アジ"]?.lastCaughtAtEpochMillis
        )
        assertFalse(result.isNewDiscovery)
        assertTrue(result.isNewLargestSize)
        assertEquals(
            FOOTHILL_STREAM_MAP_ID,
            result.records["アジ"]?.firstCaughtMapId
        )
        assertEquals(
            SUNLIT_SHALLOWS_POINT_ID,
            result.records["アジ"]?.lastCaughtPointId
        )
    }

    @Test
    fun createFishState_generatesSizeInsideFishRange() {
        val fish = Fish(
            name = "テスト魚",
            minHp = 100,
            maxHp = 100,
            minDistance = 10f,
            maxDistance = 10f,
            minSizeCm = 25f,
            maxSizeCm = 30f
        )

        val result = createFishState(fish, Random(1))

        assertTrue(result.sizeCm in 25f..30f)
    }

    @Test
    fun largeSpecimen_usesTopTwentyPercentOfEachFishSizeRange() {
        val fish = Fish(
            name = "大物判定魚",
            minHp = 10,
            maxHp = 10,
            minDistance = 1f,
            maxDistance = 1f,
            minSizeCm = 20f,
            maxSizeCm = 70f
        )

        assertFalse(
            FishState(fish, 10, 0, 1f, 0f, sizeCm = 59.9f)
                .isLargeSpecimen()
        )
        assertTrue(
            FishState(fish, 10, 0, 1f, 0f, sizeCm = 60f)
                .isLargeSpecimen()
        )
    }

    @Test
    fun recordFishCatch_marksFirstCatchAsDiscoveryAndSizeRecord() {
        val fish = Fish("新しい魚", 100, 100, 10f, 10f)
        val caughtFish = FishState(
            fish = fish,
            startHp = 100,
            currentHp = 0,
            startDistance = 10f,
            currentDistance = 0f,
            sizeCm = 18f
        )

        val result = recordFishCatch(
            records = emptyMap(),
            fishState = caughtFish,
            caughtAtEpochMillis = 1000L
        )

        assertTrue(result.isNewDiscovery)
        assertTrue(result.isNewLargestSize)
        assertEquals(1, result.records["新しい魚"]?.caughtCount)
    }

    @Test
    fun createIncorrectAnswer_neverMatchesCorrectAnswer() {
        assertFalse(
            createIncorrectAnswer("fish").equals(
                "fish",
                ignoreCase = true
            )
        )
        assertFalse(
            createIncorrectAnswer("wrong").equals(
                "wrong",
                ignoreCase = true
            )
        )
    }
}
