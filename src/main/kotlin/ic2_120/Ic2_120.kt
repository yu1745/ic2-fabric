package ic2_120

import ic2_120.content.RubberTreetapHandler
import ic2_120.content.WrenchHandler
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberBlock
import ic2_120.content.block.nuclear.ReactorChamberBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberEnergyProvider
import ic2_120.content.block.nuclear.ReactorFluidPortBlockEntity
import ic2_120.content.block.energy.EnergyNetworkManager
import ic2_120.content.fluid.ModFluids
import ic2_120.content.network.NetworkManager
import ic2_120.content.effect.ModStatusEffects
import ic2_120.content.worldgen.OreGeneration
import ic2_120.content.worldgen.RubberTreeGeneration
import ic2_120.content.item.CellAndBucketFluidRegistration
import ic2_120.content.block.BatBoxBlock
import ic2_120.content.block.CesuBlock
import ic2_120.content.block.MfeBlock
import ic2_120.content.block.MfsuBlock
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.machines.GeoGeneratorBlockEntity
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.block.machines.FluidHeatExchangerBlockEntity
import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.block.machines.FluidBottlerBlockEntity
import ic2_120.content.block.machines.OreWashingPlantBlockEntity
import ic2_120.content.block.machines.PumpBlockEntity
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.block.machines.SolarDistillerBlockEntity
import ic2_120.content.block.machines.WaterGeneratorBlockEntity
import ic2_120.content.block.storage.StorageBoxBlockEntity
import ic2_120.content.block.storage.TankBlockEntity
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PipeNetworkManager
import ic2_120.content.entity.ModEntities
import ic2_120.registry.ClassScanner
import ic2_120.registry.type
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import com.mojang.serialization.Lifecycle
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.SimpleRegistry
import net.minecraft.util.math.Direction
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import team.reborn.energy.api.EnergyStorage

object Ic2_120 : ModInitializer {

    const val MOD_ID = "ic2_120"

    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ModStatusEffects.register()

        // 流体需在 ClassScanner 之前注册（流体、方块、桶）
        ModFluids.register()

        // 船实体类型需在扫描物品前注册，供 @ModItem 船物品类构造时引用
        ModEntities.register()

        // 橡胶树世界生成（主世界植被装饰阶段）
        RubberTreeGeneration.register()
        // 矿石世界生成（锡/铅/铀，照搬铁矿/金矿/钻石矿）
        OreGeneration.register()

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

        // 特殊处理：导线 BlockEntity 需在所有方块注册后统一注册（一个 BE 类型关联多种导线方块）
        CableBlockEntity.register(MOD_ID)

        // 扳手与机器方块交互（旋转、拆卸、掉落逻辑）
        WrenchHandler.register()

        // 木龙头/电动树脂提取器与橡胶树原木交互（提取粘性树脂）
        RubberTreetapHandler.register()

        // 特殊处理：储物箱 BlockEntity 需在所有方块注册后统一注册（一个 BE 类型关联多种储物箱方块）
        StorageBoxBlockEntity.register(MOD_ID)

        // 特殊处理：储罐 BlockEntity 需在所有方块注册后统一注册（一个 BE 类型关联多种储罐方块）
        TankBlockEntity.register(MOD_ID)
        TankBlockEntity.registerFluidStorageLookup()

        // 传动轴/伞齿轮 BlockEntity（仅用于 BER 动画渲染）
        TransmissionBlockEntity.register(MOD_ID)
        // 流体管道 BlockEntity（统一网络）
        PipeBlockEntity.register(MOD_ID)

        // 地热/水力/洗矿发电机流体能力注册（Fabric Transfer API）
        GeoGeneratorBlockEntity.registerFluidStorageLookup()
        FluidHeatGeneratorBlockEntity.registerFluidStorageLookup()
        FluidHeatExchangerBlockEntity.registerFluidStorageLookup()
        SemifluidGeneratorBlockEntity.registerFluidStorageLookup()
        WaterGeneratorBlockEntity.registerFluidStorageLookup()
        OreWashingPlantBlockEntity.registerFluidStorageLookup()
        SolarDistillerBlockEntity.registerFluidStorageLookup()
        PumpBlockEntity.registerFluidStorageLookup()
        FluidBottlerBlockEntity.registerFluidStorageLookup()
        CannerBlockEntity.registerFluidStorageLookup()

        // 核反应堆的流体存储注册
        NuclearReactorBlockEntity.registerFluidStorageLookup()
        ReactorFluidPortBlockEntity.registerFluidStorageLookup()

        // 核反应仓能量能力注册（Fabric Transfer API）
        val reactorChamberType = ReactorChamberBlockEntity::class.type()
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
            { be, side -> ReactorChamberEnergyProvider.getEnergyStorage(be as ReactorChamberBlockEntity, side) },
            reactorChamberType
        )

        // 单元与桶的流体交互（右键放置/收集液体，等效桶）
        CellAndBucketFluidRegistration.register()

        // 世界卸载时清理电网缓存
        ServerWorldEvents.UNLOAD.register { _, world ->
            EnergyNetworkManager.onWorldUnload(world)
            PipeNetworkManager.onWorldUnload(world)
        }

        // 储电盒自定义 BlockItem（支持满电变体）及创造模式满电物品
        val storageConfigs = listOf(
            "batbox" to { b: net.minecraft.block.Block -> BatBoxBlock.BatBoxBlockItem(b, net.fabricmc.fabric.api.item.v1.FabricItemSettings()) },
            "cesu" to { b: net.minecraft.block.Block -> CesuBlock.CesuBlockItem(b, net.fabricmc.fabric.api.item.v1.FabricItemSettings()) },
            "mfe" to { b: net.minecraft.block.Block -> MfeBlock.MfeBlockItem(b, net.fabricmc.fabric.api.item.v1.FabricItemSettings()) },
            "mfsu" to { b: net.minecraft.block.Block -> MfsuBlock.MfsuBlockItem(b, net.fabricmc.fabric.api.item.v1.FabricItemSettings()) }
        )
        for ((id, factory) in storageConfigs) {
            val blockId = Identifier(MOD_ID, id)
            val block = Registries.BLOCK.get(blockId)
            val itemKey = RegistryKey.of(RegistryKeys.ITEM, blockId)
            val existingItem = Registries.ITEM.get(blockId)
            val rawId = Registries.ITEM.getRawId(existingItem)
            (Registries.ITEM as SimpleRegistry<Item>).set(rawId, itemKey, factory(block), Lifecycle.stable())
        }
        val ic2MachinesKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MACHINES.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MachinesKey).register { entries ->
            for ((id, _) in storageConfigs) {
                val fullStack = ItemStack(Registries.ITEM.get(Identifier(MOD_ID, id)))
                fullStack.orCreateNbt.putBoolean(EnergyStorageBlock.NBT_FULL, true)
                entries.add(fullStack)
            }
        }

        // 注册网络管理器
        NetworkManager.register()

        logger.info("IC2 1.20 模组已加载（类注解驱动自动注册）")
    }

    /**
     * 创建模组内的标识符
     */
    fun id(path: String): Identifier = Identifier(MOD_ID, path)
}
