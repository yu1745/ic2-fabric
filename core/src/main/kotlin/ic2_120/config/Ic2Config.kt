package ic2_120.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ic2_120.Ic2_120
import ic2_120.content.uu.UuTemplateEntry
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.lang.reflect.Modifier
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigComment(val value: String, val defaultValue: String = "")

data class Ic2MainConfig(
    @field:ConfigComment("通用配置。")
    val general: GeneralConfig = GeneralConfig(),
    @field:ConfigComment("回收机相关配置。")
    val recycler: RecyclerConfig = RecyclerConfig(),
    @field:ConfigComment("核能系统相关配置。")
    val nuclear: NuclearConfig = NuclearConfig(),
    @field:ConfigComment("UU 复制白名单配置。")
    val uuReplication: UuReplicationConfig = UuReplicationConfig(),
    @field:ConfigComment("采矿机配置。")
    val miner: MinerConfig = MinerConfig(),
    @field:ConfigComment("采矿镭射枪配置。")
    val miningLaser: MiningLaserConfig = MiningLaserConfig(),
    @field:ConfigComment("护甲功能配置（飞行时长、夜视时长等）。")
    val armor: ArmorConfig = ArmorConfig(),
    @field:ConfigComment("世界生成配置。")
    val worldgen: WorldgenConfig = WorldgenConfig()
)

data class GeneralConfig(
    @field:ConfigComment("启动/重载时是否把当前配置打印到日志。", "true")
    val logConfigOnLoad: Boolean = true,
    @field:ConfigComment("是否启用更新检查。", "true")
    val checkForUpdates: Boolean = true,
    @field:ConfigComment("电网不发生能量流动时是否仍会电人。设为 false 则无能量时不漏电。", "false")
    val shockWhenNoEnergyFlow: Boolean = false
)

data class RecyclerConfig(
    // Item id list, e.g. ["minecraft:stick", "ic2_120:scrap"]
    @field:ConfigComment("回收机黑名单。填写物品 id 列表，例如 minecraft:stick。", "[\"minecraft:stick\"]")
    val blacklist: List<String> = listOf("minecraft:stick")
)

data class NuclearConfig(
    /** 是否允许核反应堆在过热时爆炸 */
    @field:ConfigComment("是否允许核反应堆在过热时爆炸。", "true")
    val enableReactorExplosion: Boolean = true
)

data class UuReplicationConfig(
    /**
     * 可复制物品白名单：key 为物品 id，value 为所需 UU 物质，单位 uB。
     * 详见 [UuReplicationDefaults] 了解定价原则。
     */
    @field:ConfigComment("UU 复制白名单。key=物品 id，value=所需 UU 物质（uB）。")
    val replicationWhitelist: Map<String, Int> = UuReplicationDefaults.defaultWhitelist
)

data class MinerConfig(
    @field:ConfigComment(
        "采矿机额外可挖方块 id 列表。默认矿石通过名称自动匹配（含 ore），此列表用于添加特殊方块或者别的mod的方块。",
        "[]"
    )
    val additionalMineableBlocks: List<String> = emptyList()
)

/**
 * 护甲功能配置（飞行时长、夜视时长等）。
 * 耗电量自动通过 满电容量 / 时长(秒) 计算得出。
 */
data class ArmorConfig(
    @field:ConfigComment("喷气背包配置。")
    val jetpack: JetpackConfig = JetpackConfig(),
    @field:ConfigComment("电力喷气背包配置。")
    val electricJetpack: ElectricJetpackConfig = ElectricJetpackConfig(),
    @field:ConfigComment("量子胸甲配置。")
    val quantumChestplate: QuantumChestplateConfig = QuantumChestplateConfig(),
    @field:ConfigComment("量子头盔配置。")
    val quantumHelmet: QuantumHelmetConfig = QuantumHelmetConfig(),
    @field:ConfigComment("量子护腿配置（神行加速）。")
    val quantumLeggings: QuantumLeggingsConfig = QuantumLeggingsConfig(),
    @field:ConfigComment("量子靴子配置（大跳）。")
    val quantumBoots: QuantumBootsConfig = QuantumBootsConfig(),
    @field:ConfigComment("夜视配置。")
    val nightVision: NightVisionConfig = NightVisionConfig(),
    @field:ConfigComment("橡胶靴配置。")
    val rubberBoots: RubberBootsConfig = RubberBootsConfig()
)

