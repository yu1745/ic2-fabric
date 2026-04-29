package ic2_120.content.crop

import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.util.StringIdentifiable

enum class CropType(private val id: String) : StringIdentifiable {
    WEED("weed"),
    ACACIA_SAPLING("acacia_sapling"),
    AURELIA("aurelia"),
    BEETROOTS("beetroots"),
    BIRCH_SAPLING("birch_sapling"),
    BLACKTHORN("blackthorn"),
    BROWN_MUSHROOM("brown_mushroom"),
    CARROTS("carrots"),
    COCOA("cocoa"),
    COFFEE("coffee"),
    CYAZINT("cyazint"),
    CYPRIUM("cyprium"),
    DANDELION("dandelion"),
    DARK_OAK_SAPLING("dark_oak_sapling"),
    EATING_PLANT("eating_plant"),
    FERRU("ferru"),
    FLAX("flax"),
    HOPS("hops"),
    JUNGLE_SAPLING("jungle_sapling"),
    MELON("melon"),
    NETHER_WART("nether_wart"),
    OAK_SAPLING("oak_sapling"),
    PLUMBISCUS("plumbiscus"),
    POPPY("poppy"),
    POTATO("potato"),
    PUMPKIN("pumpkin"),
    RED_MUSHROOM("red_mushroom"),
    RED_WHEAT("red_wheat"),
    REED("reed"),
    SHINING("shining"),
    SPRUCE_SAPLING("spruce_sapling"),
    STAGNIUM("stagnium"),
    STICKY_REED("sticky_reed"),
    TERRA_WART("terra_wart"),
    TULIP("tulip"),
    VENOMILIA("venomilia"),
    WHEAT("wheat");

    override fun asString(): String = id
}

data class CropStats(
    var growth: Int = 1,
    var gain: Int = 1,
    var resistance: Int = 1,
)

enum class CropBehavior {
    NONE,
    HIGH_HYDRATION,
    DARKNESS_LOVING,
    METAL_CROP,
    POISONOUS,
    AGGRESSIVE
}

data class CropDefinition(
    val type: CropType,
    val maxVisualAge: Int,
    val tier: Int,
    val properties: IntArray,
    val attributes: List<String>,
    val baseSeedItems: List<Item>,
    val gainItem: Item? = null,
    val behavior: CropBehavior = CropBehavior.NONE,
)

