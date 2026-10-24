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
import net.minecraft.text.Text
import ic2_120_industrial_upgrade.content.item.Photoniy
import ic2_120_industrial_upgrade.content.item.SolarSplitter
import ic2_120_industrial_upgrade.content.item.SpectralCore
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

// i18n: block.ic2_120_industrial_upgrade.spectral_solar_panel
// zh_cn: 光谱太阳能发电机
// en_us: Spectral Solar Panel
@ModBlock(name = "spectral_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SpectralSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = SpectralSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, SpectralSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 十字：4 个量子太阳能 + 1 个光谱核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SpectralSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .input('B', QuantumSolarPanelBlock::class.item())
                .input('A', SpectralCore::class.instance())
                .criterion(hasItem(QuantumSolarPanelBlock::class.item()), conditionsFromItem(QuantumSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("spectral_solar_panel"))
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
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
