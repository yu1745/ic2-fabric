package ic2_120.content.block.machines

import ic2_120.content.block.FluidBottlerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.FluidBottlerScreenHandler
import ic2_120.content.sync.FluidBottlerSync
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
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
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
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.fluidToFilledCellStack

/**
 * 流体装罐机方块实体。
 * 槽位：满流体容器(A)、空容器(B)、输出、放电、4 升级
 * 逻辑：若 A 和 B 都有物品，优先将 A 倒入储罐；否则将储罐灌入 B。
 */
@ModBlockEntity(block = FluidBottlerBlock::class)
class FluidBottlerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    override val tier: Int = FluidBottlerSync.FLUID_BOTTLER_TIER

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
        const val INVENTORY_SIZE = 8
        private const val NBT_FLUID_AMOUNT = "FluidAmount"
        private const val NBT_FLUID_VARIANT = "FluidVariant"
        private const val TANK_CAPACITY_BUCKETS = 10
        private val TANK_CAPACITY = FluidConstants.BUCKET * TANK_CAPACITY_BUCKETS

        @Volatile
        private var fluidLookupRegistered = false

        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> (be as FluidBottlerBlockEntity).getFluidStorageForSide(side) }, FluidBottlerBlockEntity::class.type())
            fluidLookupRegistered = true
        }
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = FluidBottlerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(FluidBottlerSync.FLUID_BOTTLER_TIER + voltageTierBonus) }
    )

    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = true
        override fun canExtract(variant: FluidVariant): Boolean = true
        override fun onFinalCommit() {
            sync.fluidAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.fluidCapacityMb = (TANK_CAPACITY * 1000L / FluidConstants.BUCKET).toInt()
            markDirty()
        }
    }

    val fluidTank: Storage<FluidVariant> = tankInternal

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { FluidBottlerSync.FLUID_BOTTLER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(FluidBottlerBlockEntity::class.type(), pos, state)

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

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.fluid_bottler")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        FluidBottlerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(FluidBottlerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        tankInternal.amount = nbt.getLong(NBT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val fluidTag = nbt.getCompound(NBT_FLUID_VARIANT)
        tankInternal.variant = if (fluidTag.isEmpty) FluidVariant.blank() else FluidVariant.fromNbt(fluidTag)
        sync.fluidAmountMb = (tankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        sync.fluidCapacityMb = (TANK_CAPACITY * 1000L / FluidConstants.BUCKET).toInt()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(FluidBottlerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_FLUID_AMOUNT, tankInternal.amount)
        if (!tankInternal.variant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, tankInternal.variant.toNbt())
        }
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val facing = world?.getBlockState(pos)?.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        if (side == facing) return null
        return fluidTank
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val filled = getStack(SLOT_INPUT_FILLED)
        val empty = getStack(SLOT_INPUT_EMPTY)
        val outputSlot = getStack(SLOT_OUTPUT)

        // 优先：若 A 和 B 都有物品，先倒 A 入储罐
        val doPourOut = !filled.isEmpty
        val doFill = !empty.isEmpty && filled.isEmpty && tankInternal.amount >= FluidConstants.BUCKET

        val operating = when {
            doPourOut -> tryPourOut(filled, outputSlot)
            doFill -> tryFill(empty, outputSlot)
            else -> false
        }

        if (operating) {
            val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
            val need = (FluidBottlerSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
            if (sync.consumeEnergy(need) > 0L) {
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                sync.progress += progressIncrement
                if (sync.progress >= FluidBottlerSync.PROGRESS_MAX) {
                    completeCurrentOperation(doPourOut)
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
        val emptyResult = getEmptyContainerFor(filled) ?: return false
        if (!canAcceptOutput(outputSlot, emptyResult)) return false
        lastOperationPourOut = true
        return true
    }

    private fun tryFill(empty: ItemStack, outputSlot: ItemStack): Boolean {
        if (tankInternal.amount < FluidConstants.BUCKET || tankInternal.variant.isBlank) return false
        val ctx = ContainerItemContext.withConstant(empty)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        if (!itemStorage.supportsInsertion()) return false
        val fluid = tankInternal.variant.fluid
        val filledResult = when (empty.item) {
            Items.BUCKET -> when (fluid) {
                Fluids.WATER, Fluids.FLOWING_WATER -> ItemStack(Items.WATER_BUCKET)
                Fluids.LAVA, Fluids.FLOWING_LAVA -> ItemStack(Items.LAVA_BUCKET)
                else -> return false  // 桶只支持水/岩浆
            }
            else -> fluidToFilledCellStack(fluid)
        }
        if (!canAcceptOutput(outputSlot, filledResult)) return false
        lastOperationPourOut = false
        return true
    }

    private fun getEmptyContainerFor(filled: ItemStack): ItemStack? = when (filled.item) {
        is BucketItem -> ItemStack(Items.BUCKET)
        is ModFluidCell -> ItemStack((filled.item as ModFluidCell).getEmptyCell())
        else -> {
            val path = Registries.ITEM.getId(filled.item).path
            if (path == "fluid_cell" || path.endsWith("_cell")) {
                ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
            } else null
        }
    }

    private fun canAcceptOutput(outputSlot: ItemStack, result: ItemStack): Boolean {
        if (outputSlot.isEmpty) return true
        return ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= result.maxCount
    }

    private fun completeCurrentOperation(wasPourOut: Boolean) {
        if (wasPourOut) {
            val filled = getStack(SLOT_INPUT_FILLED)
            val outputSlot = getStack(SLOT_OUTPUT)
            val ctx = ContainerItemContext.withConstant(filled)
            val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
            Transaction.openOuter().use { tx ->
                for (view in itemStorage) {
                    if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                        val extracted = view.extract(view.resource, FluidConstants.BUCKET, tx)
                        if (extracted > 0) {
                            val inserted = tankInternal.insert(FluidVariant.of(view.resource.fluid), extracted, tx)
                            if (inserted > 0) {
                                tx.commit()
                                val emptyResult = ctx.itemVariant.toStack(ctx.amount.toInt().coerceAtLeast(1))
                                filled.decrement(1)
                                if (filled.isEmpty) setStack(SLOT_INPUT_FILLED, ItemStack.EMPTY)
                                if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, emptyResult)
                                else outputSlot.increment(emptyResult.count)
                                sync.fluidAmountMb = (tankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
                                return
                            }
                        }
                    }
                }
            }
        } else {
            val empty = getStack(SLOT_INPUT_EMPTY)
            val outputSlot = getStack(SLOT_OUTPUT)
            val ctx = ContainerItemContext.withConstant(empty)
            val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
            val variant = tankInternal.variant
            if (variant.isBlank) return
            val fluid = variant.fluid
            Transaction.openOuter().use { tx ->
                val inserted = itemStorage.insert(variant, FluidConstants.BUCKET, tx)
                if (inserted > 0) {
                    val extracted = tankInternal.extract(variant, inserted, tx)
                    if (extracted > 0) {
                        tx.commit()
                        val filledResult = when (empty.item) {
                            Items.BUCKET -> when (fluid) {
                                Fluids.WATER, Fluids.FLOWING_WATER -> ItemStack(Items.WATER_BUCKET)
                                Fluids.LAVA, Fluids.FLOWING_LAVA -> ItemStack(Items.LAVA_BUCKET)
                                else -> return@use
                            }
                            else -> fluidToFilledCellStack(fluid)
                        }
                        empty.decrement(1)
                        if (empty.isEmpty) setStack(SLOT_INPUT_EMPTY, ItemStack.EMPTY)
                        if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, filledResult)
                        else outputSlot.increment(filledResult.count)
                        sync.fluidAmountMb = (tankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
                    }
                }
            }
        }
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(FluidBottlerBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(FluidBottlerBlock.ACTIVE, active))
        }
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
}
