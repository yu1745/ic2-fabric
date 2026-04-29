package ic2_120.content.entity

import net.minecraft.nbt.NbtCompound

/**
 * 追踪动物的喂食进度和状态
 *
 * @property uuid 动物的唯一标识符
 * @property foodConsumed 累计喂食的食物数量
 * @property canBreed 是否满足繁殖条件
 * @property lastFeedTick 上次喂食的 tick 时间
 * @property foodToday 今天已喂食的数量
 * @property currentDay 当前所在的天数
 */
data class AnimalGrowthData(
    val uuid: java.util.UUID,
    var foodConsumed: Int = 0,
    var canBreed: Boolean = false,
    var lastFeedTick: Long = 0L,
    var foodToday: Int = 0,
    var currentDay: Int = 0
) {
    /**
     * 将数据序列化为 NBT
     */
    fun toNbt(): NbtCompound {
        val nbt = NbtCompound()
        nbt.putUuid("UUID", uuid)
        nbt.putInt("FoodConsumed", foodConsumed)
        nbt.putBoolean("CanBreed", canBreed)
        nbt.putLong("LastFeedTick", lastFeedTick)
        nbt.putInt("FoodToday", foodToday)
        nbt.putInt("CurrentDay", currentDay)
        return nbt
    }

    companion object {
        /**
         * 从 NBT 反序列化数据
         */
        fun fromNbt(nbt: NbtCompound): AnimalGrowthData {
            return AnimalGrowthData(
                uuid = nbt.getUuid("UUID"),
                foodConsumed = nbt.getInt("FoodConsumed"),
                canBreed = nbt.getBoolean("CanBreed"),
                lastFeedTick = nbt.getLong("LastFeedTick"),
                foodToday = nbt.getInt("FoodToday"),
                currentDay = nbt.getInt("CurrentDay")
            )
        }
    }
}
