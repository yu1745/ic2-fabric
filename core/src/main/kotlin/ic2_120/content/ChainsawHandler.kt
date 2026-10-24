package ic2_120.content

import ic2_120.compat.isClient

import ic2_120.content.item.Chainsaw
import ic2_120.integration.ftbchunks.ClaimProtection
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Shearable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.tags.BlockTags
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos

/** 1.12 IC2 链锯的剪刀模式：实体剪切与剪刀专用方块掉落。 */
object ChainsawHandler {
    fun register() {
        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            val stack = player.getItemInHand(hand)
            if (stack.item !is Chainsaw || !Chainsaw.isShearEnabled(stack)) {
                return@register InteractionResult.PASS
            }
            if (entity !is Shearable || !entity.readyForShearing()) {
                return@register InteractionResult.PASS
            }
            if (!world.isClient && ClaimProtection.isProtected(world, entity.blockPosition(), player, ClaimProtection.INTERACT_ENTITY)) {
                return@register InteractionResult.FAIL
            }
            if (world.isClient) return@register InteractionResult.SUCCESS
            if (!consumeUseEnergy(stack, player)) return@register InteractionResult.FAIL

            entity.shear(SoundSource.PLAYERS)
            InteractionResult.SUCCESS
        }

        AttackBlockCallback.EVENT.register { player, world, hand, pos, _ ->
            val stack = player.getItemInHand(hand)
            if (stack.item !is Chainsaw || !Chainsaw.isShearEnabled(stack)) {
                return@register InteractionResult.PASS
            }
            val state = world.getBlockState(pos)
            if (!isShearableBlock(state)) return@register InteractionResult.PASS
            if (!world.isClient && ClaimProtection.isProtected(world, pos, player, ClaimProtection.EDIT_BLOCK)) {
                return@register InteractionResult.FAIL
            }
            if (world.isClient) return@register InteractionResult.SUCCESS
            if (!hasUseEnergy(stack, player)) return@register InteractionResult.FAIL
            if (world !is ServerLevel) return@register InteractionResult.FAIL

            val blockEntity = world.getBlockEntity(pos)
            val shears = ItemStack(Items.SHEARS)
            val broken = world.destroyBlock(pos, false, player, 512)
            if (!broken) return@register InteractionResult.FAIL

            Block.getDrops(state, world, pos, blockEntity, player, shears).forEach { dropped ->
                Block.popResource(world, pos, dropped)
            }
            state.spawnAfterBreak(world, pos, shears, true)
            consumeUseEnergy(stack, player)
            InteractionResult.SUCCESS
        }
    }

    private fun consumeUseEnergy(stack: ItemStack, player: Player): Boolean {
        if (!hasUseEnergy(stack, player)) return false
        if (player.isCreative) return true
        val energy = (stack.item as Chainsaw).getEnergy(stack)
        (stack.item as Chainsaw).setEnergy(stack, energy - Chainsaw.ENERGY_PER_USE)
        return true
    }

    private fun hasUseEnergy(stack: ItemStack, player: Player): Boolean =
        player.isCreative || (stack.item as Chainsaw).getEnergy(stack) >= Chainsaw.ENERGY_PER_USE

    private fun isShearableBlock(state: BlockState): Boolean =
        state.`is`(BlockTags.LEAVES) ||
            state.`is`(Blocks.COBWEB) ||
            state.`is`(Blocks.SHORT_GRASS) ||
            state.`is`(Blocks.FERN) ||
            state.`is`(Blocks.DEAD_BUSH) ||
            state.`is`(Blocks.HANGING_ROOTS) ||
            state.`is`(Blocks.VINE) ||
            state.`is`(Blocks.TRIPWIRE) ||
            state.`is`(BlockTags.WOOL)
}
