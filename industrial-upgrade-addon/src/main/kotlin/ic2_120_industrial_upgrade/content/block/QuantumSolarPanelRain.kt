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
import ic2_120_industrial_upgrade.content.item.RainLinse
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

// i18n: block.ic2_120_industrial_upgrade.quantum_solar_panelrain
// zh_cn: 量子雨能太阳能发电机  en_us: Quantum Rain Panel
@ModBlock(name = "quantum_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class QuantumSolarPanelRainBlock : QuantumSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        QuantumSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, QuantumSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：量子太阳能发电机（ASA）+ RainLinse
            ShapedRecipeBuilder.create(RecipeCategory.MISC, QuantumSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', QuantumSolarPanelBlock::class.item())
                .input('A', RainLinse::class.instance())
                .criterion(hasItem(QuantumSolarPanelBlock::class.item()), conditionsFromItem(QuantumSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("quantum_solar_panelrain"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.create(RecipeCategory.MISC, QuantumSolarPanelBlock::class.item(), 1)
                .input(QuantumSolarPanelRainBlock::class.item())
                .criterion(hasItem(QuantumSolarPanelRainBlock::class.item()), conditionsFromItem(QuantumSolarPanelRainBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("quantum_solar_panelrain_revert"))
        }
    }
}

@ModBlockEntity(block = QuantumSolarPanelRainBlock::class)
class QuantumSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    QuantumSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 4096, nightPower = 2048, maxStorage = 10000000L, tier = 5,
    rainPower = 2048, activeProperty = QuantumSolarPanelBlock.ACTIVE
) {
    override fun getBlockName(): String = "quantum_solar_panelrain"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
