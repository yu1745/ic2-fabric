package ic2_120

import ic2_120.client.ArmorKeybinds
import ic2_120.client.ArmorTooltipHandler
import ic2_120.client.AnimalmatronTooltipHandler
import ic2_120.client.DrillTooltipHandler
import ic2_120.client.FoamSprayerTooltipHandler
import ic2_120.client.MiningLaserTooltipHandler
import ic2_120.client.ModeKeybinds
import ic2_120.client.BatteryModelPredicates
import ic2_120.client.ModItemTooltip
import ic2_120.client.ModFluidClient
import ic2_120.client.ClientEntityRenderers
import ic2_120.client.ClientScreenRegistrar
import ic2_120.client.RubberLogModelPlugin
import ic2_120.client.colorprovider.StorageBoxColorProvider
import ic2_120.client.colorprovider.PipeColorProvider
import ic2_120.client.colorprovider.PeatOreColorProvider
import ic2_120.client.colorprovider.RubberLeavesColorProvider
import ic2_120.client.PainterModelPredicates
import ic2_120.client.UpgradeDirectionModelProperty
import ic2_120.client.PeatOreTooltipHandler
import ic2_120.client.QuantumLeggingsSpeedController
import ic2_120.client.ClientBlockEntityRenderers
import ic2_120.client.JetpackSoundController
import ic2_120.client.MachineLoopSoundController
import ic2_120.client.IridiumDrillModeHandler
import ic2_120.client.ChainsawModeHandler
import ic2_120.client.MiningLaserModeHandler
import ic2_120.client.SodiumCompatibilityWarning
import ic2_120.client.TransformerTooltipConfig
import ic2_120.client.WindMeterClientInitializer
import ic2_120.client.network.NetworkManager
import ic2_120.analytics.AnalyticsClientReporter
import ic2_120.integration.jei.ClientLiveRecipeSource
import ic2_120.integration.jei.LiveRecipeSource
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft

object Ic2_120Client : ClientModInitializer {
	override fun onInitializeClient() {
		// 注入客户端 RecipeManager 提供者，供 JEI plugin 收集运行时实际加载的配方
		// （与机器判定逻辑同源，覆盖 core + 所有附属命名空间）
		LiveRecipeSource.instance = ClientLiveRecipeSource

		// 注入客户端主线程调度器：JEI RecipeManager 操作必须在客户端主线程执行，
		// 而 UU 索引重建由 SERVER_STARTED 在 Server thread 触发 refreshReplicatorRecipes，
		// 需经此切回主线程，否则 ErrorUtil.assertMainThread 抛异常导致进世界必崩。
		//
		// 仅在装了 JEI 时注入；且字段位于 LiveRecipeSource（不引用 JEI 类）而非 Ic2JeiPlugin。
		// 历史教训：曾把字段放在 Ic2JeiPlugin（继承 mezz.jei.api.IModPlugin），即便用
		// isModLoaded 守卫，赋值左侧 `Ic2JeiPlugin.scheduleOnClientThread` 仍会触发
		// Ic2JeiPlugin 类初始化 → 父类 IModPlugin 解析 → 无 JEI 时 NoClassDefFoundError
		// （Connector + 仅 EMI 客户端环境，client entrypoint 阶段必崩）。挪到 LiveRecipeSource
		// 后，isModLoaded 守卫即可彻底挡住对 JEI 类图的触碰。
		if (FabricLoader.getInstance().isModLoaded("jei")) {
			ic2_120.integration.jei.LiveRecipeSource.scheduleOnClientThread = { action ->
				val client = Minecraft.getInstance()
				if (client.isSameThread) false else { client.execute(action); true }
			}
		}

		ModFluidClient.register()
		ClientScreenRegistrar.registerScreens(Ic2_120.MOD_ID, listOf("ic2_120.client"))
		ModItemTooltip.register()
		ClientEntityRenderers.register()
		ClientBlockEntityRenderers.register()
		BatteryModelPredicates.register() // 注册电池模型 predicate

		// 注册网络管理器
		NetworkManager.register()

		// 匿名使用统计：客户端每次加入世界上报一次（每会话一次）
		AnalyticsClientReporter.register()
		ModeKeybinds.register()
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
		TransformerTooltipConfig.register()
		JetpackSoundController.register()
		MachineLoopSoundController.register()
		WindMeterClientInitializer.init()

		// 注册储物箱着色器
		StorageBoxColorProvider.register()
		PeatOreColorProvider.register()
		RubberLeavesColorProvider.register()
		// 注册通用流体单元着色器（流体颜色渲染到中心）
		ic2_120.client.renderers.FluidCellItemRenderer.register()
		ic2_120.client.DevResourceReloadTrigger.register()
		// 橡胶树原木动态模型（替代 243 个 blockstate 变体）
		RubberLogModelPlugin.register()
		ic2_120.client.model.obj.ObjModelLoadingPlugin.register()
		// 注册管道着色器（青铜和碳纤维材质）
		PipeColorProvider.register()
		PainterModelPredicates.register()
		UpgradeDirectionModelProperty.register()
	}
}
