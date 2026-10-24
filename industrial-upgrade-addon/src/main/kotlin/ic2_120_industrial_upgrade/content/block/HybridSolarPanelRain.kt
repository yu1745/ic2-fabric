package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.content.block.HybridSolarPanelBlock
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.item.RainLinse
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.compat.*
import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has

// i18n: block.ic2_120_industrial_upgrade.hybrid_solar_panelrain
// zh_cn: 混合雨能太阳能发电机  en_us: Hybrid Rain Panel
@ModBlock(name = "hybrid_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class HybridSolarPanelRainBlock : HybridSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        HybridSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, HybridSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // "BA"：混合太阳能发电机（ASA）+ RainLinse
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HybridSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', HybridSolarPanelBlock::class.item())
                .define('A', RainLinse::class.instance())
                .unlockedBy(getHasName(HybridSolarPanelBlock::class.item()), has(HybridSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("hybrid_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HybridSolarPanelBlock::class.item(), 1)
                .requires(HybridSolarPanelRainBlock::class.item())
                .unlockedBy(getHasName(HybridSolarPanelRainBlock::class.item()), has(HybridSolarPanelRainBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("hybrid_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = HybridSolarPanelRainBlock::class)
class HybridSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    HybridSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 64, nightPower = 8, maxStorage = 100000L, tier = 2,
    rainPower = 8, activeProperty = HybridSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "hybrid_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
