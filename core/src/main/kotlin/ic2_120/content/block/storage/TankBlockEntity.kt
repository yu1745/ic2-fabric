package ic2_120.content.block.storage

import ic2_120.content.screen.TankScreenHandler
import ic2_120.content.sync.TankSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import io.netty.buffer.Unpooled


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
@ModBlockEntity(
    name = "tank",
    blocks = [
        BronzeTankBlock::class,
        IronTankBlock::class,
        SteelTankBlock::class,
        IridiumTankBlock::class
    ]
)
class TankBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(TankBlockEntity::class.type(), pos, state), ExtendedScreenHandlerFactory<PacketByteBuf> {

    companion object {
        /** 青铜/铁储罐容量 (mB) */
        const val BRONZE_IRON_CAPACITY_MB = 32000

        /** 钢制储罐容量 (mB) */
        const val STEEL_CAPACITY_MB = 128000

        /** 铱储罐容量 (mB) */
        const val IRIDIUM_CAPACITY_MB = 102400

        private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L

        /** 容量常量（droplets） */
        private val BRONZE_IRON_CAPACITY = mbToDroplets(BRONZE_IRON_CAPACITY_MB)
        private val STEEL_CAPACITY = mbToDroplets(STEEL_CAPACITY_MB)
        private val IRIDIUM_CAPACITY = mbToDroplets(IRIDIUM_CAPACITY_MB)

        /** NBT 键 */
        private const val NBT_FLUID_AMOUNT = "FluidAmount"
        private const val NBT_FLUID_VARIANT = "FluidVariant"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity(
                { be, _ -> (be as TankBlockEntity).fluidTank },
                TankBlockEntity::class.type()
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
        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }
        override fun onFinalCommit() {
            syncFluidState()
            markDirty()
        }
    }

    val syncedData = SyncedData(this)
    val sync = TankSync(syncedData) { getCapacityMb() }
    val guiSlotInv = SimpleInventory(1)
    var shouldDropOnBreak = true

    fun syncFluidState() {
        val variant = tankInternal.variant
        sync.fluidRawId = if (variant.isBlank) -1 else Registries.FLUID.getRawId(variant.fluid)
        sync.fluidAmountMb = getFluidAmountMb()
        sync.capacityMb = getCapacityMb()
    }

    fun extractFluidForBucket(player: PlayerEntity): Boolean {
        if (tankInternal.amount < FluidConstants.BUCKET) return false
        val current = tankInternal.variant
        if (current.isBlank) return false

        Transaction.openOuter().use { tx ->
            val extracted = tankInternal.extract(current, FluidConstants.BUCKET, tx)
            if (extracted < FluidConstants.BUCKET) {
                tx.abort()
                return false
            }
            tx.commit()
            syncFluidState()
            return true
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

    /** 获取容量 (mB) */
    fun getCapacityMb(): Int {
        val blockId = cachedState.block.toString()
        return when {
            blockId.contains("steel_tank") -> STEEL_CAPACITY_MB
            blockId.contains("iridium_tank") -> IRIDIUM_CAPACITY_MB
            else -> BRONZE_IRON_CAPACITY_MB
        }
    }

    /** 获取当前流体量 (mB) */
    fun getFluidAmountMb(): Int =
        (tankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

    // ========== ExtendedScreenHandlerFactory ==========

    override fun getDisplayName(): Text {
        val block = cachedState.block
        return Text.translatable(block.translationKey)
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        syncFluidState()
        return TankScreenHandler(
            syncId,
            syncedData,
            ScreenHandlerContext.create(world!!, pos)
        )
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
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

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putLong(NBT_FLUID_AMOUNT, tankInternal.amount)
        if (!tankInternal.variant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, tankInternal.variant).result().orElse(NbtCompound()))
        }
        syncedData.writeNbt(nbt)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        tankInternal.amount = nbt.getLong(NBT_FLUID_AMOUNT).coerceIn(0L, getCapacity())
        val fluidTag = nbt.getCompound(NBT_FLUID_VARIANT)
        tankInternal.variant = if (fluidTag.isEmpty) FluidVariant.blank() else FluidVariant.CODEC.decode(NbtOps.INSTANCE, fluidTag).result().map { it.first }.orElse(FluidVariant.blank())
        syncedData.readNbt(nbt)
        syncFluidState()
    }
}
