package ic2_120.content.block

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.machines.NuclearReactorBlockEntity
import ic2_120.content.block.machines.ReactorChamberBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 核反应仓。单独放置无 UI、无容量。
 * 与核反应堆相邻时，右键等效于右键反应堆，打开反应堆的 UI。
 */
@ModBlock(name = "reactor_chamber", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorChamberBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        // 查找相邻核反应堆，打开其 UI
        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            val neighborState = world.getBlockState(neighborPos)
            if (neighborState.block is NuclearReactorBlock) {
                val be = world.getBlockEntity(neighborPos)
                if (be is net.minecraft.screen.NamedScreenHandlerFactory) {
                    player.openHandledScreen(be)
                    return ActionResult.SUCCESS
                }
            }
        }
        return ActionResult.PASS
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return ReactorChamberBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (world.isClient) null
        else BlockEntityTicker { world, pos, state, blockEntity ->
            if (blockEntity is ReactorChamberBlockEntity) {
                ReactorChamberBlockEntity.tick(world, pos, state, blockEntity)
            }
        }
    }
}

/**
 * 核反应堆。中心方块，六面可各接触 0 或 1 个核反应仓扩展容量。
 * 支持 facing 与 active 状态以正确显示模型。
 */
@ModBlock(name = "nuclear_reactor", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class NuclearReactorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    init {
        defaultState = stateManager.defaultState
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        NuclearReactorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ModBlockEntities.getType(NuclearReactorBlockEntity::class)) { w, p, s, be ->
            (be as NuclearReactorBlockEntity).tick(w, p, s)
        }

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

    /**
     * 相邻方块变化时（如反应仓被拆），立即掉落超出新容量的物品。
     */
    override fun neighborUpdate(
        state: BlockState,
        world: World,
        pos: BlockPos,
        sourceBlock: Block,
        sourcePos: BlockPos,
        notify: Boolean
    ) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? NuclearReactorBlockEntity
            be?.dropOverflowItems(world, pos)
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}
