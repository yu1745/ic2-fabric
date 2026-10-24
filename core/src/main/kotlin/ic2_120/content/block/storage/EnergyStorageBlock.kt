package ic2_120.content.block.storage

import ic2_120.compat.orCreateNbt

import ic2_120.compat.stack

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.block.DirectionalMachineBlock
import ic2_120.content.item.ICreativeFullVariant
import ic2_120.content.sync.EnergyStorageSync
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.item.TooltipFlag
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
) : DirectionalMachineBlock(ic2_120.content.block.MachineBlock.defaultMachineSettings() .noOcclusion()) { // 3D 模型不填满整格：关闭相邻面剔除

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    abstract override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity?

    /** 红石模式输出（原版 IC2 7 档模式，默认 0 = 不输出；详见 BE.cycleRedstoneMode）。 */
    override fun isSignalSource(state: BlockState): Boolean = config.emitRedstoneWhenNotFull

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getSignal(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        direction: Direction
    ): Int {
        if (!config.emitRedstoneWhenNotFull) return 0
        val be = world.getBlockEntity(pos) as? EnergyStorageBlockEntity ?: return 0
        return if (be.emitsRedstoneNow) 15 else 0
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult, hand: InteractionHand): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        const val NBT_FULL = "Full"
        const val NBT_BLOCK_ENTITY_TAG = "BlockEntityTag"
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
    }

   abstract class EnergyStorageBlockItem(
       block: Block,
       settings: Item.Properties,
       protected val config: EnergyStorageConfig
    ) : BlockItem(block, settings), ICreativeFullVariant {
        private fun getStoredEnergy(stack: ItemStack): Long {
            // TODO(1.21.1): 1.21.1 ItemStack 无 getTag/getSubTag，改走 CUSTOM_DATA 组件。
            val nbt = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return 0L
            if (nbt.getBoolean(NBT_FULL)) return config.capacity
            val blockEntityTag = nbt.getCompound(NBT_BLOCK_ENTITY_TAG)
            return blockEntityTag.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
        }

        override fun place(context: BlockPlaceContext): InteractionResult {
            val result = super.place(context)
            if (result.consumesAction() && !context.level.isClientSide) {
                val nbt = context.itemInHand.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return result
                val be = context.level.getBlockEntity(context.clickedPos) as? EnergyStorageBlockEntity ?: return result
                val blockEntityTag = nbt.getCompound(NBT_BLOCK_ENTITY_TAG)
                if (!blockEntityTag.isEmpty && blockEntityTag.contains(EnergyStorageSync.NBT_ENERGY_STORED)) {
                    val stored = blockEntityTag.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
                    be.sync.restoreEnergy(stored)
                    be.setChanged()
                } else if (nbt.getBoolean(NBT_FULL)) {
                    be.sync.restoreEnergy(config.capacity)
                    be.setChanged()
                }
            }
            return result
        }

        override fun getName(stack: ItemStack): net.minecraft.network.chat.Component =
            if (stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getBoolean(NBT_FULL) == true)
                net.minecraft.network.chat.Component.translatable(translationKeyFull)
            else
                super.getName(stack)

        @Environment(EnvType.CLIENT)
        override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
            super.appendHoverText(stack, context, tooltip, type)
            val stored = getStoredEnergy(stack)
            tooltip.add(Component.literal("能量: %,d / %,d EU".format(stored, config.capacity)).withStyle(ChatFormatting.GRAY))
        }

        override fun isBarVisible(stack: ItemStack): Boolean = true

        override fun getBarWidth(stack: ItemStack): Int {
            val ratio = if (config.capacity > 0L) getStoredEnergy(stack).toDouble() / config.capacity.toDouble() else 0.0
            return (ratio * 13.0).toInt().coerceIn(0, 13)
        }

        override fun getBarColor(stack: ItemStack): Int {
            val ratio = if (config.capacity > 0L) getStoredEnergy(stack).toDouble() / config.capacity.toDouble() else 0.0
            return when {
                ratio > 0.5 -> 0x4AFF4A
                ratio > 0.2 -> 0xFFFF4A
                else -> 0xFF4A4A
            }
        }

        protected abstract val translationKeyFull: String

        override fun createFullVariant(): ItemStack {
            return ItemStack(this).also { it.orCreateNbt = it.orCreateNbt.apply { putBoolean(NBT_FULL, true) } }
        }
    }
}
