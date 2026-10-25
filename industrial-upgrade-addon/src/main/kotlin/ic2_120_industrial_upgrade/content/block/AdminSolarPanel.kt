package ic2_120_industrial_upgrade.content.block

import ic2_120.compat.save

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
import ic2_120_industrial_upgrade.content.item.QuantCore2
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
import ic2_120.compat.has

// i18n: block.ic2_120_industrial_upgrade.admin_solar_panel
// zh_cn: 管理员太阳能发电机
// en_us: Admin Solar Panel
@ModBlock(name = "admin_solar_panel", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdminSolarPanelBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AdminSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AdminSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 十字：4 个奇点太阳能 + 1 个量子核心II
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, AdminSolarPanelBlock::class.item(), 1)
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .define('B', SingularSolarPanelBlock::class.item())
                .define('A', QuantCore2::class.instance())
                .unlockedBy(getHasName(SingularSolarPanelBlock::class.item()), has(SingularSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("admin_solar_panel"))
        }
    }
}

@ModBlockEntity(block = AdminSolarPanelBlock::class)
class AdminSolarPanelBlockEntity(pos: BlockPos, state: BlockState) : SolarPanelBlockEntity(
    AdminSolarPanelBlockEntity::class.type(), pos, state,
    dayPower = 1048576, nightPower = 1048576, maxStorage = 100_000_000_000L, tier = 9,
    activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "admin_solar_panel"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
