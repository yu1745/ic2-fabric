package ic2_120

import ic2_120.client.ArmorKeybinds
import ic2_120.client.ArmorTooltipHandler
import ic2_120.client.BandwidthHudKeybinds
import ic2_120.client.DrillTooltipHandler
import ic2_120.client.FoamSprayerTooltipHandler
import ic2_120.client.ModeKeybinds
import ic2_120.client.BatteryModelPredicates
import ic2_120.client.ModItemTooltip
import ic2_120.client.ModFluidClient
import ic2_120.client.ClientBlockRenderLayers
import ic2_120.client.ClientEntityRenderers
import ic2_120.client.ClientScreenRegistrar
import ic2_120.client.colorprovider.FluidCellColorProvider
import ic2_120.client.colorprovider.RubberLeavesColorProvider
import ic2_120.client.RubberLogModelPlugin
import ic2_120.client.colorprovider.StorageBoxColorProvider
import ic2_120.client.colorprovider.PipeColorProvider
import ic2_120.client.ClientBlockEntityRenderers
import ic2_120.client.JetpackSoundController
import ic2_120.client.MachineLoopSoundController
import ic2_120.client.IridiumDrillModeHandler
import ic2_120.client.network.NetworkManager
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
		ModeKeybinds.register()
		BandwidthHudKeybinds.register()
		ArmorKeybinds.register()
		ArmorTooltipHandler.register()
		DrillTooltipHandler.register()
		IridiumDrillModeHandler.register()
		FoamSprayerTooltipHandler.register()
		JetpackSoundController.register()
		MachineLoopSoundController.register()

		// 注册储物箱着色器
		StorageBoxColorProvider.register()
		// 注册通用流体单元着色器（流体颜色渲染到中心）
		FluidCellColorProvider.register()
		// 注册橡胶树叶生物群系着色器
		RubberLeavesColorProvider.register()
		// 橡胶树原木动态模型（替代 243 个 blockstate 变体）
		RubberLogModelPlugin.register()
		// 注册管道着色器（青铜和碳纤维材质）
		PipeColorProvider.register()
	}
}
