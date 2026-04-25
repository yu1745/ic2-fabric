package ic2_120.content.block

import ic2_120.content.block.machines.RtHeatGeneratorBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberBlock
import ic2_120.content.item.HeatConductor
import ic2_120.content.item.IronCasing
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "rt_heat_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "heat")
class RtHeatGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        RtHeatGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, RtHeatGeneratorBlockEntity::class.type()){ w, p, s, be ->
            (be as RtHeatGeneratorBlockEntity).tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState
            .with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing)
            .with(ACTIVE, false)

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
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
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val ironCasing = IronCasing::class.instance()
            val reactorChamber = ReactorChamberBlock::class.item()
            val heatConductor = HeatConductor::class.instance()
            if (ironCasing != Items.AIR && reactorChamber != Items.AIR && heatConductor != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RtHeatGeneratorBlock::class.item(), 1)
                    .pattern("CCC")
                    .pattern("CRC")
                    .pattern("CHC")
                    .input('C', ironCasing)
                    .input('R', reactorChamber)
                    .input('H', heatConductor)
                    .criterion(hasItem(reactorChamber), conditionsFromItem(reactorChamber))
                    .offerTo(exporter, RtHeatGeneratorBlock::class.id())
            }
        }
    }
}
