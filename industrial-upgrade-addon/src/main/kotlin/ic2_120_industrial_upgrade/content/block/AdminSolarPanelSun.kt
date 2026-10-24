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

// i18n: block.ic2_120_industrial_upgrade.admin_solar_panelsun
// zh_cn: 管理员日光太阳能发电机  en_us: Admin Sun Panel
@ModBlock(name = "admin_solar_panelsun", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdminSolarPanelSunBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdminSolarPanelSunBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, AdminSolarPanelSunBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // "BA"：管理员太阳能发电机 + SunLinse（ASA）
            ShapedRecipeBuilder.create(RecipeCategory.MISC, AdminSolarPanelSunBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', AdminSolarPanelBlock::class.item())
                .input('A', SunLinse::class.instance())
                .criterion(hasItem(AdminSolarPanelBlock::class.item()), conditionsFromItem(AdminSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("admin_solar_panelsun"))
            // 变体回退：变体 → 原版（透镜不返还）
            ShapelessRecipeBuilder.create(RecipeCategory.MISC, AdminSolarPanelBlock::class.item(), 1)
                .input(AdminSolarPanelSunBlock::class.item())
                .criterion(hasItem(AdminSolarPanelSunBlock::class.item()), conditionsFromItem(AdminSolarPanelSunBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("admin_solar_panelsun_revert"))
        }
    }
}

@ModBlockEntity(block = AdminSolarPanelSunBlock::class)
class AdminSolarPanelSunBlockEntity(pos: BlockPos, state: BlockState) : SunPanelBlockEntity(
    AdminSolarPanelSunBlockEntity::class.type(), pos, state,
    dayPower = 1048576, nightPower = 1048576, maxStorage = 100000000000L, tier = 9,
    sunPower = 2097152, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "admin_solar_panelsun"
    override fun getDisplayName(): Component = Component.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