data class JetpackConfig(
    /** 燃料容量 (mB)。 */
    @field:ConfigComment("喷气背包燃料容量（mB）。", "30000")
    val maxFuel: Long = 30_000L,
    /** 飞行时长（秒）。耗电量自动通过 满燃料容量 / 时长 计算。 */
    @field:ConfigComment("喷气背包飞行时长（秒），耗电量自动计算。", "750")
    val flightDurationSeconds: Int = 750
)

data class ElectricJetpackConfig(
    /** 最大能量 (EU)。 */
    @field:ConfigComment("电力喷气背包最大能量（EU）。", "30000")
    val maxEnergy: Long = 30_000L,
    /** 飞行时长（秒）。耗电量自动通过 满电容量 / 时长 计算。 */
    @field:ConfigComment("电力喷气背包飞行时长（秒），耗电量自动计算。", "750")
    val flightDurationSeconds: Int = 750
)

data class QuantumChestplateConfig(
    /** 最大能量 (EU)。 */
    @field:ConfigComment("量子胸甲最大能量（EU）。", "10000000")
    val maxEnergy: Long = 10_000_000L,
    /** 飞行时长（秒）。耗电量自动通过 满电容量 / 时长 计算。 */
    @field:ConfigComment("量子胸甲飞行时长（秒），耗电量自动计算。", "1200")
    val flightDurationSeconds: Int = 1200
)

data class QuantumHelmetConfig(
    /** 夜视时长（秒）。耗电量自动通过 满电容量 / 时长 计算。 */
    @field:ConfigComment("量子头盔夜视时长（秒），耗电量自动计算。", "28800")
    val nightVisionDurationSeconds: Int = 28800
)

data class QuantumLeggingsConfig(
    /** 最大能量 (EU)。用于计算神行耗电速率。 */
    @field:ConfigComment("量子护腿计算耗电用的参考容量（EU）。", "10000000")
    val maxEnergy: Long = 10_000_000L,
    /** 神行时长（秒）。满电 2 档可连续神行多久。 */
    @field:ConfigComment("量子护腿神行时长（秒，2档满电），耗电量自动计算。", "1800")
    val speedBoostDurationSeconds: Int = 1800,
    /** 神行 1 档速度倍率。 */
    @field:ConfigComment("神行 1 档速度倍率。0.2 表示 +20% 移速，半耗电。", "0.2")
    val speedMultiplierTier1: Double = 0.2,
    /** 神行 2 档速度倍率。 */
    @field:ConfigComment("神行 2 档速度倍率。0.4 表示 +40% 移速，全耗电。", "0.4")
    val speedMultiplierTier2: Double = 0.4
)

data class QuantumBootsConfig(
    /** 最大能量 (EU)。用于参考。 */
    @field:ConfigComment("量子靴子计算跳跃耗电的参考容量（EU）。", "10000000")
    val maxEnergy: Long = 10_000_000L,
    /** 每次大跳消耗能量（EU）。默认 1000 次跳空靴子。 */
    @field:ConfigComment("每次大跳消耗能量（EU）。10M EU / 1000 次 = 10000 EU/次。", "10000")
    val jumpEnergyCost: Long = 10_000,
    /** 跳跃高度倍率。3.0 表示 3 倍正常跳跃高度（约 3.75 格）。 */
    @field:ConfigComment("跳跃高度倍率。3.0 = 3 倍正常高度（约 3.75 格）。", "3.0")
    val jumpHeightMultiplier: Double = 3.0
)

data class NightVisionConfig(
    /** 夜视镜最大能量 (EU)。 */
    @field:ConfigComment("夜视镜最大能量（EU）。", "100000")
    val nightVisionGogglesMaxEnergy: Long = 100_000L,
    /** 夜视镜夜视时长（秒）。耗电量自动通过 满电容量 / 时长 计算。 */
    @field:ConfigComment("夜视镜夜视时长（秒），耗电量自动计算。", "5000")
    val nightVisionGogglesDurationSeconds: Int = 5000,
    /** 纳米头盔最大能量 (EU)。 */
    @field:ConfigComment("纳米头盔最大能量（EU）。", "1000000")
    val nanoHelmetMaxEnergy: Long = 1_000_000L,
    /** 纳米头盔夜视时长（秒）。耗电量自动通过 满电容量 / 时长 计算。 */
    @field:ConfigComment("纳米头盔夜视时长（秒），耗电量自动计算。", "3571")
    val nanoHelmetDurationSeconds: Int = 3571
)

