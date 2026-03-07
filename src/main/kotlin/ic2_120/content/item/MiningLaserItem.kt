package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 采矿镭射枪：电动采矿工具，使用镭射束远程破坏方块。
 * 消耗耐久（后续可接 EU 能源系统）。
 */
@ModItem(name = "mining_laser", tab = CreativeTab.IC2_TOOLS)
class MiningLaserItem : Item(
    FabricItemSettings()
        .maxDamage(1000)
) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (stack.damage >= stack.maxDamage) return TypedActionResult.pass(stack)

        val hit = user.raycast(8.0, 0f, false)
        if (hit is BlockHitResult) {
            val pos = hit.blockPos
            val state = world.getBlockState(pos)
            if (!world.isClient && state.block.hardness >= 0 && canBreak(state)) {
                val broken = world.breakBlock(pos, true, user)
                if (broken) {
                    stack.damage(1, user as LivingEntity) { it.sendToolBreakStatus(hand) }
                    return TypedActionResult.success(stack, true)
                }
            }
        }
        return TypedActionResult.pass(stack)
    }

    private fun canBreak(state: BlockState): Boolean {
        if (state.isIn(BlockTags.WITHER_IMMUNE) || state.isIn(BlockTags.DRAGON_IMMUNE)) return false
        return state.block.hardness >= 0
    }

    override fun isDamageable() = true

    override fun getItemBarColor(stack: ItemStack) = 0x00BFFF // 电量/耐久条颜色（青蓝）
}
