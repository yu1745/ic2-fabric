package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.content.block.AdvancedSolarPanelBlock
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

// i18n: block.ic2_120_industrial_upgrade.advanced_solar_panelsun
// zh_cn: 高级日光太阳能发电机  en_us: Advanced Sun Panel
@ModBlock(name = "advanced_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdvancedSolarPanelSunBlock : AdvancedSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdvancedSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AdvancedSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：高级太阳能发电机（ASA）+ SunLinse
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', AdvancedSolarPanelBlock::class.item())
                .define('A', SunLinse::class.instance())
                .unlockedBy(has(AdvancedSolarPanelBlock::class.item()), has(AdvancedSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("advanced_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
                .requires(AdvancedSolarPanelSunBlock::class.item())
                .unlockedBy(has(AdvancedSolarPanelSunBlock::class.item()), has(AdvancedSolarPanelSunBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("advanced_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = AdvancedSolarPanelSunBlock::class)
class AdvancedSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    AdvancedSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 8, nightPower = 1, maxStorage = 32000L, tier = 1,
    sunPower = 16, activeProperty = AdvancedSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "advanced_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
