package buildcraft_addon.content.block

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PumpBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.state.StateManager
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "pump", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class PumpBlock : BlockWithEntity(
    Settings.create().strength(5.0f, 10.0f).nonOpaque()
), BlockEntityProvider {

    init {
        defaultState = stateManager.defaultState
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {}

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return PumpBlockEntity(pos, state)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? PumpBlockEntity ?: return ActionResult.PASS
            player.sendMessage(net.minecraft.text.Text.literal("液泵: ${be.getTankInfo()}"), true)
        }
        return ActionResult.SUCCESS
    }

    override fun neighborUpdate(state: BlockState, world: World, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? PumpBlockEntity ?: return
            be.checkRedstonePower()
        }
    }

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (!world.isClient) {
            BlockEntityTicker { _, _, _, be -> (be as PumpBlockEntity).serverTick() }
        } else null
    }

    override fun getPickStack(world: net.minecraft.world.BlockView, pos: BlockPos, state: BlockState): ItemStack {
        return ItemStack(this)
    }

    @Suppress("DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        // BC TileMiner.onRemove: 方块被破坏时清除管道
        if (!state.isOf(newState.block) && !world.isClient) {
            val tubeBlock = Registries.BLOCK.get(BuildCraftAddon.id("tube"))
            for (y in pos.y - 1 downTo pos.y - PumpBlockEntity.MAX_DEPTH) {
                val bp = BlockPos(pos.x, y, pos.z)
                if (world.getBlockState(bp).isOf(tubeBlock)) {
                    world.setBlockState(bp, Blocks.AIR.defaultState, 3)
                } else break
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }
}
