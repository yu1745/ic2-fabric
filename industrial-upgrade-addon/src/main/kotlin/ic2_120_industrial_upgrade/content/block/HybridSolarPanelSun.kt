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
import ic2_120_industrial_upgrade.content.item.SunLinse
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

// i18n: block.ic2_120_industrial_upgrade.hybrid_solar_panelsun
// zh_cn: 混合日光太阳能发电机  en_us: Hybrid Sun Panel
@ModBlock(name = "hybrid_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class HybridSolarPanelSunBlock : HybridSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        HybridSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, HybridSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：混合太阳能发电机（ASA）+ SunLinse
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HybridSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', HybridSolarPanelBlock::class.item())
                .define('A', SunLinse::class.instance())
                .unlockedBy(has(HybridSolarPanelBlock::class.item()), has(HybridSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("hybrid_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HybridSolarPanelBlock::class.item(), 1)
                .requires(HybridSolarPanelSunBlock::class.item())
                .unlockedBy(has(HybridSolarPanelSunBlock::class.item()), has(HybridSolarPanelSunBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("hybrid_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = HybridSolarPanelSunBlock::class)
class HybridSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    HybridSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 64, nightPower = 8, maxStorage = 100000L, tier = 2,
    sunPower = 128, activeProperty = HybridSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "hybrid_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
