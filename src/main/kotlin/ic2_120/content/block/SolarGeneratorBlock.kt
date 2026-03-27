package ic2_120.content.block

import ic2_120.content.block.machines.SolarGeneratorBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.CoalDust
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import java.util.function.Consumer

/**
 * 太阳能发电机方块。
 * 仅在白天、主世界、无遮挡、无雨雪时发电，1 EU/t 输出。
 */
@ModBlock(name = "solar_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class SolarGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        SolarGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, SolarGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as SolarGeneratorBlockEntity).tick(w, p, s)
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

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val generator = GeneratorBlock::class.item()
            val coalDust = CoalDust::class.instance()
            val circuit = Circuit::class.instance()
            if (generator != Items.AIR && coalDust != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SolarGeneratorBlock::class.item(), 1)
                    .pattern("DLD")
                    .pattern("LDL")
                    .pattern("CGC")
                    .input('D', coalDust)
                    .input('L', Items.GLASS)
                    .input('C', circuit)
                    .input('G', generator)
                    .criterion(hasItem(generator), conditionsFromItem(generator))
                    .offerTo(exporter, SolarGeneratorBlock::class.id())
            }
        }
    }
}
