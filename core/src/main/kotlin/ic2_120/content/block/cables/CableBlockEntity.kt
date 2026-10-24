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

    /** 限流值，0 表示不限流。由限流导线 GUI 设置。 */
    var configuredLimit: Long = 0

    /** 分流导线的红石触发阈值（1–15）。 */
    var splitterThreshold: Int = DEFAULT_SPLITTER_THRESHOLD

    /** 分流导线是否反相：反相时，信号低于阈值才断开。 */
    var splitterInverted: Boolean = false

    /** 有效传输速率，受限于 [configuredLimit]。 */
    val effectiveTransferRate: Long
        get() {
            val block = cachedState.block as? BaseCableBlock ?: return 0
            val base = block.getTransferRate()
            return if (configuredLimit > 0) minOf(base, configuredLimit) else base
        }

    /** 导线当前负载（本 tick 内已传输的能量），仅用于 Jade 显示。不影响实际能量传输逻辑。 */
    var cableLoad: Long by FilteredValue(20)

    override fun setWorld(world: World) {
        super.setWorld(world)
        if (!world.isClient) {
            EnergyNetworkManager.queueNetworkBuild(world, pos)
        }
    }

    /** 对外暴露的 [EnergyStorage]，insert/extract 均委托给电网池。 */
    val energyStorage: EnergyStorage = object : EnergyStorage {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun getAmount(): Long = 0

        override fun getCapacity(): Long = 0

        override fun insert(maxAmount: Long, transaction: TransactionContext): Long {
            val w = world ?: return 0
            val net = network ?: EnergyNetworkManager.getOrCreateNetwork(w, pos).also { network = it }
            return net.insertAndDeliver(this@CableBlockEntity.pos.asLong(), maxAmount, w, transaction)
        }


        override fun extract(maxAmount: Long, transaction: TransactionContext): Long {
            val w = world ?: return 0
            val net = network ?: EnergyNetworkManager.getOrCreateNetwork(w, pos).also { network = it }
            return net.extractFromCable(this@CableBlockEntity.pos.asLong(), maxAmount, w, transaction)
        }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        localEnergy = nbt.getLong(NBT_ENERGY)
        configuredLimit = if (nbt.contains(NBT_LIMIT)) nbt.getLong(NBT_LIMIT) else 0
        splitterThreshold = if (nbt.contains(NBT_SPLITTER_THRESHOLD)) {
            nbt.getInt(NBT_SPLITTER_THRESHOLD).coerceIn(MIN_SPLITTER_THRESHOLD, MAX_SPLITTER_THRESHOLD)
        } else {
            DEFAULT_SPLITTER_THRESHOLD
        }
        splitterInverted = nbt.getBoolean(NBT_SPLITTER_INVERTED)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putLong(NBT_ENERGY, network?.getEnergySharePerCable() ?: localEnergy)
        if (configuredLimit > 0) {
            nbt.putLong(NBT_LIMIT, configuredLimit)
        }
        nbt.putInt(NBT_SPLITTER_THRESHOLD, splitterThreshold)
        nbt.putBoolean(NBT_SPLITTER_INVERTED, splitterInverted)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        val net = network ?: EnergyNetworkManager.getOrCreateNetwork(world, pos).also { network = it }
        net.tickIfNeeded(world)
    }

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120/CableBlockEntity")
        private const val NBT_ENERGY = "CableEnergy"
        private const val NBT_LIMIT = "ConfiguredLimit"
        private const val NBT_SPLITTER_THRESHOLD = "SplitterThreshold"
        private const val NBT_SPLITTER_INVERTED = "SplitterInverted"
        const val MIN_SPLITTER_THRESHOLD = 1
        const val MAX_SPLITTER_THRESHOLD = 15
        const val DEFAULT_SPLITTER_THRESHOLD = 1
        // private const val NBT_LOAD = "CableLoad"

        lateinit var TYPE: BlockEntityType<CableBlockEntity>
            private set

        /**
         * 注册统一的导线 [BlockEntityType]（id = `<modId>:cable`），关联全部 [BaseCableBlock]。
         *
         * 流程：
         * 1. 通过 fabric "ic2_120:cables" entrypoint 收集各附属贡献的导线方块（让附属
         *    实例化并注册自己的 BaseCableBlock 到 Registries.BLOCK）；
         *    （附属 main entrypoint 晚于 core，其 @ModBlock 导线此时尚未注册，改由此处由 core 代为注册）；
         * 2. 扫描 Registries.BLOCK 里全部 BaseCableBlock（core 自有 + 刚由附属贡献的），合并成一个 BE 类型；
         * 3. 向 Energy API 注册 SIDED 查找。
         *
         * 调用时机：core.onInitialize 期间（注册表冻结前）。
         */
        fun registerWithAddons(modId: String) {
            // 1. 通过 fabric "ic2_120:cables" entrypoint 收集各附属贡献的导线方块
            //    （附属 main entrypoint 晚于 core，其 @ModBlock 导线此时尚未注册，
            //     故由 core 在此触发附属的 CableProvider 完成注册）
            val providers = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getEntrypoints("ic2_120:cables", CableProvider::class.java)
            val addonCables = providers.flatMap { it.registerCables() }
            if (addonCables.isNotEmpty()) {
                logger.info("附属通过 ic2_120:cables entrypoint 贡献了 {} 个导线方块", addonCables.size)
            }

            // 2. 扫描注册表，收集全部 BaseCableBlock（core 自有 + 附属贡献）
            val cableBlocks = mutableListOf<Block>()
            for (block in Registries.BLOCK) {
                if (block is BaseCableBlock) cableBlocks.add(block)
            }
            if (cableBlocks.isEmpty()) {
                logger.warn("未发现任何 BaseCableBlock，跳过 CableBlockEntity 注册")
                return
            }

            // 3. 构建并注册统一的 BlockEntityType
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
