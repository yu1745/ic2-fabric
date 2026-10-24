package ic2_120.content.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.registry.Registries
import ic2_120.Ic2_120

/**
 * 废料箱抽奖物品
 * 右键使用可获得随机物品
 */
open class ScrapBoxItem : Item(Item.Settings()) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)

        if (world.isClient) {
            return TypedActionResult.success(stack)
        }

        // 生成随机物品
        val reward = getRandomDrop(world.random)

        // 给予玩家物品
        if (!user.inventory.insertStack(reward.copy())) {
            // 如果背包满了，就丢在地上
            user.dropItem(reward, false)
        }

        // 消耗废料箱
        stack.decrement(1)

        // 发送消息
        user.sendMessage(
            Text.translatable("item.ic2_120.scrap_box.opened", reward.toHoverableText()),
            true
        )

        return TypedActionResult.success(stack)
    }

    companion object {
        /**
         * 奖池数据：物品及其权重
         * 权重计算方式：百分比 * 10000
         * 例如：0.08% = 8 权重，3.87% = 387 权重
         */
        private val REWARD_POOL = listOf(
            // 稀有物品 (低概率)
            Items.BLAZE_ROD to 8,           // 0.08%
            Items.GOLDEN_HELMET to 2,       // 0.02%
            Items.DIAMOND to 19,            // 0.19%
            Items.ENDER_PEARL to 15,        // 0.15%
            Items.MINECART to 2,            // 0.02%
            Items.EMERALD to 10,            // 0.10%

            // 较稀有物品
            Items.CAKE to 97,               // 0.97%
            Items.IRON_ORE to 97,           // 0.97%
            Items.GOLD_ORE to 97,           // 0.97%

            // 普通物品
            Items.NETHERRACK to 387,        // 3.87%
            Items.ROTTEN_FLESH to 387,      // 3.87%
            Items.COOKED_PORKCHOP to 174,   // 1.74%
            Items.WOODEN_SHOVEL to 193,     // 1.93%
            Items.LEATHER to 193,           // 1.93%
            Items.APPLE to 290,             // 2.90%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "filled_tin_can")) to 290,  // 2.90%
            Items.OAK_SIGN to 193,          // 1.93%
            Items.WOODEN_SWORD to 193,      // 1.93%
            Items.COOKED_BEEF to 174,       // 1.74%
            Items.COAL to 155,              // 1.55% (煤粉对应煤)
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "tin_dust")) to 155,       // 1.55%
            Items.BONE to 193,              // 1.93%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "iron_dust")) to 135,      // 1.35%
            Items.REDSTONE to 174,          // 1.74%
            Items.DIRT to 967,              // 9.67%
            Items.BREAD to 290,             // 2.90%
            Items.STICK to 774,             // 7.74%
            Items.WOODEN_PICKAXE to 193,    // 1.93%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "tin_ore")) to 135,       // 1.35%
            Items.GRASS_BLOCK to 580,       // 5.80%
            Items.COOKED_CHICKEN to 174,    // 1.74%
            Items.GLOWSTONE_DUST to 155,    // 1.55%
            Items.PUMPKIN to 174,           // 1.74%
            Items.GRAVEL to 580,            // 5.80%
            Items.WOODEN_HOE to 969,        // 9.69%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "gold_dust")) to 135,      // 1.35%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "copper_dust")) to 155,    // 1.55%
            Items.SLIME_BALL to 116,        // 1.16%
            Items.FEATHER to 193,           // 1.93%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "single_use_battery")) to 135,  // 1.35%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "copper_ore")) to 135,     // 1.35%
            Items.SOUL_SAND to 193,         // 1.93%
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "rubber")) to 155,         // 1.55%
            Items.EGG to 155                // 1.55%
        )

        // 计算总权重
        private val TOTAL_WEIGHT = REWARD_POOL.sumOf { it.second }

        /**
         * 根据权重随机获取一个物品
         */
        fun getRandomDrop(random: Random): ItemStack {
            val roll = random.nextBetween(1, TOTAL_WEIGHT)
            var currentWeight = 0

            for ((item, weight) in REWARD_POOL) {
                currentWeight += weight
                if (roll <= currentWeight) {
                    return ItemStack(item)
                }
            }

            // 默认返回泥土（最常见）
            return ItemStack(Items.DIRT)
        }
    }
}
