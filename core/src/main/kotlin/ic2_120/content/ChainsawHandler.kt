package ic2_120.content

import ic2_120.content.item.Chainsaw
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Shearable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

/** 1.12 IC2 链锯的剪刀模式：实体剪切与剪刀专用方块掉落。 */
object ChainsawHandler {
    fun register() {
        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            val stack = player.getStackInHand(hand)
            if (stack.item !is Chainsaw || !Chainsaw.isShearEnabled(stack)) {
                return@register ActionResult.PASS
            }
            if (entity !is Shearable || !entity.isShearable()) {
                return@register ActionResult.PASS
            }
            if (world.isClient) return@register ActionResult.SUCCESS
            if (!consumeUseEnergy(stack, player)) return@register ActionResult.FAIL

            entity.sheared(SoundCategory.PLAYERS)
            ActionResult.SUCCESS
        }

        AttackBlockCallback.EVENT.register { player, world, hand, pos, _ ->
            val stack = player.getStackInHand(hand)
            if (stack.item !is Chainsaw || !Chainsaw.isShearEnabled(stack)) {
                return@register ActionResult.PASS
            }
            val state = world.getBlockState(pos)
            if (!isShearableBlock(state)) return@register ActionResult.PASS
            if (world.isClient) return@register ActionResult.SUCCESS
            if (!hasUseEnergy(stack, player)) return@register ActionResult.FAIL
            if (world !is ServerWorld) return@register ActionResult.FAIL

            val blockEntity = world.getBlockEntity(pos)
            val shears = ItemStack(Items.SHEARS)
            val broken = world.breakBlock(pos, false, player)
            if (!broken) return@register ActionResult.FAIL

            Block.dropStacks(state, world, pos, blockEntity, player, shears)
            consumeUseEnergy(stack, player)
            ActionResult.SUCCESS
        }
    }

    private fun consumeUseEnergy(stack: ItemStack, player: PlayerEntity): Boolean {
        if (!hasUseEnergy(stack, player)) return false
        if (player.isCreative) return true
        val energy = (stack.item as Chainsaw).getEnergy(stack)
        (stack.item as Chainsaw).setEnergy(stack, energy - Chainsaw.ENERGY_PER_USE)
        return true
    }

    private fun hasUseEnergy(stack: ItemStack, player: PlayerEntity): Boolean =
        player.isCreative || (stack.item as Chainsaw).getEnergy(stack) >= Chainsaw.ENERGY_PER_USE

    private fun isShearableBlock(state: BlockState): Boolean =
        state.isIn(BlockTags.LEAVES) ||
            state.isOf(Blocks.COBWEB) ||
            state.isOf(Blocks.GRASS) ||
            state.isOf(Blocks.FERN) ||
            state.isOf(Blocks.DEAD_BUSH) ||
            state.isOf(Blocks.HANGING_ROOTS) ||
            state.isOf(Blocks.VINE) ||
            state.isOf(Blocks.TRIPWIRE) ||
            state.isIn(BlockTags.WOOL)
}
