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
            "タカハヤ"
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
    fun everyFishingPoint_referencesExistingMapAndFish() {
        fishingPoints.forEach { point ->
            assertTrue(findFishingMap(point.mapId) != null)
            assertTrue(point.fishSpawns.isNotEmpty())
            point.fishSpawns.forEach { spawn ->
                assertTrue(fishes.any { it.name == spawn.fishName })
                assertTrue(spawn.weight > 0)
            }
        }
    }
}
