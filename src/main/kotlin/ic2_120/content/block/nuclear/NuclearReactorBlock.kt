package ic2_120.content.block.nuclear

import ic2_120.Ic2_120
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
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
        else validateTicker(type, NuclearReactorBlockEntity::class.type()){ w, p, s, be ->
            (be as NuclearReactorBlockEntity).tick(w, p, s)
        }

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

    override fun neighborUpdate(
        state: BlockState,
        world: World,
        pos: BlockPos,
        sourceBlock: net.minecraft.block.Block,
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
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val generator = GeneratorBlock::class.item()
            val denseLeadPlate = ic2_120.content.item.DenseLeadPlate::class.instance()
            val advancedCircuit = ic2_120.content.item.AdvancedCircuit::class.instance()
            val reactorChamber = ReactorChamberBlock::class.item()
            if (generator != Items.AIR && denseLeadPlate != Items.AIR && advancedCircuit != Items.AIR &&
                reactorChamber != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, NuclearReactorBlock::class.item(), 1)
                    .pattern("DCD").pattern("RRR").pattern("DGD")
                    .input('D', denseLeadPlate).input('C', advancedCircuit).input('R', reactorChamber).input('G', generator)
                    .criterion(hasItem(reactorChamber), conditionsFromItem(reactorChamber))
                    .offerTo(exporter, NuclearReactorBlock::class.id())
            }
        }
    }
}
