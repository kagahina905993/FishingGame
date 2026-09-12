package com.example.fishinggame

import kotlin.random.Random

data class FishingMap(
    val id: String,
    val name: String,
    val description: String,
    val pointIds: List<String>,
    val environment: FishingEnvironment = FishingEnvironment.STREAM
)

enum class FishingEnvironment {
    STREAM,
    OPEN_SEA
}

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
const val OPEN_SEA_MAP_ID = "open_sea"
const val OFFSHORE_CURRENT_POINT_ID = "offshore_current"

// コンテスト試遊版だけで使う簡易的な主解放ルール。
const val CONTEST_LORD_UNLOCK_ENABLED = true

val fishingMaps = listOf(
    FishingMap(
        id = FOOTHILL_STREAM_MAP_ID,
        name = "麓の渓流",
        description = "山の木漏れ日が差し込む、町から近い穏やかな渓流。",
        pointIds = listOf(SUNLIT_SHALLOWS_POINT_ID)
    ),
    FishingMap(
        id = OPEN_SEA_MAP_ID,
        name = "青潮の沖合",
        description = "小魚の群れと大型の回遊魚が行き交う、深い沖の海。",
        pointIds = listOf(OFFSHORE_CURRENT_POINT_ID),
        environment = FishingEnvironment.OPEN_SEA
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
    ),
    FishingPoint(
        id = OFFSHORE_CURRENT_POINT_ID,
        mapId = OPEN_SEA_MAP_ID,
        name = "回遊魚の潮目",
        description = "潮の流れがぶつかり、イワシやアジを追って大物も現れる。",
        fishSpawns = listOf(
            FishSpawn(fishName = "イワシ", weight = 100),
            FishSpawn(fishName = "アジ", weight = 80),
            FishSpawn(fishName = "マグロ", weight = 600)
        )
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

fun lordFishNamesForMap(mapId: String): Set<String> =
    pointsForMap(mapId).mapNotNullTo(mutableSetOf()) { it.lordFishName }

fun nonLordFishesForMap(mapId: String): List<Fish> {
    val lordFishNames = lordFishNamesForMap(mapId)
    val fishNames = pointsForMap(mapId)
        .flatMapTo(mutableSetOf()) { point ->
            point.fishSpawns.map { it.fishName }
        }
    return fishes.filter { fish ->
        fish.name in fishNames && fish.name !in lordFishNames
    }
}

fun caughtNonLordFishCountForMap(
    mapId: String,
    fishCollectionRecords: Map<String, FishCollectionRecord>
): Int = nonLordFishesForMap(mapId).count { fish ->
    (fishCollectionRecords[fish.name]?.caughtCount ?: 0) > 0
}

fun isMapLordUnlocked(
    mapId: String,
    fishCollectionRecords: Map<String, FishCollectionRecord>
): Boolean {
    val nonLordFishes = nonLordFishesForMap(mapId)
    return lordFishNamesForMap(mapId).isNotEmpty() &&
        nonLordFishes.isNotEmpty() &&
        nonLordFishes.all { fish ->
            (fishCollectionRecords[fish.name]?.caughtCount ?: 0) > 0
        }
}

fun fishesForPoint(pointId: String): List<Fish> {
    val point = findFishingPoint(pointId) ?: return emptyList()
    val fishNames = point.fishSpawns.map { it.fishName }.toSet()
    return fishes.filter { it.name in fishNames }
}

fun selectRandomFishForPoint(
    pointId: String,
    fishCollectionRecords: Map<String, FishCollectionRecord> = emptyMap(),
    random: Random = Random.Default
): Fish {
    val point = requireNotNull(findFishingPoint(pointId)) {
        "釣りポイントが見つかりません: $pointId"
    }
    val lordFishName = point.lordFishName
    val lordIsUnlocked = isMapLordUnlocked(
        mapId = point.mapId,
        fishCollectionRecords = fishCollectionRecords
    )
    val lordHasBeenCaught = lordFishName != null &&
        (fishCollectionRecords[lordFishName]?.caughtCount ?: 0) > 0
    val eligibleSpawns = when {
        !CONTEST_LORD_UNLOCK_ENABLED -> point.fishSpawns
        lordFishName == null -> point.fishSpawns
        !lordIsUnlocked -> point.fishSpawns.filterNot {
            it.fishName == lordFishName
        }
        !lordHasBeenCaught -> point.fishSpawns.filter {
            it.fishName == lordFishName
        }
        else -> point.fishSpawns
    }
    val candidates = eligibleSpawns.mapNotNull { spawn ->
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
    fishCollectionRecords: Map<String, FishCollectionRecord> = emptyMap(),
    random: Random = Random.Default
): FishState = createFishState(
    fish = selectRandomFishForPoint(
        pointId = pointId,
        fishCollectionRecords = fishCollectionRecords,
        random = random
    ),
    random = random
)
