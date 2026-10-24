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

// i18n: block.ic2_120_industrial_upgrade.advanced_solar_panelsun
// zh_cn: 高级日光太阳能发电机  en_us: Advanced Sun Panel
@ModBlock(name = "advanced_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdvancedSolarPanelSunBlock : AdvancedSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdvancedSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, AdvancedSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // "BA"：高级太阳能发电机（ASA）+ SunLinse
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', AdvancedSolarPanelBlock::class.item())
                .input('A', SunLinse::class.instance())
                .criterion(hasItem(AdvancedSolarPanelBlock::class.item()), conditionsFromItem(AdvancedSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("advanced_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedSolarPanelBlock::class.item(), 1)
                .input(AdvancedSolarPanelSunBlock::class.item())
                .criterion(hasItem(AdvancedSolarPanelSunBlock::class.item()), conditionsFromItem(AdvancedSolarPanelSunBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("advanced_solar_panelsun_revert"))
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
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
