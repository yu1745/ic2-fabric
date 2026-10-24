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

// i18n: block.ic2_120_industrial_upgrade.hybrid_solar_panelsun
// zh_cn: 混合日光太阳能发电机  en_us: Hybrid Sun Panel
@ModBlock(name = "hybrid_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class HybridSolarPanelSunBlock : HybridSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        HybridSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, HybridSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // "BA"：混合太阳能发电机（ASA）+ SunLinse
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, HybridSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', HybridSolarPanelBlock::class.item())
                .input('A', SunLinse::class.instance())
                .criterion(hasItem(HybridSolarPanelBlock::class.item()), conditionsFromItem(HybridSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("hybrid_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, HybridSolarPanelBlock::class.item(), 1)
                .input(HybridSolarPanelSunBlock::class.item())
                .criterion(hasItem(HybridSolarPanelSunBlock::class.item()), conditionsFromItem(HybridSolarPanelSunBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("hybrid_solar_panelsun_revert"))
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
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
