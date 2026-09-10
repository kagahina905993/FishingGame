package com.example.fishinggame

import kotlin.random.Random

data class FishingMap(
    val id: String,
    val name: String,
    val description: String,
    val pointIds: List<String>
)

data class FishSpawn(
    val fishName: String,
    val weight: Int
)

data class FishingPoint(
    val id: String,
    val mapId: String,
    val name: String,
    val description: String,
    val fishSpawns: List<FishSpawn>,
    val lordFishName: String? = null
)

const val FOOTHILL_STREAM_MAP_ID = "foothill_stream"
const val SUNLIT_SHALLOWS_POINT_ID = "sunlit_shallows"

val fishingMaps = listOf(
    FishingMap(
        id = FOOTHILL_STREAM_MAP_ID,
        name = "麓の渓流",
        description = "山の木漏れ日が差し込む、町から近い穏やかな渓流。",
        pointIds = listOf(SUNLIT_SHALLOWS_POINT_ID)
    )
)

val fishingPoints = listOf(
    FishingPoint(
        id = SUNLIT_SHALLOWS_POINT_ID,
        mapId = FOOTHILL_STREAM_MAP_ID,
        name = "木漏れ日の浅瀬",
        description = "丸い石の間を澄んだ水が流れる、最初の釣りポイント。",
        fishSpawns = listOf(
            FishSpawn(fishName = "カワムツ", weight = 100),
            FishSpawn(fishName = "オイカワ", weight = 80),
            FishSpawn(fishName = "タカハヤ", weight = 70),
            FishSpawn(fishName = "鯉", weight = 50),
            FishSpawn(fishName = "錦鯉", weight = 20)
        ),
        lordFishName = "錦鯉"
    )
)

val currentlyAvailableFishes: List<Fish>
    get() {
        val fishNames = fishingPoints
            .flatMap { point -> point.fishSpawns.map { it.fishName } }
            .toSet()
        return fishes.filter { it.name in fishNames }
    }

fun findFishingMap(mapId: String?): FishingMap? =
    fishingMaps.firstOrNull { it.id == mapId }

fun findFishingPoint(pointId: String?): FishingPoint? =
    fishingPoints.firstOrNull { it.id == pointId }

fun isFishingAreaLord(fishName: String): Boolean =
    fishingPoints.any { it.lordFishName == fishName }

fun pointsForMap(mapId: String): List<FishingPoint> =
    fishingPoints.filter { it.mapId == mapId }

fun fishesForPoint(pointId: String): List<Fish> {
    val point = findFishingPoint(pointId) ?: return emptyList()
    val fishNames = point.fishSpawns.map { it.fishName }.toSet()
    return fishes.filter { it.name in fishNames }
}

fun selectRandomFishForPoint(
    pointId: String,
    random: Random = Random.Default
): Fish {
    val point = requireNotNull(findFishingPoint(pointId)) {
        "釣りポイントが見つかりません: $pointId"
    }
    val candidates = point.fishSpawns.mapNotNull { spawn ->
        fishes.firstOrNull { it.name == spawn.fishName }
            ?.let { fish -> fish to spawn.weight.coerceAtLeast(1) }
    }
    require(candidates.isNotEmpty()) {
        "${point.name}に出現する魚が設定されていません"
    }

    val totalWeight = candidates.sumOf { (fish, pointWeight) ->
        fish.rarity.spawnWeight * pointWeight
    }
    var selectedWeight = random.nextInt(totalWeight)
    candidates.forEach { (fish, pointWeight) ->
        selectedWeight -= fish.rarity.spawnWeight * pointWeight
        if (selectedWeight < 0) return fish
    }
    return candidates.last().first
}

fun createRandomFishStateForPoint(
    pointId: String,
    random: Random = Random.Default
): FishState = createFishState(
    fish = selectRandomFishForPoint(pointId, random),
    random = random
)
