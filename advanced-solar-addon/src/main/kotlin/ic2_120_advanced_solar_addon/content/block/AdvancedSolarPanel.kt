package ic2_120_advanced_solar_addon.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.MenuProvider
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer

// i18n: block.ic2_120_advanced_solar_addon.advanced_solar_panel
// zh_cn: 高级太阳能发电机
// en_us: Advanced Solar Panel
@ModBlock(name = "advanced_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
open class AdvancedSolarPanelBlock : MachineBlock() {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdvancedSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, AdvancedSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val solarPanel = SolarGeneratorBlock::class.item()
            val circuit = AdvancedCircuit::class.instance()
            val glass = ReinforcedGlassBlock::class.item()
            val alloy = Alloy::class.instance()
            val irradiantPlate = IrradiantReinforcedPlate::class.instance()

            // // 配方 1: 简易合成模式 (防爆玻璃 + 太阳能发电机 + 高级合金 + 高级机械外壳 + 高级电路板)
            // ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
            //     .pattern("GGG")
            //     .pattern("ASA")
            //     .pattern("C C")
            //     .define('G', Items.GLASS)
            //     .define('A', alloy)
            //     .define('S', solarPanel)
            //     .define('C', circuit)
            //     .unlockedBy(getHasName(solarPanel), has(solarPanel))
            //     .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("advanced_solar_panel_simple"))

            // 配方 2: 使用光辉玻璃板
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
                .pattern("GGG")
                .pattern("ASA")
                .pattern("CIC")
                .define('G', glass)
                .define('A', alloy)
                .define('S', solarPanel)
                .define('C', circuit)
                .define('I', irradiantPlate)
                .unlockedBy(getHasName(solarPanel), has(solarPanel))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("advanced_solar_panel_irradiant"))

            // // 配方 3: 使用防爆玻璃 + 光辉铱铁合金板
            // ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
            //     .pattern("GGG")
            //     .pattern("ASA")
            //     .pattern("CIC")
            //     .define('G', Items.GLASS)
            //     .define('A', alloy)
            //     .define('S', solarPanel)
            //     .define('C', circuit)
            //     .define('I', irradiantPlate)
            //     .unlockedBy(getHasName(solarPanel), has(solarPanel))
            //     .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("advanced_solar_panel_glass"))
        }
    }
}

@ModBlockEntity(block = AdvancedSolarPanelBlock::class)
class AdvancedSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(AdvancedSolarPanelBlockEntity::class.type(), pos, state, dayPower = 8, nightPower = 1, maxStorage = 32000, tier = 1, activeProperty = AdvancedSolarPanelBlock.ACTIVE)