object CropSystem {
    private val defs: Map<CropType, CropDefinition> = buildMap {
        fun add(
            type: CropType,
            maxAge: Int,
            tier: Int,
            props: IntArray,
            attrs: List<String>,
            seeds: List<Item> = emptyList(),
            gain: Item? = null,
            behavior: CropBehavior = CropBehavior.NONE,
        ) {
            put(type, CropDefinition(type, maxAge, tier, props, attrs, seeds, gain, behavior))
        }

        add(CropType.WHEAT, 7, 1, intArrayOf(0, 2, 0, 0, 0), listOf("yellow", "food", "wheat"), listOf(Items.WHEAT_SEEDS), Items.WHEAT)
        add(CropType.WEED, 4, 0, intArrayOf(3, 0, 0, 0, 0), listOf("weed", "aggressive"), behavior = CropBehavior.AGGRESSIVE)
        add(CropType.PUMPKIN, 3, 2, intArrayOf(0, 2, 0, 1, 0), listOf("orange", "food", "stem"), listOf(Items.PUMPKIN_SEEDS), Items.PUMPKIN)
        add(CropType.MELON, 3, 2, intArrayOf(0, 2, 0, 1, 0), listOf("green", "food", "stem"), listOf(Items.MELON_SEEDS), Items.MELON_SLICE)
        add(CropType.DANDELION, 3, 1, intArrayOf(1, 0, 0, 2, 0), listOf("yellow", "flower"), listOf(Items.DANDELION), Items.DANDELION)
        add(CropType.POPPY, 3, 1, intArrayOf(1, 0, 0, 2, 0), listOf("red", "flower"), listOf(Items.POPPY), Items.POPPY)
        add(CropType.BLACKTHORN, 3, 2, intArrayOf(2, 0, 1, 2, 1), listOf("black", "flower"))
        add(CropType.TULIP, 3, 2, intArrayOf(2, 0, 0, 2, 0), listOf("purple", "flower"), listOf(Items.ALLIUM))
        add(CropType.CYAZINT, 3, 3, intArrayOf(2, 0, 0, 2, 1), listOf("blue", "flower"))
        add(CropType.VENOMILIA, 5, 4, intArrayOf(2, 1, 3, 1, 2), listOf("purple", "poison", "flower"), behavior = CropBehavior.POISONOUS)
        add(CropType.REED, 2, 1, intArrayOf(0, 1, 0, 0, 1), listOf("green", "reed"), listOf(Items.SUGAR_CANE), Items.SUGAR_CANE, behavior = CropBehavior.HIGH_HYDRATION)
        add(CropType.STICKY_REED, 3, 4, intArrayOf(0, 1, 1, 0, 2), listOf("green", "reed", "resin"), behavior = CropBehavior.HIGH_HYDRATION)
        add(CropType.COCOA, 3, 2, intArrayOf(1, 2, 0, 1, 1), listOf("brown", "food", "bean"), listOf(Items.COCOA_BEANS), Items.COCOA_BEANS)
        add(CropType.FLAX, 3, 3, intArrayOf(0, 1, 0, 1, 0), listOf("fiber", "crop"))
        add(CropType.RED_MUSHROOM, 2, 2, intArrayOf(2, 1, 0, 1, 1), listOf("red", "mushroom"), listOf(Items.RED_MUSHROOM), Items.RED_MUSHROOM, behavior = CropBehavior.DARKNESS_LOVING)
        add(CropType.BROWN_MUSHROOM, 2, 2, intArrayOf(2, 1, 0, 1, 1), listOf("brown", "mushroom"), listOf(Items.BROWN_MUSHROOM), Items.BROWN_MUSHROOM, behavior = CropBehavior.DARKNESS_LOVING)
        add(CropType.NETHER_WART, 2, 2, intArrayOf(2, 1, 0, 1, 2), listOf("nether", "wart"), listOf(Items.NETHER_WART), Items.NETHER_WART, behavior = CropBehavior.DARKNESS_LOVING)
        add(CropType.TERRA_WART, 2, 3, intArrayOf(1, 1, 0, 1, 2), listOf("terra", "wart"), behavior = CropBehavior.DARKNESS_LOVING)
        add(CropType.OAK_SAPLING, 4, 2, intArrayOf(1, 0, 0, 0, 1), listOf("tree", "oak"), listOf(Items.OAK_SAPLING), Items.OAK_SAPLING)
        add(CropType.SPRUCE_SAPLING, 4, 2, intArrayOf(1, 0, 0, 0, 1), listOf("tree", "spruce"), listOf(Items.SPRUCE_SAPLING), Items.SPRUCE_SAPLING)
        add(CropType.BIRCH_SAPLING, 4, 2, intArrayOf(1, 0, 0, 0, 1), listOf("tree", "birch"), listOf(Items.BIRCH_SAPLING), Items.BIRCH_SAPLING)
        add(CropType.JUNGLE_SAPLING, 4, 2, intArrayOf(1, 0, 0, 0, 1), listOf("tree", "jungle"), listOf(Items.JUNGLE_SAPLING), Items.JUNGLE_SAPLING)
        add(CropType.ACACIA_SAPLING, 4, 2, intArrayOf(1, 0, 0, 0, 1), listOf("tree", "acacia"), listOf(Items.ACACIA_SAPLING), Items.ACACIA_SAPLING)
        add(CropType.DARK_OAK_SAPLING, 4, 2, intArrayOf(1, 0, 0, 0, 1), listOf("tree", "dark_oak"), listOf(Items.DARK_OAK_SAPLING), Items.DARK_OAK_SAPLING)
        add(CropType.FERRU, 3, 4, intArrayOf(0, 0, 2, 2, 2), listOf("metal", "gray"), behavior = CropBehavior.METAL_CROP)
        add(CropType.CYPRIUM, 3, 4, intArrayOf(0, 0, 2, 2, 2), listOf("metal", "copper"), behavior = CropBehavior.METAL_CROP)
        add(CropType.STAGNIUM, 3, 4, intArrayOf(0, 0, 2, 2, 2), listOf("metal", "tin"), behavior = CropBehavior.METAL_CROP)
        add(CropType.PLUMBISCUS, 3, 4, intArrayOf(0, 0, 2, 2, 2), listOf("metal", "lead"), behavior = CropBehavior.METAL_CROP)
        add(CropType.AURELIA, 4, 5, intArrayOf(0, 0, 3, 2, 3), listOf("metal", "gold"), behavior = CropBehavior.METAL_CROP)
        add(CropType.SHINING, 4, 5, intArrayOf(0, 0, 3, 2, 3), listOf("metal", "silver"), behavior = CropBehavior.METAL_CROP)
        add(CropType.RED_WHEAT, 6, 4, intArrayOf(1, 2, 1, 1, 2), listOf("red", "wheat"))
        add(CropType.COFFEE, 4, 3, intArrayOf(1, 2, 0, 1, 1), listOf("coffee", "bean"), listOf(), Items.COCOA_BEANS)
        add(CropType.HOPS, 6, 3, intArrayOf(1, 2, 0, 1, 1), listOf("hops", "plant"))
        add(CropType.CARROTS, 3, 1, intArrayOf(0, 2, 0, 1, 0), listOf("orange", "food"), listOf(Items.CARROT), Items.CARROT)
        add(CropType.POTATO, 3, 1, intArrayOf(0, 2, 0, 1, 0), listOf("brown", "food"), listOf(Items.POTATO), Items.POTATO)
        add(CropType.EATING_PLANT, 5, 5, intArrayOf(3, 2, 3, 1, 2), listOf("hostile", "plant"), behavior = CropBehavior.POISONOUS)
        add(CropType.BEETROOTS, 3, 1, intArrayOf(0, 2, 0, 1, 0), listOf("red", "food"), listOf(Items.BEETROOT_SEEDS), Items.BEETROOT)
    }

