package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.FermenterBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.screen.FermenterScreenHandler
import ic2_120.content.sync.FermenterSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
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
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 发酵机：
 * - 消耗热量和生物质，产出生物燃料（沼气）
 * - 每 2 秒（40 ticks）处理 1 次
 * - 每次消耗：4000 HU + 20 mB 生物质
 * - 每次产出：400 mB 生物燃料
 */
@ModBlockEntity(block = FermenterBlock::class)
class FermenterBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = FermenterBlock.ACTIVE
    override val tier: Int = 1

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    companion object {
        const val SLOT_INPUT_FILLED_CONTAINER = 0
        const val SLOT_INPUT_EMPTY_CONTAINER = 1
        const val SLOT_OUTPUT_EMPTY_CONTAINER = 2
        const val SLOT_OUTPUT_FILLED_CONTAINER = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_FILLED_CONTAINER)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_FILLED_CONTAINER)
        const val INVENTORY_SIZE = 8

        private const val NBT_INPUT_TANK = "InputTank"
        private const val NBT_OUTPUT_TANK = "OutputTank"
        private const val NBT_HEAT_BUFFER = "HeatBuffer"
        private const val MAX_HEAT_BUFFER = 40_000L

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = FermenterBlockEntity::class.type()
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
            ItemInsertRoute(intArrayOf(SLOT_INPUT_FILLED_CONTAINER), matcher = { isBiomassFilledContainer(it) }),
            ItemInsertRoute(intArrayOf(SLOT_OUTPUT_EMPTY_CONTAINER), matcher = { isEmptyContainerForFermenter(it) })
        ),
        extractSlots = intArrayOf(
            SLOT_INPUT_FILLED_CONTAINER,
            SLOT_INPUT_EMPTY_CONTAINER,
            SLOT_OUTPUT_EMPTY_CONTAINER,
            SLOT_OUTPUT_FILLED_CONTAINER
        ),
        markDirty = { markDirty() }
    )
    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "fluid_cell")) }
    private val biomassCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "biomass_cell")) }

    private val inputPerCycle = mbToDroplets(FermenterSync.INPUT_MB_PER_CYCLE)
    private val outputPerCycle = mbToDroplets(FermenterSync.OUTPUT_MB_PER_CYCLE)

    val syncedData = SyncedData(this)
    val sync = FermenterSync(syncedData)

    private var heatBuffer: Long = 0L
    private var heatReceivedThisTick: Long = 0L

    private val inputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 10

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isBiomass(variant.fluid)
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.inputBiomassMb = toMilliBuckets(amount)
            markDirty()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.BIOMASS_STILL) else FluidVariant.blank()
            sync.inputBiomassMb = toMilliBuckets(amount)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.BIOMASS_STILL)
            sync.inputBiomassMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.inputBiomassMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }
    }

    private val outputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 10

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = isBiogas(variant.fluid)

        override fun onFinalCommit() {
            sync.outputBiogasMb = toMilliBuckets(amount)
            markDirty()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.BIOFUEL_STILL) else FluidVariant.blank()
            sync.outputBiogasMb = toMilliBuckets(amount)
        }

        fun availableSpace(): Long = (tankCapacity - amount).coerceAtLeast(0L)

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.BIOFUEL_STILL)
            sync.outputBiogasMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.outputBiogasMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (!isBiomass(resource.fluid)) return 0L
            return inputTankInternal.insert(FluidVariant.of(ModFluids.BIOMASS_STILL), maxAmount, transaction)
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (!isBiogas(resource.fluid)) return 0L
            return outputTankInternal.extract(FluidVariant.of(ModFluids.BIOFUEL_STILL), maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!inputTankInternal.variant.isBlank && inputTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = inputTankInternal.variant
                    override fun getAmount(): Long = inputTankInternal.amount
                    override fun getCapacity(): Long = inputTankInternal.getTankCapacity()
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!outputTankInternal.variant.isBlank && outputTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = outputTankInternal.variant
                    override fun getAmount(): Long = outputTankInternal.amount
                    override fun getCapacity(): Long = outputTankInternal.getTankCapacity()
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        outputTankInternal.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        FermenterBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_FILLED_CONTAINER -> isBiomassFilledContainer(stack)
        SLOT_INPUT_EMPTY_CONTAINER -> false
        SLOT_OUTPUT_EMPTY_CONTAINER -> isEmptyContainerForFermenter(stack)
        SLOT_OUTPUT_FILLED_CONTAINER -> false
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun markDirty() {
        super.markDirty()
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.fermenter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        FermenterScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        inputTankInternal.setStoredFluid(nbt.getLong(NBT_INPUT_TANK))
        outputTankInternal.setStoredFluid(nbt.getLong(NBT_OUTPUT_TANK))
        heatBuffer = nbt.getLong(NBT_HEAT_BUFFER).coerceIn(0L, MAX_HEAT_BUFFER)
        sync.bufferedHeat = heatBuffer.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_INPUT_TANK, inputTankInternal.getStoredAmount())
        nbt.putLong(NBT_OUTPUT_TANK, outputTankInternal.getStoredAmount())
        nbt.putLong(NBT_HEAT_BUFFER, heatBuffer)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        val space = MAX_HEAT_BUFFER - heatBuffer
        if (space <= 0L) return 0L
        val accepted = minOf(hu, space)
        heatBuffer += accepted
        heatReceivedThisTick += accepted
        sync.bufferedHeat = heatBuffer.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
        return accepted
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            val front = state.get(Properties.HORIZONTAL_FACING)
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, outputTankInternal, fluidPipeProviderFilter, fluidPipeProviderSide, front)
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        handleInputBiomassContainers()
        fillOutputBiogasContainers()

        val inputHu = heatReceivedThisTick.coerceAtLeast(0L)
        sync.heatInputPerTick = inputHu.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        sync.heatConsumePerTick = 0
        sync.bufferedHeat = heatBuffer.toInt().coerceIn(0, Int.MAX_VALUE)

        val canProcess = heatBuffer >= FermenterSync.HEAT_PER_CYCLE.toLong() &&
            inputTankInternal.getStoredAmount() >= inputPerCycle &&
            outputTankInternal.availableSpace() >= outputPerCycle

        if (canProcess) {
            sync.progress += 1
            if (sync.progress >= FermenterSync.PROCESS_INTERVAL_TICKS) {
                val consumedInput = inputTankInternal.consumeInternal(inputPerCycle)
                if (consumedInput >= inputPerCycle) {
                    heatBuffer = (heatBuffer - FermenterSync.HEAT_PER_CYCLE).coerceAtLeast(0L)
                    outputTankInternal.insertInternal(outputPerCycle)
                    sync.heatConsumePerTick = FermenterSync.HEAT_PER_CYCLE
                    sync.bufferedHeat = heatBuffer.toInt().coerceIn(0, Int.MAX_VALUE)
                }
                sync.progress = 0
                markDirty()
            }
        } else if (sync.progress != 0) {
            sync.progress = 0
        }

        sync.isWorking = if (canProcess) 1 else 0
        setActiveState(world, pos, state, canProcess)
        heatReceivedThisTick = 0L
    }

    private fun handleInputBiomassContainers() {
        val input = getStack(SLOT_INPUT_FILLED_CONTAINER)
        if (input.isEmpty) return
        if (inputTankInternal.getTankCapacity() - inputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val emptyContainer = when {
            input.item == ModFluids.BIOMASS_BUCKET -> ItemStack(Items.BUCKET)
            input.item == biomassCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.getFluidCellVariant()?.fluid?.let { isBiomass(it) } == true -> ItemStack(emptyCellItem)
            else -> ItemStack.EMPTY
        }
        if (emptyContainer.isEmpty) return

        val emptyOutput = getStack(SLOT_INPUT_EMPTY_CONTAINER)
        if (!canMergeIntoSlot(emptyOutput, emptyContainer)) return

        val inserted = inputTankInternal.insertInternal(FluidConstants.BUCKET)
        if (inserted < FluidConstants.BUCKET) return

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT_FILLED_CONTAINER, ItemStack.EMPTY)
        if (emptyOutput.isEmpty) setStack(SLOT_INPUT_EMPTY_CONTAINER, emptyContainer.copy())
        else emptyOutput.increment(1)
        markDirty()
    }

    private fun fillOutputBiogasContainers() {
        if (outputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val emptyInput = getStack(SLOT_OUTPUT_EMPTY_CONTAINER)
        if (emptyInput.isEmpty) return

        val filled = when {
            emptyInput.item == Items.BUCKET -> ItemStack(ModFluids.BIOFUEL_BUCKET)
            emptyInput.item == emptyCellItem -> fluidToFilledCellStack(ModFluids.BIOFUEL_STILL)
            emptyInput.item is FluidCellItem && emptyInput.isFluidCellEmpty() -> ItemStack(fluidCellItem).apply {
                setFluidCellVariant(FluidVariant.of(ModFluids.BIOFUEL_STILL))
            }
            else -> ItemStack.EMPTY
        }
        if (filled.isEmpty) return

        val filledOutput = getStack(SLOT_OUTPUT_FILLED_CONTAINER)
        if (!canMergeIntoSlot(filledOutput, filled)) return

        val consumed = outputTankInternal.consumeInternal(FluidConstants.BUCKET)
        if (consumed < FluidConstants.BUCKET) return

        emptyInput.decrement(1)
        if (emptyInput.isEmpty) setStack(SLOT_OUTPUT_EMPTY_CONTAINER, ItemStack.EMPTY)
        if (filledOutput.isEmpty) setStack(SLOT_OUTPUT_FILLED_CONTAINER, filled.copy())
        else filledOutput.increment(1)
        markDirty()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == getFrontFacing()) return null
        return ioStorage
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH

    private fun isBiomass(fluid: Fluid): Boolean =
        fluid == ModFluids.BIOMASS_STILL || fluid == ModFluids.BIOMASS_FLOWING

    private fun isBiogas(fluid: Fluid): Boolean =
        fluid == ModFluids.BIOFUEL_STILL || fluid == ModFluids.BIOFUEL_FLOWING

    private fun toMilliBuckets(amount: Long): Int =
        (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

    private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.areItemsAndComponentsEqual(current, toInsert) && current.count < current.maxCount)
    }

    private fun isBiomassFilledContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when {
            stack.item == ModFluids.BIOMASS_BUCKET -> true
            Registries.ITEM.getId(stack.item) == Identifier.of("ic2_120", "biomass_cell") -> true
            stack.item is FluidCellItem -> {
                val fluid = stack.getFluidCellVariant()?.fluid
                fluid != null && isBiomass(fluid)
            }
            else -> false
        }
    }

    private fun isEmptyContainerForFermenter(stack: ItemStack): Boolean =
        !stack.isEmpty && (
            stack.item == Items.BUCKET ||
                Registries.ITEM.getId(stack.item) == Identifier.of("ic2_120", "empty_cell") ||
                (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            )
}
