package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.item.RainLinse
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

// i18n: block.ic2_120_industrial_upgrade.singular_solar_panelrain
// zh_cn: 奇异雨能太阳能发电机  en_us: Singular Rain Panel
@ModBlock(name = "singular_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SingularSolarPanelRainBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        SingularSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, SingularSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：奇异太阳能发电机 + RainLinse（ASA）
            ShapedRecipeBuilder.create(RecipeCategory.MISC, SingularSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', SingularSolarPanelBlock::class.item())
                .input('A', RainLinse::class.instance())
                .criterion(hasItem(SingularSolarPanelBlock::class.item()), conditionsFromItem(SingularSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("singular_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.create(RecipeCategory.MISC, SingularSolarPanelBlock::class.item(), 1)
                .input(SingularSolarPanelRainBlock::class.item())
                .criterion(hasItem(SingularSolarPanelRainBlock::class.item()), conditionsFromItem(SingularSolarPanelRainBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("singular_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = SingularSolarPanelRainBlock::class)
class SingularSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    SingularSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 262144, nightPower = 131072, maxStorage = 10000000000L, tier = 8,
    rainPower = 131072, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "singular_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
