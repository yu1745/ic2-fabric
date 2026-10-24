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
import ic2_120_industrial_upgrade.content.item.SingularCore
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

// i18n: block.ic2_120_industrial_upgrade.singular_solar_panel
// zh_cn: 奇点太阳能发电机
// en_us: Singular Solar Panel
@ModBlock(name = "singular_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SingularSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = SingularSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, SingularSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 十字：4 个质子太阳能 + 1 个奇点核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SingularSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .input('B', ProtonSolarPanelBlock::class.item())
                .input('A', SingularCore::class.instance())
                .criterion(hasItem(ProtonSolarPanelBlock::class.item()), conditionsFromItem(ProtonSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("singular_solar_panel"))
        }
    }
}

@ModBlockEntity(block = SingularSolarPanelBlock::class)
class SingularSolarPanelBlockEntity(pos: BlockPos, state: BlockState) : SolarPanelBlockEntity(
    SingularSolarPanelBlockEntity::class.type(), pos, state,
    dayPower = 262144, nightPower = 131072, maxStorage = 10_000_000_000L, tier = 8,
    activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "singular_solar_panel"
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
