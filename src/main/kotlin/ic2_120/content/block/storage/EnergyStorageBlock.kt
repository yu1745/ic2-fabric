package ic2_120.content.block.storage

import ic2_120.content.block.MachineBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.state.StateManager
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 储电盒方块基类。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
abstract class EnergyStorageBlock(
    val config: EnergyStorageConfig
) : MachineBlock() {

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
    }

    abstract override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity?

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    companion object {
        const val NBT_FULL = "Full"
    }

    abstract class EnergyStorageBlockItem(
        block: Block,
        settings: Item.Settings,
        protected val config: EnergyStorageConfig
    ) : BlockItem(block, settings) {
        override fun place(context: ItemPlacementContext): ActionResult {
            val result = super.place(context)
            if (result.isAccepted && !context.world.isClient) {
                val nbt = context.stack.nbt ?: return result
                if (nbt.getBoolean(NBT_FULL)) {
                    val be = context.world.getBlockEntity(context.blockPos) as? EnergyStorageBlockEntity ?: return result
                    be.sync.restoreEnergy(config.capacity)
                    be.markDirty()
                }
            }
            return result
        }

        override fun getName(stack: ItemStack): net.minecraft.text.Text =
            if (stack.nbt?.getBoolean(NBT_FULL) == true)
                net.minecraft.text.Text.translatable(translationKeyFull)
            else
                super.getName(stack)

        protected abstract val translationKeyFull: String
    }
}
