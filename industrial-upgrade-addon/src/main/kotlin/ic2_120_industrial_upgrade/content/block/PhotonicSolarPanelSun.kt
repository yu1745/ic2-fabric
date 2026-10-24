package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.item.SunLinse
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.compat.*
import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has

// i18n: block.ic2_120_industrial_upgrade.photonic_solar_panelsun
// zh_cn: 光子日光太阳能发电机  en_us: Photonic Sun Panel
@ModBlock(name = "photonic_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class PhotonicSolarPanelSunBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PhotonicSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, PhotonicSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // "BA"：光子太阳能发电机 + SunLinse（ASA）
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PhotonicSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', PhotonicSolarPanelBlock::class.item())
                .define('A', SunLinse::class.instance())
                .unlockedBy(getHasName(PhotonicSolarPanelBlock::class.item()), has(PhotonicSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("photonic_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, PhotonicSolarPanelBlock::class.item(), 1)
                .requires(PhotonicSolarPanelSunBlock::class.item())
                .unlockedBy(getHasName(PhotonicSolarPanelSunBlock::class.item()), has(PhotonicSolarPanelSunBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("photonic_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = PhotonicSolarPanelSunBlock::class)
class PhotonicSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    PhotonicSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 4194304, nightPower = 4194304, maxStorage = 1000000000000L, tier = 10,
    sunPower = 8388608, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "photonic_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
