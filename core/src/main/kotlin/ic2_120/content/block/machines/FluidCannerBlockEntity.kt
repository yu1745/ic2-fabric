package ic2_120.content.block.machines

import ic2_120.content.block.FluidCannerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.fluid.FluidFuelRegistry
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.FluidCannerScreenHandler
import ic2_120.content.sync.FluidCannerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.BucketItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import ic2_120.Ic2_120
import ic2_120.content.item.CfPack
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.fluidToFilledCellStack

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
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = FluidCannerBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override val tier: Int = FluidCannerSync.FLUID_CANNER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
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

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_FILLED), matcher = { !it.isEmpty && it.item !is IBatteryItem && isFilledFluidContainer(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_EMPTY), matcher = { !it.isEmpty && it.item !is IBatteryItem && isEmptyFluidContainerForBottler(it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT_FILLED, SLOT_INPUT_EMPTY, SLOT_OUTPUT, SLOT_DISCHARGING),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData(this)
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
            markDirty()
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

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
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

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_FILLED -> !stack.isEmpty && stack.item !is IBatteryItem && isFilledFluidContainer(stack)
        SLOT_INPUT_EMPTY -> !stack.isEmpty && stack.item !is IBatteryItem && isEmptyFluidContainerForBottler(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE)
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.fluid_canner")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        FluidCannerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(FluidCannerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        tankInternal.amount = nbt.getLong(NBT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val fluidTag = nbt.getCompound(NBT_FLUID_VARIANT)
        tankInternal.variant = if (fluidTag.isEmpty) FluidVariant.blank() else FluidVariant.fromNbt(fluidTag)
        syncTankState()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(FluidCannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_FLUID_AMOUNT, tankInternal.amount)
        if (!tankInternal.variant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, tankInternal.variant.toNbt())
        }
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        // val facing = world?.getBlockState(pos)?.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        // if (side == facing) return null
        return fluidTank
    }

    private fun syncTankState() {
        sync.fluidAmount = tankInternal.amount.toInt().coerceAtLeast(0)
        sync.fluidCapacity = TANK_CAPACITY.toInt()
        sync.fluidRawId = if (tankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(tankInternal.variant.fluid)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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

        val filled = getStack(SLOT_INPUT_FILLED)
        val empty = getStack(SLOT_INPUT_EMPTY)
        val outputSlot = getStack(SLOT_OUTPUT)

        // 判断左上角槽位是空容器还是流体容器
        val isEmptyContainer = isEmptyContainer(filled)
        val isFluidContainer = isFluidContainer(filled)

        // 优先级：空容器填充 > 流体容器倒出 > 第二槽的空容器填充
        val doFillFromFilled = isEmptyContainer && tankInternal.amount >= FluidConstants.BUCKET
        val doPourOutFromFilled = isFluidContainer
        val doFillFromEmpty = !empty.isEmpty && !doFillFromFilled && !doPourOutFromFilled && tankInternal.amount >= FluidConstants.BUCKET

        val operating = when {
            doFillFromFilled -> tryFill(filled, outputSlot, isPrimarySlot = true)
            doPourOutFromFilled -> tryPourOut(filled, outputSlot)
            doFillFromEmpty -> tryFill(empty, outputSlot, isPrimarySlot = false)
            else -> false
        }

        if (operating) {
            val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
            val need = (FluidCannerSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
            if (sync.consumeEnergy(need) > 0L) {
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                sync.progress += progressIncrement
                if (sync.progress >= FluidCannerSync.PROGRESS_MAX) {
                    completeCurrentOperation(doFillFromFilled || doFillFromEmpty, doFillFromFilled)
                    sync.progress = 0
                }
                markDirty()
                setActiveState(world, pos, state, true)
            } else {
                setActiveState(world, pos, state, false)
            }
        } else {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private var lastOperationPourOut: Boolean = true
    private var lastOperationWasPrimarySlot: Boolean = true

    private fun isEmptyContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val ctx = ContainerItemContext.withConstant(stack)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        for (view in itemStorage) {
            if (view.amount > 0 && !view.resource.isBlank) return false
        }
        return true
    }

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
        lastOperationWasPrimarySlot = true
        return true
    }

    private fun tryFill(empty: ItemStack, outputSlot: ItemStack, isPrimarySlot: Boolean): Boolean {
        if (tankInternal.amount < FluidConstants.BUCKET || tankInternal.variant.isBlank) return false

        val ctx = ContainerItemContext.withConstant(empty)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        if (!itemStorage.supportsInsertion()) return false
        val fluid = tankInternal.variant.fluid
        if (empty.item == Items.BUCKET && fluid !in listOf(Fluids.WATER, Fluids.FLOWING_WATER, Fluids.LAVA, Fluids.FLOWING_LAVA)) return false
        lastOperationPourOut = false
        lastOperationWasPrimarySlot = isPrimarySlot
        return true
    }

    private fun canAcceptOutput(outputSlot: ItemStack, result: ItemStack): Boolean {
        if (outputSlot.isEmpty) return true
        return ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= result.maxCount
    }

    private fun completeCurrentOperation(wasFill: Boolean, wasPrimarySlot: Boolean) {
        if (!wasFill) {
            // Pour Out: 从流体容器倒入储罐
            val filled = getStack(SLOT_INPUT_FILLED)
            val outputSlot = getStack(SLOT_OUTPUT)
            Transaction.openOuter().use { tx ->
                @Suppress("DEPRECATION")
                val ctx = ContainerItemContext.withInitial(filled.copyWithCount(1))
                val itemStorage = ctx.find(FluidStorage.ITEM) ?: return@use
                for (view in itemStorage) {
                    if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                        val extracted = view.extract(view.resource, FluidConstants.BUCKET, tx)
                        if (extracted > 0) {
                            val inserted = tankInternal.insert(FluidVariant.of(view.resource.fluid), extracted, tx)
                           if (inserted == extracted) {
                               val emptyResult = ctx.itemVariant.toStack(1)
                               if (!canAcceptOutput(outputSlot, emptyResult)) return@use
                               filled.decrement(1)
                               if (filled.isEmpty) setStack(SLOT_INPUT_FILLED, ItemStack.EMPTY)

                                if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, emptyResult)
                                else outputSlot.increment(1)

                               syncTankState()
                               tx.commit()
                               return
                            }
                        }
                    }
                }
            }
                } else {
                    // Fill: 从储罐填充容器
                    val inputSlot = if (wasPrimarySlot) getStack(SLOT_INPUT_FILLED) else getStack(SLOT_INPUT_EMPTY)
                    val outputSlot = getStack(SLOT_OUTPUT)
                    val variant = tankInternal.variant
                    if (variant.isBlank) return
                    val fluid = variant.fluid

            Transaction.openOuter().use { tx ->
                @Suppress("DEPRECATION")
                val ctx = ContainerItemContext.withInitial(inputSlot.copyWithCount(1))
                val itemStorage = ctx.find(FluidStorage.ITEM) ?: return@use
                val inserted = itemStorage.insert(variant, FluidConstants.BUCKET, tx)
                if (inserted == FluidConstants.BUCKET) {
                    val extracted = tankInternal.extract(variant, inserted, tx)
                   if (extracted == inserted) {
                       val filledResult = ctx.itemVariant.toStack(1)
                       if (!canAcceptOutput(outputSlot, filledResult)) return@use
                       inputSlot.decrement(1)
                       if (inputSlot.isEmpty) {
                           if (wasPrimarySlot) setStack(SLOT_INPUT_FILLED, ItemStack.EMPTY)
                           else setStack(SLOT_INPUT_EMPTY, ItemStack.EMPTY)
                       }

                       if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, filledResult)
                        else outputSlot.increment(filledResult.count)

                        syncTankState()
                        tx.commit()
                    }
                }
            }
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
        markDirty()
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
