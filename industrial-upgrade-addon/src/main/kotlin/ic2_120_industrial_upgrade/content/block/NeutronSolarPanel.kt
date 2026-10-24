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
import ic2_120_industrial_upgrade.content.item.NeutronCore
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

// i18n: block.ic2_120_industrial_upgrade.neutron_solar_panel
// zh_cn: 中子太阳能发电机
// en_us: Neutron Solar Panel
@ModBlock(name = "neutron_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class NeutronSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = NeutronSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, NeutronSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 十字：4 个光子太阳能 + 1 个中子核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, NeutronSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .input('B', PhotonicSolarPanelBlock::class.item())
                .input('A', NeutronCore::class.instance())
                .criterion(hasItem(PhotonicSolarPanelBlock::class.item()), conditionsFromItem(PhotonicSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("neutron_solar_panel"))
        }
    }
}

@ModBlockEntity(block = NeutronSolarPanelBlock::class)
class NeutronSolarPanelBlockEntity(pos: BlockPos, state: BlockState) : SolarPanelBlockEntity(
    NeutronSolarPanelBlockEntity::class.type(), pos, state,
    dayPower = 16777216, nightPower = 16777216, maxStorage = 10_000_000_000_000L, tier = 11,
    activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "neutron_solar_panel"
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
