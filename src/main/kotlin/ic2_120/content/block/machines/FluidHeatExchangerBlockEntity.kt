package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.FluidHeatExchangerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.FluidCellItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.screen.FluidHeatExchangerScreenHandler
import ic2_120.content.sync.FluidHeatExchangerSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 流体热交换机：
 * - 10 个热传导器槽，每槽 1 个热传导器
 * - 每个热传导器输出 10 HU/t，满槽 100 HU/t
 * - 热冷却液/岩浆 -> 冷却液/冷却岩浆
 * - 换热比例：1000 mB -> 20,000 HU
 * - 仅背面传热
 */
@ModBlockEntity(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatGeneratorBlockEntityBase(type, pos, state), Inventory, IFluidPipeUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = FluidHeatExchangerBlock.ACTIVE

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    override val tier: Int = 1

    companion object {
        const val SLOT_EXCHANGER_0 = 0
        const val SLOT_EXCHANGER_1 = 1
        const val SLOT_EXCHANGER_2 = 2
        const val SLOT_EXCHANGER_3 = 3
        const val SLOT_EXCHANGER_4 = 4
        const val SLOT_EXCHANGER_5 = 5
        const val SLOT_EXCHANGER_6 = 6
        const val SLOT_EXCHANGER_7 = 7
        const val SLOT_EXCHANGER_8 = 8
        const val SLOT_EXCHANGER_9 = 9
        val SLOT_EXCHANGER_INDICES = intArrayOf(
            SLOT_EXCHANGER_0,
            SLOT_EXCHANGER_1,
            SLOT_EXCHANGER_2,
            SLOT_EXCHANGER_3,
            SLOT_EXCHANGER_4,
            SLOT_EXCHANGER_5,
            SLOT_EXCHANGER_6,
            SLOT_EXCHANGER_7,
            SLOT_EXCHANGER_8,
            SLOT_EXCHANGER_9
        )

        const val SLOT_UPGRADE_0 = 10
        const val SLOT_UPGRADE_1 = 11
        const val SLOT_UPGRADE_2 = 12
        const val SLOT_UPGRADE_3 = 13
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val SLOT_INPUT_FILLED_CONTAINER = 14
        const val SLOT_INPUT_EMPTY_CONTAINER = 15
        const val SLOT_OUTPUT_EMPTY_CONTAINER = 16
        const val SLOT_OUTPUT_FILLED_CONTAINER = 17

        const val INVENTORY_SIZE = 18

        private const val NBT_INPUT_TANK = "InputTank"
        private const val NBT_INPUT_FLUID = "InputFluid"
        private const val NBT_OUTPUT_TANK = "OutputTank"
        private const val NBT_OUTPUT_FLUID = "OutputFluid"
        private const val NBT_BUFFERED_HEAT = "BufferedHeat"
        private const val NBT_HEAT_FROM_FLUID_REMAINDER = "HeatFromFluidRemainder"

        private const val HU_PER_EXCHANGER_PER_TICK = 10L
        private const val HU_PER_BUCKET = 20_000L
        private const val FLUID_PROCESS_AMOUNT = FluidConstants.BUCKET

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = FluidHeatExchangerBlockEntity::class.type()
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
        insertRoutes = buildList {
            add(ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }))
            for (idx in SLOT_EXCHANGER_INDICES) {
                add(ItemInsertRoute(intArrayOf(idx), matcher = { isValid(idx, it) }, maxPerSlot = 1))
            }
            add(ItemInsertRoute(intArrayOf(SLOT_INPUT_FILLED_CONTAINER), matcher = { isValid(SLOT_INPUT_FILLED_CONTAINER, it) }))
            add(ItemInsertRoute(intArrayOf(SLOT_OUTPUT_EMPTY_CONTAINER), matcher = { isValid(SLOT_OUTPUT_EMPTY_CONTAINER, it) }))
        },
        extractSlots = IntArray(INVENTORY_SIZE) { it },
        markDirty = { markDirty() }
    )
    private val heatConductorItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "heat_conductor")) }
    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "fluid_cell")) }
    private val lavaCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "lava_cell")) }
    private val hotCoolantCellItem by lazy { Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "hot_coolant_cell")) }

    val syncedData = SyncedData(this)
    override val heatFlow = HeatFlowSync(syncedData, this)
    val sync = FluidHeatExchangerSync(syncedData, heatFlow)

    // 通过流体换热累计的可输出热量（HU）。
    private var bufferedHeat: Long = 0L
    // 实际流体滴数 -> HU 换算余数（分母：FLUID_PROCESS_AMOUNT）。
    private var heatFromFluidRemainder: Long = 0L

    private val inputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 10

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isAcceptedInputFluid(variant.fluid)
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.inputFluidMb = toMilliBuckets(amount)
            markDirty()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        fun setStoredFluid(newAmount: Long, fluid: Fluid?) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = when {
                amount <= 0L || fluid == null || !isAcceptedInputFluid(fluid) -> FluidVariant.blank()
                isHotCoolant(fluid) -> FluidVariant.of(ModFluids.HOT_COOLANT_STILL)
                else -> FluidVariant.of(Fluids.LAVA)
            }
            sync.inputFluidMb = toMilliBuckets(amount)
        }

        fun insertInternal(toInsert: Long, fluid: Fluid): Long {
            if (toInsert <= 0L || !isAcceptedInputFluid(fluid)) return 0L
            val normalized = when {
                isHotCoolant(fluid) -> ModFluids.HOT_COOLANT_STILL
                else -> Fluids.LAVA
            }
            if (amount > 0L && variant.fluid != normalized) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L

            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(normalized)
            sync.inputFluidMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L

            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.inputFluidMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }
    }

    private val outputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 10

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = isAcceptedOutputFluid(variant.fluid)

        override fun onFinalCommit() {
            sync.outputFluidMb = toMilliBuckets(amount)
            markDirty()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        fun setStoredFluid(newAmount: Long, fluid: Fluid?) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = when {
                amount <= 0L || fluid == null || !isAcceptedOutputFluid(fluid) -> FluidVariant.blank()
                isCoolant(fluid) -> FluidVariant.of(ModFluids.COOLANT_STILL)
                else -> FluidVariant.of(ModFluids.PAHOEHOE_LAVA_STILL)
            }
            sync.outputFluidMb = toMilliBuckets(amount)
        }

        fun canAccept(toInsert: Long, outputVariant: FluidVariant): Boolean {
            if (toInsert <= 0L) return false
            if (amount > 0L && variant.fluid != outputVariant.fluid) return false
            return (tankCapacity - amount) >= toInsert
        }

        fun availableSpaceFor(outputVariant: FluidVariant): Long {
            if (amount > 0L && variant.fluid != outputVariant.fluid) return 0L
            return (tankCapacity - amount).coerceAtLeast(0L)
        }

        fun insertInternal(toInsert: Long, outputVariant: FluidVariant): Long {
            if (toInsert <= 0L) return 0L
            if (amount > 0L && variant.fluid != outputVariant.fluid) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L

            amount += actual
            if (variant.isBlank) variant = outputVariant
            sync.outputFluidMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L

            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.outputFluidMb = toMilliBuckets(amount)
            markDirty()
            return actual
        }
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            val target = when {
                isHotCoolant(resource.fluid) -> FluidVariant.of(ModFluids.HOT_COOLANT_STILL)
                isLava(resource.fluid) -> FluidVariant.of(Fluids.LAVA)
                else -> return 0L
            }
            return inputTankInternal.insert(target, maxAmount, transaction)
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            val target = when {
                isCoolant(resource.fluid) -> FluidVariant.of(ModFluids.COOLANT_STILL)
                isPahoehoe(resource.fluid) -> FluidVariant.of(ModFluids.PAHOEHOE_LAVA_STILL)
                else -> return 0L
            }
            return outputTankInternal.extract(target, maxAmount, transaction)
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
        FluidHeatExchangerBlockEntity::class.type(),
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
    override fun markDirty() {
        super.markDirty()
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        val isExchangerSlot = slot in SLOT_EXCHANGER_0..SLOT_EXCHANGER_9
        val isUpgradeSlot = slot in SLOT_UPGRADE_0..SLOT_UPGRADE_3

        if (isExchangerSlot && !stack.isEmpty && stack.item != heatConductorItem) return

        if (isExchangerSlot && !stack.isEmpty && stack.count > 1) {
            val single = stack.copy()
            single.count = 1
            inventory[slot] = single
        } else if (isUpgradeSlot && !stack.isEmpty && stack.count > 1) {
            val single = stack.copy()
            single.count = 1
            inventory[slot] = single
        } else {
            inventory[slot] = stack
            if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        }
        markDirty()
    }

    private fun fheMatchesInputFilledContainer(stack: ItemStack): Boolean = when {
        stack.item == Items.LAVA_BUCKET -> true
        stack.item == ModFluids.HOT_COOLANT_BUCKET -> true
        Registries.ITEM.getId(stack.item) == Identifier.of("ic2_120", "lava_cell") -> true
        Registries.ITEM.getId(stack.item) == Identifier.of("ic2_120", "hot_coolant_cell") -> true
        stack.item is FluidCellItem -> {
            val fluid = stack.getFluidCellVariant()?.fluid
            fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA ||
                fluid == ModFluids.HOT_COOLANT_STILL || fluid == ModFluids.HOT_COOLANT_FLOWING
        }
        else -> false
    }

    private fun fheMatchesOutputEmptyContainer(stack: ItemStack): Boolean =
        stack.item == Items.BUCKET ||
            Registries.ITEM.getId(stack.item) == Identifier.of("ic2_120", "empty_cell") ||
            (stack.item is FluidCellItem && stack.isFluidCellEmpty())

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot in SLOT_EXCHANGER_INDICES -> stack.item == heatConductorItem
        slot in SLOT_UPGRADE_INDICES -> stack.item is IUpgradeItem
        slot == SLOT_INPUT_FILLED_CONTAINER -> fheMatchesInputFilledContainer(stack)
        slot == SLOT_INPUT_EMPTY_CONTAINER -> false
        slot == SLOT_OUTPUT_EMPTY_CONTAINER -> fheMatchesOutputEmptyContainer(stack)
        slot == SLOT_OUTPUT_FILLED_CONTAINER -> false
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.liquid_heat_exchanger")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        FluidHeatExchangerScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)

        bufferedHeat = nbt.getLong(NBT_BUFFERED_HEAT).coerceAtLeast(0L)
        heatFromFluidRemainder = nbt.getLong(NBT_HEAT_FROM_FLUID_REMAINDER).coerceIn(0L, FLUID_PROCESS_AMOUNT - 1L)
        inputTankInternal.setStoredFluid(
            nbt.getLong(NBT_INPUT_TANK),
            readFluidId(nbt, NBT_INPUT_FLUID)
        )
        outputTankInternal.setStoredFluid(
            nbt.getLong(NBT_OUTPUT_TANK),
            readFluidId(nbt, NBT_OUTPUT_FLUID)
        )
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)

        nbt.putLong(NBT_BUFFERED_HEAT, bufferedHeat)
        nbt.putLong(NBT_HEAT_FROM_FLUID_REMAINDER, heatFromFluidRemainder)
        nbt.putLong(NBT_INPUT_TANK, inputTankInternal.getStoredAmount())
        nbt.putLong(NBT_OUTPUT_TANK, outputTankInternal.getStoredAmount())
        writeFluidId(nbt, NBT_INPUT_FLUID, inputTankInternal.variant.fluid)
        writeFluidId(nbt, NBT_OUTPUT_FLUID, outputTankInternal.variant.fluid)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            val front = state.get(Properties.HORIZONTAL_FACING)
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, outputTankInternal, fluidPipeProviderFilter, fluidPipeProviderSide, front)
        }
        handleInputFluidContainers()
        fillOutputFluidContainers()
        tickHeatMachine(world, pos, state)
    }

    override fun generateHeat(world: World, pos: BlockPos, state: BlockState): Long {
        val exchangerCount = countHeatExchangers()
        if (exchangerCount <= 0) return 0L

        val maxHeatPerTick = exchangerCount * HU_PER_EXCHANGER_PER_TICK
        val before = bufferedHeat

        val needHeatFromFluid = (maxHeatPerTick - bufferedHeat).coerceAtLeast(0L)
        if (needHeatFromFluid > 0L) {
            bufferedHeat += processFluidForHeat(needHeatFromFluid)
        }

        if (bufferedHeat <= 0L) return 0L

        val generated = minOf(maxHeatPerTick, bufferedHeat)
        bufferedHeat -= generated
        if (bufferedHeat != before || generated > 0L) markDirty()
        return generated
    }

    override fun syncAdditionalData() {
        sync.isWorking = if (getLastGeneratedHeat() > 0L) 1 else 0
    }

    override fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean =
        generatedHeat > 0L && hasValidConsumer

    override fun getActiveState(state: BlockState): Boolean =
        state.get(FluidHeatExchangerBlock.ACTIVE)

    private fun processFluidForHeat(targetHeat: Long): Long {
        if (targetHeat <= 0L) return 0L
        if (inputTankInternal.getStoredAmount() <= 0L) return 0L

        val inputFluid = inputTankInternal.variant.fluid
        val outputVariant = when {
            isHotCoolant(inputFluid) -> FluidVariant.of(ModFluids.COOLANT_STILL)
            isLava(inputFluid) -> FluidVariant.of(ModFluids.PAHOEHOE_LAVA_STILL)
            else -> return 0L
        }

        val requiredNumerator = targetHeat * FLUID_PROCESS_AMOUNT - heatFromFluidRemainder
        val dropletsNeeded = if (requiredNumerator <= 0L) {
            0L
        } else {
            (requiredNumerator + HU_PER_BUCKET - 1L) / HU_PER_BUCKET
        }
        if (dropletsNeeded <= 0L) return 0L

        val inputAvailable = inputTankInternal.getStoredAmount()
        val outputAvailable = outputTankInternal.availableSpaceFor(outputVariant)
        val dropletsToProcess = minOf(dropletsNeeded, inputAvailable, outputAvailable)
        if (dropletsToProcess <= 0L) return 0L

        val consumed = inputTankInternal.consumeInternal(dropletsToProcess)
        if (consumed <= 0L) return 0L
        val inserted = outputTankInternal.insertInternal(consumed, outputVariant)
        if (inserted <= 0L) return 0L

        val heatNumerator = inserted * HU_PER_BUCKET + heatFromFluidRemainder
        val produced = heatNumerator / FLUID_PROCESS_AMOUNT
        heatFromFluidRemainder = heatNumerator % FLUID_PROCESS_AMOUNT
        return produced
    }

    private fun handleInputFluidContainers() {
        val input = getStack(SLOT_INPUT_FILLED_CONTAINER)
        if (input.isEmpty) return

        val parsed = resolveInputContainer(input) ?: return
        if (inputTankInternal.getTankCapacity() - inputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val emptyOutput = getStack(SLOT_INPUT_EMPTY_CONTAINER)
        if (!canMergeIntoSlot(emptyOutput, parsed.emptyContainer)) return

        val inserted = inputTankInternal.insertInternal(FluidConstants.BUCKET, parsed.fluid)
        if (inserted < FluidConstants.BUCKET) return

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT_FILLED_CONTAINER, ItemStack.EMPTY)
        if (emptyOutput.isEmpty) setStack(SLOT_INPUT_EMPTY_CONTAINER, parsed.emptyContainer.copy())
        else emptyOutput.increment(1)
        markDirty()
    }

    private fun fillOutputFluidContainers() {
        if (outputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val emptyInput = getStack(SLOT_OUTPUT_EMPTY_CONTAINER)
        if (emptyInput.isEmpty) return

        val fluid = outputTankInternal.variant.fluid
        val filled = resolveOutputFilledContainer(emptyInput, fluid) ?: return

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

    private fun resolveInputContainer(stack: ItemStack): InputContainerInfo? {
        return when {
            stack.item == Items.LAVA_BUCKET -> InputContainerInfo(Fluids.LAVA, ItemStack(Items.BUCKET))
            stack.item == ModFluids.HOT_COOLANT_BUCKET -> InputContainerInfo(ModFluids.HOT_COOLANT_STILL, ItemStack(Items.BUCKET))
            stack.item == lavaCellItem -> InputContainerInfo(Fluids.LAVA, ItemStack(emptyCellItem))
            stack.item == hotCoolantCellItem -> InputContainerInfo(ModFluids.HOT_COOLANT_STILL, ItemStack(emptyCellItem))
            stack.item == fluidCellItem -> {
                val fluid = stack.getFluidCellVariant()?.fluid ?: return null
                if (!isAcceptedInputFluid(fluid)) return null
                InputContainerInfo(fluid, ItemStack(emptyCellItem))
            }

            else -> null
        }
    }

    private fun resolveOutputFilledContainer(emptyContainer: ItemStack, fluid: Fluid): ItemStack? {
        return when {
            emptyContainer.item == Items.BUCKET -> when {
                isCoolant(fluid) -> ItemStack(ModFluids.COOLANT_BUCKET)
                isPahoehoe(fluid) -> ItemStack(ModFluids.PAHOEHOE_LAVA_BUCKET)
                else -> null
            }

            emptyContainer.item == emptyCellItem -> {
                if (!isAcceptedOutputFluid(fluid)) return null
                fluidToFilledCellStack(fluid)
            }

            emptyContainer.item is FluidCellItem && emptyContainer.isFluidCellEmpty() -> {
                if (!isAcceptedOutputFluid(fluid)) return null
                ItemStack(fluidCellItem).apply { setFluidCellVariant(FluidVariant.of(fluid)) }
            }

            else -> null
        }
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == getFrontFacing()) return null
        return ioStorage
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH

    private fun countHeatExchangers(): Long =
        SLOT_EXCHANGER_INDICES.count { !inventory[it].isEmpty && inventory[it].item == heatConductorItem }.toLong()

    private fun isAcceptedInputFluid(fluid: Fluid): Boolean = isHotCoolant(fluid) || isLava(fluid)

    private fun isAcceptedOutputFluid(fluid: Fluid): Boolean = isCoolant(fluid) || isPahoehoe(fluid)

    private fun isHotCoolant(fluid: Fluid): Boolean =
        fluid == ModFluids.HOT_COOLANT_STILL || fluid == ModFluids.HOT_COOLANT_FLOWING

    private fun isLava(fluid: Fluid): Boolean =
        fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA

    private fun isCoolant(fluid: Fluid): Boolean =
        fluid == ModFluids.COOLANT_STILL || fluid == ModFluids.COOLANT_FLOWING

    private fun isPahoehoe(fluid: Fluid): Boolean =
        fluid == ModFluids.PAHOEHOE_LAVA_STILL || fluid == ModFluids.PAHOEHOE_LAVA_FLOWING

    private fun toMilliBuckets(amount: Long): Int =
        (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

    private fun readFluidId(nbt: NbtCompound, key: String): Fluid? {
        if (!nbt.contains(key)) return null
        val raw = nbt.getString(key)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        if (!Registries.FLUID.containsId(id)) return null
        return Registries.FLUID.get(id)
    }

    private fun writeFluidId(nbt: NbtCompound, key: String, fluid: Fluid?) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            nbt.remove(key)
            return
        }
        nbt.putString(key, Registries.FLUID.getId(fluid).toString())
    }

    private data class InputContainerInfo(
        val fluid: Fluid,
        val emptyContainer: ItemStack
    )
}
