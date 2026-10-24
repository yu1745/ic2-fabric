package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.content.block.UltimateSolarPanelBlock
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

// i18n: block.ic2_120_industrial_upgrade.ultimate_solar_panelrain
// zh_cn: 终极混合雨能太阳能发电机  en_us: Ultimate Rain Panel
@ModBlock(name = "ultimate_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class UltimateSolarPanelRainBlock : UltimateSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UltimateSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, UltimateSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：终极混合太阳能发电机（ASA）+ RainLinse
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, UltimateSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', UltimateSolarPanelBlock::class.item())
                .define('A', RainLinse::class.instance())
                .unlockedBy(has(UltimateSolarPanelBlock::class.item()), has(UltimateSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("ultimate_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, UltimateSolarPanelBlock::class.item(), 1)
                .requires(UltimateSolarPanelRainBlock::class.item())
                .unlockedBy(has(UltimateSolarPanelRainBlock::class.item()), has(UltimateSolarPanelRainBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("ultimate_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = UltimateSolarPanelRainBlock::class)
class UltimateSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    UltimateSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 512, nightPower = 64, maxStorage = 1000000L, tier = 3,
    rainPower = 64, activeProperty = UltimateSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "ultimate_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
