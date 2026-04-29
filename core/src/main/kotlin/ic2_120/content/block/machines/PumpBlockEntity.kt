package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.PumpBlock
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.screen.PumpScreenHandler
import ic2_120.content.sync.PumpSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.sound.SoundCategory
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = PumpBlock::class)
class PumpBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    ITieredMachine,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport,
    IEjectorUpgradeSupport,
    ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = PumpBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.pump.operate", volume = 0.5f, pitch = 1.0f, intervalTicks = 20
    )

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    override val tier: Int = PUMP_TIER
    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val PUMP_TIER = 1
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT)
        const val INVENTORY_SIZE = 7

        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_TANK_FLUID = "TankFluid"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = PumpBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isValid(SLOT_INPUT, it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT, SLOT_OUTPUT, SLOT_DISCHARGING, *SLOT_UPGRADE_INDICES),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = PumpSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(PUMP_TIER + voltageTierBonus) }
    )

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { PUMP_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = !variant.isBlank

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = !this.variant.isBlank && this.variant == variant

        override fun onFinalCommit() {
            sync.fluidAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStored(fluidId: String, stored: Long) {
            amount = stored.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) {
                val id = Identifier.tryParse(fluidId)
                if (id != null && Registries.FLUID.containsId(id)) FluidVariant.of(Registries.FLUID.get(id)) else FluidVariant.blank()
            } else {
                FluidVariant.blank()
            }
            if (variant.isBlank) amount = 0L
            sync.fluidAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }
    }

    val tank: Storage<FluidVariant> = tankInternal

    constructor(pos: BlockPos, state: BlockState) : this(
        PumpBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun markDirty() = super.markDirty()

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == SLOT_INPUT -> {
            val emptyCell = Registries.ITEM.get(Identifier.of("ic2_120", "empty_cell"))
            stack.item == emptyCell || stack.item is FluidCellItem
        }
        slot == SLOT_OUTPUT -> false
        slot == SLOT_DISCHARGING -> stack.item is IBatteryItem
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.pump")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        PumpScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(PumpSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        tankInternal.setStored(nbt.getString(NBT_TANK_FLUID), nbt.getLong(NBT_TANK_AMOUNT))
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(PumpSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.amount)
        nbt.putString(NBT_TANK_FLUID, if (tankInternal.variant.isBlank) "" else Registries.FLUID.getId(tankInternal.variant.fluid).toString())
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        fillFluidCellFromTank()

        var pumped = false
        if (world.time % 20L == 0L) {
            val operations = speedMultiplier.toInt().coerceAtLeast(1)
            repeat(operations) {
                if (tryPumpOneBucket(world, pos, state)) pumped = true else return@repeat
            }
        }

        if (fluidPipeProviderEnabled) {
            ejectFluidToNeighbors(world, pos, state)
        }

        setActiveState(world, pos, state, pumped)
        sync.syncCurrentTickFlow()
    }

    private fun tryPumpOneBucket(world: World, pos: BlockPos, state: BlockState): Boolean {
        val euNeed = (PumpSync.ENERGY_PER_BUCKET * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.amount < euNeed) return false

        // 提前检查：储罐空间不足 1 桶时不抽取，避免浪费流体和能量
        val spaceLeft = tankInternal.capacity - tankInternal.amount
        if (spaceLeft < FluidConstants.BUCKET) return false

        val front = state.get(Properties.HORIZONTAL_FACING)
        val positions = (1..3).map { pos.offset(front, it) }
        for (target in positions) {
            val drained = tryDrainFromStorage(world, target, front.opposite)
            if (drained > 0L) {
                sync.consumeEnergy(euNeed)
                sync.energy = sync.amount.toInt().coerceAtLeast(0)
                markDirty()
                return true
            }
            val drainedWorld = tryDrainFromWorldSource(world, target)
            if (drainedWorld > 0L) {
                sync.consumeEnergy(euNeed)
                sync.energy = sync.amount.toInt().coerceAtLeast(0)
                markDirty()
                return true
            }
        }
        return false
    }

    private fun tryDrainFromStorage(world: World, targetPos: BlockPos, fromSide: Direction): Long {
        val external = FluidStorage.SIDED.find(world, targetPos, fromSide) ?: return 0L

        for (view in external) {
            if (view.isResourceBlank) continue
            val resource = view.resource
            if (!canAccept(resource.fluid)) continue
            if (view.amount <= 0L) continue

            Transaction.openOuter().use { tx ->
                val extracted = view.extract(resource, FluidConstants.BUCKET, tx)
                if (extracted <= 0L) return@use
                val inserted = tankInternal.insert(resource, extracted, tx)
                if (inserted <= 0L) return@use
                tx.commit()
                return inserted
            }
        }
        return 0L
    }

    private fun tryDrainFromWorldSource(world: World, targetPos: BlockPos): Long {
        val fluidState = world.getFluidState(targetPos)
        if (fluidState.isEmpty || !fluidState.isStill) return 0L
        val fluid = fluidState.fluid
        if (!canAccept(fluid)) return 0L
        if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, targetPos, ownerUuid, ic2_120.integration.ftbchunks.ClaimProtection.EDIT_FLUID)) return 0L

        val variant = FluidVariant.of(fluid)
        val toDrain = FluidConstants.BUCKET
        Transaction.openOuter().use { tx ->
            val inserted = tankInternal.insert(variant, toDrain, tx)
            if (inserted < FluidConstants.BUCKET) return 0L
            tx.commit()
        }
        world.setBlockState(targetPos, net.minecraft.block.Blocks.AIR.defaultState, 3)
        return FluidConstants.BUCKET
    }

    private fun canAccept(fluid: Fluid): Boolean {
        if (tankInternal.amount <= 0L || tankInternal.variant.isBlank) return true
        return tankInternal.variant.fluid == fluid
    }

    private fun fillFluidCellFromTank() {
        val input = getStack(SLOT_INPUT)
        if (input.isEmpty || tankInternal.amount < FluidConstants.BUCKET || tankInternal.variant.isBlank) return

        val output = getStack(SLOT_OUTPUT)
        val filled = when {
            input.item == Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")) ->
                fluidToFilledCellStack(tankInternal.variant.fluid)
            input.item is FluidCellItem && input.isFluidCellEmpty() ->
                ItemStack(input.item).apply { setFluidCellVariant(FluidVariant.of(tankInternal.variant.fluid)) }
            else -> ItemStack.EMPTY
        }
        if (filled.isEmpty) return

        val canOutput = output.isEmpty || (ItemStack.areItemsAndComponentsEqual(output, filled) && output.count < output.maxCount)
        if (!canOutput) return

        Transaction.openOuter().use { tx ->
            val extracted = tankInternal.extract(tankInternal.variant, FluidConstants.BUCKET, tx)
            if (extracted < FluidConstants.BUCKET) return
            tx.commit()
        }

        input.decrement(1)
        if (output.isEmpty) setStack(SLOT_OUTPUT, filled.copy())
        else output.increment(1)
        markDirty()
    }

    private fun ejectFluidToNeighbors(world: World, pos: BlockPos, state: BlockState) {
        if (tankInternal.amount <= 0L || tankInternal.variant.isBlank) return
        val front = state.get(Properties.HORIZONTAL_FACING)
        val ejectSide = fluidPipeProviderSide
        for (dir in Direction.values()) {
            if (dir == front) continue
            if (ejectSide != null && dir != ejectSide) continue
            val neighbor = FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
            val resource = tankInternal.variant
            val maxPerTick = minOf(FluidConstants.BUCKET / 4, tankInternal.amount)
            Transaction.openOuter().use { tx ->
                val extracted = tankInternal.extract(resource, maxPerTick, tx)
                if (extracted <= 0L) return@use
                val accepted = neighbor.insert(resource, extracted, tx)
                if (accepted <= 0L) return@use
                if (accepted < extracted) {
                    tankInternal.insert(resource, extracted - accepted, tx)
                }
                tx.commit()
            }
            if (tankInternal.amount <= 0L) break
        }
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val front = world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        return if (side == front) null else tank
    }
}

