package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.instance
import ic2_120.registry.id
import java.util.function.Consumer
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
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 风力发电机方块。
 *
 * 以高度为动力的发电机，发电量受高度、障碍、天气影响。
 * 极限约 11.46 EU/t，平均每 6.4 秒刷新一次。
 */
@ModBlock(name = "wind_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class WindGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        WindGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, WindGeneratorBlockEntity::class.type()){ w, p, s, be ->
            (be as WindGeneratorBlockEntity).tick(w, p, s)
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
            val generator = GeneratorBlock::class.item()
            if (generator != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WindGeneratorBlock::class.item(), 1)
                    .pattern("I I").pattern(" G ").pattern("I I")
                    .input('I', Items.IRON_INGOT).input('G', generator)
                    .criterion(hasItem(generator), conditionsFromItem(generator))
                    .offerTo(exporter, WindGeneratorBlock::class.id())
            }
        }
    }
}
