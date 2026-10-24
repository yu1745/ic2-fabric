package ic2_120_industrial_upgrade

import ic2_120.registry.ClassScanner
import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120_industrial_upgrade.content.fluid.NeutronFluid
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object IC2IndustrialUpgrade : ModInitializer {
    const val MOD_ID = "ic2_120_industrial_upgrade"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): ResourceLocation = ResourceLocation(MOD_ID, path)

    override fun onInitialize() {
        LOGGER.info("Initializing IC2 Industrial Upgrade addon...")

        // 1. 注册中子流体（独立注册，不走 ClassScanner）
        NeutronFluid.register()

        // 2. 使用本体 mod 的 ClassScanner 注册附属内容（方块/物品/AbstractContainerMenu）
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120_industrial_upgrade.content.block",
                "ic2_120_industrial_upgrade.content.item",
                "ic2_120_industrial_upgrade.content.screen",
                "ic2_120_industrial_upgrade.content.tab"
            )
        )

        // 3. 分子重组仪配方：铱锭 → 光子（默认耗能 25,000,000 EU）
        //    a) 追加到 ASA 配置文件（若该 input 不存在），服主可直接在 ic2_120_advanced_solar_addon.json 调整耗电量
        Ic2AdvancedSolarAddonConfig.ensureRecipeExists(
            inputId = "ic2_120_advanced_solar_addon:iridium_ingot",
            outputId = "ic2_120_industrial_upgrade:photoniy",
            energy = 25_000_000L
        )
        //    b) 注册内存扩展配方兜底：配置被删除/损坏时仍提供默认耗电量；配置存在时由配置优先
        MTRecipes.registerExtensionRecipe(
            inputId = "ic2_120_advanced_solar_addon:iridium_ingot",
            outputId = "ic2_120_industrial_upgrade:photoniy",
            defaultEnergy = 25_000_000L
        )

        LOGGER.info("IC2 Industrial Upgrade addon initialized!")
    }
}
