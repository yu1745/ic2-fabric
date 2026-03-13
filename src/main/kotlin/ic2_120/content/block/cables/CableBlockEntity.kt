package ic2_120.content.block.cables

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.energy.EnergyNetwork
import ic2_120.content.block.energy.EnergyNetworkManager
import ic2_120.content.block.misc.FilteredValue
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import team.reborn.energy.api.EnergyStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction

val Nothing = Transaction.openOuter()

/**
 * 导线方块实体。能量存储委托给所属的 [EnergyNetwork]（电网共享池）。
 *
 * - 发电机通过 [energyStorage] 向电网注入能量。
 * - 电网在 tick 中统一向所有边界消费者推送能量，不依赖 tick 顺序。
 * - [localEnergy] 仅用于 NBT 持久化和电网重建时的中转。
 */
class CableBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(TYPE, pos, state) {

    /** 所属电网；首次 tick 时惰性构建。 */
    var network: EnergyNetwork? = null

    /** 本地暂存能量，仅用于 NBT 存取和电网重建时的中转。 */
    var localEnergy: Long = 0

    /** 导线当前负载（本 tick 内已传输的能量），仅用于 Jade 显示。不影响实际能量传输逻辑。 */
    var cableLoad: Long by FilteredValue(20)


    /** 对外暴露的 [EnergyStorage]，insert/extract 均委托给电网池。 */
    val energyStorage: EnergyStorage = object : EnergyStorage {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        // 返回导线当前负载（供 Jade 显示），而不是网络总能量
        override fun getAmount(): Long = /* if (network != null) cableLoad else localEnergy */ cableLoad

        override fun getCapacity(): Long = /* network?.capacity ?:  */defaultTransferRate()

        override fun insert(maxAmount: Long, transaction: TransactionContext): Long {
            return if (transaction != Nothing) network?.insert(maxAmount, transaction) ?: 0
            else 0L
        }


        override fun extract(maxAmount: Long, transaction: TransactionContext): Long {
            return if (transaction != Nothing) network?.extract(maxAmount, transaction) ?: 0
            else 0L
        }
    }

    private fun defaultTransferRate(): Long =
        (cachedState.block as? BaseCableBlock)?.getTransferRate() ?: 128L

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        localEnergy = nbt.getLong(NBT_ENERGY)
        // println("readNbt load: ${nbt.getLong(NBT_LOAD)}")
        // repeat(20) {
        //     cableLoad = nbt.getLong(NBT_LOAD)
        // }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putLong(NBT_ENERGY, network?.getEnergySharePerCable() ?: localEnergy)
        // nbt.putLong(NBT_LOAD, cableLoad)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        val net = network ?: EnergyNetworkManager.getOrCreateNetwork(world, pos).also { network = it }
        net.tickIfNeeded(world)
    }

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120/CableBlockEntity")
        private const val NBT_ENERGY = "CableEnergy"
        // private const val NBT_LOAD = "CableLoad"

        lateinit var TYPE: BlockEntityType<CableBlockEntity>
            private set

        /**
         * 在所有方块注册完毕后调用，为全部 [BaseCableBlock] 创建并注册统一的 [BlockEntityType]，
         * 同时向 Energy API 注册 SIDED 查找。
         */
        fun register(modId: String) {
            val cableBlocks = mutableListOf<Block>()
            for (block in Registries.BLOCK) {
                if (block is BaseCableBlock) cableBlocks.add(block)
            }
            if (cableBlocks.isEmpty()) {
                logger.warn("未发现任何 BaseCableBlock，跳过 CableBlockEntity 注册")
                return
            }

            val factory = FabricBlockEntityTypeBuilder.Factory<CableBlockEntity> { p, s ->
                CableBlockEntity(p, s)
            }
            @Suppress("UNCHECKED_CAST")
            TYPE = FabricBlockEntityTypeBuilder.create(factory, *cableBlocks.toTypedArray())
                .build() as BlockEntityType<CableBlockEntity>

            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier(modId, "cable"), TYPE)
            EnergyStorage.SIDED.registerForBlockEntity({ be, _ -> be.energyStorage }, TYPE)

            logger.info("已注册 CableBlockEntity（电网模型），关联 {} 种导线方块", cableBlocks.size)
        }
    }
}