    private val seedToCrop: Map<Item, CropType> = buildMap {
        for (def in defs.values) {
            for (seed in def.baseSeedItems) put(seed, def.type)
        }
    }

    fun definition(type: CropType): CropDefinition = defs.getValue(type)
    fun allTypes(): List<CropType> = CropType.entries
    fun maxAge(type: CropType): Int = definition(type).maxVisualAge
    fun optimalHarvestAge(type: CropType): Int {
        val max = maxAge(type)
        return when (type) {
            CropType.WEED -> 1
            CropType.VENOMILIA -> 3
            CropType.POTATO -> max - 1
            CropType.EATING_PLANT -> max - 2
            else -> max
        }.coerceIn(0, 7)
    }
    fun baseSeed(item: Item): CropType? = seedToCrop[item]
    fun behavior(type: CropType): CropBehavior = definition(type).behavior
    fun canBeHarvested(type: CropType, age: Int): Boolean {
        val max = maxAge(type)
        return when (type) {
            CropType.WEED -> false
            CropType.REED, CropType.STICKY_REED -> age > 0
            CropType.VENOMILIA -> age >= 3
            CropType.COFFEE, CropType.POTATO -> age >= (max - 1)
            CropType.EATING_PLANT -> age >= (max - 2) && age < max
            else -> age >= max
        }
    }

    fun ageAfterHarvest(type: CropType, currentAge: Int, randomBit: Int = 0): Int? {
        val max = maxAge(type)
        val next = when (type) {
            CropType.WHEAT -> 2
            CropType.PUMPKIN, CropType.MELON -> max - 1
            CropType.DANDELION, CropType.POPPY, CropType.BLACKTHORN, CropType.TULIP, CropType.CYAZINT -> max - 1
            CropType.REED -> 0
            CropType.STICKY_REED -> if (currentAge == max) 2 - (randomBit and 1) else 0
            CropType.COCOA -> max - 1
            CropType.FLAX -> 0
            CropType.OAK_SAPLING, CropType.SPRUCE_SAPLING, CropType.BIRCH_SAPLING,
            CropType.JUNGLE_SAPLING, CropType.ACACIA_SAPLING, CropType.DARK_OAK_SAPLING -> max - 1
            CropType.FERRU, CropType.CYPRIUM, CropType.STAGNIUM, CropType.PLUMBISCUS,
            CropType.AURELIA, CropType.SHINING -> 1
            CropType.RED_WHEAT -> 1
            CropType.COFFEE -> max - 2
            CropType.HOPS -> 2
            CropType.VENOMILIA -> 2
            CropType.EATING_PLANT -> 0
            else -> null
        }
        return next?.coerceIn(0, 7)
    }

