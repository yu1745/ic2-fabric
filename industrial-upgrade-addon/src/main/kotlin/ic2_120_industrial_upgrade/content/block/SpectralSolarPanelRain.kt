package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.item.RainLinse
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.text.Text
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

// i18n: block.ic2_120_industrial_upgrade.spectral_solar_panelrain
// zh_cn: 光谱雨能太阳能发电机  en_us: Spectral Rain Panel
@ModBlock(name = "spectral_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SpectralSolarPanelRainBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        SpectralSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, SpectralSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // "BA"：光谱太阳能发电机 + RainLinse（ASA）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SpectralSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', SpectralSolarPanelBlock::class.item())
                .input('A', RainLinse::class.instance())
                .criterion(hasItem(SpectralSolarPanelBlock::class.item()), conditionsFromItem(SpectralSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("spectral_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SpectralSolarPanelBlock::class.item(), 1)
                .input(SpectralSolarPanelRainBlock::class.item())
                .criterion(hasItem(SpectralSolarPanelRainBlock::class.item()), conditionsFromItem(SpectralSolarPanelRainBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("spectral_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = SpectralSolarPanelRainBlock::class)
class SpectralSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    SpectralSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 16384, nightPower = 8192, maxStorage = 100000000L, tier = 6,
    rainPower = 8192, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "spectral_solar_panelrain"
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
