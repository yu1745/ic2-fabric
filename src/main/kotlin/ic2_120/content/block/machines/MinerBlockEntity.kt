package ic2_120.content.block.machines

import ic2_120.content.block.AdvancedMinerBlock
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MinerBlock
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.AdvancedScannerItem
import ic2_120.content.item.DiamondDrill
import ic2_120.content.item.Drill
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.ScannerType
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.MinerScreenHandler
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.sync.MinerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.config.Ic2Config
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.enchantment.Enchantments
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
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import kotlin.math.floor
import kotlin.random.Random

abstract class BaseMinerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    private val blockKey: String,
    private val baseTier: Int,
    private val acceptsAdvancedScanner: Boolean
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine,
    IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty = BaseMinerBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.miner.operate",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    override fun getInventory(): Inventory = this

    override val tier: Int = baseTier
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
        const val SLOT_SCANNER = 0
        const val SLOT_DRILL = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_FILTER_START = 3
        const val FILTER_SLOT_COUNT = 15
        const val SLOT_FILTER_END = SLOT_FILTER_START + FILTER_SLOT_COUNT - 1
        const val SLOT_UPGRADE_0 = SLOT_FILTER_END + 1
        const val SLOT_UPGRADE_1 = SLOT_FILTER_END + 2
        const val SLOT_UPGRADE_2 = SLOT_FILTER_END + 3
        const val SLOT_UPGRADE_3 = SLOT_FILTER_END + 4
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val SLOT_OUTPUT_0 = SLOT_UPGRADE_3 + 1
        const val SLOT_OUTPUT_1 = SLOT_UPGRADE_3 + 2
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1)
        const val INVENTORY_SIZE = SLOT_OUTPUT_1 + 1

        private const val NBT_WORK_OFFSET = "WorkOffset"
        private const val NBT_CURSOR_INITIALIZED = "CursorInitialized"
        private const val NBT_ENERGY_STORED = "EnergyStored"
        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_TANK_FLUID = "TankFluid"
        private const val NBT_PENDING_BREAK_ENERGY = "PendingBreakEnergy"
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
            ItemInsertRoute(intArrayOf(SLOT_SCANNER), matcher = { isValid(SLOT_SCANNER, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_DRILL), matcher = { isValid(SLOT_DRILL, it) }, maxPerSlot = 1),
            ItemInsertRoute((SLOT_FILTER_START..SLOT_FILTER_END).toList().toIntArray(), matcher = { !it.isEmpty })
        ),
        extractSlots = IntArray(INVENTORY_SIZE) { it },
        markDirty = { markDirty() }
    )
    private val workOffset = Random.nextInt(20)
    private var cursorInitialized = false
    private var pendingBreakEnergy: Long = 0L

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = MinerSync(
        syncedData,
        { world?.time },
        { getCurrentBaseCapacity() + capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(baseTier + voltageTierBonus) }
    )

    private val discharger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { baseTier },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    private val scannerCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = SLOT_SCANNER,
        machineTierProvider = { baseTier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { req -> sync.consumeEnergy(req) },
        canChargeNow = { sync.amount > 0L }
    )

    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = FluidConstants.BUCKET
        override fun canInsert(variant: FluidVariant): Boolean = !variant.isBlank
        override fun canExtract(variant: FluidVariant): Boolean = !this.variant.isBlank && this.variant == variant

        override fun onFinalCommit() {
            markDirty()
        }

        fun setStored(fluidId: String, stored: Long) {
            amount = stored.coerceIn(0L, FluidConstants.BUCKET)
            variant = if (amount > 0L) {
                val id = net.minecraft.util.Identifier.tryParse(fluidId)
                if (id != null && Registries.FLUID.containsId(id)) FluidVariant.of(Registries.FLUID.get(id)) else FluidVariant.blank()
            } else {
                FluidVariant.blank()
            }
            if (variant.isBlank) amount = 0L
        }
    }
    val tank: Storage<FluidVariant> = tankInternal

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

    override fun isValid(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            SLOT_SCANNER -> when {
                stack.item is OdScannerItem -> true
                stack.item is AdvancedScannerItem -> acceptsAdvancedScanner
                else -> false
            }
            SLOT_DRILL -> stack.item is Drill || stack.item is DiamondDrill || stack.item is IridiumDrill
            SLOT_DISCHARGING -> stack.item is IBatteryItem
            in SLOT_FILTER_START..SLOT_FILTER_END -> true
            in SLOT_UPGRADE_INDICES -> stack.item is IUpgradeItem
            in SLOT_OUTPUT_INDICES -> false
            else -> false
        }
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeBoolean(acceptsAdvancedScanner)
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.$blockKey")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MinerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        cursorInitialized = nbt.getBoolean(NBT_CURSOR_INITIALIZED)
        tankInternal.setStored(nbt.getString(NBT_TANK_FLUID), nbt.getLong(NBT_TANK_AMOUNT))
        pendingBreakEnergy = nbt.getLong(NBT_PENDING_BREAK_ENERGY)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_WORK_OFFSET, workOffset)
        nbt.putBoolean(NBT_CURSOR_INITIALIZED, cursorInitialized)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.amount)
        nbt.putString(NBT_TANK_FLUID, if (tankInternal.variant.isBlank) "" else Registries.FLUID.getId(tankInternal.variant.fluid).toString())
        nbt.putLong(NBT_PENDING_BREAK_ENERGY, pendingBreakEnergy)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        chargeScanner()

        if (fluidPipeProviderEnabled) {
            ejectFluidToNeighbors(world, pos, state)
        }

        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        if (sync.running == 0) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val scannerType = getScannerType(getStack(SLOT_SCANNER))
        if (scannerType == null) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val scanCost = (MinerSync.SCAN_ENERGY_PER_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.amount < scanCost) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val effectivePeriod = getEffectivePeriodTicks()
        if (((world.time + workOffset) % effectivePeriod) != 0L) {
            // 周期间隔中的等待态仍视为“正在工作”。
            setActiveState(world, pos, state, true)
            sync.syncCurrentTickFlow()
            return
        }

        var active = false
        var scannedThisCycle = 0
        var minedThisCycle = false
        var pumpedThisCycle = false

        // 有待挖掘的矿石：每 tick 将充入的能量转入待挖掘池，攒够了再挖
        if (pendingBreakEnergy > 0L) {
            val drillBreakCost = getDrillBreakCost()
            if (drillBreakCost == null) {
                pendingBreakEnergy = 0L
            } else {
                val silkMultiplier = if (sync.silkTouch != 0 && acceptsAdvancedScanner) MinerSync.SILK_TOUCH_MULTIPLIER else 1L
                val breakEnergy = (drillBreakCost * silkMultiplier * energyMultiplier).toLong().coerceAtLeast(1L)

                val toReserve = minOf(sync.amount, breakEnergy - pendingBreakEnergy)
                if (toReserve > 0L) {
                    sync.consumeEnergy(toReserve)
                    pendingBreakEnergy += toReserve
                }

                if (pendingBreakEnergy >= breakEnergy) {
                    val targetPos = pos.add(sync.cursorX, sync.cursorY - pos.y, sync.cursorZ)
                    val blockState = world.getBlockState(targetPos)
                    if (!blockState.isAir && shouldMine(world, targetPos, blockState) && canMineWithCurrentDrill(blockState)) {
                        pendingBreakEnergy = 0L
                        advanceCursor(scannerType.scanRadius)
                        mineBlock(world as ServerWorld, targetPos, blockState)
                        active = true
                        minedThisCycle = true
                    } else {
                        pendingBreakEnergy = 0L
                    }
                } else {
                    active = true
                }
            }
            sync.energy = sync.amount.toInt().coerceAtLeast(0)
            setActiveState(world, pos, state, active)
            sync.syncCurrentTickFlow()
            return
        }

        // 每个工作周期可连续扫描多个格子，但最多只执行一次有效挖掘。
        while (sync.running != 0) {
            val targetPos = ensureAndGetCursorTarget(scannerType.scanRadius)

            if (sync.running == 0 || targetPos.y < world.bottomY || targetPos.y < -64) {
                sync.running = 0
                break
            }

            val blockState = world.getBlockState(targetPos)

            // 先检查是否可挖掘（不消耗扫描能量）
            if (shouldMine(world, targetPos, blockState) && canMineWithCurrentDrill(blockState)) {
                val breakCost = getDrillBreakCost() ?: 0L
                if (breakCost <= 0L) break

                val silkMultiplier = if (sync.silkTouch != 0 && acceptsAdvancedScanner) MinerSync.SILK_TOUCH_MULTIPLIER else 1L
                val breakEnergy = (breakCost * silkMultiplier * energyMultiplier).toLong().coerceAtLeast(1L)

                if (sync.consumeEnergy(breakEnergy) > 0L) {
                    advanceCursor(scannerType.scanRadius)
                    mineBlock(world as ServerWorld, targetPos, blockState)
                    active = true
                    minedThisCycle = true
                } else {
                    // 能量不足，将现有能量转入待挖掘池，下一 tick 继续攒
                    pendingBreakEnergy = sync.amount
                    if (pendingBreakEnergy > 0L) sync.consumeEnergy(pendingBreakEnergy)
                }
                break
            }

            // 非矿石方块，消耗扫描能量并前进
            if (sync.consumeEnergy(scanCost) <= 0L) break

            if (tryAutoPumpFluid(world, pos, state, targetPos)) {
                advanceCursor(scannerType.scanRadius)
                active = true
                pumpedThisCycle = true
                break
            }

            advanceCursor(scannerType.scanRadius)
            scannedThisCycle++
        }

        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        // 只要本周期执行了扫描，即视为工作中（不仅限于挖掘/抽液瞬间）。
        setActiveState(world, pos, state, active || scannedThisCycle > 0)
        sync.syncCurrentTickFlow()
    }

    fun toggleMode() {
        sync.mode = if (sync.mode == 0) 1 else 0
        markDirty()
    }

    fun toggleSilkTouch() {
        sync.silkTouch = if (sync.silkTouch == 0) 1 else 0
        markDirty()
    }

    fun restartScan() {
        sync.running = 1
        cursorInitialized = false
        markDirty()
    }

    private fun getCurrentBaseCapacity(): Long {
        val breakCost = getDrillBreakCost() ?: 0L
        val silkMultiplier = if (sync.silkTouch != 0) MinerSync.SILK_TOUCH_MULTIPLIER else 1L
        return MinerSync.SCAN_ENERGY_PER_STEP + breakCost * silkMultiplier
    }

    private fun getDrillBreakCost(): Long? {
        val drill = getStack(SLOT_DRILL).item
        return when (drill) {
            is Drill -> MinerSync.DRILL_ENERGY_PER_BREAK
            is DiamondDrill, is IridiumDrill -> MinerSync.DIAMOND_OR_IRIDIUM_ENERGY_PER_BREAK
            else -> null
        }
    }

    private fun getScannerType(stack: ItemStack): ScannerType? {
        val item = stack.item
        return when {
            item is OdScannerItem -> ScannerType.OD
            acceptsAdvancedScanner && item is AdvancedScannerItem -> ScannerType.OV
            else -> null
        }
    }

    private fun ensureAndGetCursorTarget(range: Int): BlockPos {
        if (!cursorInitialized) {
            sync.cursorX = -range
            sync.cursorZ = -range
            sync.cursorY = pos.y - 1
            cursorInitialized = true
        }
        return pos.add(sync.cursorX, sync.cursorY - pos.y, sync.cursorZ)
    }

    private fun advanceCursor(range: Int) {
        if (sync.cursorZ < range) {
            sync.cursorZ += 1
            return
        }
        sync.cursorZ = -range
        if (sync.cursorX < range) {
            sync.cursorX += 1
            return
        }
        sync.cursorX = -range
        sync.cursorY -= 1
        if (sync.cursorY < -64) {
            sync.running = 0
        }
    }

    private fun shouldMine(world: World, targetPos: BlockPos, state: BlockState): Boolean {
        if (state.isAir) return false
        if (state.getHardness(world, targetPos) < 0f) return false

        val id = Registries.BLOCK.getId(state.block)
        val idString = id.toString()
        val oreLike = id.path.contains("ore") || id.path == "ancient_debris"
            || idString in Ic2Config.current.miner.additionalMineableBlocks
        if (!oreLike) return false

        if (!acceptsAdvancedScanner) return true  // 普通采矿机无过滤，挖所有矿石

        val filters = (SLOT_FILTER_START..SLOT_FILTER_END)
            .map { getStack(it) }
            .filter { !it.isEmpty }
            .map { it.item }
            .toSet()
        if (filters.isEmpty()) return true

        val blockItem = state.block.asItem()
        val matched = blockItem != Items.AIR && filters.contains(blockItem)
        return if (sync.mode == 0) matched else !matched
    }

    private fun mineBlock(world: ServerWorld, targetPos: BlockPos, state: BlockState) {
        val tool = getLootToolStack()
        val drops = net.minecraft.block.Block.getDroppedStacks(
            state,
            world,
            targetPos,
            world.getBlockEntity(targetPos),
            null,
            tool
        )
        world.setBlockState(targetPos, net.minecraft.block.Blocks.AIR.defaultState, 3)

        for (drop in drops) {
            insertIntoOutputBuffer(drop)
        }

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)

        // 本轮无法弹出的输出槽产物在机器位置掉落
        for (slot in SLOT_OUTPUT_INDICES) {
            val remaining = getStack(slot)
            if (!remaining.isEmpty) {
                ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), remaining.copy())
                setStack(slot, ItemStack.EMPTY)
            }
        }

        markDirty()
    }

    private fun getLootToolStack(): ItemStack {
        val drillItem = getStack(SLOT_DRILL).item
        val baseTool = when (drillItem) {
            is Drill -> ItemStack(Items.IRON_PICKAXE)
            is DiamondDrill, is IridiumDrill -> ItemStack(Items.DIAMOND_PICKAXE)
            else -> ItemStack(Items.IRON_PICKAXE)
        }
        if (sync.silkTouch != 0 && acceptsAdvancedScanner) {
            baseTool.addEnchantment(Enchantments.SILK_TOUCH, 1)
        }
        return baseTool
    }

    private fun canMineWithCurrentDrill(state: BlockState): Boolean {
        val tool = getLootToolStack()
        return state.isToolRequired.not() || tool.isSuitableFor(state)
    }

    private fun insertIntoOutputBuffer(stack: ItemStack) {
        var remaining = stack.copy()
        for (slot in SLOT_OUTPUT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getStack(slot)
            if (existing.isEmpty) {
                val toInsert = minOf(remaining.count, maxCountPerStack)
                val inserted = remaining.copy()
                inserted.count = toInsert
                setStack(slot, inserted)
                remaining.decrement(toInsert)
            } else if (ItemStack.canCombine(existing, remaining)) {
                val room = minOf(existing.maxCount, maxCountPerStack) - existing.count
                if (room > 0) {
                    val move = minOf(room, remaining.count)
                    existing.increment(move)
                    remaining.decrement(move)
                }
            }
        }
        if (!remaining.isEmpty) {
            ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), remaining)
        }
    }

    private fun tryAutoPumpFluid(world: World, pos: BlockPos, state: BlockState, targetPos: BlockPos): Boolean {
        if (!fluidPipeProviderEnabled) return false
        if (tankInternal.amount > 0L) return false
        if (world.time % 20L != 0L) return false

        val fs = world.getFluidState(targetPos)
        if (fs.isEmpty || !fs.isStill) return false
        val variant = FluidVariant.of(fs.fluid)
        Transaction.openOuter().use { tx ->
            val inserted = tankInternal.insert(variant, FluidConstants.BUCKET, tx)
            if (inserted < FluidConstants.BUCKET) return false
            tx.commit()
        }
        world.setBlockState(targetPos, net.minecraft.block.Blocks.AIR.defaultState, 3)
        ejectFluidToNeighbors(world, pos, state)
        return true
    }

    private fun ejectFluidToNeighbors(world: World, pos: BlockPos, state: BlockState) {
        if (tankInternal.amount <= 0L || tankInternal.variant.isBlank) return
        val front = state.get(Properties.HORIZONTAL_FACING)
        val configuredSide = fluidPipeProviderSide
        val dirs = if (configuredSide != null) listOf(configuredSide) else Direction.values().toList()

        for (dir in dirs) {
            if (dir == front) continue
            val neighbor = FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
            val resource = tankInternal.variant
            val accepted = Transaction.openOuter().use { tx ->
                val extracted = tankInternal.extract(resource, FluidConstants.BUCKET, tx)
                if (extracted <= 0L) return@use 0L
                val inserted = neighbor.insert(resource, extracted, tx)
                if (inserted <= 0L) return@use 0L
                tx.commit()
                inserted
            }
            if (accepted > 0L) {
                break
            }
        }
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = discharger.tick(request)
        if (extracted <= 0L) return

        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        markDirty()
    }

    private fun chargeScanner() {
        val stack = getStack(SLOT_SCANNER)
        if (stack.isEmpty || stack.item !is IElectricTool) return
        val charged = scannerCharger.tick()
        if (charged > 0L) {
            sync.energy = sync.amount.toInt().coerceAtLeast(0)
            markDirty()
        }
    }

    private fun getEffectivePeriodTicks(): Long =
        floor(20.0 / speedMultiplier.coerceAtLeast(1f).toDouble()).toLong().coerceAtLeast(1L)

    protected fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val front = world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        return if (side == front) null else tank
    }
}

@ModBlockEntity(block = MinerBlock::class)
class MinerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BaseMinerBlockEntity(type, pos, state, "miner", 2, false) {

    constructor(pos: BlockPos, state: BlockState) : this(MinerBlockEntity::class.type(), pos, state)

    companion object {
        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = MinerBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }
}

@ModBlockEntity(block = AdvancedMinerBlock::class)
class AdvancedMinerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BaseMinerBlockEntity(type, pos, state, "advanced_miner", 3, true) {

    constructor(pos: BlockPos, state: BlockState) : this(AdvancedMinerBlockEntity::class.type(), pos, state)

    companion object {
        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = AdvancedMinerBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }
}
