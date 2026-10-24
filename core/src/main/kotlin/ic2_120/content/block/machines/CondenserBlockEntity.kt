package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.cachedState
import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
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
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.material.Fluid
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
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
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

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
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = CondenserBlock.ACTIVE
    override val tier: Int = 3
    override fun getInventory(): net.minecraft.world.Container? = this

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

        // fabric 1.21.1：FluidVariant.fromNbt/toNbt 已移除，用 CODEC + NbtOps 桥接
        fun encodeFluidVariant(variant: net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant): net.minecraft.nbt.CompoundTag {
            val tag = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, variant)
                .getOrThrow()
            return if (tag is net.minecraft.nbt.CompoundTag) tag else net.minecraft.nbt.CompoundTag()
        }

        fun decodeFluidVariant(tag: net.minecraft.nbt.CompoundTag): net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant {
            if (tag.isEmpty) return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank()
            return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag)
                .result()
                .orElse(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank())
        }

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

        /** 蒸馏水每进度产率：100 mB = 8,100 droplets */
        private const val WATER_PER_PROGRESS = 8100L
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_VENT_INDICES, matcher = { isValidVent(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_UPGRADE), matcher = { canPlaceItem(SLOT_UPGRADE, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGE), matcher = { canPlaceItem(SLOT_DISCHARGE, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_WATER_INPUT), matcher = { canPlaceItem(SLOT_WATER_INPUT, it) })
        ),
        extractSlots = (intArrayOf(SLOT_UPGRADE, SLOT_WATER_OUTPUT, SLOT_DISCHARGE) + SLOT_VENT_INDICES),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = CondenserSync(
        schema = syncedData,
        getFacing = { cachedState.getOptional(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH) },
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
            sync.steamAmount = amount.toInt().coerceAtLeast(0)
            setChanged()
        }
    }

    private val distilledWaterTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = WATER_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = !variant.isBlank

        override fun onFinalCommit() {
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            setChanged()
        }

        fun injectWater(droplets: Long): Boolean {
            if (droplets <= 0L) return false
            val space = WATER_TANK_CAPACITY - amount
            val actual = minOf(droplets, space)
            if (actual <= 0L) return false
            Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.isBlank) variant = FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)
                tx.commit()
            }
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            setChanged()
            return true
        }

        fun ejectToNeighbors(world: Level, pos: BlockPos) {
            if (amount <= 0L || variant.isBlank) return
            for (side in Direction.entries) {
                if (amount <= 0L) break
                val neighborPos = pos.offset(side)
                val storage = FluidStorage.SIDED.find(world, neighborPos, side.opposite) ?: continue
                try {
                    val extracted = storage.insert(variant, amount, null)
                    if (extracted > 0L) {
                        Transaction.openOuter().use { tx ->
                            updateSnapshots(tx)
                            amount -= extracted
                            if (amount <= 0L) variant = FluidVariant.blank()
                            tx.commit()
                        }
                        sync.waterAmount = amount.toInt().coerceAtLeast(0)
                        setChanged()
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
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = steamTank.variant
                    override fun getAmount(): Long = steamTank.amount
                    override fun getCapacity(): Long = STEAM_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = steamTank.variant.isBlank
                })
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = distilledWaterTank.variant
                    override fun getAmount(): Long = distilledWaterTank.amount
                    override fun getCapacity(): Long = WATER_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        distilledWaterTank.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = distilledWaterTank.variant.isBlank
                })
            return views.iterator()
        }
    }

    /** 流体存储方向访问 (SolarDistiller 模式) */
    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? = ioStorage

    constructor(pos: BlockPos, state: BlockState) : this(CondenserBlockEntity::class.type(), pos, state)

    // ==== Container ====

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot !in listOf(SLOT_WATER_INPUT, SLOT_WATER_OUTPUT) && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        setChanged()
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_UPGRADE -> isBatteryItem(stack)
        SLOT_DISCHARGE -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
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
        stack.item == net.minecraft.world.item.Items.BUCKET || stack.item == net.minecraft.world.item.Items.WATER_BUCKET

    // ==== AbstractContainerMenu ====

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.size)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.condenser")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        CondenserScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    // ==== NBT ====

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CondenserSync.NBT_ENERGY_STORED).coerceIn(0L, CondenserSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progress = nbt.getInt(NBT_PROGRESS)
        machineTier = nbt.getInt(NBT_TIER).coerceAtLeast(3)
        if (nbt.contains(NBT_STEAM_TANK)) {
            val t = nbt.getCompound(NBT_STEAM_TANK)
            steamTank.variant = decodeFluidVariant(t.getCompound("variant"))
            steamTank.amount = t.getLong("amount")
        }
        if (nbt.contains(NBT_WATER_TANK)) {
            val t = nbt.getCompound(NBT_WATER_TANK)
            distilledWaterTank.variant = decodeFluidVariant(t.getCompound("variant"))
            distilledWaterTank.amount = t.getLong("amount")
        }
        sync.steamAmount = steamTank.amount.toInt().coerceAtLeast(0)
        sync.waterAmount = distilledWaterTank.amount.toInt().coerceAtLeast(0)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CondenserSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_PROGRESS, progress)
        nbt.putInt(NBT_TIER, machineTier)
        val st = CompoundTag()
        st.put("variant", steamTank.encodeFluidVariant(variant))
        st.putLong("amount", steamTank.amount)
        nbt.put(NBT_STEAM_TANK, st)
        val wt = CompoundTag()
        wt.put("variant", distilledWaterTank.encodeFluidVariant(variant))
        wt.putLong("amount", distilledWaterTank.amount)
        nbt.put(NBT_WATER_TANK, wt)
    }

    // ==== Tick ====

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 电池放电
        val euSpace = (CondenserSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0)
        val discharged = batteryDischarger.tick(euSpace.coerceAtMost(CondenserSync.MAX_INSERT))
        if (discharged > 0) {
            val inserted = sync.insertEnergy(discharged)
            if (discharged > inserted) sync.forceInsertEnergy(discharged - inserted)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        }

        // 蒸馏水输出
        FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, distilledWaterTank,
            fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)

        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, steamTank, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }

        // 升级槽 → 更新等级（对齐 ic2_origin updateTier）
        val upgradeStack = getItem(SLOT_UPGRADE)
        val newTier = if (!upgradeStack.isEmpty && upgradeStack.item is IBatteryItem) {
            (upgradeStack.item as IBatteryItem).tier.coerceIn(3, Int.MAX_VALUE)
        } else 3
        if (newTier != machineTier) {
            machineTier = newTier
            setChanged()
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
        val steamAvailable = steamTank.amount
        val hasEnergy: Boolean
        if (steamAvailable > 0L && distilledWaterTank.amount < WATER_TANK_CAPACITY) {
            // 对齐 ic2_origin: 有散热口时必须消耗 EU，否则不工作（不消耗蒸汽）
            hasEnergy = euPerTick == 0L || sync.consumeEnergy(euPerTick) > 0L
            if (hasEnergy) {
                val toConsume = minOf(steamAvailable, coolingRate.toLong())
                if (toConsume > 0L && steamTank.amount > 0L) {
                val consumed = steamTank.amount.coerceAtMost(toConsume)
                if (consumed > 0L) {
                    Transaction.openOuter().use { tx ->
                        steamTank.updateSnapshots(tx)
                        steamTank.amount -= consumed
                        if (steamTank.amount <= 0L) steamTank.variant = FluidVariant.blank()
                        tx.commit()
                    }
                    steamConsumed = consumed.toInt().coerceAtLeast(0).toLong()
                    progress += steamConsumed.toInt()
                    sync.steamAmount = steamTank.amount.toInt().coerceAtLeast(0)

                    // 进度够了产出蒸馏水
                    while (progress >= CondenserSync.PROGRESS_MAX &&
                        distilledWaterTank.amount + WATER_PER_PROGRESS <= WATER_TANK_CAPACITY) {
                        progress -= CondenserSync.PROGRESS_MAX
                        distilledWaterTank.injectWater(WATER_PER_PROGRESS)
                    }
                }
            }
            }  // close if(hasEnergy)
        } else {
            hasEnergy = true
        }

        // 同步
        sync.steamAmount = steamTank.amount.toInt().coerceAtLeast(0)
        sync.waterAmount = distilledWaterTank.amount.toInt().coerceAtLeast(0)
        sync.progress = progress
        sync.ventCount = ventCount
        sync.coolingRate = if (hasEnergy) coolingRate else 0

        val active = steamConsumed > 0L
        setActiveState(world, pos, state, active)

        sync.syncCurrentTickFlow()
        setChanged()
    }

    /** 将蒸馏水装填到空单元/桶中 — 对齐 ic2_origin waterInputSlot.processFromTank */
    private fun processWaterItems() {
        val inputStack = getItem(SLOT_WATER_INPUT)
        if (inputStack.isEmpty) return
        if (distilledWaterTank.amount < FluidConstants.BUCKET) return

        val result: ItemStack = when (val item = inputStack.item) {
            is ic2_120.content.item.EmptyCell ->
                ic2_120.content.item.fluidToFilledCellStack(ModFluids.DISTILLED_WATER_STILL)
            is net.minecraft.world.item.BucketItem -> {
                // 空桶 → 水桶（无蒸馏水桶，退而用水桶）
                if (item == net.minecraft.world.item.Items.BUCKET) ItemStack(net.minecraft.world.item.Items.WATER_BUCKET)
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

        val outputStack = getItem(SLOT_WATER_OUTPUT)
        if (!outputStack.isEmpty) {
            if (!ItemStack.isSameItem(outputStack, result)) return
            if (outputStack.count >= outputStack.maxStackSize) return
        }

        // 消耗 1 桶 (1000 mB) 蒸馏水 — 走 extract（触发 onFinalCommit，确保同步）
        val distilledDroplets = FluidConstants.BUCKET
        Transaction.openOuter().use { tx ->
            val actual = distilledWaterTank.extract(FluidVariant.of(ModFluids.DISTILLED_WATER_STILL), distilledDroplets, tx)
            if (actual >= distilledDroplets) tx.commit() else return
        }
        sync.waterAmount = distilledWaterTank.amount.toInt().coerceAtLeast(0)

        // 扣输入
        inputStack.shrink(1)
        if (inputStack.isEmpty) inventory[SLOT_WATER_INPUT] = ItemStack.EMPTY

        // 放输出
        if (outputStack.isEmpty) inventory[SLOT_WATER_OUTPUT] = result
        else outputStack.grow(1)

        setChanged()
    }
}
