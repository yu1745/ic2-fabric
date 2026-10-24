package ic2_120.content.block.nuclear

import ic2_120.Ic2_120
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 核反应堆。中心方块，六面可各接触 0 或 1 个核反应仓扩展容量。
 * 支持 facing 与 active 状态以正确显示模型。
 */
@ModBlock(name = "nuclear_reactor", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class NuclearReactorBlock(settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    init {
        defaultState = stateManager.defaultState
            .with(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        NuclearReactorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, NuclearReactorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun neighborUpdate(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: net.minecraft.world.level.block.Block,
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

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val generator = GeneratorBlock::class.item()
            val denseLeadPlate = ic2_120.content.item.DenseLeadPlate::class.instance()
            val advancedCircuit = ic2_120.content.item.AdvancedCircuit::class.instance()
            val reactorChamber = ReactorChamberBlock::class.item()
            if (generator != Items.AIR && denseLeadPlate != Items.AIR && advancedCircuit != Items.AIR &&
                reactorChamber != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, NuclearReactorBlock::class.item(), 1)
                    .pattern("DCD").pattern("RRR").pattern("DGD")
                    .input('D', denseLeadPlate).input('C', advancedCircuit).input('R', reactorChamber).input('G', generator)
                    .criterion(hasItem(reactorChamber), conditionsFromItem(reactorChamber))
                    .offerTo(exporter, NuclearReactorBlock::class.id())
            }
        }
    }
}
