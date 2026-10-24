package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.content.block.UltimateSolarPanelBlock
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.item.SunLinse
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

// i18n: block.ic2_120_industrial_upgrade.ultimate_solar_panelsun
// zh_cn: 终极混合日光太阳能发电机  en_us: Ultimate Sun Panel
@ModBlock(name = "ultimate_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class UltimateSolarPanelSunBlock : UltimateSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UltimateSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, UltimateSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：终极混合太阳能发电机（ASA）+ SunLinse
            ShapedRecipeBuilder.create(RecipeCategory.MISC, UltimateSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', UltimateSolarPanelBlock::class.item())
                .input('A', SunLinse::class.instance())
                .criterion(hasItem(UltimateSolarPanelBlock::class.item()), conditionsFromItem(UltimateSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("ultimate_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.create(RecipeCategory.MISC, UltimateSolarPanelBlock::class.item(), 1)
                .input(UltimateSolarPanelSunBlock::class.item())
                .criterion(hasItem(UltimateSolarPanelSunBlock::class.item()), conditionsFromItem(UltimateSolarPanelSunBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("ultimate_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = UltimateSolarPanelSunBlock::class)
class UltimateSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    UltimateSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 512, nightPower = 64, maxStorage = 1000000L, tier = 3,
    sunPower = 1024, activeProperty = UltimateSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "ultimate_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
