package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.CropmatronBlock
import ic2_120.content.crop.CropCareTarget
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.Fertilizer
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.CropmatronScreenHandler
import ic2_120.content.sync.CropmatronSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.instance
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
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
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
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

@ModBlockEntity(block = CropmatronBlock::class)
class CropmatronBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    IRoutedSidedInventory,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CropmatronBlock.ACTIVE
    override val tier: Int = CROPMATRON_TIER

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_WATER_INPUT), matcher = { isValid(SLOT_WATER_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_WEED_EX_INPUT), matcher = { isValid(SLOT_WEED_EX_INPUT, it) }),
            ItemInsertRoute(SLOT_FERTILIZER_INDICES, matcher = { isValid(SLOT_FERTILIZER_0, it) })
        ),
        extractSlots = IntArray(INVENTORY_SIZE) { it },
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = CropmatronSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CROPMATRON_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val waterTankCapacity = FluidConstants.BUCKET * CropmatronSync.WATER_TANK_CAPACITY_BUCKETS
    private val weedExTankCapacity = FluidConstants.BUCKET * CropmatronSync.WEED_EX_TANK_CAPACITY_BUCKETS

    private val waterTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = waterTankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isWater(variant.fluid) && ModFluids.isFluid(variant.fluid)
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, waterTankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = waterTankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(Fluids.WATER)
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun getStoredWater(): Long = amount
    }

    private val weedExTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = weedExTankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isWeedEx(variant.fluid) && ModFluids.isFluid(variant.fluid)
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, weedExTankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.WEED_EX_STILL) else FluidVariant.blank()
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = weedExTankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.WEED_EX_STILL)
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun getStoredWeedEx(): Long = amount
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (isWater(resource.fluid)) return waterTankInternal.insert(resource, maxAmount, transaction)
            if (isWeedEx(resource.fluid)) return weedExTankInternal.insert(resource, maxAmount, transaction)
            return 0L
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!waterTankInternal.variant.isBlank && waterTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = waterTankInternal.variant
                    override fun getAmount(): Long = waterTankInternal.amount
                    override fun getCapacity(): Long = waterTankCapacity
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!weedExTankInternal.variant.isBlank && weedExTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = weedExTankInternal.variant
                    override fun getAmount(): Long = weedExTankInternal.amount
                    override fun getCapacity(): Long = weedExTankCapacity
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    private var workOffset: Int = random.nextBetween(0, WORK_INTERVAL_TICKS - 1)

    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) }
    private val waterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "water_cell")) }
    private val distilledWaterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "distilled_water_cell")) }
    private val weedExCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "weed_ex_cell")) }

    constructor(pos: BlockPos, state: BlockState) : this(CropmatronBlockEntity::class.type(), pos, state)

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    private fun cropmatronMatchesWaterInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val fluidCellId = Identifier(Ic2_120.MOD_ID, "fluid_cell")
        val waterCellId = Identifier(Ic2_120.MOD_ID, "water_cell")
        val distilledWaterCellId = Identifier(Ic2_120.MOD_ID, "distilled_water_cell")
        return when {
            stack.item == Items.WATER_BUCKET || stack.item == ModFluids.DISTILLED_WATER_BUCKET -> true
            Registries.ITEM.getId(stack.item) == waterCellId -> true
            Registries.ITEM.getId(stack.item) == distilledWaterCellId -> true
            Registries.ITEM.getId(stack.item) == fluidCellId && stack.item is FluidCellItem -> {
                val fluid = stack.getFluidCellVariant()?.fluid
                fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                    fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
            }
            else -> false
        }
    }

    private fun cropmatronMatchesWeedExInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val fluidCellId = Identifier(Ic2_120.MOD_ID, "fluid_cell")
        val weedExCellId = Identifier(Ic2_120.MOD_ID, "weed_ex_cell")
        return when {
            stack.item == ModFluids.WEED_EX_BUCKET -> true
            Registries.ITEM.getId(stack.item) == weedExCellId -> true
            Registries.ITEM.getId(stack.item) == fluidCellId && stack.item is FluidCellItem -> {
                val fluid = stack.getFluidCellVariant()?.fluid
                fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING
            }
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == SLOT_WATER_OUTPUT || slot == SLOT_WEED_EX_OUTPUT -> false
        slot == SLOT_WATER_INPUT -> cropmatronMatchesWaterInput(stack)
        slot == SLOT_WEED_EX_INPUT -> cropmatronMatchesWeedExInput(stack)
SLOT_FERTILIZER_INDICES.contains(slot) -> stack.item is Fertilizer
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.cropmatron")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CropmatronScreenHandler(
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
        sync.amount = nbt.getLong(CropmatronSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        waterTankInternal.setStoredFluid(nbt.getLong(NBT_WATER_MB).coerceIn(0, waterTankCapacity))
        weedExTankInternal.setStoredFluid(nbt.getLong(NBT_WEED_EX_MB).coerceIn(0, weedExTankCapacity))
        workOffset = nbt.getInt(NBT_WORK_OFFSET).coerceIn(0, WORK_INTERVAL_TICKS - 1)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CropmatronSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_WATER_MB, waterTankInternal.amount)
        nbt.putLong(NBT_WEED_EX_MB, weedExTankInternal.amount)
        nbt.putInt(NBT_WORK_OFFSET, workOffset)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, waterTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        processWaterInputContainer()
        processWeedExInputContainer()

        var active = false
        val interval = (WORK_INTERVAL_TICKS.toFloat() / speedMultiplier).toInt().coerceAtLeast(1)
        if ((world.time + workOffset) % interval.toLong() == 0L) {
            val report = runScan(world)
            sync.touchedThisRun = report.touched
            sync.fertilizedThisRun = report.fertilized
            sync.hydratedThisRun = report.hydrated
            sync.weedExAppliedThisRun = report.weedExApplied
            sync.farmlandHydratedThisRun = report.farmlandHydrated
            active = report.fertilized > 0 || report.hydrated > 0 || report.weedExApplied > 0 || report.farmlandHydrated > 0
        }

        sync.waterAmount = waterTankInternal.amount.toInt().coerceAtLeast(0)
        sync.weedExAmount = weedExTankInternal.amount.toInt().coerceAtLeast(0)
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: World): ScanReport {
        val report = ScanReport()
        val basePos = this.pos
        val scanEnergyCost = (ENERGY_PER_SCAN * energyMultiplier).toLong().coerceAtLeast(1L)
        val applyEnergyCost = (ENERGY_PER_APPLY * energyMultiplier).toLong().coerceAtLeast(1L)

        for (dx in -SCAN_RADIUS..SCAN_RADIUS) {
            for (dz in -SCAN_RADIUS..SCAN_RADIUS) {
                if (sync.amount < scanEnergyCost) return report
                sync.consumeEnergy(scanEnergyCost)

                // 监管机作用在"作物层"（与机器同 Y）；耕地在作物层下方一格。
                val cropPos = basePos.add(dx, 0, dz)
                val be = world.getBlockEntity(cropPos)
                if (be is CropCareTarget) {
                    report.touched++
                    tryApplyFertilizer(be, applyEnergyCost, report)
                    tryApplyHydration(be, applyEnergyCost, report)
                    tryApplyWeedEx(be, applyEnergyCost, report)
                } else if (waterTankInternal.amount > 0 && sync.amount >= applyEnergyCost && tryHydrateFarmland(world, cropPos.down())) {
                    if (sync.consumeEnergy(applyEnergyCost) > 0L) {
                        report.farmlandHydrated++
                    }
                }
            }
        }

        return report
    }

    private fun tryApplyFertilizer(target: CropCareTarget, applyEnergyCost: Long, report: ScanReport) {
        if (sync.amount < applyEnergyCost) return
        if (!hasFertilizer()) return
        if (target.applyFertilizer(1, simulate = true) <= 0) return

        if (!consumeOneFertilizer()) return
        if (sync.consumeEnergy(applyEnergyCost) <= 0L) {
            refundOneFertilizer()
            return
        }

        val used = target.applyFertilizer(1, simulate = false)
        if (used > 0) {
            report.fertilized += used
        } else {
            refundOneFertilizer()
        }
    }

    private fun tryApplyHydration(target: CropCareTarget, applyEnergyCost: Long, report: ScanReport) {
        if (sync.amount < applyEnergyCost) return
        val waterDroplets = waterTankInternal.getStoredWater().toInt()
        if (waterDroplets <= 0) return

        val request = waterDroplets.coerceAtMost(200)
        val simulated = target.applyHydration(request, simulate = true)
        if (simulated <= 0) return

        if (sync.consumeEnergy(applyEnergyCost) <= 0L) return
        val used = target.applyHydration(request, simulate = false).coerceAtMost(waterDroplets)
        if (used <= 0) return
        waterTankInternal.consumeInternal(mbToDroplets(used))
        report.hydrated += used
    }

    private fun tryApplyWeedEx(target: CropCareTarget, applyEnergyCost: Long, report: ScanReport) {
        if (sync.amount < applyEnergyCost) return
        val weedExDroplets = weedExTankInternal.getStoredWeedEx().toInt()
        if (weedExDroplets <= 0) return

        val request = weedExDroplets.coerceAtMost(200)
        val simulated = target.applyWeedEx(request, simulate = true)
        if (simulated <= 0) return

        if (sync.consumeEnergy(applyEnergyCost) <= 0L) return
        val used = target.applyWeedEx(request, simulate = false).coerceAtMost(weedExDroplets)
        if (used <= 0) return
        weedExTankInternal.consumeInternal(mbToDroplets(used))
        report.weedExApplied += used
    }

    private fun tryHydrateFarmland(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        if (state.block !is FarmlandBlock) return false
        val moisture = state.get(FarmlandBlock.MOISTURE)
        if (moisture >= 7) return false

        val waterMb = waterTankInternal.getStoredWater().toInt()
        val add = minOf(7 - moisture, waterMb)
        if (add <= 0) return false

        waterTankInternal.consumeInternal(mbToDroplets(add))
        world.setBlockState(pos, state.with(FarmlandBlock.MOISTURE, moisture + add), 2)
        return true
    }

    private fun processWaterInputContainer() {
        if (waterTankInternal.amount > waterTankCapacity - FluidConstants.BUCKET) return

        val input = getStack(SLOT_WATER_INPUT)
        if (input.isEmpty) return

        val emptyRemainder = when {
            input.item == Items.WATER_BUCKET || input.item == ModFluids.DISTILLED_WATER_BUCKET -> ItemStack(Items.BUCKET)
            input.item == waterCellItem || input.item == distilledWaterCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.item is FluidCellItem -> {
                val fluid = input.getFluidCellVariant()?.fluid
                if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                    fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
                ) ItemStack(emptyCellItem) else ItemStack.EMPTY
            }
            else -> ItemStack.EMPTY
        }
        if (emptyRemainder.isEmpty) return

        val out = getStack(SLOT_WATER_OUTPUT)
        if (!canMergeIntoSlot(out, emptyRemainder)) return

        waterTankInternal.insertInternal(FluidConstants.BUCKET)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WATER_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WATER_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun processWeedExInputContainer() {
        if (weedExTankInternal.amount > weedExTankCapacity - FluidConstants.BUCKET) return

        val input = getStack(SLOT_WEED_EX_INPUT)
        if (input.isEmpty) return

        val emptyRemainder = when {
            input.item == ModFluids.WEED_EX_BUCKET -> ItemStack(Items.BUCKET)
            input.item == weedExCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.item is FluidCellItem -> {
                val fluid = input.getFluidCellVariant()?.fluid
                if (fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING) ItemStack(emptyCellItem)
                else ItemStack.EMPTY
            }
            else -> ItemStack.EMPTY
        }
        if (emptyRemainder.isEmpty) return

        val out = getStack(SLOT_WEED_EX_OUTPUT)
        if (!canMergeIntoSlot(out, emptyRemainder)) return

        weedExTankInternal.insertInternal(FluidConstants.BUCKET)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WEED_EX_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WEED_EX_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun hasFertilizer(): Boolean {
        for (slot in SLOT_FERTILIZER_INDICES) {
            val stack = getStack(slot)
            if (!stack.isEmpty && stack.item == Fertilizer::class.instance()) return true
        }
        return false
    }

    private fun consumeOneFertilizer(): Boolean {
        for (slot in SLOT_FERTILIZER_INDICES) {
            val stack = getStack(slot)
            if (!stack.isEmpty && stack.item == Fertilizer::class.instance()) {
                stack.decrement(1)
                if (stack.isEmpty) setStack(slot, ItemStack.EMPTY)
                markDirty()
                return true
            }
        }
        return false
    }

    private fun refundOneFertilizer() {
        val fertilizer = ItemStack(Fertilizer::class.instance())
        for (slot in SLOT_FERTILIZER_INDICES) {
            val stack = getStack(slot)
            if (stack.isEmpty) {
                setStack(slot, fertilizer)
                return
            }
            if (ItemStack.canCombine(stack, fertilizer) && stack.count < stack.maxCount) {
                stack.increment(1)
                markDirty()
                return
            }
        }
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return ioStorage
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH

    private fun isWater(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
            fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING

    private fun isWeedEx(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING

    private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L

    data class ScanReport(
        var touched: Int = 0,
        var fertilized: Int = 0,
        var hydrated: Int = 0,
        var weedExApplied: Int = 0,
        var farmlandHydrated: Int = 0
    )

    companion object {
        const val CROPMATRON_TIER = 1

        const val SLOT_WATER_INPUT = 0
        const val SLOT_WATER_OUTPUT = 1
        const val SLOT_WEED_EX_INPUT = 2
        const val SLOT_WEED_EX_OUTPUT = 3

        const val SLOT_FERTILIZER_0 = 4
        const val SLOT_FERTILIZER_1 = 5
        const val SLOT_FERTILIZER_2 = 6
        const val SLOT_FERTILIZER_3 = 7
        const val SLOT_FERTILIZER_4 = 8
        const val SLOT_FERTILIZER_5 = 9
        const val SLOT_FERTILIZER_6 = 10

        const val SLOT_UPGRADE_0 = 11
        const val SLOT_UPGRADE_1 = 12
        const val SLOT_UPGRADE_2 = 13
        const val SLOT_UPGRADE_3 = 14

        val SLOT_FERTILIZER_INDICES = intArrayOf(
            SLOT_FERTILIZER_0,
            SLOT_FERTILIZER_1,
            SLOT_FERTILIZER_2,
            SLOT_FERTILIZER_3,
            SLOT_FERTILIZER_4,
            SLOT_FERTILIZER_5,
            SLOT_FERTILIZER_6
        )
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val INVENTORY_SIZE = 15

        private const val WORK_INTERVAL_TICKS = 10
        private const val SCAN_RADIUS = 4
        private const val ENERGY_PER_SCAN = 1
        private const val ENERGY_PER_APPLY = 10

        private const val NBT_WATER_MB = "WaterMb"
        private const val NBT_WEED_EX_MB = "WeedExMb"
        private const val NBT_WORK_OFFSET = "WorkOffset"

        private val random: Random = Random.create()

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = CropmatronBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }
}
