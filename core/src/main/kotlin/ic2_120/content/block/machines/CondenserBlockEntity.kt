package ic2_120.content.block.machines

import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.CondenserBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.HeatVentItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.CondenserScreenHandler
import ic2_120.content.sync.CondenserSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.fluid.Fluid
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 冷凝器：消耗蒸汽 + EU → 产出蒸馏水。
 *
 * 参考 ic2_origin TileEntityCondenser 数值：
 * - EU 存储: 100,000 EU
 * - 蒸汽罐: 100,000 mB
 * - 蒸馏水罐: 1,000 mB
 * - 被动冷却: 100 mB/t
 * - 每个散热口: +100 mB/t, 消耗 2 EU/t
 * - 最大 4 个散热口
 */
@ModBlockEntity(block = CondenserBlock::class)
class CondenserBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CondenserBlock.ACTIVE
    override val tier: Int = 3
    override fun getInventory(): net.minecraft.inventory.Inventory? = this

    // IFluidPipeUpgradeSupport — 默认开启推拉，无需流体升级
    override var fluidPipeProviderEnabled: Boolean = true
    override var fluidPipeReceiverEnabled: Boolean = true
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 1
    override var fluidPipePullingCount: Int = 1

    companion object {
        const val SLOT_VENT_0 = 0
        const val SLOT_VENT_1 = 1
        const val SLOT_VENT_2 = 2
        const val SLOT_VENT_3 = 3
        const val SLOT_UPGRADE = 4
        const val SLOT_WATER_INPUT = 5
        const val SLOT_WATER_OUTPUT = 6
        const val SLOT_DISCHARGE = 7
        val SLOT_VENT_INDICES = intArrayOf(SLOT_VENT_0, SLOT_VENT_1, SLOT_VENT_2, SLOT_VENT_3)
        const val INVENTORY_SIZE = 8

        private const val NBT_STEAM_TANK = "SteamTank"
        private const val NBT_WATER_TANK = "WaterTank"
        private const val NBT_PROGRESS = "Progress"
        private const val NBT_TIER = "Tier"

        private const val STEAM_TANK_CAPACITY = FluidConstants.BUCKET * 8  // 8,000 mB
        private const val WATER_TANK_CAPACITY = FluidConstants.BUCKET * 8  // 8,000 mB

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val beType = CondenserBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, beType)
            fluidLookupRegistered = true
        }

        fun toMilliBuckets(droplets: Long): Int =
            (droplets * 1000 / FluidConstants.BUCKET).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        fun mbToDroplets(mb: Long): Long = mb * FluidConstants.BUCKET / 1000
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_VENT_INDICES, matcher = { isValidVent(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_UPGRADE), matcher = { isValid(SLOT_UPGRADE, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGE), matcher = { isValid(SLOT_DISCHARGE, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_WATER_INPUT), matcher = { isValid(SLOT_WATER_INPUT, it) })
        ),
        extractSlots = (intArrayOf(SLOT_UPGRADE, SLOT_WATER_OUTPUT, SLOT_DISCHARGE) + SLOT_VENT_INDICES),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = CondenserSync(
        schema = syncedData,
        getFacing = { cachedState.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH) },
        currentTickProvider = { world?.time }
    )
    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGE,
        machineTierProvider = { machineTier },
        canDischargeNow = { sync.amount < CondenserSync.ENERGY_CAPACITY }
    )

    private var progress: Int = 0
    private var machineTier: Int = 3

    // ==== 流体槽 ====

    private val steamTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = STEAM_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean = ModFluids.isSteam(variant.fluid)

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.steamAmount = toMilliBuckets(amount)
            markDirty()
        }
    }

    private val distilledWaterTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = WATER_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = ModFluids.isFluid(variant.fluid)

        override fun onFinalCommit() {
            sync.waterAmount = toMilliBuckets(amount)
            markDirty()
        }

        fun injectWater(droplets: Long): Boolean {
            if (droplets <= 0L) return false
            val space = WATER_TANK_CAPACITY - amount
            val actual = minOf(droplets, space)
            if (actual <= 0L) return false
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)
            sync.waterAmount = toMilliBuckets(amount)
            markDirty()
            return true
        }

        fun ejectToNeighbors(world: World, pos: BlockPos) {
            if (amount <= 0L || variant.isBlank) return
            for (side in Direction.entries) {
                if (amount <= 0L) break
                val neighborPos = pos.offset(side)
                val storage = FluidStorage.SIDED.find(world, neighborPos, side.opposite) ?: continue
                try {
                    val extracted = storage.insert(variant, amount, null)
                    if (extracted > 0L) {
                        amount -= extracted
                        if (amount <= 0L) variant = FluidVariant.blank()
                        sync.waterAmount = toMilliBuckets(amount)
                        markDirty()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.STEAM_STILL && resource.fluid != ModFluids.SUPERHEATED_STEAM_STILL) return 0L
            return steamTank.insert(resource, maxAmount, transaction)
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.DISTILLED_WATER_STILL) return 0L
            return distilledWaterTank.extract(resource, maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!steamTank.variant.isBlank && steamTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = steamTank.variant
                    override fun getAmount(): Long = steamTank.amount
                    override fun getCapacity(): Long = STEAM_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!distilledWaterTank.variant.isBlank && distilledWaterTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = distilledWaterTank.variant
                    override fun getAmount(): Long = distilledWaterTank.amount
                    override fun getCapacity(): Long = WATER_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        distilledWaterTank.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    /** 流体存储方向访问 (SolarDistiller 模式) */
    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? = ioStorage

    constructor(pos: BlockPos, state: BlockState) : this(CondenserBlockEntity::class.type(), pos, state)

    // ==== Inventory ====

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot !in listOf(SLOT_WATER_INPUT, SLOT_WATER_OUTPUT) && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_UPGRADE -> isBatteryItem(stack)
        SLOT_DISCHARGE -> isBatteryItem(stack)
        SLOT_WATER_INPUT -> isFluidCell(stack) || isWaterBucket(stack)
        SLOT_WATER_OUTPUT -> false  // 仅输出
        in SLOT_VENT_INDICES -> isValidVent(stack)
        else -> false
    }

    private fun isValidVent(stack: ItemStack): Boolean = stack.item is HeatVentItem
    private fun isBatteryItem(stack: ItemStack): Boolean = stack.item is IBatteryItem && (stack.item as IBatteryItem).tier <= machineTier
    private fun isFluidCell(stack: ItemStack): Boolean =
        stack.item is ic2_120.content.item.FluidCellItem || stack.item is ic2_120.content.item.EmptyCellItem
    private fun isWaterBucket(stack: ItemStack): Boolean =
        stack.item == net.minecraft.item.Items.BUCKET || stack.item == net.minecraft.item.Items.WATER_BUCKET

    // ==== ScreenHandler ====

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.condenser")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CondenserScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    // ==== NBT ====

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CondenserSync.NBT_ENERGY_STORED).coerceIn(0L, CondenserSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progress = nbt.getInt(NBT_PROGRESS)
        machineTier = nbt.getInt(NBT_TIER).coerceAtLeast(3)
        if (nbt.contains(NBT_STEAM_TANK)) {
            val t = nbt.getCompound(NBT_STEAM_TANK)
            steamTank.variant = FluidVariant.fromNbt(t.getCompound("variant"))
            steamTank.amount = t.getLong("amount")
        }
        if (nbt.contains(NBT_WATER_TANK)) {
            val t = nbt.getCompound(NBT_WATER_TANK)
            distilledWaterTank.variant = FluidVariant.fromNbt(t.getCompound("variant"))
            distilledWaterTank.amount = t.getLong("amount")
        }
        sync.steamAmount = toMilliBuckets(steamTank.amount)
        sync.waterAmount = toMilliBuckets(distilledWaterTank.amount)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CondenserSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_PROGRESS, progress)
        nbt.putInt(NBT_TIER, machineTier)
        val st = NbtCompound()
        st.put("variant", steamTank.variant.toNbt())
        st.putLong("amount", steamTank.amount)
        nbt.put(NBT_STEAM_TANK, st)
        val wt = NbtCompound()
        wt.put("variant", distilledWaterTank.variant.toNbt())
        wt.putLong("amount", distilledWaterTank.amount)
        nbt.put(NBT_WATER_TANK, wt)
    }

    // ==== Tick ====

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 电池放电
        val euSpace = (CondenserSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0)
        val discharged = batteryDischarger.tick(euSpace.coerceAtMost(CondenserSync.MAX_INSERT))
        if (discharged > 0) {
            sync.insertEnergy(discharged)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        }

        // 蒸馏水输出
        FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, distilledWaterTank,
            fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)

        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, steamTank, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }

        // 升级槽 → 更新等级（对齐 ic2_origin updateTier）
        val upgradeStack = getStack(SLOT_UPGRADE)
        val newTier = if (!upgradeStack.isEmpty && upgradeStack.item is IBatteryItem) {
            (upgradeStack.item as IBatteryItem).tier.coerceIn(3, Int.MAX_VALUE)
        } else 3
        if (newTier != machineTier) {
            machineTier = newTier
            markDirty()
        }

        // 水物品处理：空单元/桶 → 装蒸馏水（对齐 ic2_origin waterInputSlot.processFromTank）
        processWaterItems()

        // 统计散热口数量
        var ventCount = 0
        for (idx in SLOT_VENT_INDICES) {
            if (!inventory[idx].isEmpty && inventory[idx].item is HeatVentItem) ventCount++
        }

        val coolingRate = CondenserSync.PASSIVE_COOLING + ventCount * CondenserSync.COOLING_PER_VENT
        val euPerTick = ventCount * CondenserSync.EU_PER_VENT.toLong()

        var steamConsumed = 0L
        val steamAvailableMb = toMilliBuckets(steamTank.amount).toLong()
        val hasEnergy: Boolean
        if (steamAvailableMb > 0L && distilledWaterTank.amount < WATER_TANK_CAPACITY) {
            // 对齐 ic2_origin: 有散热口时必须消耗 EU，否则不工作（不消耗蒸汽）
            hasEnergy = euPerTick == 0L || sync.consumeEnergy(euPerTick) > 0L
            if (hasEnergy) {
                val toConsume = minOf(steamAvailableMb, coolingRate.toLong())
                if (toConsume > 0L && steamTank.amount > 0L) {
                val consumed = steamTank.amount.coerceAtMost(mbToDroplets(toConsume))
                if (consumed > 0L) {
                    steamTank.amount -= consumed
                    if (steamTank.amount <= 0L) steamTank.variant = FluidVariant.blank()
                    steamConsumed = toMilliBuckets(consumed).toLong()
                    progress += steamConsumed.toInt()
                    sync.steamAmount = toMilliBuckets(steamTank.amount)

                    // 进度够了产出蒸馏水
                    while (progress >= CondenserSync.PROGRESS_MAX) {
                        progress -= CondenserSync.PROGRESS_MAX
                        distilledWaterTank.injectWater(mbToDroplets(100))
                    }
                }
            }
            }  // close if(hasEnergy)
        } else {
            hasEnergy = true
        }

        // 同步
        sync.steamAmount = toMilliBuckets(steamTank.amount)
        sync.waterAmount = toMilliBuckets(distilledWaterTank.amount)
        sync.progress = progress
        sync.ventCount = ventCount
        sync.coolingRate = if (hasEnergy) coolingRate else 0

        val active = steamConsumed > 0L
        setActiveState(world, pos, state, active)

        sync.syncCurrentTickFlow()
        markDirty()
    }

    /** 将蒸馏水装填到空单元/桶中 — 对齐 ic2_origin waterInputSlot.processFromTank */
    private fun processWaterItems() {
        val inputStack = getStack(SLOT_WATER_INPUT)
        if (inputStack.isEmpty) return
        if (distilledWaterTank.amount < FluidConstants.BUCKET) return

        val result: ItemStack = when (val item = inputStack.item) {
            is ic2_120.content.item.EmptyCell ->
                ic2_120.content.item.fluidToFilledCellStack(ModFluids.DISTILLED_WATER_STILL)
            is net.minecraft.item.BucketItem -> {
                // 空桶 → 水桶（无蒸馏水桶，退而用水桶）
                if (item == net.minecraft.item.Items.BUCKET) ItemStack(net.minecraft.item.Items.WATER_BUCKET)
                else return
            }
            is ic2_120.content.item.ModFluidCell -> {
                // 已有蒸馏水单元（返还），或误放入的其他单元
                if (item.getFluid() == ModFluids.DISTILLED_WATER_STILL) inputStack.copy()
                else return
            }
            else -> return
        }
        if (result.isEmpty) return

        val outputStack = getStack(SLOT_WATER_OUTPUT)
        if (!outputStack.isEmpty) {
            if (!ItemStack.areItemsEqual(outputStack, result)) return
            if (outputStack.count >= outputStack.maxCount) return
        }

        // 消耗 1 桶 (1000 mB) 蒸馏水 — 走 extract（触发 onFinalCommit，确保同步）
        val distilledDroplets = FluidConstants.BUCKET
        Transaction.openOuter().use { tx ->
            val actual = distilledWaterTank.extract(FluidVariant.of(ModFluids.DISTILLED_WATER_STILL), distilledDroplets, tx)
            if (actual >= distilledDroplets) tx.commit() else return
        }
        sync.waterAmount = toMilliBuckets(distilledWaterTank.amount)

        // 扣输入
        inputStack.decrement(1)
        if (inputStack.isEmpty) inventory[SLOT_WATER_INPUT] = ItemStack.EMPTY

        // 放输出
        if (outputStack.isEmpty) inventory[SLOT_WATER_OUTPUT] = result
        else outputStack.increment(1)

        markDirty()
    }
}
