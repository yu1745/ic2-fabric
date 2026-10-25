package ic2_120.content.block.machines

import ic2_120.compat.ContainerHelper26

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
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
import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 发酵机：
 * - 消耗热量和生物质，产出生物燃料 + 肥料
 * - 每 2 秒（40 ticks）处理 1 次
 * - 每次消耗：4000 HU + 20 mB 生物质
 * - 每次产出：400 mB 生物燃料
 * - 每 1000 mB（1桶）生物质消耗累计产出 1 个肥料
 */
@ModBlockEntity(block = FermenterBlock::class)
class FermenterBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Container, IRoutedSidedInventory, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedMenuProvider<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = FermenterBlock.ACTIVE
    override val tier: Int = 1

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    companion object {
        const val SLOT_INPUT_FILLED_CONTAINER = 0
        const val SLOT_INPUT_EMPTY_CONTAINER = 1
        const val SLOT_OUTPUT_EMPTY_CONTAINER = 2
        const val SLOT_OUTPUT_FILLED_CONTAINER = 3
        const val SLOT_OUTPUT_FERTILIZER = 4
        const val SLOT_UPGRADE_0 = 5
        const val SLOT_UPGRADE_1 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1)
        // SLOT_INPUT_EMPTY_CONTAINER 虽然名字带 INPUT，但它是消耗满生物质容器后返还的空容器槽
        // （AbstractContainerMenu 用 OUTPUT_ONLY_SLOT_SPEC，玩家只能取不能放），必须纳入弹出升级扫描范围。
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_FILLED_CONTAINER, SLOT_OUTPUT_FERTILIZER, SLOT_INPUT_EMPTY_CONTAINER)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_FILLED_CONTAINER)
        const val INVENTORY_SIZE = 7

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

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is FluidEjectorUpgrade || it.item is FluidPullingUpgrade }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_FILLED_CONTAINER), matcher = { isBiomassFilledContainer(it) }),
            ItemInsertRoute(intArrayOf(SLOT_OUTPUT_EMPTY_CONTAINER), matcher = { isEmptyContainerForFermenter(it) })
        ),
        extractSlots = intArrayOf(
            SLOT_INPUT_FILLED_CONTAINER,
            SLOT_INPUT_EMPTY_CONTAINER,
            SLOT_OUTPUT_EMPTY_CONTAINER,
            SLOT_OUTPUT_FILLED_CONTAINER,
            SLOT_OUTPUT_FERTILIZER
        ),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    private val emptyCellItem by lazy { BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "fluid_cell")) }
    private val biomassCellItem by lazy { BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "biomass_cell")) }
    private val fertilizerItem by lazy { BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "fertilizer")) }

    private val inputPerCycle = FermenterSync.INPUT_DROPLETS_PER_CYCLE.toLong()
    private val outputPerCycle = FermenterSync.OUTPUT_DROPLETS_PER_CYCLE.toLong()

    val syncedData = SyncedData()
    val sync = FermenterSync(syncedData)

    private var heatBuffer: Long = 0L
    private var heatReceivedThisTick: Long = 0L

    private val inputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isBiomass(variant.fluid) && !variant.isBlank

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.inputBiomass = toMilliBuckets(amount)
            sync.inputFluidType = sync.fluidToType(if (amount > 0L) variant.fluid else null)
            setChanged()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.BIOMASS_STILL) else FluidVariant.blank()
            sync.inputBiomass = toMilliBuckets(amount)
            sync.inputFluidType = sync.fluidToType(if (amount > 0L) variant.fluid else null)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.isBlank) variant = FluidVariant.of(ModFluids.BIOMASS_STILL)
                tx.commit()
                sync.inputBiomass = toMilliBuckets(amount)
                sync.inputFluidType = sync.fluidToType(variant.fluid)
                setChanged()
                actual
            }
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                val becameEmpty = amount <= 0L
                if (becameEmpty) variant = FluidVariant.blank()
                tx.commit()
                sync.inputBiomass = toMilliBuckets(amount)
                sync.inputFluidType = sync.fluidToType(if (becameEmpty) null else variant.fluid)
                setChanged()
                actual
            }
        }
    }

    private val outputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = false

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = !variant.isBlank

        override fun onFinalCommit() {
            sync.outputBiogas = toMilliBuckets(amount)
            sync.outputFluidType = sync.fluidToType(if (amount > 0L) variant.fluid else null)
            setChanged()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.BIOFUEL_STILL) else FluidVariant.blank()
            sync.outputBiogas = toMilliBuckets(amount)
            sync.outputFluidType = sync.fluidToType(if (amount > 0L) variant.fluid else null)
        }

        fun availableSpace(): Long = (tankCapacity - amount).coerceAtLeast(0L)

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.isBlank) variant = FluidVariant.of(ModFluids.BIOFUEL_STILL)
                tx.commit()
                sync.outputBiogas = toMilliBuckets(amount)
                sync.outputFluidType = sync.fluidToType(variant.fluid)
                setChanged()
                actual
            }
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                val becameEmpty = amount <= 0L
                if (becameEmpty) variant = FluidVariant.blank()
                tx.commit()
                sync.outputBiogas = toMilliBuckets(amount)
                sync.outputFluidType = sync.fluidToType(if (becameEmpty) null else variant.fluid)
                setChanged()
                actual
            }
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
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = inputTankInternal.variant
                    override fun getAmount(): Long = inputTankInternal.amount
                    override fun getCapacity(): Long = inputTankInternal.getTankCapacity()
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = inputTankInternal.variant.isBlank
                })
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = outputTankInternal.variant
                    override fun getAmount(): Long = outputTankInternal.amount
                    override fun getCapacity(): Long = outputTankInternal.getTankCapacity()
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        outputTankInternal.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = outputTankInternal.variant.isBlank
                })
            return views.iterator()
        }
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        FermenterBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this
    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_FILLED_CONTAINER -> isBiomassFilledContainer(stack)
        SLOT_INPUT_EMPTY_CONTAINER -> false
        SLOT_OUTPUT_EMPTY_CONTAINER -> isEmptyContainerForFermenter(stack)
        SLOT_OUTPUT_FILLED_CONTAINER -> false
        SLOT_OUTPUT_FERTILIZER -> false
        in SLOT_UPGRADE_0..SLOT_UPGRADE_1 -> stack.item is FluidEjectorUpgrade || stack.item is FluidPullingUpgrade
        else -> false
    }

    override fun setChanged() {
        super.setChanged()
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.fermenter")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        FermenterScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        inputTankInternal.setStoredFluid(nbt.getLongOr(NBT_INPUT_TANK, 0L))
        outputTankInternal.setStoredFluid(nbt.getLongOr(NBT_OUTPUT_TANK, 0L))
        heatBuffer = nbt.getLongOr(NBT_HEAT_BUFFER, 0L).coerceIn(0L, MAX_HEAT_BUFFER)
        sync.bufferedHeat = heatBuffer.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
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
        setChanged()
        return accepted
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, outputTankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, inputTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
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
                    sync.fertilizerProgress += FermenterSync.INPUT_DROPLETS_PER_CYCLE
                    if (sync.fertilizerProgress >= FermenterSync.FERTILIZER_PER_BIOMASS_BUCKET_DROPLETS) {
                        tryProduceFertilizer()
                    }
                    sync.heatConsumePerTick = FermenterSync.HEAT_PER_CYCLE
                    sync.bufferedHeat = heatBuffer.toInt().coerceIn(0, Int.MAX_VALUE)
                }
                sync.progress = 0
                setChanged()
            }
        } else if (sync.progress != 0) {
            sync.progress = 0
        }

        sync.isWorking = if (canProcess) 1 else 0
        setActiveState(world, pos, state, canProcess)
        heatReceivedThisTick = 0L
    }

    private fun handleInputBiomassContainers() {
        val input = getItem(SLOT_INPUT_FILLED_CONTAINER)
        if (input.isEmpty) return
        if (inputTankInternal.getTankCapacity() - inputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val emptyContainer = when {
            input.item == ModFluids.BIOMASS_BUCKET -> ItemStack(Items.BUCKET)
            input.item == biomassCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.getFluidCellVariant()?.fluid?.let { isBiomass(it) } == true -> ItemStack(emptyCellItem)
            else -> ItemStack.EMPTY
        }
        if (emptyContainer.isEmpty) return

        val emptyOutput = getItem(SLOT_INPUT_EMPTY_CONTAINER)
        if (!canMergeIntoSlot(emptyOutput, emptyContainer)) return

        val inserted = inputTankInternal.insertInternal(FluidConstants.BUCKET)
        if (inserted < FluidConstants.BUCKET) return

        input.shrink(1)
        if (input.isEmpty) setItem(SLOT_INPUT_FILLED_CONTAINER, ItemStack.EMPTY)
        if (emptyOutput.isEmpty) setItem(SLOT_INPUT_EMPTY_CONTAINER, emptyContainer.copy())
        else emptyOutput.grow(1)
        setChanged()
    }

    private fun fillOutputBiogasContainers() {
        if (outputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val emptyInput = getItem(SLOT_OUTPUT_EMPTY_CONTAINER)
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

        val filledOutput = getItem(SLOT_OUTPUT_FILLED_CONTAINER)
        if (!canMergeIntoSlot(filledOutput, filled)) return

        val consumed = outputTankInternal.consumeInternal(FluidConstants.BUCKET)
        if (consumed < FluidConstants.BUCKET) return

        emptyInput.shrink(1)
        if (emptyInput.isEmpty) setItem(SLOT_OUTPUT_EMPTY_CONTAINER, ItemStack.EMPTY)
        if (filledOutput.isEmpty) setItem(SLOT_OUTPUT_FILLED_CONTAINER, filled.copy())
        else filledOutput.grow(1)
        setChanged()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return ioStorage
    }

    private fun tryProduceFertilizer() {
        val fertilizerStack = ItemStack(fertilizerItem, 1)
        val currentOutput = getItem(SLOT_OUTPUT_FERTILIZER)
        if (!canMergeIntoSlot(currentOutput, fertilizerStack)) return
        sync.fertilizerProgress -= FermenterSync.FERTILIZER_PER_BIOMASS_BUCKET_DROPLETS
        if (currentOutput.isEmpty) setItem(SLOT_OUTPUT_FERTILIZER, fertilizerStack.copy())
        else currentOutput.grow(1)
        setChanged()
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH

    private fun isBiomass(fluid: Fluid): Boolean =
        fluid == ModFluids.BIOMASS_STILL || fluid == ModFluids.BIOMASS_FLOWING

    private fun isBiogas(fluid: Fluid): Boolean =
        fluid == ModFluids.BIOFUEL_STILL || fluid == ModFluids.BIOFUEL_FLOWING

    private fun toMilliBuckets(amount: Long): Int =
        amount.toInt().coerceAtLeast(0)

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.isSameItemSameComponents(current, toInsert) && current.count < current.maxStackSize)
    }

    private fun isBiomassFilledContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when {
            stack.item == ModFluids.BIOMASS_BUCKET -> true
            BuiltInRegistries.ITEM.getKey(stack.item) == Identifier.fromNamespaceAndPath("ic2_120", "biomass_cell") -> true
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
                BuiltInRegistries.ITEM.getKey(stack.item) == Identifier.fromNamespaceAndPath("ic2_120", "empty_cell") ||
                (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            )
}
