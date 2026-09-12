package com.example.fishinggame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FishingAreaTest {
    @Test
    fun foothillStream_containsSunlitShallows() {
        val map = requireNotNull(findFishingMap(FOOTHILL_STREAM_MAP_ID))
        val points = pointsForMap(map.id)

        assertEquals(listOf(SUNLIT_SHALLOWS_POINT_ID), map.pointIds)
        assertEquals(1, points.size)
        assertEquals("木漏れ日の浅瀬", points.single().name)
    }

    @Test
    fun sunlitShallows_spawnsOnlyConfiguredFish() {
        val availableFish = fishesForPoint(SUNLIT_SHALLOWS_POINT_ID)
        val configuredFishNames = setOf(
            "カワムツ",
            "オイカワ",
            "タカハヤ",
            "鯉",
            "錦鯉"
        )
        val selectedFishNames = mutableSetOf<String>()

        assertEquals(configuredFishNames, availableFish.map { it.name }.toSet())
        repeat(100) { seed ->
            val selected = selectRandomFishForPoint(
                pointId = SUNLIT_SHALLOWS_POINT_ID,
                random = Random(seed)
            )
            assertTrue(selected.name in configuredFishNames)
            selectedFishNames += selected.name
        }
        assertEquals(configuredFishNames, selectedFishNames)
    }

    @Test
    fun sunlitShallows_hasNishikigoiAsItsLord() {
        val point = requireNotNull(
            findFishingPoint(SUNLIT_SHALLOWS_POINT_ID)
        )

        assertEquals("錦鯉", point.lordFishName)
        assertTrue(isFishingAreaLord("錦鯉"))
        assertTrue(
            point.fishSpawns.any { it.fishName == point.lordFishName }
        )
    }

    @Test
    fun openSea_containsOffshoreCurrent() {
        val map = requireNotNull(findFishingMap(OPEN_SEA_MAP_ID))
        val points = pointsForMap(map.id)

        assertEquals(listOf(OFFSHORE_CURRENT_POINT_ID), map.pointIds)
        assertEquals(FishingEnvironment.OPEN_SEA, map.environment)
        assertEquals(1, points.size)
        assertEquals("回遊魚の潮目", points.single().name)
    }

    @Test
    fun offshoreCurrent_spawnsOnlyIwashiAjiAndMaguro() {
        val availableFish = fishesForPoint(OFFSHORE_CURRENT_POINT_ID)
        val configuredFishNames = setOf("イワシ", "アジ", "マグロ")
        val selectedFishNames = mutableSetOf<String>()

        assertEquals(configuredFishNames, availableFish.map { it.name }.toSet())
        repeat(500) { seed ->
            val selected = selectRandomFishForPoint(
                pointId = OFFSHORE_CURRENT_POINT_ID,
                random = Random(seed)
            )
            assertTrue(selected.name in configuredFishNames)
            selectedFishNames += selected.name
        }
        assertEquals(configuredFishNames, selectedFishNames)
    }

    @Test
    fun everyFishingPoint_referencesExistingMapAndFish() {
        fishingPoints.forEach { point ->
            assertTrue(findFishingMap(point.mapId) != null)
            assertTrue(point.fishSpawns.isNotEmpty())
            point.fishSpawns.forEach { spawn ->
                assertTrue(fishes.any { it.name == spawn.fishName })
                assertTrue(spawn.weight > 0)
            }
            point.lordFishName?.let { lordFishName ->
                assertTrue(fishes.any { it.name == lordFishName })
                assertTrue(
                    point.fishSpawns.any { it.fishName == lordFishName }
                )
            }
        }
    }

    @Test
    fun everyFishingMap_referencesOnlyItsOwnExistingPoints() {
        fishingMaps.forEach { map ->
            val points = map.pointIds.map(::findFishingPoint)
            assertTrue(points.all { it != null })
            assertTrue(points.filterNotNull().all { it.mapId == map.id })
            assertEquals(map.pointIds.toSet(), pointsForMap(map.id).map { it.id }.toSet())
        }
    }
}
