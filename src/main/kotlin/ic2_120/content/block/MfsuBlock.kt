package ic2_120.content.block

import ic2_120.content.sync.MfsuSync
import ic2_120.content.block.machines.MfsuBlockEntity
import ic2_120.content.ModBlockEntities
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.state.StateManager
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * MFSU 储电箱方块。仅能量存储，无物品槽。正面允许输入，其余面仅输出。
 * 能量等级：4
 */
@ModBlock(name = "mfsu", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class MfsuBlock : MachineBlock() {

    override val tier: Int = 4

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        MfsuBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ModBlockEntities.getType(MfsuBlockEntity::class)) { w, p, s, be -> (be as MfsuBlockEntity).tick(w, p, s) }

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
        /** NBT 键：物品为满电变体（仅创造模式可拿） */
        const val NBT_FULL = "Full"
    }

    /**
     * 自定义 BlockItem：放置时若物品带 [NBT_FULL]，则方块实体设为满电。
     */
    class MfsuBlockItem(block: Block, settings: net.minecraft.item.Item.Settings) : BlockItem(block, settings) {
        override fun place(context: ItemPlacementContext): ActionResult {
            val result = super.place(context)
            if (result.isAccepted && !context.world.isClient) {
                val nbt = context.stack.nbt ?: return result
                if (nbt.getBoolean(NBT_FULL)) {
                    val be = context.world.getBlockEntity(context.blockPos) as? MfsuBlockEntity ?: return result
                    be.sync.amount = MfsuSync.ENERGY_CAPACITY
                    be.sync.syncCommittedAmount()
                    be.markDirty()
                }
            }
            return result
        }

        override fun getName(stack: ItemStack): Text =
            if (stack.nbt?.getBoolean(NBT_FULL) == true)
                Text.translatable("block.ic2_120.mfsu_full")
            else
                super.getName(stack)
    }
}
