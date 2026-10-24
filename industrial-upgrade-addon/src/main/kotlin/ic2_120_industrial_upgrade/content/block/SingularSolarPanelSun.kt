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

// i18n: block.ic2_120_industrial_upgrade.singular_solar_panelsun
// zh_cn: 奇异日光太阳能发电机  en_us: Singular Sun Panel
@ModBlock(name = "singular_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SingularSolarPanelSunBlock : IndustrialSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        SingularSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, SingularSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：奇异太阳能发电机 + SunLinse（ASA）
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SingularSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', SingularSolarPanelBlock::class.item())
                .define('A', SunLinse::class.instance())
                .unlockedBy(has(SingularSolarPanelBlock::class.item()), has(SingularSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("singular_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SingularSolarPanelBlock::class.item(), 1)
                .requires(SingularSolarPanelSunBlock::class.item())
                .unlockedBy(has(SingularSolarPanelSunBlock::class.item()), has(SingularSolarPanelSunBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("singular_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = SingularSolarPanelSunBlock::class)
class SingularSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    SingularSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 262144, nightPower = 131072, maxStorage = 10000000000L, tier = 8,
    sunPower = 524288, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "singular_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
