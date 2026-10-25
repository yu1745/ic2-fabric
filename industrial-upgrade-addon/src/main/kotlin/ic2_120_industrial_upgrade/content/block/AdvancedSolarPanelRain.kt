package ic2_120_industrial_upgrade.content.block

import ic2_120.compat.save

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.content.block.AdvancedSolarPanelBlock
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
import ic2_120.compat.has

// i18n: block.ic2_120_industrial_upgrade.advanced_solar_panelrain
// zh_cn: 高级雨能太阳能发电机  en_us: Advanced Rain Panel
@ModBlock(name = "advanced_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdvancedSolarPanelRainBlock : AdvancedSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdvancedSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AdvancedSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // "BA"：高级太阳能发电机（ASA）+ RainLinse
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, AdvancedSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', AdvancedSolarPanelBlock::class.item())
                .define('A', RainLinse::class.instance())
                .unlockedBy(getHasName(AdvancedSolarPanelBlock::class.item()), has(AdvancedSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("advanced_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
                .requires(AdvancedSolarPanelRainBlock::class.item())
                .unlockedBy(getHasName(AdvancedSolarPanelRainBlock::class.item()), has(AdvancedSolarPanelRainBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("advanced_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = AdvancedSolarPanelRainBlock::class)
class AdvancedSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    AdvancedSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 8, nightPower = 1, maxStorage = 32000L, tier = 1,
    rainPower = 1, activeProperty = AdvancedSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "advanced_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
