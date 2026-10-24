package ic2_120.content.block.machines

import ic2_120.content.AdjacentEnergyTransferComponent
import kotlin.math.ceil
import ic2_120.content.block.AdvancedMinerBlock
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.block.IClaimSensitive
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MinerBlock
import ic2_120.content.block.MiningPipeBlock
import ic2_120.content.energy.EnergyTier
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
import ic2_120.content.screen.MinerScreenHandler
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.sync.MinerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.integration.ftbchunks.ClaimProtection
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import kotlin.math.max

/**
 * IC2 2.8 miner implementation.
 *
 * The inventory is intentionally project-specific. The normal miner uses a
 * vertical pipe, layer scan, and drill progress; the advanced miner performs
 * remote scanning on a fixed 20-tick work cycle.
 */
abstract class BaseMinerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    private val blockKey: String,
    private val baseTier: Int,
    private val advanced: Boolean
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine,
    IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, IClaimSensitive, ExtendedScreenHandlerFactory {

    override val activeProperty = BaseMinerBlock.ACTIVE
    override val soundConfig = MachineSoundConfig.operate("machine.miner.operate", 0.5f, 1.0f, 20)
    override fun getInventory(): Inventory = this
    override val tier: Int = baseTier
    override var speedMultiplier = 1f
    override var energyMultiplier = 1f
    override var capacityBonus = 0L
    override var voltageTierBonus = 0
    override var fluidPipeProviderEnabled = false
    override var fluidPipeReceiverEnabled = false
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSides = mutableSetOf<Direction>()
    override var fluidPipeReceiverSides = mutableSetOf<Direction>()
    override var fluidPipeEjectorCount = 0
    override var fluidPipePullingCount = 0

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

        const val NORMAL_CAPACITY = 1_000L
        const val ADVANCED_CAPACITY = 4_000_000L
        const val NORMAL_SCAN_COST = 50L
        const val ADVANCED_SCAN_COST = 250L
        const val ADVANCED_SCAN_STEP = 64L
        const val ADVANCED_MINE_COST = 512L
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private val syncedData = SyncedData()
    private var scanLevel = Int.MIN_VALUE
    private var scanRange = 0
    private var progressMode = -1
    private var advancedTicker = 0
    private var advancedTarget: BlockPos? = null
    private var advancedScanPos: BlockPos? = null
    private var normalPipeRecoveryTicks = 0
    /** 普通矿机到底标志：钻到不可破坏方块（基岩）或世界底部后自动进入管道回收，直到收完或 GUI 重启。 */
    private var normalReachedBottom = false
    private var resetWaitingForRedstone = false
    private var redstoneStateAtReset = false
    private var renderTarget: BlockPos? = null
    private var renderTargetTime = -1L
    private var normalPipeY: Int? = null
    // 保留现有方块/GUI 对高级机缓存状态的访问；远程扫描流程不依赖额外寻路状态。
    val itemCache = mutableListOf<ItemStack>()
    var cacheItemCount = 0

    private val fluidTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant() = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant) = FluidConstants.BUCKET
        override fun canInsert(variant: FluidVariant) = !variant.isBlank && (variant.fluid == Fluids.WATER || variant.fluid == Fluids.LAVA)
        override fun canExtract(variant: FluidVariant) = true
        override fun onFinalCommit() = markDirty()
    }
    val fluidTank: Storage<FluidVariant> = fluidTankInternal
    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant> = fluidTank

    @RegisterEnergy
    val sync = MinerSync(
        syncedData,
        { world?.time },
        { baseCapacity() + capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(baseTier + voltageTierBonus) },
        baseCapacity = baseCapacity()
    )

    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory,
        { maxCountPerStack },
        { slot, stack -> isValid(slot, stack) },
        insertRoutes = buildList {
            add(ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = ::isAllowedUpgrade))
            add(ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isDischargeItem(it) }, maxPerSlot = 1))
            add(ItemInsertRoute(intArrayOf(SLOT_SCANNER), matcher = { isValid(SLOT_SCANNER, it) }, maxPerSlot = 1))
            add(ItemInsertRoute(intArrayOf(SLOT_DRILL), matcher = { isValid(SLOT_DRILL, it) }, maxPerSlot = 1))
            add(ItemInsertRoute(SLOT_ITEM_INDICES, matcher = { advanced || isNormalOutputItem(it) }, maxPerSlot = if (advanced) 1 else maxCountPerStack))
            if (!advanced) {
                add(ItemInsertRoute(intArrayOf(SLOT_PIPE), matcher = { it.item === MiningPipeBlock::class.item() }, maxPerSlot = PIPE_SLOT_MAX_COUNT))
            }
        },
        extractSlots = (if (advanced) intArrayOf() else SLOT_ITEM_INDICES) + SLOT_UPGRADE_INDICES + intArrayOf(SLOT_SCANNER, SLOT_DRILL, SLOT_DISCHARGING) + (if (advanced) intArrayOf() else intArrayOf(SLOT_PIPE)) + SLOT_OUTPUT_INDICES,
        markDirty = { markDirty() }
    )
    override val routedItemStorage get() = itemStorage
    val pipeStorage = PipeSlotStorage(inventory, SLOT_PIPE, PIPE_SLOT_MAX_COUNT, { MiningPipeBlock::class.item() }, { markDirty() })
    @RegisterItemStorage
    val combinedItemStorage = CombinedMinerItemStorage(itemStorage, pipeStorage)

    private val discharger = BatteryDischargerComponent(this, SLOT_DISCHARGING, { baseTier }, { sync.amount < sync.getEffectiveCapacity() })
    // 扫描仪充电：速率默认受扫描仪自身电压等级限制（OD 32 / OV 512 EU/t），超频后回充跟不上扫描耗电（1.6^n）会卡住。
    // 高压升级同样提升扫描仪充电电压等级：每装入一个高压升级，充电速率按 4 倍提升
    // （等级门限仍按机器基础等级，普通采矿机不用 OV，OV 只进高级采矿机，无门限问题）。
    private val scannerCharger = BatteryChargerComponent(
        this,
        SLOT_SCANNER,
        { baseTier },
        { sync.amount },
        { sync.consumeEnergy(it) },
        transferRateProvider = { stack ->
            val tool = stack.item as? IElectricTool
            val tier = (tool?.tier ?: 1) + TransformerUpgradeComponent.countUpgrades(this, SLOT_UPGRADE_INDICES)
            EnergyTier.euPerTickFromTier(tier)
        },
        canChargeNow = { sync.amount > 0 }
    )
    private val drillCharger = BatteryChargerComponent(this, SLOT_DRILL, { baseTier }, { sync.amount }, { sync.consumeEnergy(it) }, canChargeNow = { !advanced && sync.amount > 0 })
    private val adjacentEnergy = AdjacentEnergyTransferComponent(this, sync)

    init { sync.cursorY = pos.y - 1 }

    private fun baseCapacity() = if (advanced) ADVANCED_CAPACITY else NORMAL_CAPACITY
    private fun isDischargeItem(stack: ItemStack) = !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
    private fun isNormalOutputItem(stack: ItemStack) = !stack.isEmpty && stack.item !== MiningPipeBlock::class.item() && stack.item !is IUpgradeItem && stack.item !is Drill && stack.item !is DiamondDrill && stack.item !is IridiumDrill && stack.item !is OdScannerItem && stack.item !is AdvancedScannerItem && stack.item !is IBatteryItem

    override fun size() = INVENTORY_SIZE
    override fun isEmpty() = inventory.all { it.isEmpty }
    override fun getStack(slot: Int) = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun getMaxCountPerStack() = PIPE_SLOT_MAX_COUNT
    override fun removeStack(slot: Int, amount: Int) = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int) = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity) = Inventory.canPlayerUse(this, player)
    override fun setStack(slot: Int, stack: ItemStack) {
        val max = when {
            slot == SLOT_PIPE -> PIPE_SLOT_MAX_COUNT
            slot == SLOT_SCANNER || slot == SLOT_DRILL || slot == SLOT_DISCHARGING -> 1
            advanced && slot in SLOT_ITEM_INDICES -> 1
            else -> maxCountPerStack
        }
        if (stack.count > max) stack.count = max
        inventory[slot] = stack
        markDirty()
    }
    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_SCANNER -> stack.item is OdScannerItem || stack.item is AdvancedScannerItem
        SLOT_DRILL -> !advanced && (stack.item is Drill || stack.item is DiamondDrill || stack.item is IridiumDrill)
        SLOT_DISCHARGING -> isDischargeItem(stack)
        in SLOT_ITEM_INDICES -> if (advanced) !stack.isEmpty else isNormalOutputItem(stack)
        in SLOT_UPGRADE_INDICES -> isAllowedUpgrade(stack)
        SLOT_PIPE -> stack.item === MiningPipeBlock::class.item()
        else -> false
    }
    private fun isAllowedUpgrade(stack: ItemStack): Boolean = if (advanced) {
        stack.item is OverclockerUpgrade || stack.item is TransformerUpgrade || stack.item is RedstoneInverterUpgrade || stack.item is EnergyStorageUpgrade || stack.item is EjectorUpgrade || stack.item is FluidEjectorUpgrade
    } else {
        stack.item is EnergyStorageUpgrade || stack.item is TransformerUpgrade || stack.item is EjectorUpgrade || stack.item is PullingUpgrade || stack.item is FluidEjectorUpgrade || stack.item is FluidPullingUpgrade
    }

    fun getPipeCount() = getStack(SLOT_PIPE).takeIf { it.item === MiningPipeBlock::class.item() }?.count ?: 0
    fun insertPipesFromStack(stack: ItemStack, requestedAmount: Int = stack.count): Int {
        val move = minOf(requestedAmount, stack.count, PIPE_SLOT_MAX_COUNT - getPipeCount())
        if (move <= 0 || stack.item !== MiningPipeBlock::class.item()) return 0
        val target = getStack(SLOT_PIPE)
        if (target.isEmpty) inventory[SLOT_PIPE] = ItemStack(MiningPipeBlock::class.item(), move) else target.increment(move)
        stack.decrement(move); markDirty(); return move
    }
    fun takePipes(amount: Int): ItemStack {
        val moved = minOf(amount, getPipeCount())
        if (moved <= 0) return ItemStack.EMPTY
        val result = ItemStack(MiningPipeBlock::class.item(), moved)
        removeStack(SLOT_PIPE, moved); return result
    }

    override fun claimBlockedTargets(world: World, pos: BlockPos, state: BlockState): List<BlockPos> {
        val target = renderTarget ?: return emptyList()
        return if (!world.getBlockState(target).isAir && ClaimProtection.isProtected(world, target, ownerUuid)) listOf(target) else emptyList()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        tickComponents()
        val server = world as? ServerWorld ?: return
        val working = if (resetWaitingForRedstone) {
            val currentRedstone = server.isReceivingRedstonePower(pos)
            if (currentRedstone == redstoneStateAtReset) {
                false
            } else {
                resetWaitingForRedstone = false
                advancedTicker = 0
                if (advanced) tickAdvanced(server) else tickNormal(server)
            }
        } else if (advanced) tickAdvanced(server) else tickNormal(server)
        setActiveState(server, pos, state, working)
        sync.running = if (working) 1 else 0
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        sync.pipeCount = getPipeCount()
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        sync.syncCurrentTickFlow()
    }

    private fun tickComponents() {
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        adjacentEnergy.tick()
        if (!advanced) {
            // 弹出升级不依赖挖掘动作：罢工/回收/闲置时也持续弹出产物与流体 (#26)
            world?.let { w ->
                EjectorUpgradeComponent.ejectIfUpgraded(w, pos, this, SLOT_UPGRADE_INDICES, SLOT_ITEM_INDICES)
                if (fluidPipeProviderEnabled) {
                    FluidPipeUpgradeComponent.ejectFluidToNeighbors(w, pos, fluidTankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
                }
            }
        }
        val available = minOf(sync.getEffectiveCapacity() - sync.amount, sync.getEffectiveMaxInsertPerTick())
        if (available > 0) discharger.tick(available).also { if (it > 0) sync.insertEnergy(it) }
        if (sync.amount > 0) scannerCharger.tick()
        if (sync.amount > 0) drillCharger.tick()
    }

    private fun scannerType(): ScannerType? = when (val item = getStack(SLOT_SCANNER).item) {
        is AdvancedScannerItem -> ScannerType.OV
        is OdScannerItem -> ScannerType.OD
        else -> null
    }
    private fun useScanner(stack: ItemStack, amount: Long): Boolean {
        val tool = stack.item as? IElectricTool ?: return false
        if (tool.getEnergy(stack) < amount) return false
        tool.setEnergy(stack, tool.getEnergy(stack) - amount)
        return true
    }

    /** Process one vertical pipe level, then scan one horizontal layer. */
    private fun tickNormal(world: ServerWorld): Boolean {
        if (normalReachedBottom) return recoverNormalPipes(world)
        val drill = getStack(SLOT_DRILL)
        if (drill.isEmpty) return recoverNormalPipes(world)
        val scanner = getStack(SLOT_SCANNER)
        val type = scannerType() ?: return false
        if (advanced) return false
        var y = sync.cursorY.takeIf { it < pos.y } ?: (pos.y - 1).also { sync.cursorY = it }
        val firstPipeGap = findFirstNormalPipeGap(world, y)
        if (firstPipeGap != null) {
            y = firstPipeGap
            sync.cursorY = y
            sync.cursorX = 0
            sync.cursorZ = 0
            scanLevel = Int.MIN_VALUE
        }
        val pipe = BlockPos(pos.x, y, pos.z)
        if (world.getBlockState(pipe).block !is MiningPipeBlock) {
            val pipeState = world.getBlockState(pipe)
            if (!pipeState.isAir) {
                // 遇到不可破坏方块（基岩等）视为到底：不再钻穿，自动转入管道回收 (#26)
                if (pipeState.getHardness(world, pipe) < 0f) {
                    normalReachedBottom = true
                    markDirty()
                    return recoverNormalPipes(world)
                }
                if (!canUseDrill(pipeState)) return false
                mineNormalBlock(world, pipe, drill)
                return true
            }
            if (getPipeCount() <= 0 || sync.amount < 3L) return false
            if (ClaimProtection.isProtected(world, pipe, ownerUuid)) return false
            val placed = world.setBlockState(pipe, MiningPipeBlock::class.instance().defaultState, Block.NOTIFY_ALL)
            if (!placed || world.getBlockState(pipe).block !is MiningPipeBlock) return false
            sync.consumeEnergy(3L)
            takePipes(1)
            normalPipeY = y
            return true
        }

        if (scanLevel != y) {
            val scanCost = if (type == ScannerType.OD) NORMAL_SCAN_COST else ADVANCED_SCAN_COST
            if (!useScanner(scanner, scanCost)) return false
            scanLevel = y
            scanRange = if (type == ScannerType.OD) 3 else 6
            sync.cursorX = 0; sync.cursorZ = 0
        }
        val target = findNormalTarget(world, y, scanRange) ?: run {
            sync.cursorY = y - 1
            scanLevel = Int.MIN_VALUE
            if (sync.cursorY < world.bottomY) {
                normalReachedBottom = true
                markDirty()
                return recoverNormalPipes(world)
            }
            return true
        }
        sync.cursorX = target.x - pos.x
        sync.cursorZ = target.z - pos.z
        if (!mineNormalBlock(world, findNormalPathBlock(world, target), drill)) return true
        return true
    }

    /** Keep the deployed pipe column continuous before continuing deeper. */
    private fun findFirstNormalPipeGap(world: ServerWorld, cursorY: Int): Int? {
        for (y in (pos.y - 1) downTo cursorY) {
            val state = world.getBlockState(BlockPos(pos.x, y, pos.z))
            if (state.block !is MiningPipeBlock) return y
        }
        return null
    }

    /** Remove one pipe from the bottom upward (auto after reaching bottom, or manual with drill removed). */
    private fun recoverNormalPipes(world: ServerWorld): Boolean {
        if (advanced) return false
        val pipe = findDeepestNormalPipe(world) ?: run {
            // 管道全部收回：卸下钻头（或 GUI 重启）后复位到底标志，允许下一轮采矿
            if (normalReachedBottom && getStack(SLOT_DRILL).isEmpty) {
                normalReachedBottom = false
                markDirty()
            }
            return false
        }
        if (getPipeCount() >= PIPE_SLOT_MAX_COUNT) return false
        if (ClaimProtection.isProtected(world, pipe, ownerUuid, ClaimProtection.EDIT_BLOCK)) return false
        if (normalPipeRecoveryTicks < 20) {
            if (sync.amount < 3L) return false
            sync.consumeEnergy(3L)
            normalPipeRecoveryTicks++
            return true
        }
        normalPipeRecoveryTicks = 0
        world.setBlockState(pipe, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        insertPipesFromStack(ItemStack(MiningPipeBlock::class.item()), 1)
        return true
    }

    private fun findDeepestNormalPipe(world: ServerWorld): BlockPos? {
        // 自底向上回收：先拆最底部的管道（钻头端），列保持连续，不会出现隔空缺口 (#26)
        for (y in world.bottomY until pos.y) {
            val pipe = BlockPos(pos.x, y, pos.z)
            if (world.getBlockState(pipe).block is MiningPipeBlock) return pipe
        }
        return null
    }

    private fun findNormalTarget(world: ServerWorld, y: Int, range: Int): BlockPos? {
        for (x in pos.x - range..pos.x + range) for (z in pos.z - range..pos.z + range) {
            val p = BlockPos(x, y, z); val state = world.getBlockState(p)
            if (!state.isAir && state.getHardness(world, p) >= 0f && isOreLike(world, p, state) && canUseDrill(state)) return p
        }
        return null
    }

    /** Clear the first blocking block on the horizontal route. */
    private fun findNormalPathBlock(world: ServerWorld, target: BlockPos): BlockPos {
        var x = pos.x
        var z = pos.z
        val dx = kotlin.math.abs(target.x - x)
        val sx = if (x < target.x) 1 else -1
        val dz = -kotlin.math.abs(target.z - z)
        val sz = if (z < target.z) 1 else -1
        var error = dx + dz
        while (x != target.x || z != target.z) {
            val twice = 2 * error
            if (twice > dz) { error += dz; x += sx }
            if (twice < dx) { error += dx; z += sz }
            val candidate = BlockPos(x, target.y, z)
            val candidateState = world.getBlockState(candidate)
            if (!candidateState.isAir && candidateState.block !is MiningPipeBlock) return candidate
        }
        return target
    }

    private fun mineNormalBlock(world: ServerWorld, target: BlockPos, drill: ItemStack): Boolean {
        val mode = when (drill.item) { is Drill -> 0; is DiamondDrill -> 1; is IridiumDrill -> 2; else -> return false }
        val targetState = world.getBlockState(target)
        // 不可破坏方块（基岩等）一律不挖：竖直下探与水平路径共用此守卫 (#26)
        if (targetState.getHardness(world, target) < 0f) return false
        val (euPerTick, duration, toolCost) = when (mode) { 0 -> Triple(60L, 20, 50L); 1 -> Triple(100L, 10, 80L); else -> Triple(800L, 5, 800L) }
        if (progressMode != mode) { progressMode = mode; sync.progressTicks = 0 }
        val depthCost = max(0, pos.y - target.y) * 2L
        val tool = drill.item as IElectricTool
        if (sync.progressTicks >= duration && (tool.getEnergy(drill) < toolCost || sync.amount < depthCost)) return false
        if (sync.amount < euPerTick) return false
        sync.consumeEnergy(euPerTick); sync.progressTicks++
        if (sync.progressTicks < duration) return true
        tool.setEnergy(drill, tool.getEnergy(drill) - toolCost)
        sync.consumeEnergy(depthCost)
        harvest(world, target, getLootTool(false)); sync.progressTicks = 0; progressMode = -1
        return true
    }

    /** Remote scan with a 20-tick work period; no pipe or drill is required. */
    private fun tickAdvanced(world: ServerWorld): Boolean {
        if (world.isReceivingRedstonePower(pos) == hasRedstoneInverted()) return false
        val scanner = getStack(SLOT_SCANNER); val type = scannerType() ?: return false
        val scannerTool = scanner.item as? IElectricTool ?: return false
        // 超频耗能惩罚：1.6^n 计入单次扫描/单次开采成本（向上取整，整数 EU）
        val scanStepCost = ceil(ADVANCED_SCAN_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
        val mineCost = ceil(ADVANCED_MINE_COST * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.amount < mineCost || scannerTool.getEnergy(scanner) < scanStepCost) return false
        advancedTicker++
        if (advancedTicker < 20) return true
        advancedTicker = 0
        val range = if (type == ScannerType.OV) 32 else 16
        if (advancedScanPos == null) advancedScanPos = BlockPos(pos.x - range - 1, pos.y - 1, pos.z - range)
        // 超频速度：扫描步数按 1.4286^n 指数缩放（替代旧的线性 5×(1+n)），耗能按 1.6^n 计（ceil 取整）
        var scans = max(1, ceil(5f * speedMultiplier).toInt())
        while (scans-- > 0) {
            val next = advanceAdvancedScan(range) ?: return true
            if (!useScanner(scanner, scanStepCost)) return false
            val state = world.getBlockState(next)
            if (!state.isAir && canMineAdvanced(world, next, state)) {
                advancedTarget = next
                if (sync.amount >= mineCost) {
                    sync.consumeEnergy(mineCost)
                    harvest(world, next, getLootTool(sync.silkTouch != 0))
                }
                return true
            }
        }
        return true
    }

    private fun hasRedstoneInverted() = SLOT_UPGRADE_INDICES.any { getStack(it).item is RedstoneInverterUpgrade }
    /** Count overclocker items including the full stack size in each upgrade slot. */
    private fun advanceAdvancedScan(range: Int): BlockPos? {
        val current = advancedScanPos ?: return null
        var x = current.x; var y = current.y; var z = current.z
        if (x < pos.x + range) x++ else if (z < pos.z + range) { x = pos.x - range; z++ } else { x = pos.x - range; z = pos.z - range; y-- }
        val bottomY = world?.bottomY ?: return null
        if (y < bottomY) { advancedScanPos = BlockPos(x, y, z); return null }
        val result = BlockPos(x, y, z); advancedScanPos = result; sync.cursorX = x - pos.x; sync.cursorY = y; sync.cursorZ = z - pos.z; return result
    }

    private fun isOreLike(world: World, target: BlockPos, state: BlockState): Boolean =
        state.block !is ic2_120.content.block.MachineBlock &&
            (state.isIn(net.minecraft.registry.tag.BlockTags.COAL_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.COPPER_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.DIAMOND_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.EMERALD_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.GOLD_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.IRON_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.LAPIS_ORES) ||
                state.isIn(net.minecraft.registry.tag.BlockTags.REDSTONE_ORES) ||
                state.isOf(net.minecraft.block.Blocks.ANCIENT_DEBRIS) ||
                Registries.BLOCK.getId(state.block).path.contains("ore") ||
                Registries.BLOCK.getId(state.block).path == "ancient_debris")
    private fun canUseDrill(state: BlockState): Boolean = !state.isToolRequired || getLootTool(false).isSuitableFor(state)
    private fun canMineAdvanced(world: ServerWorld, target: BlockPos, state: BlockState): Boolean {
        if (!state.fluidState.isEmpty || state.getHardness(world, target) < 0f) return false
        // IC2 OreValues excludes ordinary tile-entity blocks (machines, chests, etc.).
        if (state.block is ic2_120.content.block.MachineBlock || world.getBlockEntity(target) != null) return false
        val drops = Block.getDroppedStacks(state, world, target, world.getBlockEntity(target), null, getLootTool(sync.silkTouch != 0))
        if (drops.isEmpty()) return false
        val filters = SLOT_ITEM_INDICES.map { getStack(it) }.filter { !it.isEmpty }.toList()
        val blockStack = state.block.asItem().takeUnless { it === Items.AIR }?.let(::ItemStack)
        val matchesFilter = { filter: ItemStack ->
            (blockStack != null && ItemStack.canCombine(blockStack, filter)) ||
                drops.any { drop -> ItemStack.canCombine(drop, filter) }
        }
        // 原矿和掉落材料都可以作为过滤条件；黑名单为空 = 不排除任何目标，白名单为空 = 没有允许目标。
        if (filters.isEmpty()) return sync.mode != 0
        val matched = filters.any(matchesFilter)
        return if (sync.mode == 0) matched else !matched
    }

    private fun getLootTool(silk: Boolean): ItemStack {
        val drill = getStack(SLOT_DRILL).item
        val tool = if (advanced) ItemStack(Items.NETHERITE_PICKAXE) else when (drill) {
            is Drill -> ItemStack(Items.IRON_PICKAXE)
            is IridiumDrill -> ItemStack(Items.NETHERITE_PICKAXE)
            else -> ItemStack(Items.DIAMOND_PICKAXE)
        }
        if (silk) tool.addEnchantment(Enchantments.SILK_TOUCH, 1)
        else if (!advanced && drill is IridiumDrill) tool.addEnchantment(Enchantments.FORTUNE, 3)
        return tool
    }
    private fun harvest(world: ServerWorld, target: BlockPos, tool: ItemStack) {
        if (ClaimProtection.isProtected(world, target, ownerUuid, ClaimProtection.EDIT_BLOCK)) return
        val drops = Block.getDroppedStacks(world.getBlockState(target), world, target, world.getBlockEntity(target), null, tool)
        val hasEjector = advanced && SLOT_UPGRADE_INDICES.any { getStack(it).item is EjectorUpgrade }
        world.syncWorldEvent(2001, target, Block.getRawIdFromState(world.getBlockState(target)))
        world.setBlockState(target, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        for (drop in drops) {
            if (advanced && !hasEjector) {
                ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), drop)
            } else {
                insertDrop(drop)
            }
        }
        renderTarget = target; renderTargetTime = world.time
        val ejectSlots = if (advanced) SLOT_OUTPUT_INDICES else SLOT_ITEM_INDICES
        if (hasEjector || !advanced) {
            EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, ejectSlots)
        }
        markDirty()
    }
    private fun insertDrop(stack: ItemStack) {
        var rest = stack.copy()
        for (slot in if (advanced) SLOT_OUTPUT_INDICES else SLOT_ITEM_INDICES) {
            if (rest.isEmpty) break
            val current = getStack(slot)
            if (current.isEmpty) { setStack(slot, rest); rest = ItemStack.EMPTY }
            else if (ItemStack.canCombine(current, rest)) { val move = minOf(rest.count, current.maxCount - current.count); if (move > 0) { current.increment(move); rest.decrement(move) } }
        }
        if (!rest.isEmpty) ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), rest)
    }

    fun getRenderDrillTarget(time: Long): BlockPos? = renderTarget?.takeIf { time - renderTargetTime <= 8 }
    /** Reset the scan target and wait for the next redstone edge before resuming. */
    fun restartScan() {
        scanLevel = Int.MIN_VALUE
        sync.cursorX = 0; sync.cursorY = pos.y - 1; sync.cursorZ = 0
        sync.progressTicks = 0
        advancedScanPos = null; advancedTarget = null
        normalReachedBottom = false
        resetWaitingForRedstone = advanced
        redstoneStateAtReset = world?.isReceivingRedstonePower(pos) ?: false
        markDirty()
    }
    fun startPipeRecovery() { /* GUI compatibility; advanced mining does not use pipes. */ }
    fun toggleMode() { if (sync.running == 0) { sync.mode = if (sync.mode == 0) 1 else 0; markDirty() } }
    fun toggleSilkTouch() { if (sync.running == 0) { sync.silkTouch = if (sync.silkTouch == 0) 1 else 0; markDirty() } }

    private fun writeMinerNbt(nbt: NbtCompound) {
        nbt.putLong("EnergyStored", sync.amount); nbt.putInt("ScanLevel", scanLevel); nbt.putInt("ProgressMode", progressMode); nbt.putInt("AdvancedTicker", advancedTicker); nbt.putInt("NormalPipeRecoveryTicks", normalPipeRecoveryTicks); nbt.putBoolean("NormalReachedBottom", normalReachedBottom)
        nbt.putLong("RenderTargetTime", renderTargetTime); nbt.putBoolean("ResetWaitingForRedstone", resetWaitingForRedstone); nbt.putBoolean("RedstoneStateAtReset", redstoneStateAtReset); renderTarget?.let { nbt.putInt("RenderTargetX", it.x); nbt.putInt("RenderTargetY", it.y); nbt.putInt("RenderTargetZ", it.z) }
    }
    override fun readNbt(nbt: NbtCompound) { super.readNbt(nbt); Inventories.readNbt(nbt, inventory); syncedData.readNbt(nbt); sync.amount = nbt.getLong("EnergyStored"); sync.syncCommittedAmount(); scanLevel = nbt.getInt("ScanLevel"); progressMode = nbt.getInt("ProgressMode"); advancedTicker = nbt.getInt("AdvancedTicker"); normalPipeRecoveryTicks = nbt.getInt("NormalPipeRecoveryTicks").coerceIn(0, 19); normalReachedBottom = nbt.getBoolean("NormalReachedBottom"); resetWaitingForRedstone = nbt.getBoolean("ResetWaitingForRedstone"); redstoneStateAtReset = nbt.getBoolean("RedstoneStateAtReset"); renderTargetTime = nbt.getLong("RenderTargetTime"); if (nbt.contains("RenderTargetX")) renderTarget = BlockPos(nbt.getInt("RenderTargetX"), nbt.getInt("RenderTargetY"), nbt.getInt("RenderTargetZ")) }
    override fun writeNbt(nbt: NbtCompound) { super.writeNbt(nbt); Inventories.writeNbt(nbt, inventory); syncedData.writeNbt(nbt); writeMinerNbt(nbt) }
    override fun toInitialChunkDataNbt() = createNbt()
    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)
    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) { buf.writeBlockPos(pos); buf.writeVarInt(syncedData.size()); buf.writeBoolean(advanced) }
    override fun getDisplayName() = Text.translatable("block.ic2_120.$blockKey")
    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?) = MinerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage, advanced)
}

@ModBlockEntity(block = MinerBlock::class)
class MinerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : BaseMinerBlockEntity(type, pos, state, "miner", 2, false) {
    constructor(pos: BlockPos, state: BlockState) : this(MinerBlockEntity::class.type(), pos, state)
    companion object { @Volatile private var registered = false; @RegisterFluidStorage fun registerFluidStorageLookup() { if (!registered) { FluidStorage.SIDED.registerForBlockEntity({ be, _ -> be.getFluidStorageForSide(null) }, MinerBlockEntity::class.type()); registered = true } } }
}

@ModBlockEntity(block = AdvancedMinerBlock::class)
class AdvancedMinerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : BaseMinerBlockEntity(type, pos, state, "advanced_miner", 3, true) {
    constructor(pos: BlockPos, state: BlockState) : this(AdvancedMinerBlockEntity::class.type(), pos, state)
    companion object { @Volatile private var registered = false; @RegisterFluidStorage fun registerFluidStorageLookup() { if (!registered) { FluidStorage.SIDED.registerForBlockEntity({ be, _ -> be.getFluidStorageForSide(null) }, AdvancedMinerBlockEntity::class.type()); registered = true } } }
}
