package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.item.EnrichedSunnarium
import ic2_120_advanced_solar_addon.content.item.ReinforcedIridiumIronPlate
import ic2_120_advanced_solar_addon.content.block.AdvancedSolarPanelBlock
import ic2_120.content.block.MachineBlock
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.CarbonPlate
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

// i18n: block.ic2_120_advanced_solar_addon.hybrid_solar_panel
// zh_cn: 混合太阳能发电机
// en_us: Hybrid Solar Panel
@ModBlock(name = "hybrid_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
class HybridSolarPanelBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        HybridSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, HybridSolarPanelBlockEntity::class.type()) { w, p, s, be -> (be as HybridSolarPanelBlockEntity).tick(w, p, s) }

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
            val advancedSolar = AdvancedSolarPanelBlock::class.item()
            val circuit = AdvancedCircuit::class.instance()
            val carbonPlate = CarbonPlate::class.instance()
            val reinforcedPlate = ReinforcedIridiumIronPlate::class.instance()
            val enrichedSunnarium = EnrichedSunnarium::class.instance()

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, HybridSolarPanelBlock::class.item(), 1)
                .pattern("CLC")
                .pattern("RAA")
                .pattern("CEC")
                .input('C', carbonPlate)
                .input('L', Items.LAPIS_BLOCK)
                .input('R', reinforcedPlate)
                .input('A', advancedSolar)
                .input('E', enrichedSunnarium)
                .criterion(hasItem(advancedSolar), conditionsFromItem(advancedSolar))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("hybrid_solar_panel"))
        }
    }
}

@ModBlockEntity(block = HybridSolarPanelBlock::class)
class HybridSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(HybridSolarPanelBlockEntity::class.type(), pos, state, dayPower = 64, nightPower = 8, maxStorage = 100000, tier = 2, activeProperty = HybridSolarPanelBlock.ACTIVE) {

    override fun getBlockName(): String = "hybrid_solar_panel"
}