data class RubberBootsConfig(
    /** 行走多少格触发一次充电。 */
    @field:ConfigComment("橡胶靴行走多少格触发一次充电。", "1")
    val distance: Double = 1.0,
    /** 每次充电的 EU 数。 */
    @field:ConfigComment("橡胶靴每次充电的 EU 数。", "20")
    val eu: Long = 20L
)

/**
 * 采矿镭射枪各模式配置。
 * 设计原则：一次发射越多弹体，射程越短；单发模式射程最远。
 */
data class MiningLaserConfig(
    @field:ConfigComment("采矿模式配置。基础远程挖矿，击穿一段距离，距离与被挖掘方块硬度有关。")
    val mining: LaserModeConfig = LaserModeConfig(
        energyCost = 2_000L,
        range = 64.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFF00BFFF.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 4f
    ),
    @field:ConfigComment("低聚焦模式配置。近距离单发，节约用电，有几率点燃方块。")
    val lowFocus: LaserModeConfig = LaserModeConfig(
        energyCost = 500L,
        range = 4.0,
        speed = 1.0,
        explosionPower = 0f,
        color = 0xFFFF8800.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 2f
    ),
    @field:ConfigComment("远距模式配置。超远射程，速度更快。")
    val longRange: LaserModeConfig = LaserModeConfig(
        energyCost = 5_000L,
        range = 64.0,
        speed = 3.0,
        explosionPower = 0f,
        color = 0xFF44FF44.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 6f
    ),
    @field:ConfigComment("超级热线模式配置。烧制方块，将矿石烧制成成品（对原木无效）。")
    val superHeat: LaserModeConfig = LaserModeConfig(
        energyCost = 5_000L,
        range = 8.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFFFF4400.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 8f
    ),
    @field:ConfigComment("散射模式配置。25发同时发射，3x3范围。")
    val scatter: LaserModeConfig = LaserModeConfig(
        energyCost = 12_500L,
        range = 10.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFFFF44FF.toInt(),
        scatterCount = 25,
        scatterSpread = 50.0,
        entityDamage = 2f
    ),
    @field:ConfigComment("爆破模式配置。约TNT当量，穿甲效果。")
    val explosive: LaserModeConfig = LaserModeConfig(
        energyCost = 10_000L,
        range = 10.0,
        speed = 1.5,
        explosionPower = 4.0f,
        color = 0xFFFF2222.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 100f
    ),
    @field:ConfigComment("3x3模式配置。9发同时发射，3x3断面向前开挖。")
    val trench3x3: LaserModeConfig = LaserModeConfig(
        energyCost = 7_200L,
        range = 20.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFF00CCFF.toInt(),
        scatterCount = 9,
        scatterSpread = 18.0,
        entityDamage = 2f
    )
)

/**
 * 单个镭射枪模式的数值配置。
 */
data class LaserModeConfig(
    @field:ConfigComment("每发消耗能量（EU）。", "2000")
    val energyCost: Long,
    @field:ConfigComment("最大射程 (blocks)。", "64.0")
    val range: Double,
    @field:ConfigComment("弹体飞行速度 (blocks/tick)。", "1.5")
    val speed: Double,
    @field:ConfigComment("爆炸威力 (0 = 不爆炸, 4.0 = TNT)。", "0.0")
    val explosionPower: Float,
    @field:ConfigComment("弹体视觉颜色 (ARGB 整数)。", "-16740353")
    val color: Int,
    @field:ConfigComment("散射弹体数量 (1 = 单发)。", "1")
    val scatterCount: Int,
    @field:ConfigComment("散射张角 (度)。", "0.0")
    val scatterSpread: Double,
    @field:ConfigComment("对实体伤害（2.0 = 1颗心）。", "4.0")
    val entityDamage: Float
)

