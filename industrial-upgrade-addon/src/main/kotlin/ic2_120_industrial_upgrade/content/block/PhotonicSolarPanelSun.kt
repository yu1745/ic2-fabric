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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
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

// i18n: block.ic2_120_industrial_upgrade.photonic_solar_panelsun
// zh_cn: 光子日光太阳能发电机  en_us: Photonic Sun Panel
@ModBlock(name = "photonic_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class PhotonicSolarPanelSunBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PhotonicSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, PhotonicSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：光子太阳能发电机 + SunLinse（ASA）
            ShapedRecipeBuilder.create(RecipeCategory.MISC, PhotonicSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', PhotonicSolarPanelBlock::class.item())
                .input('A', SunLinse::class.instance())
                .criterion(hasItem(PhotonicSolarPanelBlock::class.item()), conditionsFromItem(PhotonicSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("photonic_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.create(RecipeCategory.MISC, PhotonicSolarPanelBlock::class.item(), 1)
                .input(PhotonicSolarPanelSunBlock::class.item())
                .criterion(hasItem(PhotonicSolarPanelSunBlock::class.item()), conditionsFromItem(PhotonicSolarPanelSunBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("photonic_solar_panelsun_revert"))
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
