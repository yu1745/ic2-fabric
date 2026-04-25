package ic2_120.content.block

import ic2_120.content.block.machines.MetalFormerBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.Coil
import ic2_120.content.item.ToolBox
import ic2_120.content.recipes.metalformer.MetalFormerRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
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
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 金属成型机方块。消耗电力将锭等压制成型（如锭 -> 粒/块）。
 * 能量等级：1
 */
@ModBlock(name = "metal_former", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class MetalFormerBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        MetalFormerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MetalFormerBlockEntity::class.type()){ w, p, s, be -> (be as MetalFormerBlockEntity).tick(w, p, s) }

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

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val coil = Coil::class.instance()
            val toolBox = ToolBox::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && coil != Items.AIR && toolBox != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MetalFormerBlock::class.item(), 1)
                    .pattern(" c ")
                    .pattern("TMT")
                    .pattern("KKK")
                    .input('c', circuit)
                    .input('T', toolBox)
                    .input('M', machine)
                    .input('K', coil)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, MetalFormerBlock::class.id())
            }
            MetalFormerRecipeDatagen.generateRecipes(exporter)
        }
    }
}
