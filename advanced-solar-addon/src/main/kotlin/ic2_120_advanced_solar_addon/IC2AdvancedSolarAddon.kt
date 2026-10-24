package ic2_120_advanced_solar_addon

import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120.content.network.ConfigSyncHelper
import ic2_120_advanced_solar_addon.content.command.MolecularTransformerCommand
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120.registry.ClassScanner
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
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

        // 分子重组仪配方加载延后到 SERVER_STARTING（所有 mod 物品注册完成后）：
        // Sinytra Connector 服务器上 core 初始化晚于本附属（Fabric "depends" 拓扑序失效，
        // 实测启动顺序：ASA → weapons → IU → core），在 onInitialize 里加载配方时
        // core 物品（ic2_120:tin_ingot/silver_ingot 等）尚未注册，addRecipe 的物品
        // 解析失败被静默丢弃 → findRecipe 返回 null → 输入槽拒收（锡锭无法放入）。
        // SERVER_STARTING 在世界加载、玩家进入之前触发，此时加载即可覆盖全部物品；
        // loadFromConfig 幂等（先 clear 再全量重加），附属扩展配方经 reinject 重新注入。
        ServerLifecycleEvents.SERVER_STARTING.register { _ ->
            MTRecipes.init()
        }

        // 注册命令
        MolecularTransformerCommand.register()

        // 玩家加入时发送完整配置同步（分包）
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            ConfigSyncHelper.sendToPlayer(handler.player, id("config_sync"), Ic2AdvancedSolarAddonConfig.prettyCurrentConfig())
        }

        LOGGER.info("IC2 Advanced Solar Addon initialized!")
    }
}
