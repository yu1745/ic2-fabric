package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.item.IrradiantReinforcedPlate
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.block.MachineCasingBlock
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.Alloy
import ic2_120.content.block.ReinforcedGlassBlock
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

// i18n: block.ic2_120_advanced_solar_addon.advanced_solar_panel
// zh_cn: 高级太阳能发电机
// en_us: Advanced Solar Panel
@ModBlock(name = "advanced_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
class AdvancedSolarPanelBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdvancedSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, AdvancedSolarPanelBlockEntity::class.type()) { w, p, s, be -> (be as AdvancedSolarPanelBlockEntity).tick(w, p, s) }

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
            val solarPanel = SolarGeneratorBlock::class.item()
            val circuit = AdvancedCircuit::class.instance()
            val glass = ReinforcedGlassBlock::class.item()
            val alloy = Alloy::class.instance()
            val irradiantPlate = IrradiantReinforcedPlate::class.instance()

            // // 配方 1: 简易合成模式 (防爆玻璃 + 太阳能发电机 + 高级合金 + 高级机械外壳 + 高级电路板)
            // ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
            //     .pattern("GGG")
            //     .pattern("ASA")
            //     .pattern("C C")
            //     .input('G', Items.GLASS)
            //     .input('A', alloy)
            //     .input('S', solarPanel)
            //     .input('C', circuit)
            //     .criterion(hasItem(solarPanel), conditionsFromItem(solarPanel))
            //     .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("advanced_solar_panel_simple"))

            // 配方 2: 使用光辉玻璃板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
                .pattern("GGG")
                .pattern("ASA")
                .pattern("CIC")
                .input('G', glass)
                .input('A', alloy)
                .input('S', solarPanel)
                .input('C', circuit)
                .input('I', irradiantPlate)
                .criterion(hasItem(solarPanel), conditionsFromItem(solarPanel))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("advanced_solar_panel_irradiant"))

            // // 配方 3: 使用防爆玻璃 + 光辉铱铁合金板
            // ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
            //     .pattern("GGG")
            //     .pattern("ASA")
            //     .pattern("CIC")
            //     .input('G', Items.GLASS)
            //     .input('A', alloy)
            //     .input('S', solarPanel)
            //     .input('C', circuit)
            //     .input('I', irradiantPlate)
            //     .criterion(hasItem(solarPanel), conditionsFromItem(solarPanel))
            //     .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("advanced_solar_panel_glass"))
        }
    }
}

@ModBlockEntity(block = AdvancedSolarPanelBlock::class)
class AdvancedSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(AdvancedSolarPanelBlockEntity::class.type(), pos, state, dayPower = 8, nightPower = 1, maxStorage = 32000, tier = 1, activeProperty = AdvancedSolarPanelBlock.ACTIVE)
