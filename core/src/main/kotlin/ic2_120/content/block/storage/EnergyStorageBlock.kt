package ic2_120.content.block.storage

import ic2_120.content.block.DirectionalMachineBlock
import ic2_120.content.item.ICreativeFullVariant
import ic2_120.content.sync.EnergyStorageSync
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World
import ic2_120.getCustomData
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent

/**
 * 储电盒方块基类。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
abstract class EnergyStorageBlock(
    val config: EnergyStorageConfig
) : DirectionalMachineBlock(ic2_120.content.block.MachineBlock.defaultMachineSettings().nonOpaque()) { // 3D 模型不填满整格：关闭相邻面剔除

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    abstract override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity?

    /** 红石模式输出（原版 IC2 7 档模式，默认 0 = 不输出；详见 BE.cycleRedstoneMode）。 */
    override fun emitsRedstonePower(state: BlockState): Boolean = config.emitRedstoneWhenNotFull

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getWeakRedstonePower(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        direction: Direction
    ): Int {
        if (!config.emitRedstoneWhenNotFull) return 0
        val be = world.getBlockEntity(pos) as? EnergyStorageBlockEntity ?: return 0
        return if (be.emitsRedstoneNow) 15 else 0
    }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

<<<<<<< HEAD
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
=======
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
>>>>>>> backup/1.21.1
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
    ) : BlockItem(block, settings), ICreativeFullVariant {
        private fun getStoredEnergy(stack: ItemStack): Long {
                if (stack.getCustomData()?.getBoolean(NBT_FULL) == true) return config.capacity
                // 1.21.1 原生：读取 BLOCK_ENTITY_DATA 组件
                val beData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA)
                if (beData != null) {
                    val nbt = beData.copyNbt()
                    return nbt.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
                }
                // 兼容旧物品：读取 CUSTOM_DATA → BlockEntityTag
                val blockEntityTag = stack.getCustomData()?.getCompound(NBT_BLOCK_ENTITY_TAG) ?: return 0L
                return blockEntityTag.getLong(EnergyStorageSync.NBT_ENERGY_STORED).coerceIn(0L, config.capacity)
            }

        override fun place(context: ItemPlacementContext): ActionResult {
            val result = super.place(context)
            if (result.isAccepted && !context.world.isClient) {
                // BLOCK_ENTITY_DATA 已由 vanilla BlockItem 自动写入 BE
                val nbt = context.stack.getCustomData() ?: return result
                val be = context.world.getBlockEntity(context.blockPos) as? EnergyStorageBlockEntity ?: return result

                // 仅处理 CUSTOM_DATA 中的旧格式（BlockEntityTag）和 Full 标记
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
            context: Item.TooltipContext,
            tooltip: MutableList<Text>,
            type: TooltipType
        ) {
            super.appendTooltip(stack, context, tooltip, type)
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

        override fun createFullVariant(): ItemStack {
            return ItemStack(this).also { it.orCreateNbt.putBoolean(NBT_FULL, true) }
        }
    }
}
