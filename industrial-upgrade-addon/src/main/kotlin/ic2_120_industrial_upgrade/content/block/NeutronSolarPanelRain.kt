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

// i18n: block.ic2_120_industrial_upgrade.neutron_solar_panelrain
// zh_cn: 中子雨能太阳能发电机  en_us: Neutron Rain Panel
@ModBlock(name = "neutron_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class NeutronSolarPanelRainBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        NeutronSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, NeutronSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：中子太阳能发电机 + RainLinse（ASA）
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NeutronSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', NeutronSolarPanelBlock::class.item())
                .define('A', RainLinse::class.instance())
                .unlockedBy(has(NeutronSolarPanelBlock::class.item()), has(NeutronSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("neutron_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, NeutronSolarPanelBlock::class.item(), 1)
                .requires(NeutronSolarPanelRainBlock::class.item())
                .unlockedBy(has(NeutronSolarPanelRainBlock::class.item()), has(NeutronSolarPanelRainBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("neutron_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = NeutronSolarPanelRainBlock::class)
class NeutronSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    NeutronSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 16777216, nightPower = 16777216, maxStorage = 10000000000000L, tier = 11,
    rainPower = 16777216, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "neutron_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
