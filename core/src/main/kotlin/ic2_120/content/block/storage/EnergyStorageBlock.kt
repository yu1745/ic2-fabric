package ic2_120.content.block.storage

import ic2_120.content.block.DirectionalMachineBlock
import ic2_120.content.item.ICreativeFullVariant
import ic2_120.content.sync.EnergyStorageSync
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

/**
 * 储电盒方块基类。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
abstract class EnergyStorageBlock(
    val config: EnergyStorageConfig
) : DirectionalMachineBlock(ic2_120.content.block.MachineBlock.defaultMachineSettings().nonOpaque()) { // 3D 模型不填满整格：关闭相邻面剔除

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    abstract override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity?

    /** 红石模式输出（原版 IC2 7 档模式，默认 0 = 不输出；详见 BE.cycleRedstoneMode）。 */
    override fun emitsRedstonePower(state: BlockState): Boolean = config.emitRedstoneWhenNotFull

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getWeakRedstonePower(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        direction: Direction
    ): Int {
        if (!config.emitRedstoneWhenNotFull) return 0
        val be = world.getBlockEntity(pos) as? EnergyStorageBlockEntity ?: return 0
        return if (be.emitsRedstoneNow) 15 else 0
    }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return InteractionResult.SUCCESS
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
    ) : BlockItem(block, settings), ICreativeFullVariant {
        private fun getStoredEnergy(stack: ItemStack): Long {
            if (stack.nbt?.getBoolean(NBT_FULL) == true) return config.capacity
            val blockEntityTag = stack.getSubNbt(NBT_BLOCK_ENTITY_TAG) ?: return 0L
            return blockEntityTag.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
        }

        override fun place(context: BlockPlaceContext): InteractionResult {
            val result = super.place(context)
            if (result.isAccepted && !context.world.isClient) {
                val nbt = context.stack.nbt ?: return result
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

        override fun getName(stack: ItemStack): net.minecraft.network.chat.Component =
            if (stack.nbt?.getBoolean(NBT_FULL) == true)
                net.minecraft.network.chat.Component.translatable(translationKeyFull)
            else
                super.getName(stack)

        @Environment(EnvType.CLIENT)
        override fun appendTooltip(
            stack: ItemStack,
            world: Level?,
            tooltip: MutableList<Component>,
            context: TooltipContext
        ) {
            super.appendTooltip(stack, world, tooltip, context)
            val stored = getStoredEnergy(stack)
            tooltip.add(Component.literal("能量: %,d / %,d EU".format(stored, config.capacity)).formatted(ChatFormatting.GRAY))
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

        override fun createFullVariant(): ItemStack {
            return ItemStack(this).also { it.orCreateNbt.putBoolean(NBT_FULL, true) }
        }
    }
}
