package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.StirlingGeneratorBlockEntity
import ic2_120.content.item.HeatConductor
import ic2_120.content.item.IronCasing
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.id
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.Formatting
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

@ModBlock(name = "stirling_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class StirlingGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        StirlingGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, StirlingGeneratorBlockEntity::class.type()){ w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip.add(Text.translatable("tooltip.ic2_120.stirling_generator.ratio").formatted(Formatting.GRAY))
        tooltip.add(Text.translatable("tooltip.ic2_120.stirling_generator.max_output").formatted(Formatting.GRAY))
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
            val generator = GeneratorBlock::class.item()
            val ironCasing = IronCasing::class.instance()
            val heatConductor = HeatConductor::class.instance()
            if (generator != Items.AIR && ironCasing != Items.AIR && heatConductor != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, StirlingGeneratorBlock::class.item(), 1)
                    .pattern("CHC")
                    .pattern("CGC")
                    .pattern("CCC")
                    .input('C', ironCasing)
                    .input('H', heatConductor)
                    .input('G', generator)
                    .criterion(hasItem(generator), conditionsFromItem(generator))
                    .offerTo(exporter, StirlingGeneratorBlock::class.id())
            }
        }
    }
}
