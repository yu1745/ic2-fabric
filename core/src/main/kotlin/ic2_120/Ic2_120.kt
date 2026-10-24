package ic2_120

import ic2_120.content.RecipeCacheEpoch
import ic2_120.analytics.AnalyticsReporter
import ic2_120.content.CreativeGeneratorItemEntityHandler
import ic2_120.content.ChainsawHandler
import ic2_120.content.RubberTreetapHandler
import ic2_120.content.WrenchHandler
import ic2_120.content.block.RubberWoodFlammables
import ic2_120.content.block.RubberWoodStrippables
import ic2_120.content.block.nuclear.NuclearExplosionManager
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberBlock
import ic2_120.content.block.nuclear.ReactorChamberBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberEnergyProvider
import ic2_120.content.block.nuclear.ReactorFluidPortBlockEntity
import ic2_120.content.block.nuclear.ReactorAccessHatchBlockEntity
import ic2_120.content.block.nuclear.ReactorItemStorageProvider
import ic2_120.content.block.energy.EnergyNetworkManager
import ic2_120.content.fluid.ModFluids
import ic2_120.content.fluid.UUFluidEffectHandler
import ic2_120.content.network.NetworkManager
import ic2_120.content.network.ConfigSyncHelper
import ic2_120.content.network.ConfigSyncPacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import ic2_120.content.effect.RadiationHandler
import ic2_120.content.worldgen.ChestLootInjector
import ic2_120.content.worldgen.OreGeneration
import ic2_120.content.worldgen.RubberTreeGeneration
import ic2_120.content.item.CellAndBucketFluidRegistration
import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.PainterItem
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.machines.GeoGeneratorBlockEntity
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.block.machines.FluidHeatExchangerBlockEntity
import ic2_120.content.block.machines.FermenterBlockEntity
import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.block.machines.FluidCannerBlockEntity
import ic2_120.content.block.machines.OreWashingPlantBlockEntity
import ic2_120.content.block.machines.PumpBlockEntity
import ic2_120.content.block.machines.MinerBlockEntity
import ic2_120.content.block.machines.AdvancedMinerBlockEntity
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.block.machines.SolarDistillerBlockEntity
import ic2_120.content.block.machines.WaterGeneratorBlockEntity
import ic2_120.content.block.transmission.KineticNetworkManager
import ic2_120.content.block.pipes.PipeNetworkManager
import ic2_120.content.player.FlightManager
import ic2_120.config.Ic2Config
import ic2_120.content.command.ConfigCommand
import ic2_120.content.command.SeedCommand
import ic2_120.content.command.RubberTreeCommand
import ic2_120.content.command.UuReplicationCommand
import ic2_120.content.command.ItemIdCommand
import ic2_120.registry.ClassScanner
import ic2_120.registry.type
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.registry.FuelRegistry
import ic2_120.content.item.CreativeTabIconItemsRegistration
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.math.Direction
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import team.reborn.energy.api.EnergyStorage

object Ic2_120 : ModInitializer {

    const val MOD_ID = "ic2_120"

    /** 全局 tick 计数器，由 server tick hook 递增。0 表示尚未初始化，此时 item energy 不限速。 */
    @Volatile
    var serverTick: Long = 0L
        private set

    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        Ic2Config.loadOrThrow()

        RadiationHandler.register()

        // 流体需在 ClassScanner 之前注册（流体、方块、桶）
        ModFluids.register()

        // UU 物质液池回复生命效果（实体浸入检测）
        UUFluidEffectHandler.register()

        // 橡胶树世界生成（主世界植被装饰阶段）
        RubberTreeGeneration.register()
        // 矿石世界生成（锡/铅/铀，照搬铁矿/金矿/钻石矿）
        OreGeneration.register()
        // 奖励箱战利品追加注入（避免直接覆盖原版 loot table）
        ChestLootInjector.register()

        // 创造栏图标占位物品（无 @ModItem，须在物品栏注册前完成）
        CreativeTabIconItemsRegistration.register(MOD_ID)

