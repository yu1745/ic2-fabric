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
import net.minecraft.network.chat.Component
import ic2_120_industrial_upgrade.content.item.QuantCore1
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
import ic2_120.compat.*
import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has

// i18n: block.ic2_120_industrial_upgrade.photonic_solar_panel
// zh_cn: 光子太阳能发电机
// en_us: Photonic Solar Panel
@ModBlock(name = "photonic_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class PhotonicSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = PhotonicSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, PhotonicSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 十字：4 个管理员太阳能 + 1 个量子核心I
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PhotonicSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .define('B', AdminSolarPanelBlock::class.item())
                .define('A', QuantCore1::class.instance())
                .unlockedBy(getHasName(AdminSolarPanelBlock::class.item()), has(AdminSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("photonic_solar_panel"))
        }
    }
}

@ModBlockEntity(block = PhotonicSolarPanelBlock::class)
class PhotonicSolarPanelBlockEntity(pos: BlockPos, state: BlockState) : SolarPanelBlockEntity(
    PhotonicSolarPanelBlockEntity::class.type(), pos, state,
    dayPower = 4194304, nightPower = 4194304, maxStorage = 1_000_000_000_000L, tier = 10,
    activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "photonic_solar_panel"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
