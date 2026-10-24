package ic2_120_industrial_upgrade.content.block

import ic2_120_advanced_solar_addon.content.block.QuantumSolarPanelBlock
import ic2_120_advanced_solar_addon.content.block.SolarPanelBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.network.chat.Component
import ic2_120_industrial_upgrade.content.item.Photoniy
import ic2_120_industrial_upgrade.content.item.SolarSplitter
import ic2_120_industrial_upgrade.content.item.SpectralCore
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer

// i18n: block.ic2_120_industrial_upgrade.spectral_solar_panel
// zh_cn: 光谱太阳能发电机
// en_us: Spectral Solar Panel
@ModBlock(name = "spectral_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SpectralSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = SpectralSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, SpectralSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 十字：4 个量子太阳能 + 1 个光谱核心
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SpectralSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .define('B', QuantumSolarPanelBlock::class.item())
                .define('A', SpectralCore::class.instance())
                .unlockedBy(has(QuantumSolarPanelBlock::class.item()), has(QuantumSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("spectral_solar_panel"))
        }
    }
}

@ModBlockEntity(block = SpectralSolarPanelBlock::class)
class SpectralSolarPanelBlockEntity(pos: BlockPos, state: BlockState) : SolarPanelBlockEntity(
    SpectralSolarPanelBlockEntity::class.type(), pos, state,
    dayPower = 16384, nightPower = 8192, maxStorage = 100_000_000L, tier = 6,
    activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "spectral_solar_panel"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
