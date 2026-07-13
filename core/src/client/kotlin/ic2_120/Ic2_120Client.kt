package ic2_120

import ic2_120.client.ArmorKeybinds
import ic2_120.client.ArmorTooltipHandler
import ic2_120.client.AnimalmatronTooltipHandler
import ic2_120.client.BandwidthHudKeybinds
import ic2_120.client.DrillTooltipHandler
import ic2_120.client.FoamSprayerTooltipHandler
import ic2_120.client.MiningLaserTooltipHandler
import ic2_120.client.ModeKeybinds
import ic2_120.client.BatteryModelPredicates
import ic2_120.client.ModItemTooltip
import ic2_120.client.ModFluidClient
import ic2_120.client.ClientBlockRenderLayers
import ic2_120.client.ClientEntityRenderers
import ic2_120.client.ClientScreenRegistrar
import ic2_120.client.colorprovider.FluidCellColorProvider
import ic2_120.client.RubberLogModelPlugin
import ic2_120.client.colorprovider.StorageBoxColorProvider
import ic2_120.client.colorprovider.PipeColorProvider
import ic2_120.client.PainterModelPredicates
import ic2_120.client.PeatOreTooltipHandler
import ic2_120.client.QuantumLeggingsSpeedController
import ic2_120.client.ClientBlockEntityRenderers
import ic2_120.client.JetpackSoundController
import ic2_120.client.MachineLoopSoundController
import ic2_120.client.IridiumDrillModeHandler
import ic2_120.client.ChainsawModeHandler
import ic2_120.client.MiningLaserModeHandler
import ic2_120.client.SodiumCompatibilityWarning
import ic2_120.client.WindMeterClientInitializer
import ic2_120.client.network.NetworkManager
import ic2_120.analytics.AnalyticsClientReporter
import net.fabricmc.api.ClientModInitializer

object Ic2_120Client : ClientModInitializer {
	override fun onInitializeClient() {
		ModFluidClient.register()
		ClientScreenRegistrar.registerScreens(Ic2_120.MOD_ID, listOf("ic2_120.client"))
		ModItemTooltip.register()
		ClientEntityRenderers.register()
		ClientBlockEntityRenderers.register()
		ClientBlockRenderLayers.register()
		BatteryModelPredicates.register() // 注册电池模型 predicate

		// 注册网络管理器
		NetworkManager.register()

		// 匿名使用统计：客户端每次加入世界上报一次（每会话一次）
		AnalyticsClientReporter.register()
		ModeKeybinds.register()
		BandwidthHudKeybinds.register()
		ArmorKeybinds.register()
		QuantumLeggingsSpeedController.register()
		ArmorTooltipHandler.register()
		AnimalmatronTooltipHandler.register()
		DrillTooltipHandler.register()
		IridiumDrillModeHandler.register()
		ChainsawModeHandler.register()
		MiningLaserModeHandler.register()
		FoamSprayerTooltipHandler.register()
		MiningLaserTooltipHandler.register()
		PeatOreTooltipHandler.register()
		// 手动发版阶段：UpdateNotifier 依赖 CI 的 GITHUB_RUN_NUMBER 注入版本号，
		// 本地/手动构建时恒为 0，导致更新检查永远不触发（ciRunNumber<=0 直接 return）。
		// 暂时禁用，待迁移到语义版本号比较后重新启用。
		// UpdateNotifier.register()
		SodiumCompatibilityWarning.register()
		JetpackSoundController.register()
		MachineLoopSoundController.register()
		WindMeterClientInitializer.init()

		// 注册储物箱着色器
		StorageBoxColorProvider.register()
		// 注册通用流体单元着色器（流体颜色渲染到中心）
		FluidCellColorProvider.register()
		// 橡胶树原木动态模型（替代 243 个 blockstate 变体）
		RubberLogModelPlugin.register()
		// 注册管道着色器（青铜和碳纤维材质）
		PipeColorProvider.register()
		PainterModelPredicates.register()
	}
}
