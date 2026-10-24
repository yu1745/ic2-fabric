package ic2_120_advanced_solar_addon.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120_advanced_solar_addon.content.block.AdvancedSolarPanelBlock
import ic2_120_advanced_solar_addon.content.item.EnrichedSunnarium
import ic2_120.content.block.MachineBlock
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.CoalChunk
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

// i18n: block.ic2_120_advanced_solar_addon.ultimate_solar_panel
// zh_cn: 终极混合太阳能发电机
// en_us: Ultimate Hybrid Solar Panel
@ModBlock(name = "ultimate_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
open class UltimateSolarPanelBlock : MachineBlock() {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UltimateSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, UltimateSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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
            val advancedSolar = AdvancedSolarPanelBlock::class.item()
            val enrichedSunnarium = EnrichedSunnarium::class.instance()

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, UltimateSolarPanelBlock::class.item(), 1)
                .pattern(" L ")
                .pattern("CAC")
                .pattern("SCS")
                .define('L', Items.LAPIS_BLOCK)
                .define('C', CoalChunk::class.instance())
                .define('A', advancedSolar)
                .define('S', enrichedSunnarium)
                .unlockedBy(getHasName(advancedSolar), has(advancedSolar))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("ultimate_solar_panel"))

            // 8×混合太阳能 + 居中高级电路板 → 终极混合太阳能
            val hybridSolar = HybridSolarPanelBlock::class.item()
            val advancedCircuit = AdvancedCircuit::class.instance()

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, UltimateSolarPanelBlock::class.item(), 1)
                .pattern("HHH")
                .pattern("HCH")
                .pattern("HHH")
                .define('H', hybridSolar)
                .define('C', advancedCircuit)
                .unlockedBy(getHasName(hybridSolar), has(hybridSolar))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("ultimate_solar_panel_from_hybrid"))
        }
    }
}

@ModBlockEntity(block = UltimateSolarPanelBlock::class)
class UltimateSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(UltimateSolarPanelBlockEntity::class.type(), pos, state, dayPower = 512, nightPower = 64, maxStorage = 1000000, tier = 3, activeProperty = UltimateSolarPanelBlock.ACTIVE) {

    override fun getBlockName(): String = "ultimate_solar_panel"
}