    fun canCross(type: CropType): Boolean = type != CropType.EATING_PLANT && type != CropType.WEED
    fun canCross(type: CropType, age: Int): Boolean = canCross(type) && age >= 2

    fun getWeightInfluence(type: CropType, humidity: Int, nutrients: Int, airQuality: Int): Int {
        val h = humidity.coerceAtLeast(0)
        val n = nutrients.coerceAtLeast(0)
        val a = airQuality.coerceAtLeast(0)
        return when (behavior(type)) {
            CropBehavior.HIGH_HYDRATION -> ((h * 1.2) + n + (a * 0.8)).toInt() // Reed family
            CropBehavior.METAL_CROP -> h + n + a
            else -> when (type) {
                CropType.PUMPKIN, CropType.MELON -> ((h * 1.1) + (n * 0.9) + a).toInt()
                CropType.COCOA -> ((h * 0.8) + (n * 1.3) + (a * 0.9)).toInt()
                CropType.COFFEE -> ((h * 0.4) + (n * 1.4) + (a * 1.2)).toInt()
                else -> h + n + a
            }
        }
    }

    fun dropGainChance(type: CropType): Double {
        val tier = definition(type).tier.coerceAtLeast(0)
        val base = Math.pow(0.95, tier.toDouble())
        return when (type) {
            CropType.NETHER_WART -> 2.0
            CropType.TERRA_WART -> 0.8
            CropType.RED_WHEAT -> 0.5
            CropType.FERRU, CropType.CYPRIUM, CropType.STAGNIUM, CropType.PLUMBISCUS -> base / 2.0
            CropType.AURELIA, CropType.SHINING -> base
            else -> base
        }
    }

    fun growthDuration(type: CropType, currentAge: Int): Int {
        val max = maxAge(type)
        val base = definition(type).tier * 200
        return when (type) {
            CropType.PUMPKIN -> if (currentAge == max - 1) 600 else 200
            CropType.MELON -> if (currentAge == max - 1) 700 else 250
            CropType.REED -> 200
            CropType.STICKY_REED -> if (currentAge == max) 400 else 100
            CropType.HOPS, CropType.RED_WHEAT -> 600
            CropType.OAK_SAPLING, CropType.SPRUCE_SAPLING, CropType.BIRCH_SAPLING,
            CropType.JUNGLE_SAPLING, CropType.ACACIA_SAPLING, CropType.DARK_OAK_SAPLING ->
                if (currentAge >= max - 1) 150 else 600
            CropType.FERRU, CropType.CYPRIUM, CropType.STAGNIUM, CropType.PLUMBISCUS ->
                if (currentAge == max - 1) 2000 else 800
            CropType.AURELIA, CropType.SHINING ->
                if (currentAge == max - 1) 2200 else 750
            CropType.DANDELION, CropType.POPPY, CropType.BLACKTHORN, CropType.TULIP, CropType.CYAZINT ->
                if (currentAge == max - 1) 600 else 400
            CropType.COCOA -> if (currentAge == max - 1) 900 else 400
            CropType.VENOMILIA -> if (currentAge >= 2) 600 else 400
            CropType.COFFEE -> when (currentAge) {
                max - 2 -> (base * 0.5).toInt()
                max - 3 -> (base * 1.5).toInt()
                else -> base
            }
            else -> base
        }
    }

    fun dropSeedChance(type: CropType, currentAge: Int): Float {
        if (currentAge == 0) return 0f
        var chance = 0.5f
        if (currentAge == 1) chance *= 0.5f
        repeat(definition(type).tier.coerceAtLeast(0)) {
            chance *= 0.8f
        }
        return chance
    }

    fun calculateRatioFor(target: CropType, source: CropType): Int {
        if (target == source) return 500
        val a = definition(target)
        val b = definition(source)
        var value = 0
        for (i in 0 until 5) {
            val diff = kotlin.math.abs(a.properties[i] - b.properties[i])
            value += -diff + 2
        }
        for (attr in a.attributes) {
            if (b.attributes.any { it.equals(attr, ignoreCase = true) }) value += 5
        }
        val tierDiff = a.tier - b.tier
        if (tierDiff > 1) value -= 2 * tierDiff
        if (tierDiff < -3) value -= -tierDiff
        return value.coerceAtLeast(0)
    }
}
