package ic2_120.content.block

import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.item.EmptyCell
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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

@ModBlock(name = "fluid_heat_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "heat")
class FluidHeatGeneratorBlock : MachineBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        FluidHeatGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, FluidHeatGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as FluidHeatGeneratorBlockEntity).tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState
            .with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing)
            .with(ACTIVE, false)

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            val storage = FluidStorage.SIDED.find(world, pos, hit.side)
                ?: FluidStorage.SIDED.find(world, pos, null)
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
                return ActionResult.SUCCESS
            }
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val casing = IronCasing::class.instance()
            val emptyCell = EmptyCell::class.instance()
            val heat = HeatConductor::class.instance()
            if (casing != Items.AIR && emptyCell != Items.AIR && heat != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FluidHeatGeneratorBlock::class.item(), 1)
                    .pattern("LEL")
                    .pattern("LEL")
                    .pattern("CHC")
                    .input('L', Items.GLASS)
                    .input('E', emptyCell)
                    .input('C', casing)
                    .input('H', heat)
                    .criterion(hasItem(heat), conditionsFromItem(heat))
                    .offerTo(exporter, FluidHeatGeneratorBlock::class.id())
            }
        }
    }
}
