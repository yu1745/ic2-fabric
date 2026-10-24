package ic2_120.content.block

import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Containers
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 简单的机器方块基类。
 * 可被子类扩展以支持 BlockEntity。
 * 放置时根据玩家朝向设置 HORIZONTAL_FACING（玩家面向的方向的相反方向为方块正面）。
 *
 * 默认硬度为铁方块硬度（5.0f, 6.0f），子类可通过传入自定义 settings 覆盖。
 */
abstract class MachineBlock(settings: BlockBehaviour.Settings = defaultMachineSettings()) : BaseEntityBlock(settings) {

    companion object {
        /** 机器方块的默认设置：铁方块硬度（5.0f, 6.0f） */
        fun defaultMachineSettings(): BlockBehaviour.Settings =
            BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)

        val MACHINE_CODEC: MapCodec<MachineBlock> = Block.createCodec { error("MachineBlock cannot be deserialized from JSON") }
    }

    override fun getCodec(): MapCodec<out BaseEntityBlock> = MACHINE_CODEC

    /**
     * 非扳手拆卸时掉落的物品（默认基础机器外壳）。
     * 子类可覆写以改为高级机器外壳或其他物品。
     */
    open fun getCasingDrop(): Item =
        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "machine"))

    init {
        defaultState = stateManager.defaultState.with(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(BlockStateProperties.HORIZONTAL_FACING)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        defaultState.with(BlockStateProperties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing.opposite)

    override fun onPlaced(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.onPlaced(world, pos, state, placer, stack)
        if (placer is ServerPlayer) {
            (world.getBlockEntity(pos) as? IOwned)?.ownerUuid = placer.uuid
        }
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * 方块被破坏时，将 BlockEntity 内 Container 的物品散落掉落，而非凭空消失。
     * 仅当方块被替换（如挖掉）且未被活塞推动时生效。
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Container) {
                Containers.spawn(world, pos, be)
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    /**
     * BaseEntityBlock 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): RenderShape = RenderShape.MODEL
}

/**
 * 支持六面朝向的机器方块基类。
 *
 * 与 [MachineBlock] 的区别：
 * - 使用 [BlockStateProperties.FACING] 支持六面朝向（上下南北西东）
 * - 放置时根据玩家视线决定朝向，正面朝向玩家并保留上下方向
 * - 适用于需要任意方向放置的机器（如变压器）
 *
 * 其他行为（硬度、物品掉落、Container 散落等）与 [MachineBlock] 完全相同。
 */
abstract class DirectionalMachineBlock(settings: BlockBehaviour.Settings = defaultMachineSettings()) : BaseEntityBlock(settings) {

    companion object {
        /** 机器方块的默认设置：铁方块硬度（5.0f, 6.0f） */
        fun defaultMachineSettings(): BlockBehaviour.Settings =
            BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)

        val DIRECTIONAL_CODEC: MapCodec<DirectionalMachineBlock> = Block.createCodec { error("DirectionalMachineBlock cannot be deserialized from JSON") }
    }

    override fun getCodec(): MapCodec<out BaseEntityBlock> = DIRECTIONAL_CODEC

    /**
     * 非扳手拆卸时掉落的物品（默认基础机器外壳）。
     * 子类可覆写以改为高级机器外壳或其他物品。
     */
    open fun getCasingDrop(): Item =
        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "machine"))

    init {
        defaultState = stateManager.defaultState.with(BlockStateProperties.FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(BlockStateProperties.FACING)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? {
        // 与原版发射器、木桶一致：正面朝向玩家，同时允许上下六面放置。
        val facing = ctx.playerLookDirection.opposite
        return defaultState.with(BlockStateProperties.FACING, facing)
    }

    override fun onPlaced(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.onPlaced(world, pos, state, placer, stack)
        if (placer is ServerPlayer) {
            (world.getBlockEntity(pos) as? IOwned)?.ownerUuid = placer.uuid
        }
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * 方块被破坏时，将 BlockEntity 内 Container 的物品散落掉落，而非凭空消失。
     * 仅当方块被替换（如挖掉）且未被活塞推动时生效。
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Container) {
                Containers.spawn(world, pos, be)
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    /**
     * BaseEntityBlock 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): RenderShape = RenderShape.MODEL
}
