package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.MachineCasingBlock
import ic2_120.content.block.machines.PumpBlockEntity
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.minecraft.block.BlockState
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
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

@ModBlock(name = "pump", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class PumpBlock : MachineBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PumpBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, PumpBlockEntity::class.type()){ w, p, s, be ->
            (be as PumpBlockEntity).tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            val storage = FluidStorage.SIDED.find(world, pos, hit.side)
                ?: FluidStorage.SIDED.find(world, pos, null)
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, net.minecraft.util.Hand.MAIN_HAND)) {
                return ActionResult.SUCCESS
            }
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ActionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val machine = MachineCasingBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            val emptyCell = ic2_120.content.item.EmptyCell::class.instance()
            val miningPipe = MiningPipeBlock::class.item()
            val treetap = ic2_120.content.item.Treetap::class.instance()
            if (machine != Items.AIR && emptyCell != Items.AIR && miningPipe != Items.AIR && treetap != Items.AIR && circuit != Items.AIR) {
                ConsumeTreetapShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = PumpBlock::class.id(),
                    result = PumpBlock::class.item(),
                    pattern = listOf("ECE", "EME", "PTP"),
                    keys = mapOf('E' to emptyCell, 'C' to circuit, 'M' to machine, 'P' to miningPipe, 'T' to treetap)
                )
            }
        }
    }
}
