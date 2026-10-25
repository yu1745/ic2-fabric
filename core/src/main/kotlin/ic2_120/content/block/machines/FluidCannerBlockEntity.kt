package ic2_120.content.block.machines

import ic2_120.compat.ContainerHelper26

import net.minecraft.core.registries.BuiltInRegistries

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.FluidCannerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.FluidCannerScreenHandler
import ic2_120.content.sync.FluidCannerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyDebtAccounting
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
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
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import ic2_120.Ic2_120

/**
 * 流体装罐机方块实体。
 * 槽位：满流体容器(A)、空容器(B)、输出、放电、4 升级
 * 逻辑：若 A 和 B 都有物品，优先将 A 倒入储罐；否则将储罐灌入 B。
 */
@ModBlockEntity(block = FluidCannerBlock::class)
class FluidCannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedMenuProvider<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = FluidCannerBlock.ACTIVE

    override fun getInventory(): net.minecraft.world.Container = this

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override val tier: Int = FluidCannerSync.FLUID_CANNER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

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

        const val SLOT_INPUT_FILLED = 0   // 满流体容器（水桶、满单元等）
        const val SLOT_INPUT_EMPTY = 1   // 空容器（空单元、桶）
        const val SLOT_OUTPUT = 2
        const val SLOT_DISCHARGING = 3
        val SLOT_UPGRADE_INDICES = intArrayOf(4, 5, 6, 7)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_FILLED, SLOT_INPUT_EMPTY)
        const val INVENTORY_SIZE = 8
        private const val NBT_FLUID_AMOUNT = "FluidAmount"
        private const val NBT_FLUID_VARIANT = "FluidVariant"
        private const val TANK_CAPACITY_BUCKETS = 8
        private val TANK_CAPACITY = FluidConstants.BUCKET * TANK_CAPACITY_BUCKETS

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> (be as FluidCannerBlockEntity).getFluidStorageForSide(side) }, FluidCannerBlockEntity::class.type())
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
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_FILLED), matcher = { !it.isEmpty && it.item !is IBatteryItem && isFilledFluidContainer(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_EMPTY), matcher = { !it.isEmpty && it.item !is IBatteryItem && isEmptyFluidContainerForBottler(it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT_FILLED, SLOT_INPUT_EMPTY, SLOT_OUTPUT, SLOT_DISCHARGING),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = FluidCannerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(FluidCannerSync.FLUID_CANNER_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = !variant.isBlank
        override fun canExtract(variant: FluidVariant): Boolean = true

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            syncTankState()
            setChanged()
        }
    }

    val fluidTank: Storage<FluidVariant> = tankInternal

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { FluidCannerSync.FLUID_CANNER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(FluidCannerBlockEntity::class.type(), pos, state)

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_FILLED -> !stack.isEmpty && stack.item !is IBatteryItem && isFilledFluidContainer(stack)
        SLOT_INPUT_EMPTY -> !stack.isEmpty && stack.item !is IBatteryItem && isEmptyFluidContainerForBottler(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.count)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.fluid_canner")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        FluidCannerScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLongOr(FluidCannerSync.NBT_ENERGY_STORED, 0L)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloatOr("ProgressF", 0f).coerceIn(0f, FluidCannerSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, FluidCannerSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloatOr("EnergyDebt", 0f).coerceAtLeast(0f) else 0f
        tankInternal.amount = nbt.getLongOr(NBT_FLUID_AMOUNT, 0L).coerceIn(0L, TANK_CAPACITY)
        val fluidTag = nbt.getCompoundOrEmpty(NBT_FLUID_VARIANT)
        tankInternal.variant = if (fluidTag.isEmpty) FluidVariant.blank() else decodeFluidVariant(fluidTag)
        syncTankState()
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(FluidCannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
        nbt.putLong(NBT_FLUID_AMOUNT, tankInternal.amount)
        if (!tankInternal.variant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, encodeFluidVariant(tankInternal.variant))
        }
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        // val facing = world?.getBlockState(pos)?.getValue(et.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH
        // if (side == facing) return null
        return fluidTank
    }

    private fun syncTankState() {
        sync.fluidAmount = tankInternal.amount.toInt().coerceAtLeast(0)
        sync.fluidCapacity = TANK_CAPACITY.toInt()
        sync.fluidRawId = if (tankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(tankInternal.variant.fluid)
    }

    /** 重置全部进度状态（操作不可用时调用）。 */
    private fun resetProgress() {
        if (progressF != 0f || energyDebtF != 0f || sync.progress != 0) {
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
        }
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.lastPourOut = if (lastOperationPourOut) 1 else 0
        syncTankState()

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, tankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, tankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        val filled = getItem(SLOT_INPUT_FILLED)
        val empty = getItem(SLOT_INPUT_EMPTY)
        val outputSlot = getItem(SLOT_OUTPUT)

        val isFluidContainer = isFluidContainer(filled)

        // 优先级：可继续装填的容器 > 满流体容器倒出 > 第二槽的可装填容器。
        // 部分装填装备也由 Transfer API 决定，不再套用“一次必须整桶”的规则。
        val fillFromFilled = planContainerFill(filled)
        val doPourOutFromFilled = fillFromFilled == null && isFluidContainer
        val fillFromEmpty = if (fillFromFilled == null && !doPourOutFromFilled) planContainerFill(empty) else null

        val operating = when {
            fillFromFilled != null -> true
            doPourOutFromFilled -> tryPourOut(filled, outputSlot)
            fillFromEmpty != null -> true
            else -> false
        }

        if (operating) {
            // 耗能记账：1.6^n 按浮点累计，取整数部分消费、余数结转（EU 本身是整数）
            energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, FluidCannerSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
            val need = energyDebtF.toLong().coerceAtLeast(1L)
            if (sync.consumeEnergy(need) > 0L) {
                energyDebtF -= need
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
                progressF = (progressF + speedMultiplier).coerceAtMost(FluidCannerSync.PROGRESS_MAX.toFloat())
                sync.progress = progressF.toInt()
                if (progressF >= FluidCannerSync.PROGRESS_MAX) {
                    when {
                        fillFromFilled != null -> completeFill(SLOT_INPUT_FILLED)
                        doPourOutFromFilled -> completePourOut()
                        fillFromEmpty != null -> completeFill(SLOT_INPUT_EMPTY)
                    }
                    progressF = 0f
                    energyDebtF = 0f
                    sync.progress = 0
                }
                setChanged()
                setActiveState(world, pos, state, true)
            } else {
                // 停顿期间不累计债务：防恢复供电后 need 超容量 all-or-nothing 卡死
                energyDebtF = 0f
                setActiveState(world, pos, state, false)
            }
        } else {
            resetProgress()
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private var lastOperationPourOut: Boolean = true
    private fun isFluidContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val ctx = ContainerItemContext.withConstant(stack)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        for (view in itemStorage) {
            if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) return true
        }
        return false
    }

    private fun tryPourOut(filled: ItemStack, outputSlot: ItemStack): Boolean {
        val ctx = ContainerItemContext.withConstant(filled)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        var canExtract = false
        for (view in itemStorage) {
            if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                canExtract = true
                break
            }
        }
        if (!canExtract) return false
        val space = (TANK_CAPACITY - tankInternal.amount).coerceAtLeast(0L)
        if (space < FluidConstants.BUCKET) return false
        lastOperationPourOut = true
        return true
    }

    private data class ContainerFillPlan(
        val amount: Long,
        val result: ItemStack,
        val fullAfterFill: Boolean
    )

    private fun planContainerFill(container: ItemStack): ContainerFillPlan? {
        val variant = tankInternal.variant
        if (container.isEmpty || variant.isBlank || tankInternal.amount <= 0L) return null
        @Suppress("DEPRECATION")
        val ctx = ContainerItemContext.withConstant(container.copyWithCount(1))
        val storage = ctx.find(FluidStorage.ITEM) ?: return null
        if (!storage.supportsInsertion()) return null

        Transaction.openOuter().use { tx ->
            val inserted = storage.insert(variant, minOf(FluidConstants.BUCKET, tankInternal.amount), tx)
            if (inserted <= 0L) return null
            val result = ctx.itemVariant.toStack(1)
            val fullAfterFill = storage.insert(variant, 1L, tx) == 0L
            if (fullAfterFill && !canAcceptOutput(getItem(SLOT_OUTPUT), result)) return null
            return ContainerFillPlan(inserted, result, fullAfterFill)
        }
    }

    private fun canAcceptOutput(outputSlot: ItemStack, result: ItemStack): Boolean {
        if (outputSlot.isEmpty) return true
        return ItemStack.isSameItem(outputSlot, result) && outputSlot.count + result.count <= result.maxStackSize
    }

    private fun completePourOut() {
        val filled = getItem(SLOT_INPUT_FILLED)
        val outputSlot = getItem(SLOT_OUTPUT)
        Transaction.openOuter().use { tx ->
            @Suppress("DEPRECATION")
            val ctx = ContainerItemContext.withConstant(filled.copyWithCount(1))
            val itemStorage = ctx.find(FluidStorage.ITEM) ?: return@use
            for (view in itemStorage) {
                if (view.amount < FluidConstants.BUCKET || view.resource.isBlank) continue
                val containedFluid = view.resource
                val extracted = view.extract(containedFluid, FluidConstants.BUCKET, tx)
                if (extracted <= 0L) continue
                if (tankInternal.insert(containedFluid, extracted, tx) != extracted) continue
                val emptyResult = ctx.itemVariant.toStack(1)
                if (!canAcceptOutput(outputSlot, emptyResult)) return@use
                tx.commit()

                filled.shrink(1)
                if (filled.isEmpty) setItem(SLOT_INPUT_FILLED, ItemStack.EMPTY)
                if (outputSlot.isEmpty) setItem(SLOT_OUTPUT, emptyResult)
                else outputSlot.grow(emptyResult.count)
                syncTankState()
                return
            }
        }
    }

    private fun completeFill(slot: Int) {
        val container = getItem(slot)
        val plan = planContainerFill(container) ?: return
        val variant = tankInternal.variant
        Transaction.openOuter().use { tx ->
            @Suppress("DEPRECATION")
            val ctx = ContainerItemContext.withConstant(container.copyWithCount(1))
            val storage = ctx.find(FluidStorage.ITEM) ?: return@use
            if (storage.insert(variant, plan.amount, tx) != plan.amount) return@use
            if (tankInternal.extract(variant, plan.amount, tx) != plan.amount) return@use
            val result = ctx.itemVariant.toStack(1)
            if (plan.fullAfterFill && !canAcceptOutput(getItem(SLOT_OUTPUT), result)) return@use
            tx.commit()

            container.shrink(1)
            if (container.isEmpty) setItem(slot, ItemStack.EMPTY)
            if (plan.fullAfterFill) {
                val output = getItem(SLOT_OUTPUT)
                if (output.isEmpty) setItem(SLOT_OUTPUT, result)
                else output.grow(result.count)
            } else {
                setItem(slot, result)
            }
            lastOperationPourOut = false
            syncTankState()
        }
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

    private fun isFilledFluidContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.item == Items.WATER_BUCKET || stack.item == Items.LAVA_BUCKET) return true
        val ctx = ContainerItemContext.withConstant(stack)
        val fluidItemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        for (view in fluidItemStorage) {
            if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) return true
        }
        return false
    }

    private fun isEmptyFluidContainerForBottler(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.item == Items.BUCKET) return true
        val ctx = ContainerItemContext.withConstant(stack)
        val fluidItemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        return fluidItemStorage.supportsInsertion()
    }
}
