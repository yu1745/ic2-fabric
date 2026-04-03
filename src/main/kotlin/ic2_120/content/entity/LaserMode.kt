package ic2_120.content.entity

/**
 * 采矿镭射枪的模式。
 *
 * 设计原则：一次发射越多弹体，射程越短；单发模式射程最远。
 * 1 发标准模式 64 格为基准射程。
 */
enum class LaserMode(
    val translationKey: String,
    /** 每发消耗能量（EU） */
    val energyCost: Long,
    /** 最大射程 (blocks) */
    val range: Double,
    /** 弹体飞行速度 (blocks/tick) */
    val speed: Double,
    /** 爆炸威力 (0 = 不爆炸, 4.0f = TNT) */
    val explosionPower: Float,
    /** 弹体视觉颜色 (ARGB) */
    val color: Int,
    /** 散射弹体数量 (1 = 单发) */
    val scatterCount: Int,
    /** 散射张角 (度) */
    val scatterSpread: Double,
    /** 对实体伤害 (0 = 无伤害) */
    val entityDamage: Float,
    /** 描述翻译键 */
    val descriptionKey: String
) {
    /** 采矿模式: 基础远程挖矿，击穿一段距离，距离与被挖掘方块硬度有关 */
    MINING(
        translationKey = "mining_laser.mode.mining",
        energyCost = 2_000L,
        range = 64.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFF00BFFF.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 0f,
        descriptionKey = "mining_laser.mode.mining.desc"
    ),
    /** 低聚焦模式: 近距离单发，节约用电，有几率点燃方块 */
    LOW_FOCUS(
        translationKey = "mining_laser.mode.low_focus",
        energyCost = 500L,
        range = 4.0,
        speed = 1.0,
        explosionPower = 0f,
        color = 0xFFFF8800.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 0f,
        descriptionKey = "mining_laser.mode.low_focus.desc"
    ),
    /** 远距模式: 超远射程，对实体伤害最高 19 点 */
    LONG_RANGE(
        translationKey = "mining_laser.mode.long_range",
        energyCost = 5_000L,
        range = 64.0,
        speed = 3.0,
        explosionPower = 0f,
        color = 0xFF44FF44.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 19f,
        descriptionKey = "mining_laser.mode.long_range.desc"
    ),
    /** 超级热线模式: 烧制方块，将矿石烧制成成品（对原木无效） */
    SUPER_HEAT(
        translationKey = "mining_laser.mode.super_heat",
        energyCost = 5_000L,
        range = 8.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFFFF4400.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 0f,
        descriptionKey = "mining_laser.mode.super_heat.desc"
    ),
    /** 散射模式: 25 发采矿模式，3x3 范围 5x5 向前发射 */
    SCATTER(
        translationKey = "mining_laser.mode.scatter",
        energyCost = 12_500L,
        range = 10.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFFFF44FF.toInt(),
        scatterCount = 25,
        scatterSpread = 50.0,
        entityDamage = 0f,
        descriptionKey = "mining_laser.mode.scatter.desc"
    ),
    /** 爆破模式: 约 TNT 当量，穿甲效果，爆炸中心 100 点伤害 */
    EXPLOSIVE(
        translationKey = "mining_laser.mode.explosive",
        energyCost = 10_000L,
        range = 10.0,
        speed = 1.5,
        explosionPower = 4.0f,
        color = 0xFFFF2222.toInt(),
        scatterCount = 1,
        scatterSpread = 0.0,
        entityDamage = 100f,
        descriptionKey = "mining_laser.mode.explosive.desc"
    ),
    /** 3x3 模式: 9 发采矿模式，3x3 断面向前开挖 */
    TRENCH_3X3(
        translationKey = "mining_laser.mode.trench_3x3",
        energyCost = 7_200L,
        range = 20.0,
        speed = 1.5,
        explosionPower = 0f,
        color = 0xFF00CCFF.toInt(),
        scatterCount = 9,
        scatterSpread = 18.0,
        entityDamage = 0f,
        descriptionKey = "mining_laser.mode.trench_3x3.desc"
    );

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