        // 使用类级别注解的自动注册系统
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120.content.tab",     // 扫描物品栏类（必须先注册）
                "ic2_120.content.entity",  // 扫描 Entity 类（须在物品前注册，供船物品引用）
                "ic2_120.content.effect",  // 扫描 StatusEffect 类
                "ic2_120.content.block",   // 扫描方块类（包括 cables, machines, energy 等子包）
                "ic2_120.content.screen",  // 扫描 ScreenHandler 类
                "ic2_120.content.item"     // 扫描物品类（含 @ModItem 船物品）
            )
        )

        // 焦炭作为固体燃料：燃烧时间为煤炭 2 倍（影响原版熔炉、铁炉、发电机等读取 FuelRegistry 的设备）
        val coalBurnTicks = FuelRegistry.INSTANCE.get(Items.COAL) ?: 1600
        Registries.ITEM.getOrEmpty(Identifier(MOD_ID, "coke")).ifPresent { cokeItem ->
            FuelRegistry.INSTANCE.add(cokeItem, coalBurnTicks * 2)
        }

        // 机器 RecipeType/Serializer：扫描 recipes 包时先 Class.forName(initialize=false)，仅带 @ModMachineRecipe 的序列化器会完成初始化；须在物品/方块注册之后
        ModMachineRecipes.register()

        // 特殊处理：导线 BlockEntity 需在所有方块（含附属 mod 的 BaseCableBlock）注册后统一注册。
        // core 自有导线扫描注册表；附属导线因 core.onInitialize 先于附属（硬依赖拓扑序），无法被扫描捕获，
        // 改由 "ic2_120:cables" entrypoint 贡献：core 调 getEntrypoints 时执行附属 provider，
        // 收集其返回的 BaseCableBlock 并代为注册进 Registries.BLOCK，再合并进 CableBlockEntity。
        CableBlockEntity.registerWithAddons(MOD_ID)

        // 扳手与机器方块交互（旋转、拆卸、掉落逻辑）
        WrenchHandler.register()
        ChainsawHandler.register()

        // 创造模式发电机掉落物：永不清除、环境不伤实体
        CreativeGeneratorItemEntityHandler.register()

        // 木龙头/电动树脂提取器与橡胶树原木交互（提取粘性树脂）
        RubberTreetapHandler.register()

        // 橡胶木系列：可燃注册（与原版橡木对齐：log/wood=5/5、planks/slab/stairs/fence/fence_gate=5/20、leaves=30/60）
        RubberWoodFlammables.register()
        // 橡胶木系列：斧头去皮（rubber_log/rubber_wood → stripped_*，AXIS 保留）
        RubberWoodStrippables.register()

        // 传动轴/伞齿轮 BlockEntity（仅用于 BER 动画渲染）
        // 核反应仓能量能力注册（Fabric Transfer API）
        val reactorChamberType = ReactorChamberBlockEntity::class.type()
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
            { be, side -> ReactorChamberEnergyProvider.getEnergyStorage(be as ReactorChamberBlockEntity, side) },
            reactorChamberType
        )

        // 核反应堆/反应仓/访问接口 物品存储注册（Fabric Transfer API）
        ItemStorage.SIDED.registerForBlockEntity(
            { be, _ -> ReactorItemStorageProvider.getStorage(be as NuclearReactorBlockEntity) },
            NuclearReactorBlockEntity::class.type()
        )
        ItemStorage.SIDED.registerForBlockEntity(
            { be, _ -> ReactorItemStorageProvider.getStorageForChamber(be as ReactorChamberBlockEntity) },
            reactorChamberType
        )
        ItemStorage.SIDED.registerForBlockEntity(
            { be, _ -> ReactorItemStorageProvider.getStorageForAccessHatch(be as ReactorAccessHatchBlockEntity) },
            ReactorAccessHatchBlockEntity::class.type()
        )

        // 单元与桶的流体交互（右键放置/收集液体，等效桶）
        CellAndBucketFluidRegistration.register()

        // 世界卸载时清理电网缓存
        ServerWorldEvents.UNLOAD.register { _, world ->
            EnergyNetworkManager.onWorldUnload(world)
            PipeNetworkManager.onWorldUnload(world)
            KineticNetworkManager.onWorldUnload(world)
        }

        // 统一处理喷气背包/电力喷气背包/量子胸甲飞行（服务端 tick）
        ServerTickEvents.END_SERVER_TICK.register { server ->
            serverTick++
            for (world in server.worlds) {
                EnergyNetworkManager.tickAll(world)
            }
            FlightManager.tick(server)
            PipeNetworkManager.tickAllWorlds(server)
            NuclearExplosionManager.tick(server)
        }

        // 匿名使用统计：服务端每次启动上报一次（覆盖独立服务端 + 集成服务端）
        ServerLifecycleEvents.SERVER_STARTED.register {
            AnalyticsReporter.report("server")
        }

        // 数据包重载完成后使全部机器配方缓存失效（缓存 epoch +1），
        // 避免 /reload 后已加载机器继续使用旧配方。
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ ->
            RecipeCacheEpoch.invalidate()
        }

        // 杂交作物初始种子袋（三维属性 1/1/1）加入作物种子物品栏
        val ic2CropSeedsKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_CROP_SEEDS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2CropSeedsKey).register { entries ->
            val seedBag = Registries.ITEM.get(Identifier(MOD_ID, "crop_seed_bag"))
            val initialSeeds = CropSeedBagItem.createInitialSeedStacks()
            if (seedBag != net.minecraft.item.Items.AIR) {
                for (stack in initialSeeds) entries.addAfter(seedBag, stack)
            } else {
                for (stack in initialSeeds) entries.add(stack)
            }
        }

        // 刷子 16 色变体（基础未染色刷子由 @ModItem 自动加入）
        val ic2ToolsKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_TOOLS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2ToolsKey).register { entries ->
            val painter = Registries.ITEM.get(Identifier(MOD_ID, "painter"))
            val coloredPainters = PainterItem.createColoredStacks()
            if (painter != Items.AIR) {
                for (stack in coloredPainters) entries.addAfter(painter, stack)
            } else {
                for (stack in coloredPainters) entries.add(stack)
            }
        }

        // 注册网络管理器
        NetworkManager.register()

        // 玩家加入时发送完整配置同步（分包）
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            ConfigSyncHelper.sendToPlayer(handler.player, ConfigSyncPacket.ID, Ic2Config.prettyCurrentConfig())
            ConfigSyncHelper.sendToPlayer(
                handler.player,
                ConfigSyncPacket.REPLICATION_COSTS_ID,
                Ic2Config.prettyAllReplicationCosts()
            )
        }

        // 玩家维度切换后客户端 ClientPlayerEntity 会被重建，allowFlying 被 vanilla 按
        // 生存模式重置为 false；登记给 FlightManager 在下次 tick 强制重发 abilities 包兜底。
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, _, _ ->
            FlightManager.onWorldChanged(player)
        }

        // 注册配置重载命令（/ic2config reload）
        ConfigCommand.register()
        SeedCommand.register()
        RubberTreeCommand.register()
        UuReplicationCommand.register()
        ItemIdCommand.register()

        // [TEMP] 半流质发电机燃料 dump：启动后遍历所有流体过真实匹配逻辑并打日志。
        // 测完删掉本行 + content/debug/SemifluidFuelDump.kt
        // 2026-07-01 已注释：暂时关闭 dump，避免启动噪声。如需重新启用，取消下面这行注释即可。
        // ic2_120.content.debug.SemifluidFuelDump.register()

        // UU 成本动态计算：服务器启动时构建成本图（白名单硬约束 + Bellman-Ford 补全衍生物品）。
        // 构建完成后刷新 JEI 复制配方（覆盖 core + 附属命名空间的所有可复制物品）。
        ic2_120.content.uu.UuCostIndex.register()
        // 调试：取消下面两行注释可 dump 全量消耗表到文件
        // ic2_120.content.uu.UuCostIndex.dumpOnRebuild = true
        // ic2_120.content.uu.UuCostIndex.dumpPath = "/tmp/uu_cost_table_kilt.txt"
        ic2_120.content.uu.UuCostIndex.onRebuild {
            // JEI 是可选依赖：无 JEI 环境（如最小测试容器）不能引用其类，
            // 否则 ClassNotFoundException 直接崩服务器启动。
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
                ic2_120.integration.jei.Ic2JeiPlugin.refreshReplicatorRecipes()
            }
        }

        logger.info("IC2 1.20 模组已加载（类注解驱动自动注册）")
    }

    /**
     * 创建模组内的标识符
     */
    fun id(path: String): Identifier = Identifier(MOD_ID, path)
}
