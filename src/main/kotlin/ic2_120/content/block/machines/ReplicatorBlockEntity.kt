package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.ReplicatorBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.ReplicatorScreenHandler
import ic2_120.content.sync.ReplicatorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.uu.findUniqueAdjacentPatternStorage
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
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.BucketItem
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
import kotlin.math.ceil

@ModBlockEntity(block = ReplicatorBlock::class)
class ReplicatorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty = ReplicatorBlock.ACTIVE
    override val tier: Int = ReplicatorSync.REPLICATOR_TIER
    override fun getInventory(): Inventory = this

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    companion object {
        const val SLOT_OUTPUT = 0
        const val SLOT_CONTAINER_INPUT = 1
        const val SLOT_CONTAINER_OUTPUT = 2
        const val SLOT_DISCHARGING = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_CONTAINER_INPUT)
        const val INVENTORY_SIZE = 8
        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_PROGRESS_UB = "ProgressUb"
        private const val NBT_SINGLE_PULSE_CONSUMED = "SinglePulseConsumed"
        private const val NBT_FLUID_REMAINDER = "FluidRemainder"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity(
                { be, side -> (be as ReplicatorBlockEntity).getFluidStorageForSide(side) },
                ReplicatorBlockEntity::class.type()
            )
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
            ItemInsertRoute(intArrayOf(SLOT_CONTAINER_INPUT), matcher = { isValid(SLOT_CONTAINER_INPUT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT, SLOT_CONTAINER_INPUT, SLOT_CONTAINER_OUTPUT, SLOT_DISCHARGING),
        markDirty = { markDirty() }
    )
    private var singlePulseConsumed = false
    private var fluidConsumptionRemainder = 0L
    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "empty_cell")) }

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = ReplicatorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ReplicatorSync.REPLICATOR_TIER + voltageTierBonus) }
    )

    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * ReplicatorSync.TANK_CAPACITY_MB / 1000L

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isUuMatter(variant.fluid)
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.fluidAmountMb = toMilliBuckets(amount)
            sync.fluidCapacityMb = ReplicatorSync.TANK_CAPACITY_MB
            markDirty()
        }

        fun setStoredAmount(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.UU_MATTER_STILL) else FluidVariant.blank()
            sync.fluidAmountMb = toMilliBuckets(amount)
            sync.fluidCapacityMb = ReplicatorSync.TANK_CAPACITY_MB
        }

        fun getTankCapacity(): Long = tankCapacity
        fun getStoredAmount(): Long = amount
        fun extractInternal(toExtract: Long): Long {
            val actual = minOf(toExtract, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.fluidAmountMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }
    }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { ReplicatorSync.REPLICATOR_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(ReplicatorBlockEntity::class.type(), pos, state)

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    private fun replicatorIsDrainableUuContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val itemId = Registries.ITEM.getId(stack.item)
        val fluidCellId = Identifier.of("ic2_120", "fluid_cell")
        return itemId.path == "uu_matter_bucket" ||
            itemId.path == "uu_matter_cell" ||
            (itemId == fluidCellId &&
                !stack.isFluidCellEmpty() &&
                stack.getFluidCellVariant()?.fluid.let { it == ModFluids.UU_MATTER_STILL || it == ModFluids.UU_MATTER_FLOWING })
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_OUTPUT -> false
        SLOT_CONTAINER_INPUT -> !stack.isEmpty && stack.item !is IBatteryItem && replicatorIsDrainableUuContainer(stack)
        SLOT_CONTAINER_OUTPUT -> false
        SLOT_DISCHARGING -> !stack.isEmpty && stack.item is IBatteryItem
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.replicator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ReplicatorScreenHandler(
            syncId,
            playerInventory,
            this,
            pos,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(ReplicatorSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progressUb = nbt.getInt(NBT_PROGRESS_UB).coerceAtLeast(0)
        singlePulseConsumed = nbt.getBoolean(NBT_SINGLE_PULSE_CONSUMED)
        fluidConsumptionRemainder = nbt.getLong(NBT_FLUID_REMAINDER).coerceAtLeast(0L)
        tankInternal.setStoredAmount(nbt.getLong(NBT_TANK_AMOUNT))
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ReplicatorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.getStoredAmount())
        nbt.putInt(NBT_PROGRESS_UB, sync.progressUb)
        nbt.putBoolean(NBT_SINGLE_PULSE_CONSUMED, singlePulseConsumed)
        nbt.putLong(NBT_FLUID_REMAINDER, fluidConsumptionRemainder)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, tankInternal, fluidPipeProviderFilter, fluidPipeProviderSide, state.get(Properties.HORIZONTAL_FACING))
        }
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.fluidCapacityMb = ReplicatorSync.TANK_CAPACITY_MB

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        fillTankFromContainers()

        val hasRedstone = world.isReceivingRedstonePower(pos)
        if (!hasRedstone) {
            singlePulseConsumed = false
            sync.status = ReplicatorSync.STATUS_NO_REDSTONE
            sync.progressMaxUb = sync.currentCostUb
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val storage = findUniqueAdjacentPatternStorage(world, pos)
        if (storage == null) {
            resetProgress()
            sync.status = ReplicatorSync.STATUS_NO_STORAGE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val template = storage.getSelectedTemplate()
        if (template == null) {
            resetProgress()
            sync.status = ReplicatorSync.STATUS_NO_TEMPLATE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        sync.currentCostUb = template.uuCostUb
        sync.progressMaxUb = template.uuCostUb.coerceAtLeast(1)

        if (sync.mode == ReplicatorSync.MODE_SINGLE && singlePulseConsumed && sync.progressUb == 0) {
            sync.status = ReplicatorSync.STATUS_COMPLETE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val outputStack = getStack(SLOT_OUTPUT)
        val resultStack = createOutputStack(template.itemId) ?: run {
            resetProgress()
            sync.status = ReplicatorSync.STATUS_NO_TEMPLATE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }
        if (!canMergeIntoSlot(outputStack, resultStack)) {
            sync.status = ReplicatorSync.STATUS_NO_OUTPUT
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val ubPerTick = ceil(ReplicatorSync.BASE_UB_PER_TICK * speedMultiplier.toDouble()).toInt().coerceAtLeast(1)
        val (dropletsPerTick, nextRemainder) = previewDropletsForTick(ubPerTick)
        if (dropletsPerTick > 0L && tankInternal.getStoredAmount() < dropletsPerTick) {
            sync.status = ReplicatorSync.STATUS_NO_FLUID
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val energyNeed = ceil(ReplicatorSync.ENERGY_PER_TICK * energyMultiplier.toDouble()).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(energyNeed) <= 0L) {
            sync.status = ReplicatorSync.STATUS_NO_ENERGY
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        fluidConsumptionRemainder = nextRemainder
        if (dropletsPerTick > 0L) {
            tankInternal.extractInternal(dropletsPerTick)
        }
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progressUb = (sync.progressUb + ubPerTick).coerceAtMost(template.uuCostUb)
        sync.status = ReplicatorSync.STATUS_RUNNING
        if (sync.progressUb >= template.uuCostUb) {
            if (outputStack.isEmpty) setStack(SLOT_OUTPUT, resultStack.copy())
            else outputStack.increment(1)
            sync.progressUb = 0
            sync.status = ReplicatorSync.STATUS_COMPLETE
            singlePulseConsumed = sync.mode == ReplicatorSync.MODE_SINGLE
            markDirty()
        } else {
            markDirty()
        }

        setActiveState(world, pos, state, true)
        sync.syncCurrentTickFlow()
    }

    fun toggleMode() {
        sync.mode = if (sync.mode == ReplicatorSync.MODE_SINGLE) ReplicatorSync.MODE_CONTINUOUS else ReplicatorSync.MODE_SINGLE
        if (sync.mode == ReplicatorSync.MODE_CONTINUOUS) {
            singlePulseConsumed = false
        }
        markDirty()
    }

    fun selectTemplate(index: Int): Boolean {
        val world = world ?: return false
        val storage = findUniqueAdjacentPatternStorage(world, pos) ?: return false
        return storage.selectTemplate(index)
    }

    private fun resetProgress() {
        sync.progressUb = 0
        sync.currentCostUb = 0
        sync.progressMaxUb = 0
        fluidConsumptionRemainder = 0L
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val facing = world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        if (side == facing) return null
        return tankInternal
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

    private fun fillTankFromContainers() {
        val input = getStack(SLOT_CONTAINER_INPUT)
        if (input.isEmpty) return
        val output = getStack(SLOT_CONTAINER_OUTPUT)
        val space = tankInternal.getTankCapacity() - tankInternal.getStoredAmount()
        if (space < FluidConstants.BUCKET) return

        val emptyResult = getEmptyContainerFor(input) ?: return
        if (!canMergeIntoSlot(output, emptyResult)) return

        Transaction.openOuter().use { tx ->
            val ctx = ContainerItemContext.withInitial(input)
            val itemStorage = ctx.find(FluidStorage.ITEM) ?: return@use
            for (view in itemStorage) {
                if (view.amount < FluidConstants.BUCKET || view.resource.isBlank || !isUuMatter(view.resource.fluid)) continue
                val extracted = view.extract(view.resource, FluidConstants.BUCKET, tx)
                if (extracted < FluidConstants.BUCKET) continue
                val inserted = tankInternal.insert(FluidVariant.of(ModFluids.UU_MATTER_STILL), extracted, tx)
                if (inserted < extracted) continue

                val remaining = ctx.itemVariant.toStack(ctx.amount.toInt().coerceAtLeast(0))
                input.decrement(1)
                if (input.isEmpty) setStack(SLOT_CONTAINER_INPUT, ItemStack.EMPTY)
                else setStack(SLOT_CONTAINER_INPUT, remaining)

                if (output.isEmpty) setStack(SLOT_CONTAINER_OUTPUT, emptyResult.copy())
                else output.increment(emptyResult.count)

                sync.fluidAmountMb = toMilliBuckets(tankInternal.amount)
                tx.commit()
                return
            }
        }
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean =
        current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)

    private fun isUuMatter(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == ModFluids.UU_MATTER_STILL || fluid == ModFluids.UU_MATTER_FLOWING

    private fun createOutputStack(itemId: String): ItemStack? {
        val id = net.minecraft.util.Identifier.tryParse(itemId) ?: return null
        val item = Registries.ITEM.getOrEmpty(id).orElse(null) ?: return null
        return if (item == net.minecraft.item.Items.AIR) null else ItemStack(item)
    }

    private fun toMilliBuckets(amount: Long): Int =
        (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

    private fun previewDropletsForTick(ub: Int): Pair<Long, Long> {
        val total = fluidConsumptionRemainder + ub.toLong() * FluidConstants.BUCKET
        val droplets = total / 1_000_000L
        return droplets to (total % 1_000_000L)
    }

    private fun getEmptyContainerFor(filled: ItemStack): ItemStack? = when (filled.item) {
        is BucketItem -> net.minecraft.item.ItemStack(net.minecraft.item.Items.BUCKET)
        is ModFluidCell -> net.minecraft.item.ItemStack((filled.item as ModFluidCell).getEmptyCell())
        else -> {
            val path = Registries.ITEM.getId(filled.item).path
            if (path == "fluid_cell" || path.endsWith("_cell")) {
                net.minecraft.item.ItemStack(emptyCellItem)
            } else null
        }
    }
}
