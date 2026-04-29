package ic2_120.content.block.machines

import ic2_120.content.block.CokeKilnGrateBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.util.math.BlockPos
import net.minecraft.registry.RegistryWrapper

@ModBlockEntity(block = CokeKilnGrateBlock::class)
class CokeKilnGrateBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    companion object {
        const val TANK_CAPACITY = FluidConstants.BUCKET * 8
        private const val NBT_TANK = "CreosoteTank"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, _ -> (be as CokeKilnGrateBlockEntity).fluidIo }, CokeKilnGrateBlockEntity::class.type())
            fluidLookupRegistered = true
        }
    }

    private val creosoteTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean =
            variant.fluid == ModFluids.CREOSOTE_STILL || variant.fluid == ModFluids.CREOSOTE_FLOWING

        override fun onFinalCommit() {
            markDirty()
        }
    }

    val fluidIo: Storage<FluidVariant> = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = false
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.CREOSOTE_STILL && resource.fluid != ModFluids.CREOSOTE_FLOWING) return 0
            return creosoteTank.extract(FluidVariant.of(ModFluids.CREOSOTE_STILL), maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!creosoteTank.variant.isBlank && creosoteTank.amount > 0) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = creosoteTank.variant
                    override fun getAmount(): Long = creosoteTank.amount
                    override fun getCapacity(): Long = TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        creosoteTank.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    constructor(pos: BlockPos, state: BlockState) : this(CokeKilnGrateBlockEntity::class.type(), pos, state)

    fun canAcceptDroplets(amount: Long): Boolean = amount > 0 && (TANK_CAPACITY - creosoteTank.amount) >= amount

    fun insertDroplets(amount: Long): Long {
        if (amount <= 0) return 0
        val space = TANK_CAPACITY - creosoteTank.amount
        val moved = minOf(space, amount)
        if (moved <= 0) return 0
        creosoteTank.amount += moved
        if (creosoteTank.variant.isBlank) creosoteTank.variant = FluidVariant.of(ModFluids.CREOSOTE_STILL)
        markDirty()
        return moved
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        val tankNbt = NbtCompound()
        tankNbt.put("Variant", FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, creosoteTank.variant).result().orElse(NbtCompound()))
        tankNbt.putLong("Amount", creosoteTank.amount)
        nbt.put(NBT_TANK, tankNbt)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        val tankNbt = nbt.getCompound(NBT_TANK)
        if (!tankNbt.isEmpty) {
            creosoteTank.variant = FluidVariant.CODEC.decode(NbtOps.INSTANCE, tankNbt.getCompound("Variant")).result().map { it.first }.orElse(FluidVariant.blank())
            creosoteTank.amount = tankNbt.getLong("Amount").coerceIn(0L, TANK_CAPACITY)
        }
    }
}
