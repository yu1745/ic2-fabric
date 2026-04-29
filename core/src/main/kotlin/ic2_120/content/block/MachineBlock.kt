package ic2_120.content.block

import ic2_120.Ic2_120
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 简单的机器方块基类。
 * 可被子类扩展以支持 BlockEntity。
 * 放置时根据玩家朝向设置 HORIZONTAL_FACING（玩家面向的方向的相反方向为方块正面）。
 *
 * 默认硬度为铁方块硬度（5.0f, 6.0f），子类可通过传入自定义 settings 覆盖。
 */
abstract class MachineBlock(settings: AbstractBlock.Settings = defaultMachineSettings()) : BlockWithEntity(settings) {

    companion object {
        /** 机器方块的默认设置：铁方块硬度（5.0f, 6.0f） */
        fun defaultMachineSettings(): AbstractBlock.Settings =
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
    }

    /**
     * 非扳手拆卸时掉落的物品（默认基础机器外壳）。
     * 子类可覆写以改为高级机器外壳或其他物品。
     */
    open fun getCasingDrop(): Item =
        Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "machine"))

    init {
        defaultState = stateManager.defaultState.with(Properties.HORIZONTAL_FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.HORIZONTAL_FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing.opposite)

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.onPlaced(world, pos, state, placer, stack)
        if (placer is ServerPlayerEntity) {
            (world.getBlockEntity(pos) as? IOwned)?.ownerUuid = placer.uuid
        }
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * 方块被破坏时，将 BlockEntity 内 Inventory 的物品散落掉落，而非凭空消失。
     * 仅当方块被替换（如挖掉）且未被活塞推动时生效。
     */
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Inventory) {
                ItemScatterer.spawn(world, pos, be)
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    /**
     * BlockWithEntity 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
}

/**
 * 支持六面朝向的机器方块基类。
 *
 * 与 [MachineBlock] 的区别：
 * - 使用 [Properties.FACING] 支持六面朝向（上下南北西东）
 * - 放置时根据玩家点击的面决定朝向（点击哪个面，方块就朝向那个面）
 * - 适用于需要任意方向放置的机器（如变压器）
 *
 * 其他行为（硬度、物品掉落、Inventory 散落等）与 [MachineBlock] 完全相同。
 */
abstract class DirectionalMachineBlock(settings: AbstractBlock.Settings = defaultMachineSettings()) : BlockWithEntity(settings) {

    companion object {
        /** 机器方块的默认设置：铁方块硬度（5.0f, 6.0f） */
        fun defaultMachineSettings(): AbstractBlock.Settings =
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
    }

    /**
     * 非扳手拆卸时掉落的物品（默认基础机器外壳）。
     * 子类可覆写以改为高级机器外壳或其他物品。
     */
    open fun getCasingDrop(): Item =
        Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "machine"))

    init {
        defaultState = stateManager.defaultState.with(Properties.FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        // 根据玩家点击的面决定朝向：玩家点击哪个面，方块就朝向那个面
        val facing = ctx.side.opposite
        return defaultState.with(Properties.FACING, facing)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * 方块被破坏时，将 BlockEntity 内 Inventory 的物品散落掉落，而非凭空消失。
     * 仅当方块被替换（如挖掉）且未被活塞推动时生效。
     */
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Inventory) {
                ItemScatterer.spawn(world, pos, be)
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    /**
     * BlockWithEntity 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
}