private val DEFAULT_RUBBER_TREE_BIOMES = listOf(
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:birch_forest",
    "minecraft:dark_forest",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:swamp"
)

data class WorldgenConfig(
    @field:ConfigComment("橡胶树世界生成配置。enabled/biomes 变更后需要重启；其余多数参数 /ic2config reload 后影响未来生成。")
    val rubberTree: RubberTreeWorldgenConfig = RubberTreeWorldgenConfig()
)

data class RubberTreeWorldgenConfig(
    /** 是否允许自然生成橡胶树。 */
    @field:ConfigComment("是否允许自然生成橡胶树。", "true")
    val enabled: Boolean = true,
    /** 允许生成橡胶树的生物群系列表。 */
    @field:ConfigComment(
        "允许生成橡胶树的生物群系列表。填写 biome id，例如 minecraft:forest。",
        "[\"minecraft:forest\", \"minecraft:flower_forest\", \"minecraft:birch_forest\", \"minecraft:dark_forest\", \"minecraft:taiga\", \"minecraft:old_growth_pine_taiga\", \"minecraft:old_growth_spruce_taiga\", \"minecraft:jungle\", \"minecraft:sparse_jungle\", \"minecraft:bamboo_jungle\", \"minecraft:swamp\"]"
    )
    val biomes: List<String> = DEFAULT_RUBBER_TREE_BIOMES,
    /** 每个区块内尝试几次橡胶树放置。 */
    @field:ConfigComment("每个区块进行几次橡胶树放置尝试。", "1")
    val countPerChunk: Int = 1,
    /** 每次尝试的稀有度，64 表示平均 1/64 概率通过。 */
    @field:ConfigComment("每次尝试的稀有度。64 表示每次尝试平均 1/64 概率通过。", "64")
    val rarityChance: Int = 64,
    /** 允许的地表水深，0 表示不允许生成在水面上。 */
    @field:ConfigComment("允许生成时的最大地表水深。0 表示不允许刷在水面上。", "0")
    val maxWaterDepth: Int = 0,
    /** 树干基础高度。 */
    @field:ConfigComment("树干基础高度。", "6")
    val baseHeight: Int = 6,
    /** 树干第一段随机高度。 */
    @field:ConfigComment("树干第一段随机高度。", "2")
    val heightRandA: Int = 2,
    /** 树干第二段随机高度。 */
    @field:ConfigComment("树干第二段随机高度。", "0")
    val heightRandB: Int = 0,
    /** 树冠半径。 */
    @field:ConfigComment("树冠半径。", "2")
    val foliageRadius: Int = 2,
    /** 树冠偏移。 */
    @field:ConfigComment("树冠相对树干顶端的垂直偏移。", "0")
    val foliageOffset: Int = 0,
    /** 树冠高度。 */
    @field:ConfigComment("树冠高度。", "4")
    val foliageHeight: Int = 4,
    /** 每根自然橡胶原木生成 0 个湿孔的权重。默认 11。 */
    @field:ConfigComment("每根自然橡胶原木生成 0 个湿橡胶孔的权重。", "11")
    val zeroHoleWeight: Int = 11,
    /** 每根自然橡胶原木生成 1 个湿孔的权重。默认 2。 */
    @field:ConfigComment("每根自然橡胶原木生成 1 个湿橡胶孔的权重。", "2")
    val singleHoleWeight: Int = 2,
    /** 每根自然橡胶原木生成 2 个湿孔的权重。默认 1。 */
    @field:ConfigComment("每根自然橡胶原木生成 2 个湿橡胶孔的权重。", "1")
    val doubleHoleWeight: Int = 1,
    /** 是否忽略藤蔓阻挡。 */
    @field:ConfigComment("生成时是否忽略藤蔓阻挡。", "true")
    val ignoreVines: Boolean = true,
    /** 是否强制把树底替换成 dirt provider。 */
    @field:ConfigComment("是否强制把树底替换成 dirt provider。", "false")
    val forceDirt: Boolean = false
) {
    /**
     * 统一在读取侧做约束，避免配置写错后在世界生成阶段抛异常。
     */
    fun normalized(): RubberTreeWorldgenConfig = copy(
        biomes = biomes.map(String::trim).filter(String::isNotEmpty).distinct(),
        countPerChunk = countPerChunk.coerceIn(0, 256),
        rarityChance = rarityChance.coerceAtLeast(1),
        maxWaterDepth = maxWaterDepth.coerceAtLeast(0),
        baseHeight = baseHeight.coerceIn(0, 32),
        heightRandA = heightRandA.coerceIn(0, 24),
        heightRandB = heightRandB.coerceIn(0, 24),
        foliageRadius = foliageRadius.coerceIn(0, 8),
        foliageOffset = foliageOffset.coerceIn(-8, 8),
        foliageHeight = foliageHeight.coerceIn(1, 16),
        zeroHoleWeight = zeroHoleWeight.coerceAtLeast(0),
        singleHoleWeight = singleHoleWeight.coerceAtLeast(0),
        doubleHoleWeight = doubleHoleWeight.coerceAtLeast(0)
    )
}

