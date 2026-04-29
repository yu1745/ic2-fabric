package ic2_120.content.entity

import ic2_120.config.Ic2Config

/**
 * 采矿镭射枪的模式。
 *
 * 设计原则：一次发射越多弹体，射程越短；单发模式射程最远。
 * 1 发标准模式 64 格为基准射程。
 *
 * 各模式数值现在从 Ic2Config 配置中读取，可通过 config/ic2_120.json 调整。
 */
enum class LaserMode(
    val translationKey: String,
    /** 描述翻译键 */
    val descriptionKey: String,
    /** 配置键，用于从 Ic2Config 中查找对应的配置 */
    private val configKey: String
) {
    /** 采矿模式: 基础远程挖矿，击穿一段距离，距离与被挖掘方块硬度有关 */
    MINING(
        translationKey = "mining_laser.mode.mining",
        descriptionKey = "mining_laser.mode.mining.desc",
        configKey = "mining"
    ),
    /** 低聚焦模式: 近距离单发，节约用电，有几率点燃方块 */
    LOW_FOCUS(
        translationKey = "mining_laser.mode.low_focus",
        descriptionKey = "mining_laser.mode.low_focus.desc",
        configKey = "lowFocus"
    ),
    /** 远距模式: 超远射程，对实体 3 颗心 */
    LONG_RANGE(
        translationKey = "mining_laser.mode.long_range",
        descriptionKey = "mining_laser.mode.long_range.desc",
        configKey = "longRange"
    ),
    /** 超级热线模式: 烧制方块，将矿石烧制成成品（对原木无效） */
    SUPER_HEAT(
        translationKey = "mining_laser.mode.super_heat",
        descriptionKey = "mining_laser.mode.super_heat.desc",
        configKey = "superHeat"
    ),
    /** 散射模式: 25 发采矿模式，3x3 范围 5x5 向前发射 */
    SCATTER(
        translationKey = "mining_laser.mode.scatter",
        descriptionKey = "mining_laser.mode.scatter.desc",
        configKey = "scatter"
    ),
    /** 爆破模式: 约 TNT 当量，穿甲效果，爆炸中心 100 点伤害 */
    EXPLOSIVE(
        translationKey = "mining_laser.mode.explosive",
        descriptionKey = "mining_laser.mode.explosive.desc",
        configKey = "explosive"
    ),
    /** 3x3 模式: 9 发采矿模式，3x3 断面向前开挖 */
    TRENCH_3X3(
        translationKey = "mining_laser.mode.trench_3x3",
        descriptionKey = "mining_laser.mode.trench_3x3.desc",
        configKey = "trench3x3"
    );

    /** 每发消耗能量（EU） */
    val energyCost: Long
        get() = getConfig().energyCost

    /** 最大射程 (blocks) */
    val range: Double
        get() = getConfig().range

    /** 弹体飞行速度 (blocks/tick) */
    val speed: Double
        get() = getConfig().speed

    /** 爆炸威力 (0 = 不爆炸, 4.0f = TNT) */
    val explosionPower: Float
        get() = getConfig().explosionPower

    /** 弹体视觉颜色 (ARGB) */
    val color: Int
        get() = getConfig().color

    /** 散射弹体数量 (1 = 单发) */
    val scatterCount: Int
        get() = getConfig().scatterCount

    /** 散射张角 (度) */
    val scatterSpread: Double
        get() = getConfig().scatterSpread

    /** 对实体伤害（与 [net.minecraft.entity.LivingEntity.damage] 数值一致，2.0 = 1 颗心） */
    val entityDamage: Float
        get() = getConfig().entityDamage

    /**
     * 从 Ic2Config 获取当前模式的配置。
     */
    private fun getConfig() = when (this) {
        MINING -> Ic2Config.current.miningLaser.mining
        LOW_FOCUS -> Ic2Config.current.miningLaser.lowFocus
        LONG_RANGE -> Ic2Config.current.miningLaser.longRange
        SUPER_HEAT -> Ic2Config.current.miningLaser.superHeat
        SCATTER -> Ic2Config.current.miningLaser.scatter
        EXPLOSIVE -> Ic2Config.current.miningLaser.explosive
        TRENCH_3X3 -> Ic2Config.current.miningLaser.trench3x3
    }

    companion object {
        /** 默认模式 */
        val DEFAULT = MINING

        /** 按 ordinal 循环切换到下一个模式 */
        fun cycle(current: LaserMode): LaserMode {
            val values = entries
            return values[(current.ordinal + 1) % values.size]
        }
    }
}
