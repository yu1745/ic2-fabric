package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isSemifluidFuel
import ic2_120.content.screen.SemifluidGeneratorScreenHandler
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

@ModBlockEntity(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator, IFluidPipeUpgradeSupport,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    data class FuelProfile(
        val euPerBucket: Long,
        val euPerTick: Long
    )

    companion object {
        const val GENERATOR_TIER = 1
        private const val NBT_FUEL_AMOUNT = "FuelAmount"

        const val FUEL_SLOT = 0
        const val EMPTY_CONTAINER_SLOT = 1
        const val BATTERY_SLOT = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 7

        val FUEL_PROFILES: Map<net.minecraft.fluid.Fluid, FuelProfile> = mapOf(
            ModFluids.BIOFUEL_STILL to FuelProfile(euPerBucket = 32_000L, euPerTick = 32L),
            ModFluids.BIOFUEL_FLOWING to FuelProfile(euPerBucket = 32_000L, euPerTick = 32L)
        )

        @Volatile
        private var fluidLookupRegistered = false

        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = SemifluidGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }

        fun isSupportedFuelFluid(fluid: net.minecraft.fluid.Fluid): Boolean = FUEL_PROFILES.containsKey(fluid)

        fun getFuelProfile(fluid: net.minecraft.fluid.Fluid): FuelProfile? = FUEL_PROFILES[fluid]
    }

    override val tier: Int = GENERATOR_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = SemifluidGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val fuelTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean = isSupportedFuelFluid(variant.fluid)

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) Registries.FLUID.getRawId(variant.fluid) else -1
            markDirty()
        }

        fun setStoredFuel(newAmount: Long, fluid: net.minecraft.fluid.Fluid?) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L && fluid != null && isSupportedFuelFluid(fluid)) {
                FluidVariant.of(fluid)
            } else {
                FluidVariant.blank()
            }
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) Registries.FLUID.getRawId(variant.fluid) else -1
        }

        fun tryInsertFuel(fluid: net.minecraft.fluid.Fluid, toInsert: Long): Long {
            if (toInsert <= 0L || !isSupportedFuelFluid(fluid)) return 0L
            if (amount > 0L && variant.fluid != fluid) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != fluid) variant = FluidVariant.of(fluid)
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = Registries.FLUID.getRawId(variant.fluid)
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || !isSupportedFuelFluid(variant.fluid)) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) Registries.FLUID.getRawId(variant.fluid) else -1
            return actual
        }
    }

    val fuelTank: Storage<FluidVariant> = fuelTankInternal

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0L }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        SemifluidGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            FUEL_SLOT -> stack.isSemifluidFuel()
            EMPTY_CONTAINER_SLOT -> false
            BATTERY_SLOT -> stack.item is IBatteryItem
            else -> false
        }
    }

    private fun tryInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getStack(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) {
            setStack(EMPTY_CONTAINER_SLOT, emptyStack.copy())
            true
        } else if (ItemStack.canCombine(current, emptyStack)) {
            val toAdd = minOf(emptyStack.count, current.maxCount - current.count)
            if (toAdd > 0) {
                current.increment(toAdd)
                markDirty()
                true
            } else false
        } else false
    }

    private fun canInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getStack(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) true
        else ItemStack.canCombine(current, emptyStack) && current.count + emptyStack.count <= current.maxCount
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.semifluid_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SemifluidGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(SemifluidGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, SemifluidGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val fluidId = nbt.getString("FuelFluid")
        val fluid = if (fluidId.isNullOrBlank()) null else Registries.FLUID.get(Identifier(fluidId))
        fuelTankInternal.setStoredFuel(nbt.getLong(NBT_FUEL_AMOUNT), fluid)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(SemifluidGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_FUEL_AMOUNT, fuelTankInternal.amount)
        if (fuelTankInternal.amount > 0L && !fuelTankInternal.variant.isBlank) {
            nbt.putString("FuelFluid", Registries.FLUID.getId(fuelTankInternal.variant.fluid).toString())
        }
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        val fuelStack = getStack(FUEL_SLOT)
        when {
            fuelStack.item == ModFluids.BIOFUEL_BUCKET -> {
                val emptyBucket = ItemStack(Items.BUCKET)
                if (canInsertEmptyContainer(emptyBucket)) {
                    val inserted = fuelTankInternal.tryInsertFuel(ModFluids.BIOFUEL_STILL, FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyBucket)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
            fuelStack.item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "biofuel_cell")) -> {
                val emptyCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
                if (canInsertEmptyContainer(emptyCell)) {
                    val inserted = fuelTankInternal.tryInsertFuel(ModFluids.BIOFUEL_STILL, FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyCell)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
            fuelStack.item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) -> {
                val fluid = fuelStack.getFluidCellVariant()?.fluid
                val profile = fluid?.let { getFuelProfile(it) }
                if (fluid != null && profile != null) {
                    val emptyCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
                    if (canInsertEmptyContainer(emptyCell)) {
                        val inserted = fuelTankInternal.tryInsertFuel(fluid, FluidConstants.BUCKET)
                        if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyCell)) {
                            fuelStack.decrement(1)
                            if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                            markDirty()
                        }
                    }
                }
            }
        }

        val space = (SemifluidGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        val fuelFluid = fuelTankInternal.variant.fluid
        val fuelProfile = getFuelProfile(fuelFluid)
        if (space > 0L && fuelTankInternal.amount > 0L && fuelProfile != null) {
            val consumePerTick = (FluidConstants.BUCKET * fuelProfile.euPerTick / fuelProfile.euPerBucket).coerceAtLeast(1L)
            val toConsume = minOf(consumePerTick, fuelTankInternal.amount, space * consumePerTick / fuelProfile.euPerTick)
            val consumed = fuelTankInternal.consumeInternal(toConsume)
            if (consumed > 0L) {
                val euToAdd = consumed * fuelProfile.euPerTick / consumePerTick
                sync.generateEnergy(minOf(euToAdd, space))
                markDirty()
            }
        }

        batteryCharger.tick()

        val active = sync.amount < SemifluidGeneratorSync.ENERGY_CAPACITY &&
            fuelTankInternal.amount > 0L &&
            getFuelProfile(fuelTankInternal.variant.fluid) != null
        if (state.get(SemifluidGeneratorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(SemifluidGeneratorBlock.ACTIVE, active))
        }
        sync.syncCurrentTickFlow()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == getFrontFacing()) return null
        return fuelTank
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
}
