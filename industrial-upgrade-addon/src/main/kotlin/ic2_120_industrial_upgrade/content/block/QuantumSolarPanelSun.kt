package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.content.block.QuantumSolarPanelBlock
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.item.SunLinse
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

// i18n: block.ic2_120_industrial_upgrade.quantum_solar_panelsun
// zh_cn: 量子日光太阳能发电机  en_us: Quantum Sun Panel
@ModBlock(name = "quantum_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class QuantumSolarPanelSunBlock : QuantumSolarPanelBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        QuantumSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, QuantumSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // "BA"：量子太阳能发电机（ASA）+ SunLinse
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, QuantumSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .define('B', QuantumSolarPanelBlock::class.item())
                .define('A', SunLinse::class.instance())
                .unlockedBy(getHasName(QuantumSolarPanelBlock::class.item()), has(QuantumSolarPanelBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("quantum_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, QuantumSolarPanelBlock::class.item(), 1)
                .requires(QuantumSolarPanelSunBlock::class.item())
                .unlockedBy(getHasName(QuantumSolarPanelSunBlock::class.item()), has(QuantumSolarPanelSunBlock::class.item()))
                .save(exporter, IC2IndustrialUpgrade.id("quantum_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = QuantumSolarPanelSunBlock::class)
class QuantumSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    QuantumSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 4096, nightPower = 2048, maxStorage = 10000000L, tier = 5,
    sunPower = 8192, activeProperty = QuantumSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "quantum_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
