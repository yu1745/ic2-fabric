package ic2_120.content.block.machines

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.ReplicatorBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.AdjacentEnergyTransferComponent
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
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
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
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import kotlin.math.ceil

@ModBlockEntity(block = ReplicatorBlock::class)
class ReplicatorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport,
    IEjectorUpgradeSupport, IRedstoneInverterUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty = ReplicatorBlock.ACTIVE
    override val tier: Int = ReplicatorSync.REPLICATOR_TIER
    override fun getInventory(): Container = this

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var redstoneInverted: Boolean = false
    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

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
        // SLOT_CONTAINER_OUTPUT 是消耗满 UU 物质单元后返还的空单元槽（OUTPUT_ONLY），
        // 必须纳入弹出升级扫描范围，否则空单元会卡在机器里无法自动弹出。
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT, SLOT_CONTAINER_OUTPUT)
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

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_CONTAINER_INPUT), matcher = { isValid(SLOT_CONTAINER_INPUT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT, SLOT_CONTAINER_INPUT, SLOT_CONTAINER_OUTPUT, SLOT_DISCHARGING),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    private var singlePulseConsumed = false
    private var fluidConsumptionRemainder = 0L

    // A replicator asks for the same output stack on every progress tick.  Keep
    // the registry parse/lookup out of that hot path; the returned stack is only
    // inspected here and is copied when it is inserted into the inventory.
    private var cachedOutputItemId: String? = null
    private var cachedOutputStack: ItemStack = ItemStack.EMPTY

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = ReplicatorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ReplicatorSync.REPLICATOR_TIER + voltageTierBonus) },
        // 红石停机时拒绝外部供能，避免停工期间持续拉电填充内部缓冲（#30）。
        externalEnergyGate = {
            val w = world
            w == null || RedstoneControlComponent.canRun(w, pos, this)
        }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        /** 实际容量 = max(标称 16K, 读档存量)。只升到存量、不为存量而降，之后所有写入都被 capacity 封顶且只减不增，永不可能再超。 */
        private var tankCapacity = ReplicatorSync.TANK_CAPACITY_DROPLETS.toLong()

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isUuMatter(variant.fluid) && !variant.isBlank

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.fluidAmount = toMilliBuckets(amount)
            sync.fluidCapacity = getEffectiveCapacity()
            setChanged()
        }

        /** 方案 A：读档不截断存量；存量若超标称容量则把实际容量抬升到存量值，保全流体且保证后续不可能再被超出。 */
        fun setStoredAmount(newAmount: Long) {
            val stored = newAmount.coerceAtLeast(0L)
            amount = stored
            // 容量只升到存量、不因存量降低而回落（在存续期间保持“最高水位”作为不可再被超出的上限）。
            tankCapacity = tankCapacity.coerceAtLeast(stored)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.UU_MATTER_STILL) else FluidVariant.blank()
            sync.fluidAmount = toMilliBuckets(amount)
            sync.fluidCapacity = getEffectiveCapacity()
        }

        fun getTankCapacity(): Long = tankCapacity
        fun getStoredAmount(): Long = amount
        fun getEffectiveCapacity(): Int = tankCapacity.toInt().coerceIn(0, Int.MAX_VALUE)
        fun extractInternal(toExtract: Long): Long {
            val actual = minOf(toExtract, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
                sync.fluidAmount = toMilliBuckets(amount)
                setChanged()
                actual
            }
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
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clear() = inventory.clear()
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    private fun replicatorIsDrainableUuContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val itemId = BuiltInRegistries.ITEM.getId(stack.item)
        val fluidCellId = ResourceLocation.fromNamespaceAndPath("ic2_120", "fluid_cell")
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
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.replicator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        ReplicatorScreenHandler(
            syncId,
            playerInventory,
            this,
            pos,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(ReplicatorSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progressUb = nbt.getInt(NBT_PROGRESS_UB).coerceAtLeast(0)
        singlePulseConsumed = nbt.getBoolean(NBT_SINGLE_PULSE_CONSUMED)
        fluidConsumptionRemainder = nbt.getLong(NBT_FLUID_REMAINDER).coerceAtLeast(0L)
        tankInternal.setStoredAmount(nbt.getLong(NBT_TANK_AMOUNT))
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ReplicatorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.getStoredAmount())
        nbt.putInt(NBT_PROGRESS_UB, sync.progressUb)
        nbt.putBoolean(NBT_SINGLE_PULSE_CONSUMED, singlePulseConsumed)
        nbt.putLong(NBT_FLUID_REMAINDER, fluidConsumptionRemainder)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, tankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, tankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.fluidCapacity = tankInternal.getEffectiveCapacity()

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()
        fillTankFromContainers()

        val redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)
        if (!redstoneAllowsRun) {
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

        if (singlePulseConsumed && sync.progressUb == 0) {
            if (sync.status != ReplicatorSync.STATUS_CANCELLED) {
                sync.status = ReplicatorSync.STATUS_COMPLETE
            }
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
        // 只按本 tick 实际入账的进度计费：若 ubPerTick 超出剩余成本（最后一 tick 溢出），
        // 按完整 ubPerTick 扣流体会导致超频下每锭多扣（如 8 超频 87uB/tick → 每锭 7 tick
        // 进度 609 封顶 540、浪费 69 uB，却仍按 7 tick 扣 49.33 droplets，0.54B 只得 ~887 锭）。
        val creditedUb = minOf(ubPerTick, (template.uuCostUb - sync.progressUb).coerceAtLeast(1))
        val (dropletsPerTick, nextRemainder) = previewDropletsForTick(creditedUb)
        // 空罐（tank≤0）直接停机：droplets==0 的 tick 会免扣费照涨进度（余数进位），
        // 配合取消按钮重置余数可无限重置“免费泵”——在取消态点单次启动即可零 UU 白嫖成品。
        if (tankInternal.getStoredAmount() <= 0L || (dropletsPerTick > 0L && tankInternal.getStoredAmount() < dropletsPerTick)) {
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
        sync.progressUb = (sync.progressUb + creditedUb).coerceAtLeast(0).coerceAtMost(template.uuCostUb)
        sync.status = ReplicatorSync.STATUS_RUNNING
        if (sync.progressUb >= template.uuCostUb) {
            if (outputStack.isEmpty) setStack(SLOT_OUTPUT, resultStack.copy())
            else outputStack.grow(1)
            sync.progressUb = 0
            sync.status = ReplicatorSync.STATUS_COMPLETE
            singlePulseConsumed = sync.mode == ReplicatorSync.MODE_SINGLE
            setChanged()
        } else {
            setChanged()
        }

        setActiveState(world, pos, state, true)
        sync.syncCurrentTickFlow()
    }

    fun cancelWork() {
        resetProgress()
        singlePulseConsumed = true
        sync.status = ReplicatorSync.STATUS_CANCELLED
        setChanged()
    }

    fun setSingleMode() {
        sync.mode = ReplicatorSync.MODE_SINGLE
        singlePulseConsumed = false
        setChanged()
    }

    fun setRepeatMode() {
        sync.mode = ReplicatorSync.MODE_CONTINUOUS
        singlePulseConsumed = false
        setChanged()
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
        val facing = world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH
        if (side == facing) return null
        return tankInternal
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        setChanged()
    }

    private fun fillTankFromContainers() {
        val input = getStack(SLOT_CONTAINER_INPUT)
        if (input.isEmpty) return
        val output = getStack(SLOT_CONTAINER_OUTPUT)
        val space = tankInternal.getTankCapacity() - tankInternal.getStoredAmount()
        if (space < FluidConstants.BUCKET) return

        Transaction.openOuter().use { tx ->
            @Suppress("DEPRECATION")
            val ctx = ContainerItemContext.withInitial(input.copyWithCount(1))
            val itemStorage = ctx.find(FluidStorage.ITEM) ?: return@use
            for (view in itemStorage) {
                if (view.amount < FluidConstants.BUCKET || view.resource.isBlank) continue
                val containedFluid = view.resource
                if (!isUuMatter(containedFluid.fluid)) continue
                val extracted = view.extract(containedFluid, FluidConstants.BUCKET, tx)
                if (extracted < FluidConstants.BUCKET) continue
                val inserted = tankInternal.insert(FluidVariant.of(ModFluids.UU_MATTER_STILL), extracted, tx)
                if (inserted < extracted) continue

                val emptyResult = ctx.itemVariant.toStack(1)
                if (!canMergeIntoSlot(output, emptyResult)) return@use

                input.shrink(1)
                if (input.isEmpty) setStack(SLOT_CONTAINER_INPUT, ItemStack.EMPTY)

                if (output.isEmpty) setStack(SLOT_CONTAINER_OUTPUT, emptyResult.copy())
                else output.grow(emptyResult.count)

                sync.fluidAmount = toMilliBuckets(tankInternal.amount)
                tx.commit()
                return
            }
        }
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean =
        current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxStackSize)

    private fun isUuMatter(fluid: net.minecraft.world.level.material.Fluid): Boolean =
        fluid == ModFluids.UU_MATTER_STILL || fluid == ModFluids.UU_MATTER_FLOWING

    private fun createOutputStack(itemId: String): ItemStack? {
        if (itemId == cachedOutputItemId) {
            return cachedOutputStack.takeUnless { it.isEmpty }
        }
        cachedOutputItemId = itemId
        cachedOutputStack = ItemStack.EMPTY
        val id = net.minecraft.resources.ResourceLocation.tryParse(itemId) ?: return null
        val item = BuiltInRegistries.ITEM.getOrEmpty(id).orElse(null) ?: return null
        cachedOutputStack = if (item == net.minecraft.world.item.Items.AIR) ItemStack.EMPTY else ItemStack(item)
        return cachedOutputStack.takeUnless { it.isEmpty }
    }

    private fun toMilliBuckets(amount: Long): Int =
        amount.toInt().coerceAtLeast(0)

    private fun previewDropletsForTick(ub: Int): Pair<Long, Long> {
        val total = fluidConsumptionRemainder + ub.toLong() * FluidConstants.BUCKET
        val droplets = total / 1_000_000L
        return droplets to (total % 1_000_000L)
    }

}
