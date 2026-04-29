package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.block.AdvancedSolarPanelBlock
import ic2_120_advanced_solar_addon.content.block.HybridSolarPanelBlock
import ic2_120_advanced_solar_addon.content.item.MtCore
import ic2_120_advanced_solar_addon.content.item.EnrichedSunnariumAlloy
import ic2_120.content.block.MachineBlock
import ic2_120.content.item.CoalChunk
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
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
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

// i18n: block.ic2_120_advanced_solar_addon.ultimate_solar_panel
// zh_cn: 终极混合太阳能发电机
// en_us: Ultimate Hybrid Solar Panel
@ModBlock(name = "ultimate_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
class UltimateSolarPanelBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UltimateSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, UltimateSolarPanelBlockEntity::class.type()) { w, p, s, be -> (be as UltimateSolarPanelBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
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
            val mtCore = MtCore::class.instance()
            val enrichedSunnariumAlloy = EnrichedSunnariumAlloy::class.instance()

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, UltimateSolarPanelBlock::class.item(), 1)
                .pattern(" L ")
                .pattern("CMC")
                .pattern("SCS")
                .input('L', Items.LAPIS_BLOCK)
                .input('C', CoalChunk::class.instance())
                .input('M', mtCore)
                .input('S', enrichedSunnariumAlloy)
                .criterion(hasItem(mtCore), conditionsFromItem(mtCore))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("ultimate_solar_panel"))
        }
    }
}

@ModBlockEntity(block = UltimateSolarPanelBlock::class)
class UltimateSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(UltimateSolarPanelBlockEntity::class.type(), pos, state, dayPower = 512, nightPower = 64, maxStorage = 1000000, tier = 3, activeProperty = UltimateSolarPanelBlock.ACTIVE) {

    override fun getBlockName(): String = "ultimate_solar_panel"
}
