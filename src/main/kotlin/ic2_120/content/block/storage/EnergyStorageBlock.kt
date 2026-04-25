package ic2_120.content.block.storage

import ic2_120.content.block.MachineBlock
import ic2_120.content.sync.EnergyStorageSync
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.getCustomData

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

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    companion object {
        const val NBT_FULL = "Full"
        const val NBT_BLOCK_ENTITY_TAG = "BlockEntityTag"
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }

    abstract class EnergyStorageBlockItem(
        block: Block,
        settings: Item.Settings,
        protected val config: EnergyStorageConfig
    ) : BlockItem(block, settings) {
        private fun getStoredEnergy(stack: ItemStack): Long {
            if (stack.getCustomData()?.getBoolean(NBT_FULL) == true) return config.capacity
            val blockEntityTag = stack.getCustomData()?.getCompound(NBT_BLOCK_ENTITY_TAG) ?: return 0L
            return blockEntityTag.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
        }

        override fun place(context: ItemPlacementContext): ActionResult {
            val result = super.place(context)
            if (result.isAccepted && !context.world.isClient) {
                val nbt = context.stack.getCustomData() ?: return result
                val be = context.world.getBlockEntity(context.blockPos) as? EnergyStorageBlockEntity ?: return result
                val blockEntityTag = nbt.getCompound(NBT_BLOCK_ENTITY_TAG)
                if (!blockEntityTag.isEmpty && blockEntityTag.contains(EnergyStorageSync.NBT_ENERGY_STORED)) {
                    val stored = blockEntityTag.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
                    be.sync.restoreEnergy(stored)
                    be.markDirty()
                } else if (nbt.getBoolean(NBT_FULL)) {
                    be.sync.restoreEnergy(config.capacity)
                    be.markDirty()
                }
            }
            return result
        }

        override fun getName(stack: ItemStack): net.minecraft.text.Text =
            if (stack.getCustomData()?.getBoolean(NBT_FULL) == true)
                net.minecraft.text.Text.translatable(translationKeyFull)
            else
                super.getName(stack)

        @Environment(EnvType.CLIENT)
        override fun appendTooltip(
            stack: ItemStack,
            world: World?,
            tooltip: MutableList<Text>,
            context: TooltipContext
        ) {
            super.appendTooltip(stack, world, tooltip, context)
            val stored = getStoredEnergy(stack)
            tooltip.add(Text.literal("能量: %,d / %,d EU".format(stored, config.capacity)).formatted(Formatting.GRAY))
        }

        override fun isItemBarVisible(stack: ItemStack): Boolean = true

        override fun getItemBarStep(stack: ItemStack): Int {
            val ratio = if (config.capacity > 0L) getStoredEnergy(stack).toDouble() / config.capacity.toDouble() else 0.0
            return (ratio * 13.0).toInt().coerceIn(0, 13)
        }

        override fun getItemBarColor(stack: ItemStack): Int {
            val ratio = if (config.capacity > 0L) getStoredEnergy(stack).toDouble() / config.capacity.toDouble() else 0.0
            return when {
                ratio > 0.5 -> 0x4AFF4A
                ratio > 0.2 -> 0xFFFF4A
                else -> 0xFF4A4A
            }
        }

        protected abstract val translationKeyFull: String
    }
}
