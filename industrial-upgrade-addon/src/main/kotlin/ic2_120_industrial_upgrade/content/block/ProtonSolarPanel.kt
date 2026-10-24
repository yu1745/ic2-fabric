package ic2_120_industrial_upgrade.content.block

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
import ic2_120_industrial_upgrade.content.item.EnrichedSunnariumAlloy4
import ic2_120_industrial_upgrade.content.item.Proton
import ic2_120_industrial_upgrade.content.item.ProtonCore
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

// i18n: block.ic2_120_industrial_upgrade.proton_solar_panel
// zh_cn: 质子太阳能发电机
// en_us: Proton Solar Panel
@ModBlock(name = "proton_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class ProtonSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = ProtonSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, ProtonSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 十字：4 个光谱太阳能 + 1 个质子核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ProtonSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .input('B', SpectralSolarPanelBlock::class.item())
                .input('A', ProtonCore::class.instance())
                .criterion(hasItem(SpectralSolarPanelBlock::class.item()), conditionsFromItem(SpectralSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("proton_solar_panel"))
        }
    }
}

@ModBlockEntity(block = ProtonSolarPanelBlock::class)
class ProtonSolarPanelBlockEntity(pos: BlockPos, state: BlockState) : SolarPanelBlockEntity(
    ProtonSolarPanelBlockEntity::class.type(), pos, state,
    dayPower = 65536, nightPower = 32768, maxStorage = 1_000_000_000L, tier = 7,
    activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "proton_solar_panel"
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
