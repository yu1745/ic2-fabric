package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.MatterGeneratorScreenHandler
import ic2_120.content.sync.MatterGeneratorSync
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
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
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

@ModBlockEntity(block = MatterGeneratorBlock::class)
class MatterGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty = MatterGeneratorBlock.ACTIVE
    override val tier: Int = MatterGeneratorSync.MATTER_GENERATOR_TIER

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
        const val SLOT_SCRAP = 0
        const val SLOT_CONTAINER_INPUT = 1
        const val SLOT_CONTAINER_OUTPUT = 2
        const val SLOT_DISCHARGING = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_CONTAINER_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_CONTAINER_INPUT)
        const val INVENTORY_SIZE = 8

        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_PROGRESS = "Progress"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity(
                { be, side -> (be as MatterGeneratorBlockEntity).getFluidStorageForSide(side) },
                MatterGeneratorBlockEntity::class.type()
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
            ItemInsertRoute(intArrayOf(SLOT_SCRAP), matcher = { isValid(SLOT_SCRAP, it) }),
            ItemInsertRoute(intArrayOf(SLOT_CONTAINER_INPUT), matcher = { isValid(SLOT_CONTAINER_INPUT, it) })
        ),
        extractSlots = intArrayOf(SLOT_SCRAP, SLOT_CONTAINER_INPUT, SLOT_CONTAINER_OUTPUT, SLOT_DISCHARGING),
        markDirty = { markDirty() }
    )
    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "fluid_cell")) }
    private val uuMatterCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "uu_matter_cell")) }

    private val outputPerCycle = mbToDroplets(1)

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = MatterGeneratorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(MatterGeneratorSync.MATTER_GENERATOR_TIER + voltageTierBonus) }
    )

    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * MatterGeneratorSync.TANK_CAPACITY_MB / 1000L

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = isUuMatter(variant.fluid)

        override fun onFinalCommit() {
            sync.fluidAmountMb = toMilliBuckets(amount)
            markDirty()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity
        fun availableSpace(): Long = (tankCapacity - amount).coerceAtLeast(0L)

        fun setStoredAmount(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.UU_MATTER_STILL) else FluidVariant.blank()
            sync.fluidAmountMb = toMilliBuckets(amount)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val actual = minOf(toInsert, availableSpace())
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.UU_MATTER_STILL)
            sync.fluidAmountMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }

        fun extractInternal(toExtract: Long): Long {
            if (toExtract <= 0L || variant.isBlank) return 0L
            val actual = minOf(toExtract, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.fluidAmountMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }
    }

    private val outputStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = false
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (!isUuMatter(resource.fluid)) return 0L
            return tankInternal.extract(FluidVariant.of(ModFluids.UU_MATTER_STILL), maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            if (tankInternal.amount <= 0L || tankInternal.variant.isBlank) {
                return mutableListOf<StorageView<FluidVariant>>().iterator()
            }
            return mutableListOf(object : StorageView<FluidVariant> {
                override fun getResource(): FluidVariant = tankInternal.variant
                override fun getAmount(): Long = tankInternal.amount
                override fun getCapacity(): Long = tankInternal.getTankCapacity()
                override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
                    StoragePreconditions.notBlankNotNegative(resource, maxAmount)
                    if (!isUuMatter(resource.fluid)) return 0L
                    return tankInternal.extract(FluidVariant.of(ModFluids.UU_MATTER_STILL), maxAmount, transaction)
                }
                override fun isResourceBlank(): Boolean = false
            }).iterator()
        }
    }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { MatterGeneratorSync.MATTER_GENERATOR_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        MatterGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) {
            stack.count = 1
        }
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    private fun matterGenIsFillableContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val itemId = Registries.ITEM.getId(stack.item)
        return itemId == Identifier.of(Ic2_120.MOD_ID, "empty_cell") ||
            stack.item == Items.BUCKET ||
            (itemId == Identifier.of(Ic2_120.MOD_ID, "fluid_cell") && stack.isFluidCellEmpty())
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_SCRAP -> !stack.isEmpty && Registries.ITEM.getId(stack.item) == Identifier.of(Ic2_120.MOD_ID, "scrap")
        SLOT_CONTAINER_INPUT -> !stack.isEmpty && stack.item !is IBatteryItem && matterGenIsFillableContainer(stack)
        SLOT_CONTAINER_OUTPUT -> false
        SLOT_DISCHARGING -> !stack.isEmpty && stack.item is IBatteryItem
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.matter_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MatterGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(MatterGeneratorSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progress = nbt.getInt(NBT_PROGRESS).coerceIn(0, MatterGeneratorSync.PROGRESS_MAX)
        tankInternal.setStoredAmount(nbt.getLong(NBT_TANK_AMOUNT))
        sync.fluidCapacityMb = MatterGeneratorSync.TANK_CAPACITY_MB
        sync.mode = resolveDisplayedMode()
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MatterGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.getStoredAmount())
        nbt.putInt(NBT_PROGRESS, sync.progress)
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
        sync.fluidCapacityMb = MatterGeneratorSync.TANK_CAPACITY_MB

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        fillContainersFromTank()

        if (!world.isReceivingRedstonePower(pos)) {
            setActiveState(world, pos, state, false)
            sync.mode = resolveDisplayedMode()
            sync.syncCurrentTickFlow()
            return
        }

        if (tankInternal.availableSpace() < outputPerCycle) {
            setActiveState(world, pos, state, false)
            sync.mode = resolveDisplayedMode()
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.progress == 0) {
            sync.mode = resolveDisplayedMode()
        }

        val hasScrap = isScrap(getStack(SLOT_SCRAP))
        val euPerMb = if (hasScrap) MatterGeneratorSync.SCRAP_EU_PER_MB else MatterGeneratorSync.BASE_EU_PER_MB

        // Cap at producing 1mb per tick
        val progressIncrement = minOf(speedMultiplier.toInt().coerceAtLeast(1), MatterGeneratorSync.PROGRESS_MAX)

        val energyNeeded = ceil(
            euPerMb.toDouble() * progressIncrement / MatterGeneratorSync.PROGRESS_MAX * energyMultiplier.toDouble()
        ).toLong().coerceAtLeast(1L)

        if (sync.amount >= energyNeeded) {
            sync.consumeEnergy(energyNeeded)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = (sync.progress + progressIncrement).coerceAtMost(MatterGeneratorSync.PROGRESS_MAX)

            // Consume scrap proportionally to progress
            if (hasScrap) {
                val scrapToConsume = ceil(
                    MatterGeneratorSync.SCRAP_PER_MB.toDouble() * progressIncrement / MatterGeneratorSync.PROGRESS_MAX
                ).toInt()
                consumeScrap(scrapToConsume)
            }

            if (sync.progress >= MatterGeneratorSync.PROGRESS_MAX) {
                tankInternal.insertInternal(outputPerCycle)
                sync.progress = 0
                markDirty()
            }

            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }

        sync.mode = resolveDisplayedMode()
        sync.syncCurrentTickFlow()
    }

    private fun consumeScrap(count: Int) {
        if (count <= 0) return
        val scrapStack = getStack(SLOT_SCRAP)
        if (!isScrap(scrapStack)) return
        val actualCount = minOf(count, scrapStack.count)
        if (actualCount <= 0) return
        scrapStack.decrement(actualCount)
        if (scrapStack.isEmpty) {
            setStack(SLOT_SCRAP, ItemStack.EMPTY)
        } else {
            markDirty()
        }
    }

    private fun fillContainersFromTank() {
        if (tankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val input = getStack(SLOT_CONTAINER_INPUT)
        if (input.isEmpty) return

        val result = when (input.item) {
            Items.BUCKET -> ItemStack(ModFluids.UU_MATTER_BUCKET)
            emptyCellItem -> ItemStack(uuMatterCellItem)
            fluidCellItem -> {
                if (input.isFluidCellEmpty()) {
                    ItemStack(fluidCellItem).apply {
                        setFluidCellVariant(FluidVariant.of(ModFluids.UU_MATTER_STILL))
                    }
                } else {
                    ItemStack.EMPTY
                }
            }
            else -> ItemStack.EMPTY
        }
        if (result.isEmpty) return

        val output = getStack(SLOT_CONTAINER_OUTPUT)
        if (!canMergeIntoSlot(output, result)) return

        val drained = tankInternal.extractInternal(FluidConstants.BUCKET)
        if (drained < FluidConstants.BUCKET) return

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_CONTAINER_INPUT, ItemStack.EMPTY)
        if (output.isEmpty) setStack(SLOT_CONTAINER_OUTPUT, result.copy())
        else output.increment(result.count)
        markDirty()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val facing = world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        if (side == facing) return null
        return outputStorage
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

    private fun resolveDisplayedMode(): Int {
        val scrapCount = getStack(SLOT_SCRAP).count
        return if (scrapCount > 0) 1 else 0
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun isScrap(stack: ItemStack): Boolean =
        !stack.isEmpty && Registries.ITEM.getId(stack.item) == Identifier.of(Ic2_120.MOD_ID, "scrap")

    private fun isUuMatter(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == ModFluids.UU_MATTER_STILL || fluid == ModFluids.UU_MATTER_FLOWING

    private fun toMilliBuckets(amount: Long): Int =
        (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

    private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L
}
