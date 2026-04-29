package ic2_120_advanced_solar_addon

import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.content.command.MolecularTransformerCommand
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120.registry.ClassScanner
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object IC2AdvancedSolarAddon : ModInitializer {
    const val MOD_ID = "ic2_120_advanced_solar_addon"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): Identifier = Identifier(MOD_ID, path)

    override fun onInitialize() {
        LOGGER.info("Initializing IC2 Advanced Solar Addon...")

        // 加载配置文件
        Ic2AdvancedSolarAddonConfig.loadOrThrow()

        // 使用本体 mod 的 ClassScanner 注册附属内容
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120_advanced_solar_addon.content.tab",
                "ic2_120_advanced_solar_addon.content.block",
                "ic2_120_advanced_solar_addon.content.screen",
                "ic2_120_advanced_solar_addon.content.item"
            )
        )

        // 初始化分子重组仪配方（从配置加载）
        MTRecipes.init()

        // 注册命令
        MolecularTransformerCommand.register()

        LOGGER.info("IC2 Advanced Solar Addon initialized!")
    }
}
