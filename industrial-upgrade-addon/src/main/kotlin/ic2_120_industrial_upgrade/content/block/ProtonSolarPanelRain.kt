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

// i18n: block.ic2_120_industrial_upgrade.proton_solar_panelrain
// zh_cn: 质子雨能太阳能发电机  en_us: Proton Rain Panel
@ModBlock(name = "proton_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class ProtonSolarPanelRainBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ProtonSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, ProtonSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：质子太阳能发电机 + RainLinse（ASA）
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProtonSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', ProtonSolarPanelBlock::class.item())
                .define('A', RainLinse::class.instance())
                .unlockedBy(has(ProtonSolarPanelBlock::class.item()), has(ProtonSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("proton_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ProtonSolarPanelBlock::class.item(), 1)
                .requires(ProtonSolarPanelRainBlock::class.item())
                .unlockedBy(has(ProtonSolarPanelRainBlock::class.item()), has(ProtonSolarPanelRainBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("proton_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = ProtonSolarPanelRainBlock::class)
class ProtonSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    ProtonSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 65536, nightPower = 32768, maxStorage = 1000000000L, tier = 7,
    rainPower = 32768, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "proton_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
