package ic2_120.content.block.machines

import ic2_120.content.block.AdvancedMinerBlock
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MinerBlock
import ic2_120.content.block.MiningPipeBlock
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
import net.minecraft.block.Block
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.registry.item
import ic2_120.registry.instance
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.enchantment.Enchantments
import net.minecraft.registry.RegistryKeys
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
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
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign
import kotlin.random.Random
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

abstract class BaseMinerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    private val blockKey: String,
    private val baseTier: Int,
    private val acceptsAdvancedScanner: Boolean
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine,
    IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

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
    private val logger = LoggerFactory.getLogger("ic2_120/MinerCursor")

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
        const val SLOT_PIPE = SLOT_OUTPUT_1 + 1
        const val INVENTORY_SIZE = SLOT_PIPE + 1
        const val PIPE_SLOT_MAX_COUNT = 1024

        const val MAX_PIPES_PER_SECOND = 4
        const val PIPE_PLACE_INTERVAL = 20 / MAX_PIPES_PER_SECOND  // = 5 ticks
        const val DEFAULT_PIPE_ENERGY = 64L

        private const val NBT_CURSOR_INITIALIZED = "CursorInitialized"
        private const val NBT_ENERGY_STORED = "EnergyStored"
        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_TANK_FLUID = "TankFluid"
        private const val NBT_PENDING_BREAK_ENERGY = "PendingBreakEnergy"
        private const val NBT_PIPE_DEPTH = "PipeDepth"
        private const val NBT_RENDER_TARGET_X = "RenderTargetX"
        private const val NBT_RENDER_TARGET_Y = "RenderTargetY"
        private const val NBT_RENDER_TARGET_Z = "RenderTargetZ"
        private const val NBT_RENDER_TARGET_TIME = "RenderTargetTime"
        private const val NBT_MANUAL_STOPPED_FOR_RECOVERY = "ManualStoppedForRecovery"
        private const val RENDER_TARGET_TTL_TICKS = 8L
        private const val MAX_PATH_SEARCH_NODES = 8192
        private const val STALL_TICKS_LOG_THRESHOLD = 40
        private const val STALL_LOG_INTERVAL_TICKS = 20L
        private const val MAX_SAME_TARGET_PATH_FAILS = 40
        private const val MAX_RECOVERY_SEARCH_NODES = 16384
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
            ItemInsertRoute((SLOT_FILTER_START..SLOT_FILTER_END).toList().toIntArray(), matcher = { !it.isEmpty }),
            ItemInsertRoute(intArrayOf(SLOT_PIPE), matcher = { isValid(SLOT_PIPE, it) }, maxPerSlot = PIPE_SLOT_MAX_COUNT)
        ),
        extractSlots = IntArray(INVENTORY_SIZE) { it },
        markDirty = { markDirty() }
    )
    private val workOffset = Random.nextInt(20)
    private var cursorInitialized = false
    private var pendingBreakEnergy: Long = 0L
    private var lastPlacedPipeY: Int = pos.y
    private var pipePlacementBudget: Int = 1
    private var lastBudgetRefreshTick: Long = 0L
    private var renderTargetX: Int = pos.x
    private var renderTargetY: Int = pos.y
    private var renderTargetZ: Int = pos.z
    private var renderTargetTime: Long = -1L
    private var lastObservedCursorX: Int = Int.MIN_VALUE
    private var lastObservedCursorY: Int = Int.MIN_VALUE
    private var lastObservedCursorZ: Int = Int.MIN_VALUE
    private var stallTicks: Int = 0
    private var lastStallLogTick: Long = -1L
    private var lastDecisionLogTick: Long = -1L
    private var lastPathFailTargetX: Int = Int.MIN_VALUE
    private var lastPathFailTargetY: Int = Int.MIN_VALUE
    private var lastPathFailTargetZ: Int = Int.MIN_VALUE
    private var sameTargetPathFailCount: Int = 0
    private var recoveringPipes: Boolean = false
    private var manualStoppedForRecovery: Boolean = false
    private val pendingPipeRecovery = ArrayDeque<BlockPos>()
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

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

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
        val slotMax = when (slot) {
            SLOT_SCANNER, SLOT_DRILL, SLOT_DISCHARGING -> 1
            SLOT_PIPE -> PIPE_SLOT_MAX_COUNT
            else -> maxCountPerStack
        }
        if (stack.count > slotMax) stack.count = slotMax
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
            in SLOT_FILTER_START..SLOT_FILTER_END -> stack.item !== MiningPipeBlock::class.item()
            in SLOT_UPGRADE_INDICES -> stack.item is IUpgradeItem
            in SLOT_OUTPUT_INDICES -> false
            SLOT_PIPE -> stack.item === MiningPipeBlock::class.item()
            else -> false
        }
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeBoolean(acceptsAdvancedScanner)
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.$blockKey")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MinerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        cursorInitialized = nbt.getBoolean(NBT_CURSOR_INITIALIZED)
        tankInternal.setStored(nbt.getString(NBT_TANK_FLUID), nbt.getLong(NBT_TANK_AMOUNT))
        pendingBreakEnergy = nbt.getLong(NBT_PENDING_BREAK_ENERGY)
        manualStoppedForRecovery = nbt.getBoolean(NBT_MANUAL_STOPPED_FOR_RECOVERY)
        lastPlacedPipeY = if (nbt.contains(NBT_PIPE_DEPTH)) nbt.getInt(NBT_PIPE_DEPTH) else pos.y
        if (nbt.contains(NBT_RENDER_TARGET_TIME)) {
            renderTargetX = nbt.getInt(NBT_RENDER_TARGET_X)
            renderTargetY = nbt.getInt(NBT_RENDER_TARGET_Y)
            renderTargetZ = nbt.getInt(NBT_RENDER_TARGET_Z)
            renderTargetTime = nbt.getLong(NBT_RENDER_TARGET_TIME)
        } else {
            renderTargetTime = -1L
        }
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_ENERGY_STORED, sync.amount)
        nbt.putBoolean(NBT_CURSOR_INITIALIZED, cursorInitialized)
        nbt.putInt(NBT_PIPE_DEPTH, lastPlacedPipeY)
        nbt.putLong(NBT_TANK_AMOUNT, tankInternal.amount)
        nbt.putString(NBT_TANK_FLUID, if (tankInternal.variant.isBlank) "" else Registries.FLUID.getId(tankInternal.variant.fluid).toString())
        nbt.putLong(NBT_PENDING_BREAK_ENERGY, pendingBreakEnergy)
        nbt.putBoolean(NBT_MANUAL_STOPPED_FOR_RECOVERY, manualStoppedForRecovery)
        if (renderTargetTime >= 0L) {
            nbt.putInt(NBT_RENDER_TARGET_X, renderTargetX)
            nbt.putInt(NBT_RENDER_TARGET_Y, renderTargetY)
            nbt.putInt(NBT_RENDER_TARGET_Z, renderTargetZ)
            nbt.putLong(NBT_RENDER_TARGET_TIME, renderTargetTime)
        }
    }

    override fun toInitialChunkDataNbt(lookup: RegistryWrapper.WrapperLookup): NbtCompound = createNbt(lookup)

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

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

        val serverWorld = world as? ServerWorld ?: return
        if (recoveringPipes) {
            val recovered = processPipeRecoveryTick(serverWorld)
            setActiveState(world, pos, state, recovered)
            sync.syncCurrentTickFlow()
            return
        }

        tryAutoResumeAfterPipeRefill()
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

        observeCursorStall(world, "pre_tick")

        // 先确保游标初始化，再进行任何依赖 cursorY 的补管逻辑。
        if (!cursorInitialized) {
            ensureAndGetCursorTarget(scannerType.scanRadius)
        }

        // 垂直管道柱延伸（不受 effective period 限制，每 tick 运行）
        if (cursorInitialized && sync.cursorY < pos.y) {
            val done = ensurePipeColumnReaches(sync.cursorY)
            if (!done || sync.running == 0) {
                setActiveState(world, pos, state, sync.running != 0)
                sync.syncCurrentTickFlow()
                return
            }
        }

        val scanCost = (MinerSync.SCAN_ENERGY_PER_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.amount < scanCost) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val effectivePeriod = getEffectivePeriodTicks()
        if (((world.time + workOffset) % effectivePeriod) != 0L) {
            // 周期间隔中的等待态仍视为”正在工作”。
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
                        // 确保管道仍然到位（可能在等待能量期间被破坏）
                        ensurePipeReachesPosition(targetPos)
                        if (sync.running != 0 && isPipeAdjacentTo(targetPos)) {
                            pendingBreakEnergy = 0L
                            advanceCursor(scannerType.scanRadius)
                            mineBlock(world as ServerWorld, targetPos, blockState)
                            active = true
                            minedThisCycle = true
                        } else {
                            pendingBreakEnergy = 0L
                        }
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
                // 确保管道延伸到目标方块位置（每 tick 最多铺 MAX_PIPES_PER_TICK 格）
                when (ensurePipeReachesPosition(targetPos)) {
                    PipeReachResult.REACHED -> {
                    }
                    PipeReachResult.RETRY_LATER -> {
                        logDecision(world, "pipe_path_not_ready", targetPos)
                        if (sync.running == 0) break
                        break  // 管道铺设已达本 tick 上限或能量不足，下一 tick 继续
                    }
                    PipeReachResult.UNREACHABLE -> {
                        logDecision(world, "pipe_path_unreachable", targetPos)
                        if (recordPathFailAndShouldSkip(targetPos)) {
                            advanceCursor(scannerType.scanRadius)
                        }
                        if (sync.running == 0) break
                        break
                    }
                }

                if (!isPipeAdjacentTo(targetPos)) {
                    // 路径被非空气方块阻挡，无法铺设管道，跳过此方块
                    logDecision(world, "pipe_not_adjacent_after_path", targetPos)
                    advanceCursor(scannerType.scanRadius)
                    break
                }

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
            if (sync.consumeEnergy(scanCost) <= 0L) {
                logDecision(world, "insufficient_scan_energy", targetPos)
                break
            }

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
        manualStoppedForRecovery = false
        recoveringPipes = false
        pendingPipeRecovery.clear()
        // 强制重置到扫描起点，避免沿用异常/污染的游标坐标导致“重启无效”。
        sync.cursorX = 0
        sync.cursorZ = 0
        sync.cursorY = pos.y - 1
        cursorInitialized = false
        pendingBreakEnergy = 0L
        resetPathFailState()
        markDirty()
    }

    fun startPipeRecovery() {
        val serverWorld = world as? ServerWorld ?: return
        // 手动回收属于停机维护动作，避免边回收边重新铺管。
        sync.running = 0
        manualStoppedForRecovery = true
        pendingBreakEnergy = 0L
        buildPipeRecoveryQueue(serverWorld)
        recoveringPipes = pendingPipeRecovery.isNotEmpty()
        markDirty()
    }

    /**
     * 缺管停机后，若输入条件恢复（扫描器/钻头/采矿管齐全）则自动继续。
     * 仅在游标仍处于有效扫描区间时恢复，避免越界完成态被误拉起。
     */
    private fun tryAutoResumeAfterPipeRefill() {
        if (sync.running != 0) return
        if (manualStoppedForRecovery) return
        if (!cursorInitialized) return
        if (sync.cursorY < -64) return
        if (getScannerType(getStack(SLOT_SCANNER)) == null) return
        if (getDrillBreakCost() == null) return
        if (findPipeInInventory() == null) return
        // 自动恢复按“新一轮”开始，避免沿用缺料前的待挖能量残留。
        pendingBreakEnergy = 0L
        sync.running = 1
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
        sanitizeCursor(range)
        return pos.add(sync.cursorX, sync.cursorY - pos.y, sync.cursorZ)
    }

    private fun sanitizeCursor(range: Int) {
        var corrected = false
        if (sync.cursorX < -range || sync.cursorX > range) {
            sync.cursorX = -range
            corrected = true
        }
        if (sync.cursorZ < -range || sync.cursorZ > range) {
            sync.cursorZ = -range
            corrected = true
        }
        if (sync.cursorY > pos.y - 1) {
            sync.cursorY = pos.y - 1
            corrected = true
        }
        if (sync.cursorY < -64) {
            sync.cursorY = -64
            corrected = true
        }
        if (corrected) {
            logger.warn(
                "[{}] cursor corrected to ({}, {}, {}) range={}",
                blockKey,
                sync.cursorX,
                sync.cursorY,
                sync.cursorZ,
                range
            )
        }
    }

    private fun recordPathFailAndShouldSkip(targetPos: BlockPos): Boolean {
        val sameTarget = targetPos.x == lastPathFailTargetX &&
            targetPos.y == lastPathFailTargetY &&
            targetPos.z == lastPathFailTargetZ
        sameTargetPathFailCount = if (sameTarget) sameTargetPathFailCount + 1 else 1
        lastPathFailTargetX = targetPos.x
        lastPathFailTargetY = targetPos.y
        lastPathFailTargetZ = targetPos.z

        if (sameTargetPathFailCount < MAX_SAME_TARGET_PATH_FAILS) return false

        logger.warn(
            "[{}] skip unreachable target after {} path fails at ({}, {}, {})",
            blockKey,
            sameTargetPathFailCount,
            targetPos.x,
            targetPos.y,
            targetPos.z
        )
        resetPathFailState()
        return true
    }

    private fun resetPathFailState() {
        sameTargetPathFailCount = 0
        lastPathFailTargetX = Int.MIN_VALUE
        lastPathFailTargetY = Int.MIN_VALUE
        lastPathFailTargetZ = Int.MIN_VALUE
    }

    private fun advanceCursor(range: Int) {
        resetPathFailState()
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

    private fun getPipeEnergyCost(): Long {
        return getDrillBreakCost() ?: DEFAULT_PIPE_ENERGY
    }

    private enum class PipeReachResult {
        REACHED,
        RETRY_LATER,
        UNREACHABLE
    }

    private fun processPipeRecoveryTick(world: ServerWorld): Boolean {
        // 管道槽满时暂停回收，等待槽位空出来后继续。
        if (!canAcceptRecoveredPipe()) return false

        while (pendingPipeRecovery.isNotEmpty()) {
            val pipePos = pendingPipeRecovery.removeFirst()
            val state = world.getBlockState(pipePos)
            if (state.block !is MiningPipeBlock) continue

            world.setBlockState(pipePos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
            insertRecoveredPipeIntoSlot()
            markDirty()
            return true
        }

        recoveringPipes = false
        markDirty()
        return false
    }

    private fun canAcceptRecoveredPipe(): Boolean {
        val stack = getStack(SLOT_PIPE)
        if (stack.isEmpty) return true
        if (stack.item !== MiningPipeBlock::class.item()) return false
        return stack.count < PIPE_SLOT_MAX_COUNT
    }

    private fun insertRecoveredPipeIntoSlot() {
        val stack = getStack(SLOT_PIPE)
        if (stack.isEmpty) {
            setStack(SLOT_PIPE, ItemStack(MiningPipeBlock::class.item(), 1))
            return
        }
        stack.increment(1)
        if (stack.count > PIPE_SLOT_MAX_COUNT) stack.count = PIPE_SLOT_MAX_COUNT
        markDirty()
    }

    private fun buildPipeRecoveryQueue(world: ServerWorld) {
        pendingPipeRecovery.clear()
        val queued = HashSet<BlockPos>()
        val queue = ArrayDeque<BlockPos>()

        for (direction in Direction.entries) {
            val neighbor = pos.offset(direction)
            if (world.getBlockState(neighbor).block !is MiningPipeBlock) continue
            queue.add(neighbor)
            queued.add(neighbor)
        }
        if (queue.isEmpty()) return

        var explored = 0
        val dirs = arrayOf(
            BlockPos(1, 0, 0),
            BlockPos(-1, 0, 0),
            BlockPos(0, 1, 0),
            BlockPos(0, -1, 0),
            BlockPos(0, 0, 1),
            BlockPos(0, 0, -1)
        )
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            pendingPipeRecovery.add(current)
            explored++
            if (explored >= MAX_RECOVERY_SEARCH_NODES) break

            for (dir in dirs) {
                val next = BlockPos(current.x + dir.x, current.y + dir.y, current.z + dir.z)
                if (!queued.add(next)) continue
                if (world.getBlockState(next).block !is MiningPipeBlock) continue
                queue.add(next)
            }
        }
    }

    private fun refreshPipeBudget() {
        val currentTick = world?.time ?: return
        val elapsed = currentTick - lastBudgetRefreshTick
        if (elapsed >= PIPE_PLACE_INTERVAL) {
            // 速率均匀限制：最多只积攒 1 格预算，避免瞬时连放多格再长时间等待。
            pipePlacementBudget = 1
            lastBudgetRefreshTick = currentTick
        }
    }

    /**
     * 尝试在指定位置放置一格采矿管道。
     * 受速率限制（MAX_PIPES_PER_SECOND 格/秒），每格消耗钻头挖掘能量。
     * @return true = 已放置（或已存在/被阻挡无需放置），false = 速率或能量不足
     */
    private fun tryPlacePipeAt(world: ServerWorld, pipePos: BlockPos): Boolean {
        val currentState = world.getBlockState(pipePos)
        if (currentState.block is MiningPipeBlock) return true
        if (!currentState.isAir) {
            // 可破坏方块先清掉，再放置采矿管道；不可破坏方块则视为阻塞。
            if (currentState.getHardness(world, pipePos) < 0f) return false
            world.breakBlock(pipePos, false)
        }

        refreshPipeBudget()
        if (pipePlacementBudget <= 0) return false

        val pipeEnergy = getPipeEnergyCost()
        if (sync.consumeEnergy(pipeEnergy) <= 0L) return false

        val pipeSlot = findPipeInInventory() ?: run {
            sync.running = 0
            return false
        }
        val stack = getStack(pipeSlot)
        stack.decrement(1)
        if (stack.isEmpty) setStack(pipeSlot, ItemStack.EMPTY)
        markDirty()

        world.setBlockState(pipePos, MiningPipeBlock::class.instance().defaultState, Block.NOTIFY_ALL)
        pipePlacementBudget--
        return true
    }

    /**
     * 确保垂直管道柱延伸到目标 Y 位置。
     * @return true = 已完成，false = 还需继续
     */
    private fun ensurePipeColumnReaches(targetY: Int): Boolean {
        if (targetY >= pos.y) return true
        val serverWorld = world as? ServerWorld ?: return true

        var startY = pos.y - 1
        if (lastPlacedPipeY in targetY..startY) {
            val remembered = BlockPos(pos.x, lastPlacedPipeY, pos.z)
            if (serverWorld.getBlockState(remembered).block is MiningPipeBlock) {
                startY = lastPlacedPipeY
            }
        }

        for (y in startY downTo targetY) {
            val checkPos = BlockPos(pos.x, y, pos.z)
            if (serverWorld.getBlockState(checkPos).block is MiningPipeBlock) continue

            if (!tryPlacePipeAt(serverWorld, checkPos)) return false

            lastPlacedPipeY = y
        }
        return true
    }

    /**
     * 确保水平管道从垂直管柱延伸到目标方块位置。
     * @return true = 已铺设完成，false = 还需继续
     */
    private fun ensurePipeReachesPosition(targetPos: BlockPos): PipeReachResult {
        if (isPipeAdjacentTo(targetPos)) return PipeReachResult.REACHED

        val serverWorld = world as? ServerWorld ?: return PipeReachResult.RETRY_LATER
        val y = targetPos.y

        // 连接到目标的任一相邻格（矿石格本身不放管道）
        val goalCandidates = listOf(
            BlockPos(targetPos.x + 1, y, targetPos.z),
            BlockPos(targetPos.x - 1, y, targetPos.z),
            BlockPos(targetPos.x, y, targetPos.z + 1),
            BlockPos(targetPos.x, y, targetPos.z - 1),
            BlockPos(targetPos.x, y + 1, targetPos.z)
        )

        // 已有管道直接相邻时，无需新增
        if (goalCandidates.any { serverWorld.getBlockState(it).block is MiningPipeBlock }) return PipeReachResult.REACHED

        val starts = collectExistingPipeStarts3D(serverWorld, targetPos)
        if (starts.isEmpty()) return PipeReachResult.UNREACHABLE

        val pathToPlace = findBestPath3D(serverWorld, starts, goalCandidates, targetPos) ?: return PipeReachResult.UNREACHABLE
        for (pipePos in pathToPlace) {
            if (!tryPlacePipeAt(serverWorld, pipePos)) return PipeReachResult.RETRY_LATER
        }
        return PipeReachResult.REACHED
    }

    private fun collectExistingPipeStarts3D(world: ServerWorld, targetPos: BlockPos): List<BlockPos> {
        val dx = abs(targetPos.x - pos.x)
        val dz = abs(targetPos.z - pos.z)
        val range = maxOf(dx, dz) + 2
        val minX = pos.x - range
        val maxX = pos.x + range
        val minZ = pos.z - range
        val maxZ = pos.z + range
        val minY = targetPos.y
        val maxY = maxOf(pos.y - 1, targetPos.y) + 2

        val starts = ArrayList<BlockPos>()
        for (x in minX..maxX) {
            for (yy in minY..maxY) {
                for (z in minZ..maxZ) {
                    val p = BlockPos(x, yy, z)
                    if (world.getBlockState(p).block is MiningPipeBlock) starts.add(p)
                }
            }
        }
        return starts
    }

    private fun findBestPath3D(
        world: ServerWorld,
        starts: List<BlockPos>,
        goals: List<BlockPos>,
        targetPos: BlockPos
    ): List<BlockPos>? {
        if (goals.isEmpty() || starts.isEmpty()) return null
        if (starts.any { it in goals }) return emptyList()

        val minX = minOf(pos.x, targetPos.x) - (abs(targetPos.x - pos.x) + 2)
        val maxX = maxOf(pos.x, targetPos.x) + (abs(targetPos.x - pos.x) + 2)
        val minZ = minOf(pos.z, targetPos.z) - (abs(targetPos.z - pos.z) + 2)
        val maxZ = maxOf(pos.z, targetPos.z) + (abs(targetPos.z - pos.z) + 2)
        val minY = targetPos.y
        val maxY = maxOf(pos.y - 1, targetPos.y) + 2

        val goalSet = goals.toHashSet()
        val deque = ArrayDeque<BlockPos>()
        val prev = HashMap<BlockPos, BlockPos>()
        val distNew = HashMap<BlockPos, Int>()
        val distSteps = HashMap<BlockPos, Int>()

        for (start in starts) {
            if (start.x !in minX..maxX || start.y !in minY..maxY || start.z !in minZ..maxZ) continue
            if (!canTraverseForPipe(world, start)) continue
            if (distNew.containsKey(start)) continue
            deque.add(start)
            distNew[start] = 0
            distSteps[start] = 0
        }
        if (deque.isEmpty()) return null

        val dirs = arrayOf(
            BlockPos(1, 0, 0),
            BlockPos(-1, 0, 0),
            BlockPos(0, 0, 1),
            BlockPos(0, 0, -1),
            BlockPos(0, 1, 0),
            BlockPos(0, -1, 0)
        )

        var explored = 0
        while (deque.isNotEmpty()) {
            val cur = deque.removeFirst()
            explored++
            if (explored > MAX_PATH_SEARCH_NODES) break
            val curNew = distNew[cur] ?: continue
            val curSteps = distSteps[cur] ?: continue

            for (d in dirs) {
                val next = BlockPos(cur.x + d.x, cur.y + d.y, cur.z + d.z)
                if (next.x !in minX..maxX || next.y !in minY..maxY || next.z !in minZ..maxZ) continue
                if (!canTraverseForPipe(world, next)) continue

                val addNew = if (isExistingPipe(world, next)) 0 else 1
                val nextNew = curNew + addNew
                val nextSteps = curSteps + 1

                val oldNew = distNew[next]
                val oldSteps = distSteps[next]
                val better = oldNew == null ||
                    nextNew < oldNew ||
                    (nextNew == oldNew && nextSteps < (oldSteps ?: Int.MAX_VALUE))
                if (!better) continue

                distNew[next] = nextNew
                distSteps[next] = nextSteps
                prev[next] = cur
                if (addNew == 0) deque.addFirst(next) else deque.addLast(next)
            }
        }

        var bestGoal: BlockPos? = null
        var bestNew = Int.MAX_VALUE
        var bestSteps = Int.MAX_VALUE
        for (goal in goalSet) {
            val n = distNew[goal] ?: continue
            val s = distSteps[goal] ?: continue
            if (n < bestNew || (n == bestNew && s < bestSteps)) {
                bestGoal = goal
                bestNew = n
                bestSteps = s
            }
        }
        val goal = bestGoal ?: return null

        val reversed = ArrayList<BlockPos>()
        var cur: BlockPos? = goal
        while (cur != null && cur !in starts) {
            reversed.add(cur)
            cur = prev[cur]
        }
        if (cur == null) return null
        reversed.reverse()
        return reversed
    }

    private fun isExistingPipe(world: ServerWorld, pos: BlockPos): Boolean {
        return world.getBlockState(pos).block is MiningPipeBlock
    }

    private fun canTraverseForPipe(world: ServerWorld, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        if (state.block is MiningPipeBlock) return true
        if (state.isAir) return true
        return state.getHardness(world, pos) >= 0f
    }

    private fun observeCursorStall(world: World, phase: String) {
        if (!cursorInitialized || sync.running == 0) {
            stallTicks = 0
            lastObservedCursorX = sync.cursorX
            lastObservedCursorY = sync.cursorY
            lastObservedCursorZ = sync.cursorZ
            return
        }

        val sameCursor = sync.cursorX == lastObservedCursorX &&
            sync.cursorY == lastObservedCursorY &&
            sync.cursorZ == lastObservedCursorZ

        if (sameCursor) {
            stallTicks++
        } else {
            stallTicks = 0
            lastObservedCursorX = sync.cursorX
            lastObservedCursorY = sync.cursorY
            lastObservedCursorZ = sync.cursorZ
        }

        if (stallTicks >= STALL_TICKS_LOG_THRESHOLD && world.time - lastStallLogTick >= STALL_LOG_INTERVAL_TICKS) {
            val target = pos.add(sync.cursorX, sync.cursorY - pos.y, sync.cursorZ)
            logger.warn(
                "[{}] cursor stalled={}ticks phase={} cursor=({}, {}, {}) target=({}, {}, {}) energy={} pendingBreak={} pipeCount={} running={} scanner={} drill={}",
                blockKey,
                stallTicks,
                phase,
                sync.cursorX,
                sync.cursorY,
                sync.cursorZ,
                target.x,
                target.y,
                target.z,
                sync.amount,
                pendingBreakEnergy,
                getStack(SLOT_PIPE).count,
                sync.running,
                getStack(SLOT_SCANNER).item,
                getStack(SLOT_DRILL).item
            )
            lastStallLogTick = world.time
        }
    }

    private fun logDecision(world: World, reason: String, targetPos: BlockPos) {
        if (world.time - lastDecisionLogTick < 10L) return
        logger.info(
            "[{}] reason={} cursor=({}, {}, {}) target=({}, {}, {}) energy={} pendingBreak={} pipeCount={} running={}",
            blockKey,
            reason,
            sync.cursorX,
            sync.cursorY,
            sync.cursorZ,
            targetPos.x,
            targetPos.y,
            targetPos.z,
            sync.amount,
            pendingBreakEnergy,
            getStack(SLOT_PIPE).count,
            sync.running
        )
        lastDecisionLogTick = world.time
    }

    private fun isPipeAdjacentTo(pos: BlockPos): Boolean {
        for (dir in Direction.values()) {
            val neighbor = pos.offset(dir)
            if (world?.getBlockState(neighbor)?.block is MiningPipeBlock) return true
        }
        return false
    }

    private fun findPipeInInventory(): Int? {
        val stack = getStack(SLOT_PIPE)
        return if (!stack.isEmpty && stack.item === MiningPipeBlock::class.item()) SLOT_PIPE else null
    }

    private fun shouldMine(world: World, targetPos: BlockPos, state: BlockState): Boolean {
        if (state.isAir) return false
        if (state.getHardness(world, targetPos) < 0f) return false

        val id = Registries.BLOCK.getId(state.block)
        val idString = id.toString()
        val oreLike = (id.path.contains("ore") || id.path == "ancient_debris"
            || idString in Ic2Config.current.miner.additionalMineableBlocks)
            && state.block !is ic2_120.content.block.MachineBlock
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
        if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, targetPos, ownerUuid)) return
        val tool = getLootToolStack()
        val drops = net.minecraft.block.Block.getDroppedStacks(
            state,
            world,
            targetPos,
            world.getBlockEntity(targetPos),
            null,
            tool
        )
        updateRenderTarget(targetPos)
        // 服务端广播方块破坏粒子（2001），客户端可见挖掘动画反馈。
        world.syncWorldEvent(2001, targetPos, Block.getRawIdFromState(state))
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

    private fun updateRenderTarget(targetPos: BlockPos) {
        val world = world as? ServerWorld ?: return
        renderTargetX = targetPos.x
        renderTargetY = targetPos.y
        renderTargetZ = targetPos.z
        renderTargetTime = world.time
        markDirty()
        val state = world.getBlockState(pos)
        world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
        world.chunkManager.markForUpdate(pos)
    }

    fun getRenderDrillTarget(currentWorldTime: Long): BlockPos? {
        if (renderTargetTime < 0L) return null
        if (currentWorldTime - renderTargetTime > RENDER_TARGET_TTL_TICKS) return null
        return BlockPos(renderTargetX, renderTargetY, renderTargetZ)
    }

    private fun getLootToolStack(): ItemStack {
        val drillItem = getStack(SLOT_DRILL).item
        val baseTool = when (drillItem) {
            is Drill -> ItemStack(Items.IRON_PICKAXE)
            is DiamondDrill, is IridiumDrill -> ItemStack(Items.DIAMOND_PICKAXE)
            else -> ItemStack(Items.IRON_PICKAXE)
        }
        if (sync.silkTouch != 0 && acceptsAdvancedScanner) {
            val enchantmentRegistry = world?.registryManager?.get(RegistryKeys.ENCHANTMENT)
            if (enchantmentRegistry != null) {
                baseTool.addEnchantment(enchantmentRegistry.entryOf(Enchantments.SILK_TOUCH), 1)
            }
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
            } else if (ItemStack.areItemsAndComponentsEqual(existing, remaining)) {
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
        if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, targetPos, ownerUuid, ic2_120.integration.ftbchunks.ClaimProtection.EDIT_FLUID)) return false

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
