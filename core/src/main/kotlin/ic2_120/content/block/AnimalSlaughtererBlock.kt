package ic2_120.content.block

import ic2_120.content.block.machines.AnimalSlaughtererBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.IronPlate
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
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "animal_slaughterer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class AnimalSlaughtererBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AnimalSlaughtererBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AnimalSlaughtererBlockEntity::class.type()){ w, p, s, be ->
            (be as AnimalSlaughtererBlockEntity).tick(w, p, s)
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
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ActionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val ironPlate = IronPlate::class.instance()
            if (machine == Items.AIR || circuit == Items.AIR || ironPlate == Items.AIR) return

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AnimalSlaughtererBlock::class.item(), 1)
                .pattern("CBC")
                .pattern("SMS")
                .pattern("PNP")
                .input('C', circuit)
                .input('B', Items.BARRIER)
                .input('S', Items.SHEARS)
                .input('M', machine)
                .input('P', ironPlate)
                .input('N', Items.IRON_BARS)
                .criterion(hasItem(machine), conditionsFromItem(machine))
                .offerTo(exporter, AnimalSlaughtererBlock::class.id())
        }
    }
}