private val DEFAULT_CONFIG_TEMPLATE = Ic2MainConfig(
    general = GeneralConfig(
        logConfigOnLoad = true,
        checkForUpdates = true,
        shockWhenNoEnergyFlow = false
    ),
    recycler = RecyclerConfig(
        blacklist = listOf("minecraft:stick")
    ),
    nuclear = NuclearConfig(
        enableReactorExplosion = true
    ),
    uuReplication = UuReplicationConfig(
        replicationWhitelist = UuReplicationDefaults.defaultWhitelist
    ),
    miner = MinerConfig(),
    miningLaser = MiningLaserConfig(),
    armor = ArmorConfig(),
    worldgen = WorldgenConfig(
        rubberTree = RubberTreeWorldgenConfig()
    )
)

object Ic2Config {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/config")
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.INDENT_OUTPUT, true)
    private val configPath: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("${Ic2_120.MOD_ID}.json")
    }

    @Volatile
    var current: Ic2MainConfig = DEFAULT_CONFIG_TEMPLATE
        private set

    fun loadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("loaded")
    }

    fun reloadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("reloaded")
    }

    fun prettyCurrentConfig(): String {
        return mapper.writeValueAsString(current)
    }

    fun applyServerConfig(json: String) {
        current = mapper.readValue(json)
        logLoaded("applied from server")
    }

    fun getReplicationCostUb(itemId: String): Int? {
        val normalized = itemId.trim()
        if (normalized.isEmpty()) return null
        return current.uuReplication.replicationWhitelist[normalized]
            ?.takeIf { it > 0 }
    }

    fun getReplicationTemplate(itemId: String): UuTemplateEntry? {
        val cost = getReplicationCostUb(itemId) ?: return null
        return UuTemplateEntry(itemId, cost)
    }

    /**
     * 添加或更新物品的UU复制成本到白名单，并保存配置文件。
     * @param itemId 物品ID，如 "minecraft:diamond"
     * @param uuCostUb UU成本，单位uB (micro-buckets)
     * @return 如果保存成功返回true
     */
    fun addOrUpdateReplicationCost(itemId: String, uuCostUb: Int): Boolean {
        return try {
            val normalizedId = itemId.trim()
            if (normalizedId.isEmpty() || uuCostUb <= 0) return false

            // 更新内存中的配置
            val currentWhitelist = current.uuReplication.replicationWhitelist.toMutableMap()
            currentWhitelist[normalizedId] = uuCostUb
            current = current.copy(
                uuReplication = current.uuReplication.copy(
                    replicationWhitelist = currentWhitelist
                )
            )

            // 保存到文件
            saveCurrentConfig()
            true
        } catch (e: Exception) {
            logger.error("Failed to save replication cost for $itemId", e)
            false
        }
    }

    /**
     * 从UU复制白名单中移除物品，并保存配置文件。
     * @param itemId 物品ID
     * @return 如果移除成功返回true
     */
    fun removeReplicationCost(itemId: String): Boolean {
        return try {
            val normalizedId = itemId.trim()
            if (normalizedId.isEmpty()) return false

            // 更新内存中的配置
            val currentWhitelist = current.uuReplication.replicationWhitelist.toMutableMap()
            if (!currentWhitelist.containsKey(normalizedId)) return false

            currentWhitelist.remove(normalizedId)
            current = current.copy(
                uuReplication = current.uuReplication.copy(
                    replicationWhitelist = currentWhitelist
                )
            )

            // 保存到文件
            saveCurrentConfig()
            true
        } catch (e: Exception) {
            logger.error("Failed to remove replication cost for $itemId", e)
            false
        }
    }

    /**
     * 获取所有已配置的UU复制白名单（物品ID -> UU成本）
     */
    fun getAllReplicationCosts(): Map<String, Int> {
        return current.uuReplication.replicationWhitelist
    }

    /**
     * 喷气背包每tick燃料消耗 = 燃料容量 / (飞行时长 × 20 ticks/秒)
     */
    fun getJetpackFuelPerTick(): Double {
        val cfg = current.armor.jetpack
        return cfg.maxFuel.toDouble() / (cfg.flightDurationSeconds * 20.0)
    }

    /**
     * 电力喷气背包每tick能量消耗 = 最大能量 / (飞行时长 × 20 ticks/秒)
     */
    fun getElectricJetpackEuPerTick(): Double {
        val cfg = current.armor.electricJetpack
        return cfg.maxEnergy.toDouble() / (cfg.flightDurationSeconds * 20.0)
    }

    /**
     * 量子胸甲每tick能量消耗 = 最大能量 / (飞行时长 × 20 ticks/秒)
     */
    fun getQuantumChestplateEuPerTick(): Double {
        val cfg = current.armor.quantumChestplate
        return cfg.maxEnergy.toDouble() / (cfg.flightDurationSeconds * 20.0)
    }

    /**
     * 量子护腿神行每tick能量消耗（2档全速）。
     * 1档耗电为 2档的一半。
     */
    fun getQuantumLeggingsEuPerTick(): Double {
        val cfg = current.armor.quantumLeggings
        return cfg.maxEnergy.toDouble() / (cfg.speedBoostDurationSeconds * 20.0)
    }

    /**
     * 量子靴子大跳能量消耗 = 配置值（一次性消耗）
     */
    fun getQuantumBootsJumpEnergyCost(): Long {
        return current.armor.quantumBoots.jumpEnergyCost.coerceAtLeast(1L)
    }

    /**
     * 量子靴子大跳高度倍率
     */
    fun getQuantumBootsJumpHeightMultiplier(): Double {
        return current.armor.quantumBoots.jumpHeightMultiplier.coerceAtLeast(1.0)
    }

    /**
     * 量子头盔每tick夜视能量消耗 = 最大能量 / (夜视时长 × 20 ticks/秒)
     * 注意：量子头盔 maxCapacity 硬编码为 10M EU
     */
    fun getQuantumHelmetNightVisionEuPerTick(): Double {
        val cfg = current.armor.quantumHelmet
        return 10_000_000.0 / (cfg.nightVisionDurationSeconds * 20.0)
    }

    /**
     * 夜视镜每tick能量消耗 = 最大能量 / (夜视时长 × 20 ticks/秒)
     */
    fun getNightVisionGogglesEuPerTick(): Double {
        val cfg = current.armor.nightVision
        return cfg.nightVisionGogglesMaxEnergy.toDouble() / (cfg.nightVisionGogglesDurationSeconds * 20.0)
    }

    /**
     * 纳米头盔每tick能量消耗 = 最大能量 / (夜视时长 × 20 ticks/秒)
     */
    fun getNanoHelmetNightVisionEuPerTick(): Double {
        val cfg = current.armor.nightVision
        return cfg.nanoHelmetMaxEnergy.toDouble() / (cfg.nanoHelmetDurationSeconds * 20.0)
    }

    /**
     * 橡胶靴行走多少格触发一次充电。
     */
    fun getRubberBootsDistance(): Double {
        return current.armor.rubberBoots.distance.coerceAtLeast(1.0)
    }

    /**
     * 橡胶靴每次充电的 EU 数。
     */
    fun getRubberBootsEu(): Long {
        return current.armor.rubberBoots.eu.coerceAtLeast(1L)
    }

    private fun saveCurrentConfig() {
        Files.writeString(configPath, encodeConfigWithComments(current), StandardCharsets.UTF_8)
    }

    private fun readOrCreateDefault(): Ic2MainConfig {
        if (!Files.exists(configPath)) {
            writeDefaultConfig(configPath)
            return DEFAULT_CONFIG_TEMPLATE
        }

        return try {
            val raw = Files.readString(configPath, StandardCharsets.UTF_8)
            val config = mapper.readValue<Ic2MainConfig>(raw)
            val parsedRoot = mapper.readTree(raw) as? ObjectNode
            if (shouldRewriteConfig(parsedRoot, config)) {
                Files.writeString(configPath, encodeConfigWithComments(config), StandardCharsets.UTF_8)
            }
            config
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse config: $configPath", e)
        }
    }

    private fun writeDefaultConfig(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, defaultConfigText(), StandardCharsets.UTF_8)
    }

    private fun defaultConfigText(): String {
        return encodeConfigWithComments(DEFAULT_CONFIG_TEMPLATE)
    }

    private fun shouldRewriteConfig(root: ObjectNode?, config: Ic2MainConfig): Boolean {
        if (root == null) return true
        if (root.has("creative")) return true
        return !containsAllExpectedKeys(root, buildCommentedConfigJson(config))
    }

    private fun encodeConfigWithComments(config: Ic2MainConfig): String =
        mapper.writeValueAsString(buildCommentedConfigJson(config))

    private fun buildCommentedConfigJson(config: Ic2MainConfig): ObjectNode =
        buildCommentedObject(
            instance = config,
            jsonObject = mapper.valueToTree<ObjectNode>(config),
            rootComment = "配置文件允许保留这些 _comment_* 说明字段；程序读取时会自动忽略它们。"
        )

    private fun buildCommentedObject(
        instance: Any,
        jsonObject: ObjectNode,
        rootComment: String? = null
    ): ObjectNode {
        val result = mapper.createObjectNode()
        if (rootComment != null) {
            result.put("_comment", rootComment)
        }

        declaredConfigFields(instance.javaClass).forEach { field ->
            field.isAccessible = true
            val fieldName = field.name
            val valueElement = jsonObject.get(fieldName) ?: return@forEach
            field.getAnnotation(ConfigComment::class.java)?.let { annotation ->
                result.put("_comment_$fieldName", formatComment(annotation))
            }

            val fieldValue = field.get(instance)
            val isNestedConfigObject =
                fieldValue != null &&
                    valueElement is ObjectNode &&
                    !Map::class.java.isAssignableFrom(field.type)

            if (isNestedConfigObject) {
                result.set<JsonNode>(fieldName, buildCommentedObject(fieldValue, valueElement))
            } else {
                result.set<JsonNode>(fieldName, valueElement)
            }
        }
        return result
    }

    private fun containsAllExpectedKeys(actual: JsonNode, expected: JsonNode): Boolean {
        val fields = expected.fields()
        while (fields.hasNext()) {
            val (key, expectedValue) = fields.next()
            val actualValue = actual.get(key) ?: return false
            if (expectedValue.isObject && actualValue.isObject && !containsAllExpectedKeys(actualValue, expectedValue)) {
                return false
            }
        }
        return true
    }

    private fun declaredConfigFields(type: Class<*>): List<java.lang.reflect.Field> =
        type.declaredFields.filterNot { field ->
            field.isSynthetic || Modifier.isStatic(field.modifiers)
        }

    private inline fun <reified T : Any> commentOf(fieldName: String): String =
        T::class.java.getDeclaredField(fieldName).getAnnotation(ConfigComment::class.java)?.let { annotation ->
            if (annotation.defaultValue.isBlank()) {
                annotation.value
            } else {
                "${annotation.value} 默认值: ${annotation.defaultValue}"
            }
        } ?: error("Missing @ConfigComment on ${T::class.java.simpleName}.$fieldName")

    private fun formatComment(annotation: ConfigComment): String =
        if (annotation.defaultValue.isBlank()) {
            annotation.value
        } else {
            "${annotation.value} 默认值: ${annotation.defaultValue}"
        }

    private fun logLoaded(action: String) {
        if (!current.general.logConfigOnLoad) return
        logger.info(
            "Config {}:\n{}",
            action,
            prettyCurrentConfig()
        )
    }
}
