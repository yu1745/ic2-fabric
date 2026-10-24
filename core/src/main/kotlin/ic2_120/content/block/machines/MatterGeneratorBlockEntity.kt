package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.OverclockerUpgrade
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.AdjacentEnergyTransferComponent
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
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
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
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
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
import kotlin.math.ceil

@ModBlockEntity(block = MatterGeneratorBlock::class)
class MatterGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport,
    IEjectorUpgradeSupport, IRedstoneInverterUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty = MatterGeneratorBlock.ACTIVE
    override val tier: Int = MatterGeneratorSync.MATTER_GENERATOR_TIER

    override fun getInventory(): Inventory = this

    override var capacityBonus: Long = 0L
    override var redstoneInverted: Boolean = false
    override var voltageTierBonus: Int = 0

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    companion object {
        const val SLOT_SCRAP = 0
        const val SLOT_CONTAINER_INPUT = 1
        const val SLOT_CONTAINER_OUTPUT = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_CONTAINER_OUTPUT)
        // 抽入升级的抽入目标：除容器输入槽外还包含废料槽——修复“抽入升级无法从箱子抽废料/废料盒”（#30）。
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_SCRAP, SLOT_CONTAINER_INPUT)
        const val INVENTORY_SIZE = 7

        /** 一个废料盒按合成配方（3×3 废料）等价于 9 个废料。 */
        const val SCRAP_UNITS_PER_BOX = 9

        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_PROGRESS = "Progress"
        private const val NBT_SCRAP_CONSUMED = "ScrapConsumed"

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
            ItemInsertRoute(intArrayOf(SLOT_SCRAP), matcher = { isValid(SLOT_SCRAP, it) }),
            ItemInsertRoute(intArrayOf(SLOT_CONTAINER_INPUT), matcher = { isValid(SLOT_CONTAINER_INPUT, it) })
        ),
        extractSlots = intArrayOf(SLOT_SCRAP, SLOT_CONTAINER_INPUT, SLOT_CONTAINER_OUTPUT),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage
    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) }
    private val uuMatterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "uu_matter_cell")) }
    private val scrapItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "scrap")) }
    private val scrapBoxItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "scrap_box")) }

    private val outputPerCycle = mbToDroplets(1)
    /** 当前 1 mB 生成周期内已经消耗的废料数量。 */
    private var scrapConsumedThisCycle = 0

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = MatterGeneratorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(MatterGeneratorSync.MATTER_GENERATOR_TIER + voltageTierBonus) },
        // 红石停机时拒绝外部供能，避免停工期间持续拉电填充内部缓冲（#30）。
        externalEnergyGate = {
            val w = world
            w == null || RedstoneControlComponent.canRun(w, pos, this)
        }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        /** 实际容量 = max(标称 10K, 读档存量)。只升到存量、不为存量而降，之后所有写入都被 capacity 封顶且只减不增，永不可能再超。 */
        private var tankCapacity = MatterGeneratorSync.TANK_CAPACITY_DROPLETS.toLong()

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = false

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = isUuMatter(variant.fluid) && !variant.isBlank

        override fun onFinalCommit() {
            sync.fluidAmount = toMilliBuckets(amount)
            sync.fluidCapacity = getEffectiveCapacity()
            markDirty()
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity
        fun availableSpace(): Long = (tankCapacity - amount).coerceAtLeast(0L)
        fun getEffectiveCapacity(): Int = tankCapacity.toInt().coerceIn(0, Int.MAX_VALUE)

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

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val actual = minOf(toInsert, availableSpace())
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.isBlank) variant = FluidVariant.of(ModFluids.UU_MATTER_STILL)
                tx.commit()
                sync.fluidAmount = toMilliBuckets(amount)
                markDirty()
                actual
            }
        }

        fun extractInternal(toExtract: Long): Long {
            if (toExtract <= 0L || variant.isBlank) return 0L
            val actual = minOf(toExtract, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
                sync.fluidAmount = toMilliBuckets(amount)
                markDirty()
                actual
            }
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
            return mutableListOf(object : StorageView<FluidVariant> {
                override fun getResource(): FluidVariant = tankInternal.variant
                override fun getAmount(): Long = tankInternal.amount
                override fun getCapacity(): Long = tankInternal.getTankCapacity()
                override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
                    StoragePreconditions.notBlankNotNegative(resource, maxAmount)
                    if (!isUuMatter(resource.fluid)) return 0L
                    return tankInternal.extract(FluidVariant.of(ModFluids.UU_MATTER_STILL), maxAmount, transaction)
                }
                override fun isResourceBlank(): Boolean = tankInternal.variant.isBlank
            }).iterator()
        }
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        MatterGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun setStack(slot: Int, stack: ItemStack) {
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
        return itemId == Identifier(Ic2_120.MOD_ID, "empty_cell") ||
            stack.item == Items.BUCKET ||
            (itemId == Identifier(Ic2_120.MOD_ID, "fluid_cell") && stack.isFluidCellEmpty())
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_SCRAP -> Ic2Config.current.matterGenerator.allowScrapBoost && isScrapFuel(stack)
        SLOT_CONTAINER_INPUT -> !stack.isEmpty && matterGenIsFillableContainer(stack)
        SLOT_CONTAINER_OUTPUT -> false
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem && stack.item !is OverclockerUpgrade
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
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
            syncedData,
            itemStorage
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(MatterGeneratorSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progress = nbt.getInt(NBT_PROGRESS).coerceAtLeast(0)
        scrapConsumedThisCycle = nbt.getInt(NBT_SCRAP_CONSUMED).coerceIn(0, MatterGeneratorSync.SCRAP_PER_MB)
        tankInternal.setStoredAmount(nbt.getLong(NBT_TANK_AMOUNT))
        sync.fluidCapacity = tankInternal.getEffectiveCapacity()
        sync.mode = resolveDisplayedMode()
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MatterGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.getStoredAmount())
        nbt.putInt(NBT_PROGRESS, sync.progress)
        nbt.putInt(NBT_SCRAP_CONSUMED, scrapConsumedThisCycle)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

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
        fillContainersFromTank()

        if (!RedstoneControlComponent.canRun(world, pos, this)) {
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

        val hasScrap = Ic2Config.current.matterGenerator.allowScrapBoost && isScrapFuel(getStack(SLOT_SCRAP))
        val euPerMb = if (hasScrap) MatterGeneratorSync.SCRAP_EU_PER_MB else MatterGeneratorSync.BASE_EU_PER_MB

        // Consume as much energy as available, up to 1mb worth per tick
        // 已知缺陷（故意保留）：hasScrap 仅判断槽位非空，整个 tick 的进度都按 1/6 加速费率计费。
        // 若本 tick 废料不足 SCRAP_PER_MB，consumeScrapForProgress 会静默按实际库存截断消耗，
        // 导致每个废料最多可折扣约 166_667 EU（预期约 24_510 EU），等价于"废料稀缺时单废料价值放大 ~34 倍"。
        // 能量吞吐越高（MAX_INSERT=Long.MAX_VALUE 时甚至可单 tick 跑满 1 mB 周期），该放大效应越明显。
        val energyBudget = minOf(sync.amount, euPerMb)
        val rawProgress = (energyBudget * MatterGeneratorSync.PROGRESS_MAX / euPerMb).toInt().coerceAtLeast(0)
        val progressIncrement = minOf(rawProgress, MatterGeneratorSync.PROGRESS_MAX - sync.progress)

        val energyNeeded = ceil(
            euPerMb.toDouble() * progressIncrement / MatterGeneratorSync.PROGRESS_MAX
        ).toLong().coerceAtLeast(1L)

        if (sync.amount >= energyNeeded) {
            sync.consumeEnergy(energyNeeded)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = (sync.progress + progressIncrement).coerceAtMost(MatterGeneratorSync.PROGRESS_MAX)

            // 按累计进度逐个消耗废料，完成一个 1 mB 周期正好消耗 SCRAP_PER_MB 个。
            if (hasScrap) {
                consumeScrapForProgress()
            }

            if (sync.progress >= MatterGeneratorSync.PROGRESS_MAX) {
                tankInternal.insertInternal(outputPerCycle)
                sync.progress = 0
                scrapConsumedThisCycle = 0
                markDirty()
            }

            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }

        sync.mode = resolveDisplayedMode()
        sync.syncCurrentTickFlow()
    }

    /** 消耗废料等价量，返回实际消耗量（普通废料 1 个 = 1 等价量；废料盒整盒 = SCRAP_UNITS_PER_BOX）。 */
    private fun consumeScrapUnits(units: Int): Int {
        if (units <= 0) return 0
        val scrapStack = getStack(SLOT_SCRAP)
        if (scrapStack.isEmpty) return 0

        if (isScrapBox(scrapStack)) {
            // 废料盒不可拆分：缺口不足一整盒时不消耗，顺延到后续 tick，避免浪费
            if (units < SCRAP_UNITS_PER_BOX) return 0
            // 只消耗一整盒（=9 等价量），槽内其余盒子保留，绝不能整组清空
            scrapStack.decrement(1)
            if (scrapStack.isEmpty) {
                setStack(SLOT_SCRAP, ItemStack.EMPTY)
            } else {
                markDirty()
            }
            return SCRAP_UNITS_PER_BOX
        }

        val actualCount = minOf(units, scrapStack.count)
        if (actualCount <= 0) return 0
        scrapStack.decrement(actualCount)
        if (scrapStack.isEmpty) {
            setStack(SLOT_SCRAP, ItemStack.EMPTY)
        } else {
            markDirty()
        }
        return actualCount
    }

    private fun consumeScrapForProgress() {
        val targetConsumed = (
            MatterGeneratorSync.SCRAP_PER_MB.toLong() * sync.progress / MatterGeneratorSync.PROGRESS_MAX
            ).toInt()
        val scrapToConsume = targetConsumed - scrapConsumedThisCycle
        if (scrapToConsume <= 0) return

        scrapConsumedThisCycle += consumeScrapUnits(scrapToConsume)
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

    private fun resolveDisplayedMode(): Int {
        val scrapCount = if (Ic2Config.current.matterGenerator.allowScrapBoost) getStack(SLOT_SCRAP).count else 0
        return if (scrapCount > 0) 1 else 0
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun isScrap(stack: ItemStack): Boolean =
        !stack.isEmpty && stack.item == scrapItem

    private fun isScrapBox(stack: ItemStack): Boolean =
        !stack.isEmpty && stack.item == scrapBoxItem

    private fun isScrapFuel(stack: ItemStack): Boolean = isScrap(stack) || isScrapBox(stack)

    private fun isUuMatter(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == ModFluids.UU_MATTER_STILL || fluid == ModFluids.UU_MATTER_FLOWING

    private fun toMilliBuckets(amount: Long): Int =
        amount.toInt().coerceAtLeast(0)

    private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L
}
