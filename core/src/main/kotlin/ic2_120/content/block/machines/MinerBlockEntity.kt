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
import ic2_120.content.item.EjectorUpgrade
import ic2_120.content.item.EnergyStorageUpgrade
import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.OverclockerUpgrade
import ic2_120.content.item.PullingUpgrade
import ic2_120.content.item.RedstoneInverterUpgrade
import ic2_120.content.item.ScannerType
import ic2_120.content.item.TransformerUpgrade
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.AdjacentEnergyTransferComponent
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
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
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

abstract class BaseMinerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    private val blockKey: String,
    private val baseTier: Int,
    private val acceptsAdvancedScanner: Boolean
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine,
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
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0
    private val logger = LoggerFactory.getLogger("ic2_120/MinerCursor")

    companion object {
        const val SLOT_SCANNER = 0
        const val SLOT_DRILL = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_ITEM_START = 3
        const val ITEM_SLOT_COUNT = 15
        const val SLOT_ITEM_END = SLOT_ITEM_START + ITEM_SLOT_COUNT - 1
        const val SLOT_UPGRADE_0 = SLOT_ITEM_END + 1
        const val SLOT_UPGRADE_1 = SLOT_ITEM_END + 2
        const val SLOT_UPGRADE_2 = SLOT_ITEM_END + 3
        const val SLOT_UPGRADE_3 = SLOT_ITEM_END + 4
        val SLOT_ITEM_INDICES = (SLOT_ITEM_START..SLOT_ITEM_END).toList().toIntArray()
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_NORMAL_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2)
        const val SLOT_OUTPUT_0 = SLOT_UPGRADE_3 + 1
        const val SLOT_OUTPUT_1 = SLOT_UPGRADE_3 + 2
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1)
        const val SLOT_PIPE = SLOT_OUTPUT_1 + 1
        const val INVENTORY_SIZE = SLOT_PIPE + 1
        const val PIPE_SLOT_MAX_COUNT = 1024
        const val MAX_CACHE_ITEMS = 64
        const val ADVANCED_MIN_Y = -63
        const val NORMAL_MIN_Y = 0

        const val MAX_PIPES_PER_SECOND = 4
        const val PIPE_PLACE_INTERVAL = 20 / MAX_PIPES_PER_SECOND  // = 5 ticks
        const val DEFAULT_PIPE_ENERGY = 64L

        private const val NBT_CURSOR_INITIALIZED = "CursorInitialized"
        private const val NBT_ENERGY_STORED = "EnergyStored"
        private const val NBT_PENDING_BREAK_ENERGY = "PendingBreakEnergy"
        private const val NBT_PIPE_DEPTH = "PipeDepth"
        private const val NBT_RENDER_TARGET_X = "RenderTargetX"
        private const val NBT_RENDER_TARGET_Y = "RenderTargetY"
        private const val NBT_RENDER_TARGET_Z = "RenderTargetZ"
        private const val NBT_RENDER_TARGET_TIME = "RenderTargetTime"
        private const val NBT_MANUAL_STOPPED_FOR_RECOVERY = "ManualStoppedForRecovery"
        private const val NBT_PIPE_SLOT_COUNT = "PipeSlotCount"
        private const val RENDER_TARGET_TTL_TICKS = 8L
        private const val MAX_PATH_SEARCH_NODES = 8192
        private const val STALL_TICKS_LOG_THRESHOLD = 40
        private const val STALL_LOG_INTERVAL_TICKS = 20L
        private const val MAX_SAME_TARGET_PATH_FAILS = 1
        private const val NBT_CURSOR_INDEX = "CursorIndex"
        private const val NBT_LAST_RECYCLED_CURSOR_Y = "LastRecycledCursorY"
        private const val MAX_RECOVERY_SEARCH_NODES = 16384
        private const val NBT_TANK_AMOUNT = "TankAmount"
        private const val NBT_TANK_FLUID = "TankFluid"
        private const val NBT_REDSTONE_CHANGE_REQUIRED = "RedstoneChangeRequired"
        private const val NBT_LAST_REDSTONE_ACTIVE = "LastRedstoneActive"
        private const val NBT_MINER_STATE = "MinerState"
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = buildList {
            add(ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { isAllowedUpgrade(it) }))
            add(ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1))
            add(ItemInsertRoute(intArrayOf(SLOT_SCANNER), matcher = { isValid(SLOT_SCANNER, it) }, maxPerSlot = 1))
            add(ItemInsertRoute(intArrayOf(SLOT_DRILL), matcher = { isValid(SLOT_DRILL, it) }, maxPerSlot = 1))
            // 普通采矿机：物品槽允许外部管道插入；高级采矿机：过滤槽不允许外部插入/抽出
            if (!acceptsAdvancedScanner) {
                add(ItemInsertRoute(SLOT_ITEM_INDICES, matcher = { isValidForItemSlot(it) }))
            }
            add(ItemInsertRoute(intArrayOf(SLOT_PIPE), matcher = { it.item === MiningPipeBlock::class.item() }, maxPerSlot = PIPE_SLOT_MAX_COUNT))
        },
        extractSlots = (if (!acceptsAdvancedScanner) (SLOT_ITEM_START..SLOT_ITEM_END).toList().toIntArray() else intArrayOf())
            + SLOT_UPGRADE_INDICES
            + intArrayOf(SLOT_SCANNER, SLOT_DRILL, SLOT_DISCHARGING)
            + SLOT_OUTPUT_INDICES
            + intArrayOf(SLOT_PIPE),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage
    val pipeStorage = PipeSlotStorage(
        inventory = inventory,
        slotIndex = SLOT_PIPE,
        capacity = PIPE_SLOT_MAX_COUNT,
        pipeItem = { MiningPipeBlock::class.item() },
        markDirty = { markDirty() }
    )
    @RegisterItemStorage
    val combinedItemStorage = CombinedMinerItemStorage(itemStorage, pipeStorage)
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
    private var lastRecycledCursorY: Int = Int.MAX_VALUE
    private val pendingPipeRecovery = ArrayDeque<BlockPos>()
    private val knownPipePositions = HashSet<BlockPos>()
    private var pipeCacheDirty = true
    private var lastPipeCacheCleanupTick = -1L
    private var cursorIndex: Int = 0
    val itemCache = mutableListOf<ItemStack>()
    var cacheItemCount = 0
    val syncedData = SyncedData(this)
    private var lastRedstoneActive = false

    /** 状态机权威字段。见 [MinerState]。 */
    private var minerState: MinerState = MinerState.IDLE

    /** tickScanning 侧信道：垂直管柱遇到基岩。见 ensureCursorAndVerticalPipe。 */
    private var verticalHitBedrock = false
    /** tickScanning 侧信道：tryPlacePipeAt 链请求触发分支管道回收。 */
    private var pipeRecyclingRequested = false
    /** tickScanning 侧信道：tryPlacePipeAt 链请求停机（无管可铺且无可回收）。 */
    private var haltRequested = false

    /** 内部流体储罐（1桶容量），用于储存采矿管遇到的流体。 */
    private val fluidTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = FluidConstants.BUCKET
        override fun canInsert(variant: FluidVariant): Boolean =
            !variant.isBlank && (variant.fluid == Fluids.WATER || variant.fluid == Fluids.LAVA)
        override fun canExtract(variant: FluidVariant): Boolean = true
        override fun onFinalCommit() { markDirty() }

        fun hasSpaceForBucket(fluid: Fluid): Boolean {
            val space = FluidConstants.BUCKET - amount
            if (space < FluidConstants.BUCKET) return false
            if (amount > 0L && !variant.isBlank && variant.fluid != fluid) return false
            return true
        }

        fun insertBucket(fluid: Fluid) {
            val fv = FluidVariant.of(fluid)
            Transaction.openOuter().use { tx ->
                val inserted = insert(fv, FluidConstants.BUCKET, tx)
                if (inserted > 0L) tx.commit()
            }
        }
    }
    val fluidTank: Storage<FluidVariant> = fluidTankInternal

    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? = fluidTank

    @RegisterEnergy
    val sync = MinerSync(
        syncedData,
        { world?.time },
        { getCurrentBaseCapacity() + capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(baseTier + voltageTierBonus) }
    )

    init {
        sync.cursorY = pos.y - 1
    }

    /** 从扫描器电池消耗能量（仅用于扫描非矿石方块）。 */
    private fun consumeScannerEnergy(amount: Long): Long {
        if (!acceptsAdvancedScanner) return sync.consumeEnergy(amount)
        val scannerStack = getStack(SLOT_SCANNER)
        val tool = scannerStack.item as? IElectricTool ?: return 0L
        val currentEnergy = tool.getEnergy(scannerStack)
        if (currentEnergy <= 0L) return 0L
        val toConsume = minOf(amount, currentEnergy)
        tool.setEnergy(scannerStack, currentEnergy - toConsume)
        return toConsume
    }

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
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

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun getMaxCountPerStack(): Int = PIPE_SLOT_MAX_COUNT
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
            in SLOT_ITEM_START..SLOT_ITEM_END -> if (acceptsAdvancedScanner) 1 else maxCountPerStack
            else -> maxCountPerStack
        }
        if (stack.count > slotMax) stack.count = slotMax
        if (slot == SLOT_PIPE) sync.pipeCount = getPipeCount()
        markDirty()
    }

    fun getPipeCount(): Int {
        val stack = inventory[SLOT_PIPE]
        return if (!stack.isEmpty && stack.item === MiningPipeBlock::class.item()) {
            stack.count.coerceIn(0, PIPE_SLOT_MAX_COUNT)
        } else {
            0
        }
    }

    private fun setPipeCount(count: Int) {
        val clamped = count.coerceIn(0, PIPE_SLOT_MAX_COUNT)
        inventory[SLOT_PIPE] = if (clamped > 0) {
            ItemStack(MiningPipeBlock::class.item(), clamped)
        } else {
            ItemStack.EMPTY
        }
        sync.pipeCount = clamped
        markDirty()
    }

    fun insertPipesFromStack(stack: ItemStack, requestedAmount: Int = stack.count): Int {
        if (stack.isEmpty || stack.item !== MiningPipeBlock::class.item()) return 0
        val current = getPipeCount()
        val moveCount = minOf(requestedAmount, stack.count, PIPE_SLOT_MAX_COUNT - current)
        if (moveCount <= 0) return 0
        setPipeCount(current + moveCount)
        stack.decrement(moveCount)
        return moveCount
    }

    fun takePipes(requestedAmount: Int): ItemStack {
        val current = getPipeCount()
        val moveCount = minOf(requestedAmount, current)
        if (moveCount <= 0) return ItemStack.EMPTY
        setPipeCount(current - moveCount)
        return ItemStack(MiningPipeBlock::class.item(), moveCount)
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
            in SLOT_ITEM_START..SLOT_ITEM_END -> {
                if (acceptsAdvancedScanner) {
                    stack.item is net.minecraft.item.BlockItem
                } else {
                    isValidForItemSlot(stack)
                }
            }
            in SLOT_UPGRADE_INDICES -> isAllowedUpgrade(stack)
            in SLOT_OUTPUT_INDICES -> false
            SLOT_PIPE -> stack.item === MiningPipeBlock::class.item()
            else -> false
        }
    }

    private fun isAllowedUpgrade(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        return if (acceptsAdvancedScanner) {
            item is OverclockerUpgrade || item is TransformerUpgrade || item is RedstoneInverterUpgrade ||
                item is EjectorUpgrade || item is FluidEjectorUpgrade
        } else {
            item is OverclockerUpgrade || item is EnergyStorageUpgrade || item is TransformerUpgrade ||
                item is EjectorUpgrade || item is PullingUpgrade || item is FluidEjectorUpgrade || item is FluidPullingUpgrade
        }
    }

    /** 物品槽验证：拒绝管道、升级组件、钻头、扫描仪、电池。 */
    private fun isValidForItemSlot(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.item !== MiningPipeBlock::class.item()
            && stack.item !is IUpgradeItem
            && stack.item !is Drill && stack.item !is DiamondDrill && stack.item !is IridiumDrill
            && stack.item !is OdScannerItem && stack.item !is AdvancedScannerItem
            && stack.item !is IBatteryItem
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeBoolean(acceptsAdvancedScanner)
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.$blockKey")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MinerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage, acceptsAdvancedScanner)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        if (nbt.contains(NBT_PIPE_SLOT_COUNT)) {
            val pipeCount = nbt.getInt(NBT_PIPE_SLOT_COUNT).coerceIn(0, PIPE_SLOT_MAX_COUNT)
            inventory[SLOT_PIPE] = if (pipeCount > 0) {
                ItemStack(MiningPipeBlock::class.item(), pipeCount)
            } else {
                ItemStack.EMPTY
            }
            sync.pipeCount = pipeCount
        }
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        cursorInitialized = nbt.getBoolean(NBT_CURSOR_INITIALIZED)
        cursorIndex = nbt.getInt(NBT_CURSOR_INDEX)
        lastRecycledCursorY = nbt.getInt(NBT_LAST_RECYCLED_CURSOR_Y)
        pendingBreakEnergy = nbt.getLong(NBT_PENDING_BREAK_ENERGY)
        itemCache.clear()
        cacheItemCount = 0
        val cacheNbt = nbt.getList("ItemCache", 10)
        for (i in 0 until cacheNbt.size) {
            val stack = ItemStack.fromNbt(cacheNbt.getCompound(i))
            if (!stack.isEmpty) {
                itemCache.add(stack)
                cacheItemCount += stack.count
            }
        }
        lastPlacedPipeY = if (nbt.contains(NBT_PIPE_DEPTH)) nbt.getInt(NBT_PIPE_DEPTH) else pos.y
        pipeCacheDirty = true
        if (nbt.contains(NBT_RENDER_TARGET_TIME)) {
            renderTargetX = nbt.getInt(NBT_RENDER_TARGET_X)
            renderTargetY = nbt.getInt(NBT_RENDER_TARGET_Y)
            renderTargetZ = nbt.getInt(NBT_RENDER_TARGET_Z)
            renderTargetTime = nbt.getLong(NBT_RENDER_TARGET_TIME)
        } else {
            renderTargetTime = -1L
        }
        // 流体储罐
        val tankFluid = nbt.getString(NBT_TANK_FLUID)
        val tankAmount = nbt.getLong(NBT_TANK_AMOUNT)
        if (tankFluid.isNotEmpty() && tankAmount > 0L) {
            val id = net.minecraft.util.Identifier.tryParse(tankFluid)
            if (id != null && Registries.FLUID.containsId(id)) {
                val fluid = Registries.FLUID.get(id)
                fluidTankInternal.insertBucket(fluid)
            }
        }
        lastRedstoneActive = nbt.getBoolean(NBT_LAST_REDSTONE_ACTIVE)
        // 状态机初始化：优先读新 key，其次旧 boolean 推导（向前兼容）
        minerState = when {
            nbt.contains(NBT_MINER_STATE) -> {
                val name = nbt.getString(NBT_MINER_STATE)
                MinerState.entries.firstOrNull { it.name == name } ?: MinerState.IDLE
            }
            nbt.getBoolean(NBT_REDSTONE_CHANGE_REQUIRED) || nbt.getBoolean(NBT_MANUAL_STOPPED_FOR_RECOVERY) -> {
                if (acceptsAdvancedScanner) MinerState.REDSTONE_WAITING else MinerState.IDLE
            }
            else -> MinerState.IDLE
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        nbt.putInt(
            NBT_PIPE_SLOT_COUNT,
            getPipeCount()
        )
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_ENERGY_STORED, sync.amount)
        nbt.putBoolean(NBT_CURSOR_INITIALIZED, cursorInitialized)
        nbt.putInt(NBT_CURSOR_INDEX, cursorIndex)
        nbt.putInt(NBT_LAST_RECYCLED_CURSOR_Y, lastRecycledCursorY)
        nbt.putInt(NBT_PIPE_DEPTH, lastPlacedPipeY)
        nbt.putLong(NBT_PENDING_BREAK_ENERGY, pendingBreakEnergy)
        val cacheNbt = net.minecraft.nbt.NbtList()
        for (stack in itemCache) {
            if (!stack.isEmpty) cacheNbt.add(stack.writeNbt(NbtCompound()))
        }
        nbt.put("ItemCache", cacheNbt)
        if (renderTargetTime >= 0L) {
            nbt.putInt(NBT_RENDER_TARGET_X, renderTargetX)
            nbt.putInt(NBT_RENDER_TARGET_Y, renderTargetY)
            nbt.putInt(NBT_RENDER_TARGET_Z, renderTargetZ)
            nbt.putLong(NBT_RENDER_TARGET_TIME, renderTargetTime)
        }
        // 流体储罐
        nbt.putLong(NBT_TANK_AMOUNT, fluidTankInternal.amount)
        if (!fluidTankInternal.variant.isBlank) {
            nbt.putString(NBT_TANK_FLUID, Registries.FLUID.getId(fluidTankInternal.variant.fluid).toString())
        } else {
            nbt.putString(NBT_TANK_FLUID, "")
        }
        nbt.putBoolean(NBT_LAST_REDSTONE_ACTIVE, lastRedstoneActive)
        nbt.putString(NBT_MINER_STATE, minerState.name)
        // 旧 key 写固定值，向前兼容旧版本加载（新代码读时优先 NBT_MINER_STATE）
        nbt.putBoolean(NBT_REDSTONE_CHANGE_REQUIRED, false)
        nbt.putBoolean(NBT_MANUAL_STOPPED_FOR_RECOVERY, false)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        val serverWorld = world as? ServerWorld ?: return

        // 公共预处理（每 tick 必做，与状态无关）—— spec §4.1
        tickCommonPre(world, pos)

        // 状态分派：转移只通过返回值发生（spec §3 约束 1）
        minerState = when (minerState) {
            MinerState.PIPE_RECOVERING  -> tickPipeRecovering(serverWorld, pos, state)
            MinerState.PIPE_RECYCLING   -> tickPipeRecycling(serverWorld, pos, state)
            MinerState.REDSTONE_WAITING -> tickRedstoneWaiting(world, pos, state)
            MinerState.IDLE             -> tickIdle(world, pos, state, serverWorld)
            MinerState.SCANNING         -> tickScanning(serverWorld, pos, state)
        }

        // 派生 sync.running（spec §3 约束 2）—— 状态机内部不读它做控制
        sync.running = if (minerState == MinerState.SCANNING) 1 else 0
        sync.syncCurrentTickFlow()
    }

    /**
     * 公共预处理：每 tick 必做，与状态无关（spec §4.1）。
     * 对应原 tick() 第 539-563 行。
     */
    private fun tickCommonPre(world: World, pos: BlockPos) {
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        sync.pipeCount = getPipeCount()
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)

        // 流体弹出升级：将储罐中的流体排到相邻方块
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(
                world, pos, fluidTankInternal, fluidPipeProviderFilter,
                fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount
            )
        }

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()
        chargeScanner()
        sync.fluidBlocked = 0

        // 高级采矿机：缓存弹出
        if (acceptsAdvancedScanner) {
            tryEjectCache(world as? ServerWorld ?: return)
        }

        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
    }

    /**
     * PIPE_RECOVERING 状态：终局回收（普通机到底 / 玩家手动 / 高级机回收）。spec §4.2。
     * @return 下一状态（PIPE_RECOVERING 自环 / IDLE 普通机回收完 / REDSTONE_WAITING 高级机回收完）
     */
    private fun tickPipeRecovering(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState {
        val processed = processPipeRecoveryTick(world)
        setActiveState(world, pos, state, processed)
        // processPipeRecoveryTick 在「管道槽满（暂停）」与「队列空（完成）」两种情况都返回 false，
        // 用 pendingPipeRecovery.isNotEmpty() 区分：暂停时自环不退出，完成时才重置游标并转移。
        return when {
            processed -> MinerState.PIPE_RECOVERING  // 本 tick 回收了一格，继续
            pendingPipeRecovery.isNotEmpty() -> MinerState.PIPE_RECOVERING  // 槽满，暂停：下 tick 重试
            else -> {  // 队列空：回收完毕，重置游标
                resetCursorForRecovery()
                if (acceptsAdvancedScanner) MinerState.REDSTONE_WAITING else MinerState.IDLE
            }
        }
    }

    /**
     * PIPE_RECYCLING 状态：仅普通机，缺管时回收分支管道。spec §4.3。
     * @return 下一状态（PIPE_RECYCLING 自环 / SCANNING 回收完恢复挖矿）
     */
    private fun tickPipeRecycling(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState {
        val processed = processPipeRecoveryTick(world)
        setActiveState(world, pos, state, processed)
        // 同 tickPipeRecovering：用 pendingPipeRecovery.isNotEmpty() 区分「槽满暂停」与「队列空完成」。
        // 注意：回收完成时不重置游标（仅 tickPipeRecovering 重置）。
        return when {
            processed -> MinerState.PIPE_RECYCLING  // 本 tick 回收了一格，继续
            pendingPipeRecovery.isNotEmpty() -> MinerState.PIPE_RECYCLING  // 槽满，暂停：下 tick 重试
            else -> MinerState.SCANNING  // 回收完毕，恢复采矿（原行为：sync.running 仍为 1）
        }
    }

    /** 重置游标到初始位置（回收完成 / 重启扫描时调用）。 */
    private fun resetCursorForRecovery() {
        sync.cursorX = 0
        sync.cursorZ = 0
        sync.cursorY = pos.y - 1
        cursorInitialized = false
        cursorIndex = 0
        pendingBreakEnergy = 0L
        resetPathFailState()
        lastRecycledCursorY = Int.MAX_VALUE
    }

    /**
     * REDSTONE_WAITING 状态：仅高级机，管道回收完成后等红石信号变化才重启。spec §4.4。
     * @return 下一状态（SCANNING 信号变化 / REDSTONE_WAITING 自环）
     */
    private fun tickRedstoneWaiting(world: World, pos: BlockPos, state: BlockState): MinerState {
        val hasPower = currentRedstonePower(world, pos)
        if (hasPower != lastRedstoneActive) {
            // 红石信号变化，重启
            lastRedstoneActive = hasPower
            resetCursorForRecovery()
            setActiveState(world, pos, state, false)
            return MinerState.SCANNING
        }
        setActiveState(world, pos, state, false)
        return MinerState.REDSTONE_WAITING
    }

    /**
     * 高级机红石门控：计算是否应该工作（含红石反转升级）。spec §4.4/§4.5。
     * @return true = 红石条件满足，可工作
     */
    private fun isRedstoneActiveForWork(world: World, pos: BlockPos): Boolean {
        if (!acceptsAdvancedScanner) return true  // 普通机无红石门控
        val hasPower = world.isReceivingRedstonePower(pos)
        val hasInverter = SLOT_UPGRADE_INDICES.any { getStack(it).item is RedstoneInverterUpgrade }
        return if (hasInverter) !hasPower else hasPower
    }

    /** 读取当前红石信号原始值（用于 REDSTONE_WAITING 检测变化）。 */
    private fun currentRedstonePower(world: World, pos: BlockPos): Boolean =
        world.isReceivingRedstonePower(pos)

    /**
     * IDLE 态自动恢复判定（替换原 tryAutoResumeAfterPipeRefill）。spec §4.5。
     * @return true = 满足恢复条件，可转 SCANNING
     */
    private fun tryAutoResume(): Boolean {
        val minY = if (acceptsAdvancedScanner) ADVANCED_MIN_Y else NORMAL_MIN_Y
        if (sync.cursorY < minY) return false
        if (getScannerType(getStack(SLOT_SCANNER)) == null) return false
        if (getDrillBreakCost() == null) return false
        if (findPipeInInventory() == null) return false
        // 自动恢复按"新一轮"开始，避免沿用缺料前的待挖能量残留
        pendingBreakEnergy = 0L
        return true
    }

    /**
     * IDLE 状态：等待自动恢复（普通机）；或红石未激活（高级机）。spec §4.5。
     * @return 下一状态（IDLE 自环 / SCANNING 恢复）
     */
    private fun tickIdle(world: World, pos: BlockPos, state: BlockState, serverWorld: ServerWorld): MinerState {
        // 红石门控（仅高级机）：红石未激活则自环
        if (!isRedstoneActiveForWork(world, pos)) {
            lastRedstoneActive = currentRedstonePower(world, pos)
            setActiveState(world, pos, state, false)
            return MinerState.IDLE
        }

        // 自动恢复
        if (tryAutoResume()) {
            setActiveState(world, pos, state, false)
            return MinerState.SCANNING
        }

        setActiveState(world, pos, state, false)
        return MinerState.IDLE
    }

    /**
     * SCANNING 状态：正常工作。按子阶段顺序判定，首个返回非 null 即转移。spec §4.6。
     * 子方法返回 MinerState?：非 null = 发生转移，null = 继续下一子阶段。
     * 例外：checkScanner 返回 Int?（scanRadius），null = 应回 IDLE。
     */
    private fun tickScanning(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState {
        // 侧信道重置（每 tick 进入 SCANNING 时清零，防止跨 tick 残留）
        pipeRecyclingRequested = false
        haltRequested = false
        verticalHitBedrock = false

        // 4.6.1 红石再检查（仅高级机）—— 非 null 即转移
        checkRedstoneStillActive(world, pos, state)?.let { return it }

        // 4.6.2 扫描器存在性 —— checkScanner 返回 scanRadius（Int），null = 回 IDLE
        val scanRadius = checkScanner(world, pos, state)
        if (scanRadius == null) return MinerState.IDLE

        // 4.6.3 stall 观察（诊断，不转移）
        observeCursorStall(world, "pre_tick")

        // 4.6.4 游标初始化 + 垂直管柱延伸 —— 非 null 即转移
        //      「是否到底」通过侧信道 verticalHitBedrock 传递
        ensureCursorAndVerticalPipe(world, pos, state, scanRadius)?.let { return it }

        // 4.6.5 能量预算预检查（仅普通机）
        checkScanEnergyBudget(world, pos, state)?.let { return it }

        // 4.6.6 周期节流
        checkPeriodThrottle(world, pos, state)?.let { return it }

        // 4.6.7 攒能子阶段（需 scanRadius 推进游标）
        tickPendingBreakEnergy(world, pos, state, scanRadius)?.let { return it }

        // 4.6.8 主扫描/挖掘循环
        return runScanMineLoop(world, pos, state, scanRadius, verticalHitBedrock)
    }

    /** 4.6.1：高级机每 tick 复查红石，断开立即回 IDLE。 */
    private fun checkRedstoneStillActive(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState? {
        if (!acceptsAdvancedScanner) return null
        if (isRedstoneActiveForWork(world, pos)) return null
        lastRedstoneActive = currentRedstonePower(world, pos)
        setActiveState(world, pos, state, false)
        return MinerState.IDLE
    }

    /** 4.6.2：扫描器不在位则回 IDLE。返回 scanRadius 或 null（转移）。 */
    private fun checkScanner(world: ServerWorld, pos: BlockPos, state: BlockState): Int? {
        val scannerType = getScannerType(getStack(SLOT_SCANNER)) ?: run {
            setActiveState(world, pos, state, false)
            return null  // 调用方据此 return IDLE
        }
        // 扫描范围始终跟随放入的扫描仪类型（OD=6→13×13，OV=12→25×25），
        // 普通机与高级机一致，不再为高级机写死更大半径。
        return scannerType.scanRadius
    }

    /** 4.6.4：游标初始化 + 垂直管柱延伸。返回 MinerState? = 转移目标（null=继续）。
     *  「到底」通过侧信道 verticalHitBedrock 传递（ensurePipeColumnReaches 内设置）。 */
    private fun ensureCursorAndVerticalPipe(world: ServerWorld, pos: BlockPos, state: BlockState, scanRadius: Int): MinerState? {
        verticalHitBedrock = false
        if (!cursorInitialized) {
            ensureAndGetCursorTarget(scanRadius)
        }
        if (cursorInitialized && sync.cursorY < pos.y) {
            val done = ensurePipeColumnReaches(sync.cursorY)
            if (!done) {
                // ensurePipeColumnReaches → tryPlacePipeAt 可能已设置回收/停机侧信道。
                // 侧信道在 tickScanning 顶部重置、在子阶段间同 tick 传播，此处需就地翻译为状态转移，
                // 否则本子阶段返回 SCANNING 自环，下 tick 顶部重置会丢弃请求导致 livelock。
                setActiveState(world, pos, state, false)
                return when {
                    pipeRecyclingRequested -> MinerState.PIPE_RECYCLING
                    haltRequested -> MinerState.IDLE
                    else -> MinerState.SCANNING  // 自环，下 tick 继续
                }
            }
            // verticalHitBedrock 已由 ensurePipeColumnReaches 在遇基岩时设置，
            // 这里不再读 sync.running（新状态机不读它做控制）。
        }
        return null  // 继续
    }

    /** 4.6.5：普通机扫描能量不足则自环等待。 */
    private fun checkScanEnergyBudget(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState? {
        if (acceptsAdvancedScanner) return null
        val scanCost = (MinerSync.SCAN_ENERGY_PER_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.amount < scanCost) {
            setActiveState(world, pos, state, false)
            return MinerState.SCANNING  // 自环等能量
        }
        return null
    }

    /** 4.6.6：周期间隔等待态仍亮灯。 */
    private fun checkPeriodThrottle(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState? {
        val effectivePeriod = getEffectivePeriodTicks()
        if (((world.time + workOffset) % effectivePeriod) != 0L) {
            setActiveState(world, pos, state, true)  // 等待态视为工作中
            return MinerState.SCANNING
        }
        return null
    }

    /** 4.6.7：有待挖掘能量时攒能。返回非 null = 本 tick 结束（自环 SCANNING）。
     *  scanRadius 由 tickScanning 传入（推进游标需要）。 */
    private fun tickPendingBreakEnergy(world: ServerWorld, pos: BlockPos, state: BlockState, scanRadius: Int): MinerState? {
        if (pendingBreakEnergy <= 0L) return null  // 无待挖，继续主循环

        val drillBreakCost = getDrillBreakCost()
        if (drillBreakCost == null) {
            pendingBreakEnergy = 0L
            return null  // 继续主循环
        }

        val silkMultiplier = if (sync.silkTouch != 0 && acceptsAdvancedScanner) MinerSync.SILK_TOUCH_MULTIPLIER else 2L
        val breakEnergy = (drillBreakCost * silkMultiplier / 2L * energyMultiplier).toLong().coerceAtLeast(1L)

        val toReserve = minOf(sync.amount, breakEnergy - pendingBreakEnergy)
        if (toReserve > 0L) {
            sync.consumeEnergy(toReserve)
            pendingBreakEnergy += toReserve
        }

        if (pendingBreakEnergy >= breakEnergy) {
            val targetPos = pos.add(sync.cursorX, sync.cursorY - pos.y, sync.cursorZ)
            val blockState = world.getBlockState(targetPos)
            if (!blockState.isAir && shouldMine(world, targetPos, blockState) && canMineWithCurrentDrill(blockState)) {
                ensurePipeReachesPosition(targetPos)
                // ensurePipeReachesPosition → tryPlacePipeAt 可能已设置回收/停机侧信道：
                // 必须在继续挖矿前翻译为状态转移，否则返回 SCANNING 自环、下 tick 顶部重置会丢弃请求。
                if (pipeRecyclingRequested) return MinerState.PIPE_RECYCLING
                if (haltRequested) return MinerState.IDLE
                if (isPipeAdjacentTo(targetPos)) {
                    pendingBreakEnergy = 0L
                    advanceCursor(scanRadius)
                    mineBlock(world, targetPos, blockState)
                    sync.energy = sync.amount.toInt().coerceAtLeast(0)
                    setActiveState(world, pos, state, true)
                    return MinerState.SCANNING
                } else {
                    pendingBreakEnergy = 0L
                }
            } else {
                pendingBreakEnergy = 0L
            }
        } else {
            sync.energy = sync.amount.toInt().coerceAtLeast(0)
            setActiveState(world, pos, state, true)
            return MinerState.SCANNING  // 仍在攒能
        }
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        setActiveState(world, pos, state, false)
        return MinerState.SCANNING
    }

    /** 4.6.8：主扫描/挖掘循环。返回最终转移目标。 */
    private fun runScanMineLoop(world: ServerWorld, pos: BlockPos, state: BlockState, scanRadius: Int, reachedBottomIn: Boolean): MinerState {
        var active = false
        var scannedThisCycle = 0
        var reachedBottom = reachedBottomIn

        while (true) {
            val targetPos = ensureAndGetCursorTarget(scanRadius)
            val minY = if (acceptsAdvancedScanner) ADVANCED_MIN_Y else NORMAL_MIN_Y
            if (targetPos.y < world.bottomY || targetPos.y < minY) {
                reachedBottom = true
                break
            }

            val blockState = world.getBlockState(targetPos)

            // 高级采矿机：缓存满则停止
            if (acceptsAdvancedScanner && cacheItemCount >= MAX_CACHE_ITEMS) {
                sync.energy = sync.amount.toInt().coerceAtLeast(0)
                setActiveState(world, pos, state, active || scannedThisCycle > 0)
                return MinerState.IDLE
            }

            // 先检查是否可挖掘
            if (shouldMine(world, targetPos, blockState) && canMineWithCurrentDrill(blockState)) {
                // 确保管道延伸到目标方块
                when (ensurePipeReachesPosition(targetPos)) {
                    PipeReachResult.REACHED -> {}
                    PipeReachResult.RETRY_LATER -> {
                        logDecision(world, "pipe_path_not_ready", targetPos)
                        break  // 下一 tick 继续
                    }
                    PipeReachResult.UNREACHABLE -> {
                        logDecision(world, "pipe_path_unreachable", targetPos)
                        if (recordPathFailAndShouldSkip(targetPos)) {
                            advanceCursor(scanRadius)
                        }
                        break
                    }
                }

                if (!isPipeAdjacentTo(targetPos)) {
                    logDecision(world, "pipe_not_adjacent_after_path", targetPos)
                    advanceCursor(scanRadius)
                    break
                }

                val breakCost = getDrillBreakCost() ?: 0L
                if (breakCost <= 0L) break

                val silkMultiplier = if (sync.silkTouch != 0 && acceptsAdvancedScanner) MinerSync.SILK_TOUCH_MULTIPLIER else 2L
                val breakEnergy = (breakCost * silkMultiplier / 2L * energyMultiplier).toLong().coerceAtLeast(1L)

                if (sync.consumeEnergy(breakEnergy) > 0L) {
                    advanceCursor(scanRadius)
                    mineBlock(world, targetPos, blockState)
                    active = true
                } else {
                    // 能量不足，转入待挖掘池，下一 tick 继续攒
                    pendingBreakEnergy = sync.amount
                    if (pendingBreakEnergy > 0L) sync.consumeEnergy(pendingBreakEnergy)
                }
                break
            }

            // 非矿石方块，消耗扫描能量并前进
            val scanCost = (MinerSync.SCAN_ENERGY_PER_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
            if (consumeScannerEnergy(scanCost) <= 0L) {
                logDecision(world, "insufficient_scan_energy", targetPos)
                break
            }

            advanceCursor(scanRadius)
            scannedThisCycle++
        }

        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        setActiveState(world, pos, state, active || scannedThisCycle > 0)

        // tryPlacePipeAt 链的侧信道：检查是否请求回收/停机
        if (pipeRecyclingRequested) return MinerState.PIPE_RECYCLING
        if (haltRequested) return MinerState.IDLE

        // 到底处理：仅普通机自动回收管道（原 789 行 `if (reachedBottom && !acceptsAdvancedScanner && !recoveringPipes)`）。
        // 高级机不在此触发终局回收——它靠每层 recoverLayerPipes 循环挖矿，不"到底"。
        if (reachedBottom && !acceptsAdvancedScanner) {
            return startPipeRecoveryAndGetState()
        }

        // 缺料检测：原代码主循环不直接判缺料，依赖下 tick。返回 SCANNING，下 tick 由子阶段判定。
        return MinerState.SCANNING
    }

    /** 触发终局回收并返回新状态（供 runScanMineLoop 到底时调用）。 */
    private fun startPipeRecoveryAndGetState(): MinerState = startPipeRecoveryInternal()

    fun toggleMode() {
        sync.mode = if (sync.mode == 0) 1 else 0
        markDirty()
    }

    fun toggleSilkTouch() {
        sync.silkTouch = if (sync.silkTouch == 0) 1 else 0
        markDirty()
    }

    fun restartScan() {
        minerState = MinerState.SCANNING
        lastRecycledCursorY = Int.MAX_VALUE
        pendingPipeRecovery.clear()
        sync.cursorX = 0
        sync.cursorZ = 0
        sync.cursorY = pos.y - 1
        cursorInitialized = false
        cursorIndex = 0
        pendingBreakEnergy = 0L
        resetPathFailState()
        markDirty()
    }

    /** 玩家手动触发终局回收（UI 按钮）。返回值忽略，内部状态由 startPipeRecoveryInternal 设置。 */
    fun startPipeRecovery() {
        startPipeRecoveryInternal()
    }

    /**
     * 触发终局管道回收。返回回收后的目标状态。
     * 普通机：到底或玩家手动；高级机：玩家手动。
     */
    private fun startPipeRecoveryInternal(): MinerState {
        val serverWorld = world as? ServerWorld ?: return minerState
        pendingBreakEnergy = 0L
        knownPipePositions.clear()
        pipeCacheDirty = true
        resetPathFailState()
        buildPipeRecoveryQueue(serverWorld)

        val newState = if (pendingPipeRecovery.isNotEmpty()) {
            MinerState.PIPE_RECOVERING
        } else {
            // 队列空：无管可回收。普通机回 IDLE；高级机也回 IDLE（交红石门控），避免锁死。
            MinerState.IDLE
        }
        minerState = newState
        markDirty()
        return newState
    }

    /**
     * 普通机缺管时尝试触发分支管道回收。
     * @return true = 已触发回收（调用方应转 PIPE_RECYCLING）；false = 未触发
     *
     * 本方法不改 minerState（遵守 spec §5.3 约束：由调用层 tryPlacePipeAt 链
     * 通过 pipeRecyclingRequested / haltRequested 侧信道翻译成状态转移）。
     */
    private fun triggerPipeRecycling(): Boolean {
        if (acceptsAdvancedScanner) return false  // 仅普通机
        // 防死循环：游标未前进过则不再回收
        if (sync.cursorY >= lastRecycledCursorY) return false
        lastRecycledCursorY = sync.cursorY
        val serverWorld = world as? ServerWorld ?: return false
        buildPipeRecyclingQueue(serverWorld)
        return pendingPipeRecovery.isNotEmpty()
    }

    /**
     * 收集可回收的分支管道，按 Y 从高到低排序。
     * 排除中心柱管道（pos.x, pos.z），只收 Y > cursorY 的分支。
     */
    private fun buildPipeRecyclingQueue(world: ServerWorld) {
        pendingPipeRecovery.clear()
        val thresholdY = sync.cursorY
        val candidates = knownPipePositions
            .filter { it.y > thresholdY && (it.x != pos.x || it.z != pos.z) }
            .filter { world.getBlockState(it).block is MiningPipeBlock }
            .sortedWith(compareByDescending<BlockPos> { it.y }.thenByDescending { abs(it.x - pos.x) + abs(it.z - pos.z) })
        for (pos in candidates) pendingPipeRecovery.add(pos)
    }

    private fun getCurrentBaseCapacity(): Long {
        val breakCost = getDrillBreakCost() ?: 0L
        val silkMultiplier = if (sync.silkTouch != 0) MinerSync.SILK_TOUCH_MULTIPLIER else 2L
        return MinerSync.SCAN_ENERGY_PER_STEP + breakCost * silkMultiplier / 2L
    }

    private fun getDrillBreakCost(): Long? {
        if (acceptsAdvancedScanner) return MinerSync.DRILL_ENERGY_PER_BREAK
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
            sync.cursorY = pos.y - 1
            cursorIndex = 0
            cursorInitialized = true
        }
        if (sync.cursorY > pos.y - 1) sync.cursorY = pos.y - 1
        val minY = if (acceptsAdvancedScanner) ADVANCED_MIN_Y else NORMAL_MIN_Y
        if (sync.cursorY < minY) sync.cursorY = minY
        val totalPositions = (2 * range + 1) * (2 * range + 1)
        if (cursorIndex >= totalPositions) cursorIndex = 0
        val (x, z) = spiralXY(cursorIndex, range)
        sync.cursorX = x
        sync.cursorZ = z
        return pos.add(x, sync.cursorY - pos.y, z)
    }

    /**
     * 螺旋扫描：按切比雪夫距离从中心向外扩展，优先挖掘靠近管道柱的矿石。
     * ring 0 = (0,0)，ring n 从 (-n, n) 开始顺时针遍历 8n 个位置。
     */
    private fun spiralXY(index: Int, range: Int): Pair<Int, Int> {
        if (range == 0) return Pair(0, 0)
        if (index == 0) return Pair(0, 0)
        var n = 1
        while (n <= range) {
            val ringStart = 1 + 4 * n * (n - 1)
            val ringEnd = ringStart + 8 * n
            if (index < ringEnd) {
                val offset = index - ringStart
                val side = offset / (2 * n)
                val posInSide = offset % (2 * n)
                return when (side) {
                    0 -> Pair(-n + posInSide, n)
                    1 -> Pair(n, n - posInSide)
                    2 -> Pair(n - posInSide, -n)
                    else -> Pair(-n, -n + posInSide)
                }
            }
            n++
        }
        return Pair(range, range)
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

        logger.debug(
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
        cursorIndex++
        val totalPositions = (2 * range + 1) * (2 * range + 1)
        if (cursorIndex >= totalPositions) {
            val completedY = sync.cursorY
            sync.cursorY -= 1
            cursorIndex = 0
            // 越界（到底）由调用方通过 cursorY < minY 检测，不再设 sync.running
            if (acceptsAdvancedScanner) {
                recoverLayerPipes(completedY, range)
            }
        }
        val (x, z) = spiralXY(cursorIndex, range)
        sync.cursorX = x
        sync.cursorZ = z
    }

    /**
     * 回收指定 Y 层的所有水平采矿管道（保留中心柱）。
     * 高级采矿机每挖完一层后调用，将水平分支管道回收进管道槽。
     */
    private fun recoverLayerPipes(completedY: Int, range: Int) {
        val serverWorld = world as? ServerWorld ?: return
        for (dx in -(range + 2)..(range + 2)) {
            for (dz in -(range + 2)..(range + 2)) {
                if (dx == 0 && dz == 0) continue  // 保留中心柱
                val pipePos = BlockPos(pos.x + dx, completedY, pos.z + dz)
                if (serverWorld.getBlockState(pipePos).block !is MiningPipeBlock) continue
                if (!canAcceptRecoveredPipe()) return
                serverWorld.setBlockState(pipePos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
                knownPipePositions.remove(pipePos)
                insertRecoveredPipeIntoSlot()
            }
        }
        markDirty()
    }

    private fun getPipeEnergyCost(): Long {
        return getDrillBreakCost() ?: DEFAULT_PIPE_ENERGY
    }

    /**
     * 采矿机显式状态机。替换原散落 boolean（recoveringPipes/recyclingPipes/
     * manualStoppedForRecovery/redstoneChangeRequired）。
     * 转移只通过 tick() 的 when 分支返回值发生（外部入口方法除外，见 spec §3）。
     */
    private enum class MinerState {
        /** 等待自动恢复（普通机）；或红石未激活（高级机）。 */
        IDLE,
        /** 仅高级机：管道回收完成，等红石信号变化才重启。 */
        REDSTONE_WAITING,
        /** 终局回收（普通机到底 / 玩家手动 / 高级机回收）。 */
        PIPE_RECOVERING,
        /** 仅普通机：缺管时回收分支管道。 */
        PIPE_RECYCLING,
        /** 正常工作：扫描+挖掘，内部含 pendingBreakEnergy 子阶段。 */
        SCANNING
    }

    private enum class PipeReachResult {
        REACHED,
        RETRY_LATER,
        UNREACHABLE
    }

    /**
     * 处理管道回收队列的一格。
     * @return true = 本 tick 回收了一格（队列可能仍非空）；false = 队列已空，回收完毕
     *
     * 注意：本方法只处理「回收一格」的副作用（setBlockState AIR + 入管槽），
     * 不再改 minerState / 旧 boolean。转移由调用方（tickPipeRecovering /
     * tickPipeRecycling）根据返回值决定。spec §4.2/§4.3。
     */
    private fun processPipeRecoveryTick(world: ServerWorld): Boolean {
        // 管道槽满时暂停回收，等待槽位空出来后继续。
        if (!canAcceptRecoveredPipe()) return false

        while (pendingPipeRecovery.isNotEmpty()) {
            val pipePos = pendingPipeRecovery.removeFirst()
            val state = world.getBlockState(pipePos)
            if (state.block !is MiningPipeBlock) continue

            world.setBlockState(pipePos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
            knownPipePositions.remove(pipePos)
            insertRecoveredPipeIntoSlot()
            markDirty()
            return true
        }

        // 队列空：回收完毕。游标/状态重置由调用方做。
        return false
    }

    private fun canAcceptRecoveredPipe(): Boolean {
        return getPipeCount() < PIPE_SLOT_MAX_COUNT
    }

    private fun insertRecoveredPipeIntoSlot() {
        setPipeCount(getPipeCount() + 1)
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

        // 遇到流体：抽入内部储罐，储罐满则等待
        val fluidState = world.getFluidState(pipePos)
        if (!fluidState.isEmpty) {
            if (!handleFluidBlockForPipe(world, pipePos, fluidState)) {
                sync.fluidBlocked = 1
                return false
            }
        }

        // 重新读取方块状态（流体可能已被清除）
        val stateAfterFluid = world.getBlockState(pipePos)
        if (!stateAfterFluid.isAir && stateAfterFluid.block !is MiningPipeBlock) {
            if (stateAfterFluid.getHardness(world, pipePos) < 0f) return false
            if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, pipePos, ownerUuid)) return false
            world.breakBlock(pipePos, false)
        }

        refreshPipeBudget()
        if (pipePlacementBudget <= 0) return false

        val pipeEnergy = getPipeEnergyCost()
        if (sync.consumeEnergy(pipeEnergy) <= 0L) return false

        findPipeInInventory() ?: run {
            if (triggerPipeRecycling()) {
                pipeRecyclingRequested = true
            } else {
                haltRequested = true
            }
            return false
        }
        takePipes(1)

        world.setBlockState(pipePos, MiningPipeBlock::class.instance().defaultState, Block.NOTIFY_ALL)
        knownPipePositions.add(pipePos)
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

            // 遇到不可破坏方块（基岩等），视为已到底（通过侧信道传递，不设 sync.running）
            val state = serverWorld.getBlockState(checkPos)
            if (!state.isAir && state.block !is MiningPipeBlock && state.getHardness(serverWorld, checkPos) < 0f) {
                verticalHitBedrock = true
                return true
            }

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

        val _epNs = System.nanoTime()
        val starts = collectExistingPipeStarts3D(serverWorld, targetPos)
        if (starts.isEmpty()) {
            logPathPerf("no_starts", targetPos, _epNs, "starts=0")
            return PipeReachResult.UNREACHABLE
        }

        val pathToPlace = findBestPath3D(serverWorld, starts, goalCandidates, targetPos)
        if (pathToPlace == null) {
            logPathPerf("no_path", targetPos, _epNs, "starts=${starts.size}")
            return PipeReachResult.UNREACHABLE
        }
        for (pipePos in pathToPlace) {
            if (!tryPlacePipeAt(serverWorld, pipePos)) {
                logPathPerf("pipe_fail", targetPos, _epNs, "starts=${starts.size}")
                return PipeReachResult.RETRY_LATER
            }
        }
        return PipeReachResult.REACHED
    }

    private fun collectExistingPipeStarts3D(world: ServerWorld, targetPos: BlockPos): List<BlockPos> {
        val _csNs = System.nanoTime()
        val dx = abs(targetPos.x - pos.x)
        val dz = abs(targetPos.z - pos.z)
        val range = maxOf(dx, dz) + 2
        val minX = pos.x - range
        val maxX = pos.x + range
        val minZ = pos.z - range
        val maxZ = pos.z + range
        val minY = targetPos.y
        val maxY = minOf(targetPos.y + 6, maxOf(pos.y - 1, targetPos.y) + 2)

        val currentTick = world.time
        // 分批清理：每 600 tick 最多检查 32 条，避免单次遍历全量缓存
        if (currentTick - lastPipeCacheCleanupTick >= 600L) {
            val iter = knownPipePositions.iterator()
            var checked = 0
            while (iter.hasNext() && checked < 32) {
                if (world.getBlockState(iter.next()).block !is MiningPipeBlock) iter.remove()
                checked++
            }
            lastPipeCacheCleanupTick = currentTick
        }
        // 首次加载或缓存失效时，从世界重建缓存
        if (pipeCacheDirty) {
            for (x in minX..maxX) {
                for (yy in minY..maxY) {
                    for (z in minZ..maxZ) {
                        val p = BlockPos(x, yy, z)
                        if (world.getBlockState(p).block is MiningPipeBlock) knownPipePositions.add(p)
                    }
                }
            }
            pipeCacheDirty = false
        }

        // 从缓存中筛选搜索范围内的管道
        val starts = ArrayList<BlockPos>()
        for (pipePos in knownPipePositions) {
            if (pipePos.x in minX..maxX && pipePos.y in minY..maxY && pipePos.z in minZ..maxZ) {
                starts.add(pipePos)
            }
        }
        val rawStartCount = starts.size
        if (starts.size > 64) {
            val tx = targetPos.x
            val tz = targetPos.z
            starts.sortWith(compareBy<BlockPos> { abs(it.x - tx) + abs(it.z - tz) })
            starts.subList(64, starts.size).clear()
        }
        val csUs = (System.nanoTime() - _csNs) / 1_000
        if (csUs >= 1000 || rawStartCount > 100 || knownPipePositions.size > 500) {
            logger.debug("[{}] collectPipeStarts: cached={} found={}/{} elapsed={}us target=({},{},{})",
                blockKey, knownPipePositions.size, starts.size, rawStartCount, csUs, targetPos.x, targetPos.y, targetPos.z)
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

        val _pfNs = System.nanoTime()
        val minX = minOf(pos.x, targetPos.x) - (abs(targetPos.x - pos.x) + 2)
        val maxX = maxOf(pos.x, targetPos.x) + (abs(targetPos.x - pos.x) + 2)
        val minZ = minOf(pos.z, targetPos.z) - (abs(targetPos.z - pos.z) + 2)
        val maxZ = maxOf(pos.z, targetPos.z) + (abs(targetPos.z - pos.z) + 2)
        val minY = targetPos.y
        val maxY = minOf(targetPos.y + 6, maxOf(pos.y - 1, targetPos.y) + 2)

        val goalSet = goals.toHashSet()
        val startSet = starts.filter {
            it.x in minX..maxX && it.y in minY..maxY && it.z in minZ..maxZ && isExistingPipe(world, it)
        }.toHashSet()
        if (startSet.isEmpty()) return null

        val gScore = HashMap<BlockPos, Int>()
        val prev = HashMap<BlockPos, BlockPos>()
        val pq = java.util.PriorityQueue<IntArray>(64) { a, b -> a[0].compareTo(b[0]) }

        for (start in startSet) {
            val h = minHeuristic(start, goalSet)
            pq.add(intArrayOf(h, 0, start.x, start.y, start.z))
            gScore[start] = 0
        }

        val dirs = arrayOf(
            BlockPos(1, 0, 0), BlockPos(-1, 0, 0),
            BlockPos(0, 0, 1), BlockPos(0, 0, -1),
            BlockPos(0, 1, 0), BlockPos(0, -1, 0)
        )

        var explored = 0
        while (pq.isNotEmpty()) {
            val cur = pq.poll()!!
            explored++
            if (explored > MAX_PATH_SEARCH_NODES) break
            val curG = cur[1]
            val curPos = BlockPos(cur[2], cur[3], cur[4])
            val storedG = gScore[curPos]
            if (storedG != null && curG > storedG) continue  // 跳过过时条目（lazy deletion）

            if (curPos in goalSet) {
                val reversed = ArrayList<BlockPos>()
                var p: BlockPos? = curPos
                while (p != null && p !in startSet) {
                    reversed.add(p)
                    p = prev[p]
                }
                if (p == null) return null
                reversed.reverse()
                val pfUs = (System.nanoTime() - _pfNs) / 1_000
                if (pfUs >= 500 || explored > 500) {
                    logger.debug("[{}] A* found: explored={} starts={} elapsed={}us target=({},{},{})",
                        blockKey, explored, starts.size, pfUs, targetPos.x, targetPos.y, targetPos.z)
                }
                return reversed
            }

            for (d in dirs) {
                val nx = curPos.x + d.x
                val ny = curPos.y + d.y
                val nz = curPos.z + d.z
                if (nx !in minX..maxX || ny !in minY..maxY || nz !in minZ..maxZ) continue
                val next = BlockPos(nx, ny, nz)
                if (!canTraverseForPipe(world, next)) continue
                val newG = curG + (if (isExistingPipe(world, next)) 0 else 1)
                val oldG = gScore[next]
                if (oldG != null && newG >= oldG) continue
                gScore[next] = newG
                prev[next] = curPos
                pq.add(intArrayOf(newG + minHeuristic(next, goalSet), newG, nx, ny, nz))
            }
        }

        val pfUs = (System.nanoTime() - _pfNs) / 1_000
        if (pfUs >= 2000 || explored > 2000) {
            logger.debug("[{}] A* fail: explored={}/{} starts={} elapsed={}us target=({},{},{})",
                blockKey, explored, MAX_PATH_SEARCH_NODES, starts.size, pfUs,
                targetPos.x, targetPos.y, targetPos.z)
        }
        return null
    }

    /** 最近目标候选的曼哈顿距离（A* 启发函数下界） */
    private fun minHeuristic(pos: BlockPos, goals: Set<BlockPos>): Int {
        var minH = Int.MAX_VALUE
        for (g in goals) {
            val h = abs(pos.x - g.x) + abs(pos.y - g.y) + abs(pos.z - g.z)
            if (h < minH) minH = h
        }
        return minH
    }

    private fun isExistingPipe(world: ServerWorld, pos: BlockPos): Boolean {
        return world.getBlockState(pos).block is MiningPipeBlock
    }

    private fun logPathPerf(reason: String, targetPos: BlockPos, startNs: Long, extra: String = "") {
        val us = (System.nanoTime() - startNs) / 1_000
        if (us >= 2000) {
            logger.debug("[{}] ensurePipe: {} elapsed={}us target=({},{},{}){}",
                blockKey, reason, us, targetPos.x, targetPos.y, targetPos.z,
                if (extra.isNotEmpty()) " $extra" else "")
        }
    }

    private fun canTraverseForPipe(world: ServerWorld, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        if (state.block is MiningPipeBlock) return true
        if (state.isAir) return true
        if (!state.fluidState.isEmpty) return true  // 流体可穿越，铺设时再抽走
        return state.getHardness(world, pos) >= 0f
    }

    /**
     * 处理管道铺设遇到的流体方块。
     * - 源头：直接抽入储罐
     * - 流动：BFS 找到源头并抽走，再清除当前位置
     * @return true 成功处理，false 储罐已满需等待
     */
    private fun handleFluidBlockForPipe(world: ServerWorld, pos: BlockPos, fluidState: FluidState): Boolean {
        val fluid = fluidState.fluid
        val sourceFluid = when (fluid) {
            Fluids.WATER, Fluids.FLOWING_WATER -> Fluids.WATER
            Fluids.LAVA, Fluids.FLOWING_LAVA -> Fluids.LAVA
            else -> return true  // 未知流体直接放行
        }

        if (!fluidTankInternal.hasSpaceForBucket(sourceFluid)) return false

        if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, pos, ownerUuid, ic2_120.integration.ftbchunks.ClaimProtection.EDIT_FLUID)) return false

        if (fluidState.isStill) {
            fluidTankInternal.insertBucket(sourceFluid)
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        } else {
            val sourcePos = findFluidSourceBFS(world, pos, sourceFluid)
            if (sourcePos != null) {
                if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, sourcePos, ownerUuid, ic2_120.integration.ftbchunks.ClaimProtection.EDIT_FLUID)) return false
                fluidTankInternal.insertBucket(sourceFluid)
                world.setBlockState(sourcePos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
            }
            // 清除当前位置的流动流体
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        }
        return true
    }

    /** BFS 搜索同类型流体的源头方块（isStill），上限 256 节点。 */
    private fun findFluidSourceBFS(world: ServerWorld, start: BlockPos, sourceFluid: Fluid): BlockPos? {
        val visited = HashSet<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(start)
        visited.add(start)
        val dirs = arrayOf(
            BlockPos(1, 0, 0), BlockPos(-1, 0, 0),
            BlockPos(0, 1, 0), BlockPos(0, -1, 0),
            BlockPos(0, 0, 1), BlockPos(0, 0, -1)
        )
        var explored = 0
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            explored++
            if (explored > 256) break
            val fs = world.getFluidState(cur)
            if (fs.isEmpty) continue
            if (fs.isStill && fs.fluid == sourceFluid) return cur
            for (d in dirs) {
                val next = cur.add(d)
                if (!visited.add(next)) continue
                val nfs = world.getFluidState(next)
                if (nfs.isEmpty) continue
                val nf = nfs.fluid
                val matches = (sourceFluid == Fluids.WATER && (nf == Fluids.WATER || nf == Fluids.FLOWING_WATER))
                    || (sourceFluid == Fluids.LAVA && (nf == Fluids.LAVA || nf == Fluids.FLOWING_LAVA))
                if (matches) queue.add(next)
            }
        }
        return null
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
            logger.debug(
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
                getPipeCount(),
                sync.running,
                getStack(SLOT_SCANNER).item,
                getStack(SLOT_DRILL).item
            )
            lastStallLogTick = world.time
        }
    }

    private fun logDecision(world: World, reason: String, targetPos: BlockPos) {
        if (world.time - lastDecisionLogTick < 10L) return
        logger.debug(
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
            getPipeCount(),
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
        return if (getPipeCount() > 0) SLOT_PIPE else null
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

        val filters = (SLOT_ITEM_START..SLOT_ITEM_END)
            .map { getStack(it) }
            .mapNotNull { stack -> stack.item as? net.minecraft.item.BlockItem }
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
        world.syncWorldEvent(2001, targetPos, Block.getRawIdFromState(state))
        world.setBlockState(targetPos, net.minecraft.block.Blocks.AIR.defaultState, 3)

        if (acceptsAdvancedScanner) {
            for (drop in drops) {
                addToCache(drop)
            }
            tryEjectCache(world)
        } else {
            for (drop in drops) {
                insertItemIntoSlots(drop, SLOT_ITEM_INDICES)
            }

            EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_ITEM_INDICES)
        }

        markDirty()
    }

    private fun addToCache(stack: ItemStack) {
        var remaining = stack.copy()
        for (cached in itemCache) {
            if (remaining.isEmpty) break
            if (ItemStack.canCombine(cached, remaining)) {
                val room = cached.maxCount - cached.count
                if (room > 0) {
                    val move = minOf(room, remaining.count)
                    cached.increment(move)
                    remaining.decrement(move)
                    cacheItemCount += move
                }
            }
        }
        if (!remaining.isEmpty && cacheItemCount < MAX_CACHE_ITEMS) {
            itemCache.add(remaining.copy())
            cacheItemCount += remaining.count
        }
    }

    private fun tryEjectCache(world: ServerWorld) {
        if (itemCache.isEmpty()) return
        for (direction in Direction.entries) {
            if (itemCache.isEmpty()) break
            val neighborPos = pos.offset(direction)
            val neighborBe = world.getBlockEntity(neighborPos)
            if (neighborBe is Inventory) {
                val inv = neighborBe as Inventory
                val iter = itemCache.iterator()
                while (iter.hasNext()) {
                    val stack = iter.next()
                    for (slot in 0 until inv.size()) {
                        if (stack.isEmpty) { iter.remove(); break }
                        val existing = inv.getStack(slot)
                        if (existing.isEmpty) {
                            if (inv.isValid(slot, stack)) {
                                inv.setStack(slot, stack.copy())
                                cacheItemCount -= stack.count
                                stack.count = 0
                                iter.remove()
                                break
                            }
                        } else if (ItemStack.canCombine(existing, stack)) {
                            val room = existing.maxCount - existing.count
                            if (room > 0) {
                                val move = minOf(room, stack.count)
                                existing.increment(move)
                                stack.decrement(move)
                                cacheItemCount -= move
                                if (stack.isEmpty) { iter.remove(); break }
                            }
                        }
                    }
                }
                neighborBe.markDirty()
            }
        }
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
        if (acceptsAdvancedScanner) {
            val internalTool = ItemStack(Items.NETHERITE_PICKAXE)
            if (sync.silkTouch != 0) {
                internalTool.addEnchantment(Enchantments.SILK_TOUCH, 1)
            }
            return internalTool
        }

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
        if (acceptsAdvancedScanner) {
            val tool = ItemStack(Items.NETHERITE_PICKAXE)
            return state.isToolRequired.not() || tool.isSuitableFor(state)
        }

        val tool = getLootToolStack()
        return state.isToolRequired.not() || tool.isSuitableFor(state)
    }

    /** 将物品插入指定槽位数组，溢出掉落地面。 */
    private fun insertItemIntoSlots(stack: ItemStack, slots: IntArray) {
        var remaining = stack.copy()
        for (slot in slots) {
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
            FluidStorage.SIDED.registerForBlockEntity(
                { be, _ -> be.getFluidStorageForSide(null) },
                MinerBlockEntity::class.type()
            )
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
            FluidStorage.SIDED.registerForBlockEntity(
                { be, _ -> be.getFluidStorageForSide(null) },
                AdvancedMinerBlockEntity::class.type()
            )
            fluidLookupRegistered = true
        }
    }
}
