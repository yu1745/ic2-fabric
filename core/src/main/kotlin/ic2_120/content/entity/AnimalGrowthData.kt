package ic2_120.content.entity

import net.minecraft.nbt.CompoundTag

/**
 * 追踪动物的喂食进度和状态
 *
 * @property uuid 动物的唯一标识符
 * @property foodConsumed 累计喂食的食物数量（注意：外来成体建档即 10，仅表示"无需再长大"，
 *   不等于繁殖资格；繁殖资格看 [breedUnlocked]）
 * @property breedUnlocked 繁殖资格已解锁（本次周期喂满 FOOD_TO_BREED 或幼崽长成后达标；
 *   配对消耗后置回 false）。JADE 据此区分"待解锁（继续喂）"与"已解锁（等配偶/空位）"
 * @property canBreed 当前可参与配对（运行态标记；配对后置回 false）
 * @property lastFeedTick 上次喂食的 tick 时间
 * @property foodToday 今天已喂食的数量
 * @property currentDay 当前所在的天数
 */
data class AnimalGrowthData(
    val uuid: java.util.UUID,
    var foodConsumed: Int = 0,
    var breedUnlocked: Boolean = false,
    var canBreed: Boolean = false,
    var lastFeedTick: Long = 0L,
    var foodToday: Int = 0,
    var currentDay: Int = 0,
    var lastHarvestTick: Long = 0L,
    var insecticidePaidToday: Boolean = false  // 是否已支付当日杀虫剂（每天重置）
) {
    /**
     * 将数据序列化为 NBT
     */
    fun toNbt(): CompoundTag {
        val nbt = CompoundTag()
        nbt.putUUID("UUID", uuid)
        nbt.putInt("FoodConsumed", foodConsumed)
        nbt.putBoolean("BreedUnlocked", breedUnlocked)
        nbt.putBoolean("CanBreed", canBreed)
        nbt.putLong("LastFeedTick", lastFeedTick)
        nbt.putInt("FoodToday", foodToday)
        nbt.putInt("CurrentDay", currentDay)
        nbt.putLong("LastHarvestTick", lastHarvestTick)
        return nbt
    }

    companion object {
        /**
         * 从 NBT 反序列化数据
         */
        fun fromNbt(nbt: CompoundTag): AnimalGrowthData {
            return AnimalGrowthData(
                uuid = nbt.getUUID("UUID"),
                foodConsumed = nbt.getInt("FoodConsumed"),
                breedUnlocked = nbt.getBoolean("BreedUnlocked"),
                canBreed = nbt.getBoolean("CanBreed"),
                lastFeedTick = nbt.getLong("LastFeedTick"),
                foodToday = nbt.getInt("FoodToday"),
                currentDay = nbt.getInt("CurrentDay"),
                lastHarvestTick = nbt.getLong("LastHarvestTick")
            )
        }
    }
}
