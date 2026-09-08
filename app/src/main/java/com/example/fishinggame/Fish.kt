package com.example.fishinggame

import kotlin.random.Random

enum class FishRarity(
    val label: String,
    val stars: Int,
    val spawnWeight: Int
) {
    COMMON("コモン", 1, 50),
    UNCOMMON("アンコモン", 2, 30),
    RARE("レア", 3, 15),
    EPIC("エピック", 4, 4),
    LEGENDARY("レジェンド", 5, 1);

    val displayText: String
        get() = "$label ${"★".repeat(stars)}"
}

data class Fish(
    val name: String,

    val minHp: Int,
    val maxHp: Int,

    val minDistance: Float,
    val maxDistance: Float,

    val rarity: FishRarity = FishRarity.COMMON,
    val minSizeCm: Float = 10f,
    val maxSizeCm: Float = 20f,
    val description: String = ""
)

data class FishState(
    val fish: Fish,

    val startHp: Int,
    val currentHp: Int,

    val startDistance: Float,
    val currentDistance: Float,

    val sizeCm: Float = 0f
) {
    val distanceProgress: Float
        get() = if (startDistance <= 0f) {
            0f
        } else {
            (currentDistance / startDistance).coerceIn(0f, 1f)
        }
}

fun createFishState(
    fish: Fish,
    random: Random = Random.Default
): FishState {
    val randomHp =
        random.nextInt(fish.minHp, fish.maxHp + 1)

    val randomDistance =
        random.nextFloat() *
                (fish.maxDistance - fish.minDistance) +
                fish.minDistance

    val randomSize =
        random.nextFloat() *
                (fish.maxSizeCm - fish.minSizeCm) +
                fish.minSizeCm

    return FishState(
        fish = fish,

        startHp = randomHp,
        currentHp = randomHp,

        startDistance = randomDistance,
        currentDistance = randomDistance,

        sizeCm = randomSize
    )
}

fun selectRandomFish(random: Random = Random.Default): Fish {
    val totalWeight = fishes.sumOf { it.rarity.spawnWeight }
    var selectedWeight = random.nextInt(totalWeight)
    fishes.forEach { fish ->
        selectedWeight -= fish.rarity.spawnWeight
        if (selectedWeight < 0) return fish
    }
    return fishes.last()
}

fun createRandomFishState(
    random: Random = Random.Default
): FishState = createFishState(
    fish = selectRandomFish(random),
    random = random
)

val fishes = listOf(
    Fish(
        name = "カワムツ",
        minHp = 45,
        maxHp = 60,
        minDistance = 4.0f,
        maxDistance = 7.0f,
        rarity = FishRarity.COMMON,
        minSizeCm = 8f,
        maxSizeCm = 20f,
        description = "流れのある川に暮らす小型魚。最初の渓流で出会う魚。"
    ),

    Fish(
        name = "オイカワ",
        minHp = 45,
        maxHp = 60,
        minDistance = 4.0f,
        maxDistance = 7.0f,
        rarity = FishRarity.COMMON,
        minSizeCm = 8f,
        maxSizeCm = 18f,
        description = "澄んだ川の浅瀬に暮らす小型魚。鮮やかな体色と長いヒレが美しい。"
    ),

    Fish(
        name = "タカハヤ",
        minHp = 45,
        maxHp = 60,
        minDistance = 4.0f,
        maxDistance = 7.0f,
        rarity = FishRarity.COMMON,
        minSizeCm = 7f,
        maxSizeCm = 18f,
        description = "麓の渓流に暮らす小型魚。落ち着いた体色が水辺になじむ。"
    ),

    Fish(
        name = "アジ",
        minHp = 50,
        maxHp = 65,
        minDistance = 5.0f,
        maxDistance = 8.0f,
        rarity = FishRarity.COMMON,
        minSizeCm = 15f,
        maxSizeCm = 40f,
        description = "群れで泳ぐ身近な魚。小さくても引きは元気。"
    ),

    Fish(
        name = "イワシ",
        minHp = 40,
        maxHp = 55,
        minDistance = 3.0f,
        maxDistance = 6.0f,
        rarity = FishRarity.COMMON,
        minSizeCm = 10f,
        maxSizeCm = 25f,
        description = "大きな群れを作る小型魚。初心者でも狙いやすい。"
    ),

    Fish(
        name = "サバ",
        minHp = 60,
        maxHp = 80,
        minDistance = 6.0f,
        maxDistance = 10.0f,
        rarity = FishRarity.COMMON,
        minSizeCm = 20f,
        maxSizeCm = 50f,
        description = "素早く泳ぐ青魚。掛かると勢いよく走り回る。"
    ),

    Fish(
        name = "カサゴ",
        minHp = 80,
        maxHp = 105,
        minDistance = 5.0f,
        maxDistance = 9.0f,
        rarity = FishRarity.UNCOMMON,
        minSizeCm = 15f,
        maxSizeCm = 45f,
        description = "岩陰に潜む根魚。見た目以上に力強い。"
    ),

    Fish(
        name = "スズキ",
        minHp = 95,
        maxHp = 125,
        minDistance = 10.0f,
        maxDistance = 18.0f,
        rarity = FishRarity.UNCOMMON,
        minSizeCm = 30f,
        maxSizeCm = 100f,
        description = "成長すると名前が変わる出世魚。大型は手強い。"
    ),

    Fish(
        name = "タイ",
        minHp = 110,
        maxHp = 145,
        minDistance = 8.0f,
        maxDistance = 15.0f,
        rarity = FishRarity.RARE,
        minSizeCm = 20f,
        maxSizeCm = 80f,
        description = "美しい赤色が特徴。縁起のよい魚として有名。"
    ),

    Fish(
        name = "ヒラメ",
        minHp = 125,
        maxHp = 165,
        minDistance = 8.0f,
        maxDistance = 16.0f,
        rarity = FishRarity.RARE,
        minSizeCm = 30f,
        maxSizeCm = 100f,
        description = "海底に身を隠す平たい魚。鋭い歯を持つ。"
    ),

    Fish(
        name = "カンパチ",
        minHp = 165,
        maxHp = 220,
        minDistance = 15.0f,
        maxDistance = 28.0f,
        rarity = FishRarity.EPIC,
        minSizeCm = 60f,
        maxSizeCm = 190f,
        description = "強烈な引きを見せる大型の回遊魚。"
    ),

    Fish(
        name = "カジキ",
        minHp = 200,
        maxHp = 270,
        minDistance = 25.0f,
        maxDistance = 45.0f,
        rarity = FishRarity.EPIC,
        minSizeCm = 150f,
        maxSizeCm = 450f,
        description = "長い吻と圧倒的な速さを持つ海のハンター。"
    ),

    Fish(
        name = "マグロ",
        minHp = 240,
        maxHp = 320,
        minDistance = 20.0f,
        maxDistance = 40.0f,
        rarity = FishRarity.LEGENDARY,
        minSizeCm = 100f,
        maxSizeCm = 300f,
        description = "海を代表する巨大魚。最高クラスの獲物。"
    )
)
