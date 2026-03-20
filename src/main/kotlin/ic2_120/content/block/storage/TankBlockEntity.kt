package ic2_120.content.block.storage

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.registry.Registry
import ic2_120.Ic2_120


/**
 * 储罐 BlockEntity
 *
 * 用于存储流体的方块，支持：
 * - 流体管道自动输入输出（通过 FluidStorage.SIDED）
 * - 右键用桶或流体单元手动交互
 * - 破坏时保留流体内容（保留百分比由扳手拆卸时控制）
 *
 * 容量（单位：桶）：
 * - 青铜/铁储罐: 32 桶
 * - 钢制储罐: 128 桶
 * - 铱储罐: 1024 桶
 */
class TankBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(TANK_TYPE, pos, state) {

    companion object {
        /** 青铜/铁储罐容量 */
        private const val BRONZE_IRON_CAPACITY_BUCKETS = 32

        /** 钢制储罐容量 */
        private const val STEEL_CAPACITY_BUCKETS = 128

        /** 铱储罐容量 */
        private const val IRIDIUM_CAPACITY_BUCKETS = 1024

        /** 容量常量（mB） */
        private val BRONZE_IRON_CAPACITY = FluidConstants.BUCKET * BRONZE_IRON_CAPACITY_BUCKETS
        private val STEEL_CAPACITY = FluidConstants.BUCKET * STEEL_CAPACITY_BUCKETS
        private val IRIDIUM_CAPACITY = FluidConstants.BUCKET * IRIDIUM_CAPACITY_BUCKETS

        /** NBT 键 */
        private const val NBT_FLUID_AMOUNT = "FluidAmount"
        private const val NBT_FLUID_VARIANT = "FluidVariant"

        /** BlockEntityType 实例，由 register 方法初始化 */
        private lateinit var TANK_TYPE: BlockEntityType<TankBlockEntity>

        @Volatile
        private var fluidLookupRegistered = false

        fun register(modId: String) {
            val blockIds = listOf(
                Identifier(modId, "bronze_tank"),
                Identifier(modId, "iron_tank"),
                Identifier(modId, "steel_tank"),
                Identifier(modId, "iridium_tank")
            )

            val blocks = blockIds.mapNotNull { id ->
                Registries.BLOCK.getOrEmpty(id).orElse(null)
            }

            require(blocks.size == blockIds.size) {
                "注册储罐 BlockEntity 失败：找不到部分储罐方块。需要: ${blockIds.joinToString()}"
            }

            val factory = FabricBlockEntityTypeBuilder.Factory { pos: BlockPos, state: BlockState ->
                TankBlockEntity(pos, state)
            }

            @Suppress("UNCHECKED_CAST")
            val type = FabricBlockEntityTypeBuilder.create(factory, *blocks.toTypedArray())
                .build() as BlockEntityType<TankBlockEntity>

            TANK_TYPE = type

            val id = Identifier(modId, "tank")
            Registry.register(Registries.BLOCK_ENTITY_TYPE, id, type)
        }

        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity(
                { be, _ -> (be as TankBlockEntity).fluidTank },
                TANK_TYPE
            )
            fluidLookupRegistered = true
        }
    }

    /** 根据方块类型获取容量 */
    fun getCapacity(): Long {
        val blockId = cachedState.block.toString()
        return when {
            blockId.contains("steel_tank") -> STEEL_CAPACITY
            blockId.contains("iridium_tank") -> IRIDIUM_CAPACITY
            else -> BRONZE_IRON_CAPACITY
        }
    }

    /** 内部流体存储 */
    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = this@TankBlockEntity.getCapacity()
        override fun canInsert(variant: FluidVariant): Boolean = true
        override fun canExtract(variant: FluidVariant): Boolean = true
        override fun onFinalCommit() {
            markDirty()
        }
    }

    val fluidTank: Storage<FluidVariant> = tankInternal

    /** 流体变体 */
    val fluidVariant: FluidVariant get() = tankInternal.variant

    /** 流体总量 */
    val fluidAmount: Long get() = tankInternal.amount

    /** 获取流体填充度（用于比较器输出） */
    val comparatorOutput: Int
        get() {
            val cap = getCapacity()
            if (cap <= 0) return 0
            val amount = tankInternal.amount
            if (amount <= 0) return 0
            val ratio = (amount.toFloat() / cap)
            return (ratio * 14).toInt() + 1
        }

    /**
     * 扳手拆卸时保留指定百分比的流体
     * @param percent 保留比例（0.0 ~ 1.0）
     */
    fun retainFluidPercent(percent: Double) {
        if (percent <= 0.0) {
            tankInternal.amount = 0
            tankInternal.variant = FluidVariant.blank()
            return
        }
        val maxAmount = getCapacity()
        val target = (tankInternal.amount * percent).toLong().coerceIn(0L, maxAmount)
        tankInternal.amount = target
    }

    // ========== NBT 持久化 ==========

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putLong(NBT_FLUID_AMOUNT, tankInternal.amount)
        if (!tankInternal.variant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, tankInternal.variant.toNbt())
        }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        tankInternal.amount = nbt.getLong(NBT_FLUID_AMOUNT).coerceIn(0L, getCapacity())
        val fluidTag = nbt.getCompound(NBT_FLUID_VARIANT)
        tankInternal.variant = if (fluidTag.isEmpty) FluidVariant.blank() else FluidVariant.fromNbt(fluidTag)
    }
}
