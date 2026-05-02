package ic2_120

import ic2_120.content.CreativeGeneratorItemEntityHandler
import ic2_120.content.RubberTreetapHandler
import ic2_120.content.WrenchHandler
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberBlock
import ic2_120.content.block.nuclear.ReactorChamberBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberEnergyProvider
import ic2_120.content.block.nuclear.ReactorFluidPortBlockEntity
import ic2_120.content.block.nuclear.ReactorAccessHatchBlockEntity
import ic2_120.content.block.nuclear.ReactorItemStorageProvider
import ic2_120.content.block.energy.EnergyNetworkManager
import ic2_120.content.fluid.ModFluids
import ic2_120.content.network.NetworkManager
import ic2_120.content.network.BandwidthStatsService
import ic2_120.content.effect.ModStatusEffects
import ic2_120.content.effect.RadiationHandler
import ic2_120.content.worldgen.ChestLootInjector
import ic2_120.content.worldgen.OreGeneration
import ic2_120.content.worldgen.RubberTreeGeneration
import ic2_120.content.item.CellAndBucketFluidRegistration
import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.recipes.ModCraftingRecipes
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.block.storage.EnergyStorageBlock
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
import ic2_120.content.block.storage.TankBlockEntity
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.transmission.KineticNetworkManager
import ic2_120.content.block.pipes.PipeNetworkManager
import ic2_120.content.player.FlightManager
import ic2_120.content.entity.ModEntities
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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.registry.FuelRegistry
import ic2_120.content.item.CfPack
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.armor.JetpackItem
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

    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        Ic2Config.loadOrThrow()
        ModCraftingRecipes.register()

        ModStatusEffects.register()
        RadiationHandler.register()

        // 流体需在 ClassScanner 之前注册（流体、方块、桶）
        ModFluids.register()

        // 船实体类型需在扫描物品前注册，供 @ModItem 船物品类构造时引用
        ModEntities.register()

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

        // 特殊处理：导线 BlockEntity 需在所有方块注册后统一注册（一个 BE 类型关联多种导线方块）
        CableBlockEntity.register(MOD_ID)

        // 扳手与机器方块交互（旋转、拆卸、掉落逻辑）
        WrenchHandler.register()

        // 创造模式发电机掉落物：永不清除、环境不伤实体
        CreativeGeneratorItemEntityHandler.register()

        // 木龙头/电动树脂提取器与橡胶树原木交互（提取粘性树脂）
        RubberTreetapHandler.register()

        // 传动轴/伞齿轮 BlockEntity（仅用于 BER 动画渲染）
        TransmissionBlockEntity.register(MOD_ID)

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
            FlightManager.tick(server)
            BandwidthStatsService.onServerTick(server)
        }

        // 储电盒自定义 BlockItem（支持满电变体）及创造模式满电物品
        val storageIds = listOf(
            "batbox",
            "cesu",
            "mfe",
            "mfsu",
            "batbox_chargepad",
            "cesu_chargepad",
            "mfe_chargepad",
            "mfsu_chargepad"
        )
        val ic2MachinesKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MACHINES.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MachinesKey).register { entries ->
            for (id in storageIds) {
                val fullStack = ItemStack(Registries.ITEM.get(Identifier(MOD_ID, id)))
                fullStack.orCreateNbt.putBoolean(EnergyStorageBlock.NBT_FULL, true)
                entries.add(fullStack)
            }
        }

        // 注册满燃料喷气背包到创造模式
        val jetpackItem = Registries.ITEM.get(Identifier(MOD_ID, "jetpack"))
        if (jetpackItem != null) {
            val ic2MaterialsKey =
                RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MATERIALS.id))
            ItemGroupEvents.modifyEntriesEvent(ic2MaterialsKey).register { entries ->
                // 添加满燃料的喷气背包
                val fullFuelJetpack = ItemStack(jetpackItem).also {
                    JetpackItem.setFuel(it, JetpackItem.MAX_FUEL)
                }
                entries.addAfter(jetpackItem, fullFuelJetpack)
            }
        }

        // 建筑泡沫喷枪：满流体（8 桶）变体
        val ic2MaterialsKey =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MATERIALS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MaterialsKey).register { entries ->
            val sprayer = Registries.ITEM.get(Identifier(MOD_ID, "foam_sprayer"))
            if (sprayer == Items.AIR || sprayer !is FoamSprayerItem) return@register
            val fullSprayer = ItemStack(sprayer).also {
                FoamSprayerItem.setFluidAmount(it, FoamSprayerItem.CAPACITY_DROPLETS)
            }
            entries.addAfter(sprayer, fullSprayer)

            val cfPack = Registries.ITEM.get(Identifier.of(MOD_ID, "cf_pack"))
            if (cfPack != Items.AIR) {
                val fullCfPack = ItemStack(cfPack).also {
                    CfPack.setFluidAmount(it, CfPack.CAPACITY_DROPLETS)
                }
                entries.addAfter(cfPack, fullCfPack)
            }
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

        // 注册网络管理器
        NetworkManager.register()
        // 注册配置重载命令（/ic2config reload）
        ConfigCommand.register()
        SeedCommand.register()
        RubberTreeCommand.register()
        UuReplicationCommand.register()
        ItemIdCommand.register()

        logger.info("IC2 1.20 模组已加载（类注解驱动自动注册）")
    }

    /**
     * 创建模组内的标识符
     */
    fun id(path: String): Identifier = Identifier(MOD_ID, path)
}
