package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.ModBlockEntities
import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.WaterCell
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.isWaterFuel
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
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
import net.minecraft.world.World

@ModBlockEntity(block = SolarDistillerBlock::class)
class SolarDistillerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, IEjectorUpgradeSupport,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    companion object {
        const val SLOT_INPUT_WATER = 0
        const val SLOT_OUTPUT_EMPTY = 1
        const val SLOT_INPUT_CELL = 2
        const val SLOT_OUTPUT_CELL = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 8
        private const val NBT_INPUT_TANK = "InputTank"
        private const val NBT_OUTPUT_TANK = "OutputTank"

        private const val DAY_START_TICK = 333
        private const val DAY_END_TICK = 11750

        @Volatile
        private var fluidLookupRegistered = false

        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = ModBlockEntities.getType(SolarDistillerBlockEntity::class)
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData(this)
    val sync = SolarDistillerSync(syncedData)

    private val inputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 10
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == Fluids.WATER || variant.fluid == Fluids.FLOWING_WATER
        override fun canExtract(variant: FluidVariant): Boolean = false
        override fun onFinalCommit() {
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }
        fun setStoredWater(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }
        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity
        fun tryInsertWater(toInsert: Long): Long {
            if (toInsert <= 0L || (amount > 0L && variant.fluid != Fluids.WATER)) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != Fluids.WATER) variant = FluidVariant.of(Fluids.WATER)
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.WATER) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    private val outputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 10
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean =
            variant.fluid == ModFluids.DISTILLED_WATER_STILL || variant.fluid == ModFluids.DISTILLED_WATER_FLOWING
        override fun onFinalCommit() {
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }
        fun setStoredDistilled(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.DISTILLED_WATER_STILL) else FluidVariant.blank()
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }
        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity
        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            if (amount > 0L && variant.fluid != ModFluids.DISTILLED_WATER_STILL) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            return if (resource.fluid == Fluids.WATER || resource.fluid == Fluids.FLOWING_WATER) {
                inputTankInternal.insert(FluidVariant.of(Fluids.WATER), maxAmount, transaction)
            } else {
                0L
            }
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.DISTILLED_WATER_STILL && resource.fluid != ModFluids.DISTILLED_WATER_FLOWING) return 0L
            return outputTankInternal.extract(FluidVariant.of(ModFluids.DISTILLED_WATER_STILL), maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!inputTankInternal.variant.isBlank && inputTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = inputTankInternal.variant
                    override fun getAmount(): Long = inputTankInternal.amount
                    override fun getCapacity(): Long = inputTankInternal.getTankCapacity()
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!outputTankInternal.variant.isBlank && outputTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = outputTankInternal.variant
                    override fun getAmount(): Long = outputTankInternal.amount
                    override fun getCapacity(): Long = outputTankInternal.getTankCapacity()
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        outputTankInternal.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator() as MutableIterator<StorageView<FluidVariant>>
        }
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(SolarDistillerBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.solar_distiller")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SolarDistillerScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        inputTankInternal.setStoredWater(nbt.getLong(NBT_INPUT_TANK))
        outputTankInternal.setStoredDistilled(nbt.getLong(NBT_OUTPUT_TANK))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_INPUT_TANK, inputTankInternal.getStoredAmount())
        nbt.putLong(NBT_OUTPUT_TANK, outputTankInternal.getStoredAmount())
    }

    private fun canWorkNow(world: World, pos: BlockPos): Boolean {
        if (world.registryKey != World.OVERWORLD) return false
        if (world.isRaining) return false
        val time = world.timeOfDay % 24000
        if (time < DAY_START_TICK || time > DAY_END_TICK) return false
        val topY = world.topY
        var y = pos.y + 1
        while (y < topY) {
            val scanPos = BlockPos(pos.x, y, pos.z)
            val blockState = world.getBlockState(scanPos)
            if (blockState.isOpaqueFullCube(world, scanPos)) return false
            y++
        }
        return true
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        handleWaterInputSlot()
        handleDistilledCellFill()

        val canWork = canWorkNow(world, pos) &&
            inputTankInternal.getStoredAmount() >= SolarDistillerSync.PRODUCE_MB_PER_CYCLE &&
            outputTankInternal.getStoredAmount() < outputTankInternal.getTankCapacity()
        sync.isWorking = if (canWork) 1 else 0

        if (canWork) {
            sync.progress += 1
            if (sync.progress >= SolarDistillerSync.PRODUCE_INTERVAL_TICKS) {
                val consumed = inputTankInternal.consumeInternal(SolarDistillerSync.PRODUCE_MB_PER_CYCLE.toLong())
                if (consumed >= SolarDistillerSync.PRODUCE_MB_PER_CYCLE) {
                    outputTankInternal.insertInternal(SolarDistillerSync.PRODUCE_MB_PER_CYCLE.toLong())
                }
                sync.progress = 0
            }
        } else if (sync.progress != 0) {
            sync.progress = 0
        }

        val active = sync.isWorking != 0
        if (state.get(SolarDistillerBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(SolarDistillerBlock.ACTIVE, active))
        }
    }

    private fun handleWaterInputSlot() {
        val input = getStack(SLOT_INPUT_WATER)
        if (input.isEmpty) return

        val emptyOutput = getStack(SLOT_OUTPUT_EMPTY)
        val emptyCellItem = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell"))

        val emptyContainer = when {
            input.item == Items.WATER_BUCKET -> ItemStack(Items.BUCKET)
            input.item is WaterCell -> ItemStack(emptyCellItem)
            input.item is FluidCellItem && input.isWaterFuel() -> ItemStack(emptyCellItem)
            else -> ItemStack.EMPTY
        }
        if (emptyContainer.isEmpty) return
        if (inputTankInternal.getTankCapacity() - inputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        val canAcceptEmpty = emptyOutput.isEmpty ||
            (ItemStack.areItemsEqual(emptyOutput, emptyContainer) && emptyOutput.count < emptyOutput.maxCount)
        if (!canAcceptEmpty) return

        if (inputTankInternal.tryInsertWater(FluidConstants.BUCKET) < FluidConstants.BUCKET) return

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT_WATER, ItemStack.EMPTY)
        if (emptyOutput.isEmpty) {
            setStack(SLOT_OUTPUT_EMPTY, emptyContainer)
        } else {
            emptyOutput.increment(1)
        }
        markDirty()
    }

    private fun handleDistilledCellFill() {
        if (outputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return
        val input = getStack(SLOT_INPUT_CELL)
        if (input.isEmpty) return

        val distilledCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "distilled_water_cell")))
        val outputCell = when {
            input.item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")) -> distilledCell
            input.item is FluidCellItem && input.isFluidCellEmpty() -> {
                ItemStack(input.item).apply { setFluidCellVariant(FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)) }
            }
            else -> ItemStack.EMPTY
        }
        if (outputCell.isEmpty) return

        val output = getStack(SLOT_OUTPUT_CELL)
        val canAccept = output.isEmpty ||
            (ItemStack.canCombine(output, outputCell) && output.count < output.maxCount)
        if (!canAccept) return

        if (outputTankInternal.consumeInternal(FluidConstants.BUCKET) < FluidConstants.BUCKET) return

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT_CELL, ItemStack.EMPTY)
        if (output.isEmpty) setStack(SLOT_OUTPUT_CELL, outputCell)
        else output.increment(1)
        markDirty()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == (world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH)) return null
        return ioStorage
    }
}
