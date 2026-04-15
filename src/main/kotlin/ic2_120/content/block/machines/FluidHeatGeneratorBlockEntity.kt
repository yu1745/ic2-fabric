package ic2_120.content.block.machines

import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.screen.FluidHeatGeneratorScreenHandler
import ic2_120.content.sync.FluidHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
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
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 流体加热机（Liquid Fuel Firebox）。
 * - 支持沼气（Biofuel）
 * - 每秒结算：消耗 10mB/s，产 32HU/t（等价 640HU/s）
 * - 仅背面单面传热
 */
@ModBlockEntity(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatGeneratorBlockEntityBase(type, pos, state), Inventory, IFluidPipeUpgradeSupport, Storage<ItemVariant>, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = FluidHeatGeneratorBlock.ACTIVE

    // 流体管道升级支持属性
    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    enum class FuelType(
        val fluid: net.minecraft.fluid.Fluid,
        val heatPerTick: Long,
        val mbPerSecond: Long,
        private val matcher: (net.minecraft.fluid.Fluid) -> Boolean
    ) {
        BIOFUEL(ModFluids.BIOFUEL_STILL, 32L, 10L, { fluid ->
            fluid == ModFluids.BIOFUEL_STILL || fluid == ModFluids.BIOFUEL_FLOWING
        });

        val heatPerSecond = heatPerTick * 20L

        fun matches(fluid: net.minecraft.fluid.Fluid): Boolean = matcher(fluid)

        companion object {
            fun fromFluid(fluid: net.minecraft.fluid.Fluid?): FuelType? {
                if (fluid == null) return null
                return values().firstOrNull { it.matches(fluid) }
            }
        }
    }

    companion object {
        private const val NBT_FUEL_AMOUNT = "FuelAmount"
        private const val NBT_BUFFERED_HEAT = "BufferedHeat"
        private const val NBT_FUEL_TYPE = "FuelType"
        private const val NBT_FUEL_MB_ACCUMULATOR = "FuelMbAccumulator"

        const val FUEL_SLOT = 0
        const val EMPTY_CONTAINER_SLOT = 1
        const val SLOT_UPGRADE_0 = 2
        const val SLOT_UPGRADE_1 = 3
        const val SLOT_UPGRADE_2 = 4
        const val SLOT_UPGRADE_3 = 5
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 6

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = FluidHeatGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(FUEL_SLOT), matcher = { isValid(FUEL_SLOT, it) })
        ),
        extractSlots = intArrayOf(FUEL_SLOT, EMPTY_CONTAINER_SLOT),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)
    override val heatFlow = HeatFlowSync(syncedData, this)
    val sync = FluidHeatGeneratorSync(syncedData, heatFlow)

    private var currentFuelType: FuelType? = null

    private var fuelMbAccumulator: Long = 0L

    private val fuelTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canExtract(variant: FluidVariant): Boolean = false
        override fun canInsert(variant: FluidVariant): Boolean = isSupportedFuelFluid(variant.fluid)
        override fun onFinalCommit() {
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        fun tryConsume(amountToConsume: Long): Long {
            if (amountToConsume <= 0L || !isSupportedFuelFluid(variant.fluid)) return 0L
            val actual = minOf(amountToConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }

        fun setStoredFuel(newAmount: Long, fluid: net.minecraft.fluid.Fluid?) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L && fluid != null && isSupportedFuelFluid(fluid)) {
                FluidVariant.of(fluid)
            } else {
                FluidVariant.blank()
            }
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }

        fun tryInsertFuel(fluid: net.minecraft.fluid.Fluid, toInsert: Long): Long {
            if (toInsert <= 0L || !isSupportedFuelFluid(fluid)) return 0L
            if (amount > 0L && variant.fluid != fluid) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != fluid) variant = FluidVariant.of(fluid)
            sync.fuelAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    val fuelTank: Storage<FluidVariant> = fuelTankInternal

    constructor(pos: BlockPos, state: BlockState) : this(
        FluidHeatGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        val isUpgradeSlot = slot in SLOT_UPGRADE_0..SLOT_UPGRADE_3
        if (isUpgradeSlot && !stack.isEmpty && stack.count > 1) {
            val single = stack.copy()
            single.count = 1
            inventory[slot] = single
        } else {
            inventory[slot] = stack
            if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        }
        markDirty()
    }

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            FUEL_SLOT -> isSupportedFuelContainer(stack)
            EMPTY_CONTAINER_SLOT -> false // 禁止手动放入空容器槽
            in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
        itemStorage.insert(resource, maxAmount, transaction)

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
        itemStorage.extract(resource, maxAmount, transaction)

    override fun iterator(): MutableIterator<StorageView<ItemVariant>> = itemStorage.iterator()

    private fun isSupportedFuelContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        // 支持沼气桶
        if (item == Items.BUCKET) return false // 空桶不是燃料
        // 检查是否是沼气桶
        val itemId = Registries.ITEM.getId(item)
        if (itemId.path == "biofuel_bucket" && itemId.namespace == "ic2_120") return true
        // 检查是否是沼气单元
        if (itemId.path == "biofuel_cell" && itemId.namespace == "ic2_120") return true
        // 检查是否是流体单元（包含沼气）
        if (itemId.path == "fluid_cell" && itemId.namespace == "ic2_120") {
            val fluid = stack.getFluidCellVariant()?.fluid
            return fluid != null && isSupportedFuelFluid(fluid)
        }
        return false
    }

    private fun tryInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getStack(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) {
            setStack(EMPTY_CONTAINER_SLOT, emptyStack.copy())
            true
        } else if (ItemStack.canCombine(current, emptyStack)) {
            val toAdd = minOf(emptyStack.count, current.maxCount - current.count)
            if (toAdd > 0) {
                current.increment(toAdd)
                markDirty()
                true
            } else false
        } else false
    }

    private fun canInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getStack(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) true
        else ItemStack.canCombine(current, emptyStack) && current.count + emptyStack.count <= current.maxCount
    }

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.fluid_heat_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        FluidHeatGeneratorScreenHandler(
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
        val fluidId = nbt.getString("FuelFluid")
        val fluid = if (fluidId.isNullOrBlank()) null else Registries.FLUID.get(Identifier(fluidId))
        fuelTankInternal.setStoredFuel(nbt.getLong(NBT_FUEL_AMOUNT), fluid)
        val fuelTypeName = nbt.getString(NBT_FUEL_TYPE)
        currentFuelType = if (fuelTypeName.isNotBlank()) FuelType.valueOf(fuelTypeName) else null
        fuelMbAccumulator = nbt.getLong(NBT_FUEL_MB_ACCUMULATOR)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_FUEL_AMOUNT, fuelTankInternal.amount)
        if (fuelTankInternal.amount > 0L && !fuelTankInternal.variant.isBlank) {
            nbt.putString("FuelFluid", Registries.FLUID.getId(fuelTankInternal.variant.fluid).toString())
        }
        currentFuelType?.let { nbt.putString(NBT_FUEL_TYPE, it.name) }
        nbt.putLong(NBT_FUEL_MB_ACCUMULATOR, fuelMbAccumulator)
    }

    override fun generateHeat(world: World, pos: BlockPos, state: BlockState): Long {
        val fuelType = currentFuelType ?: FuelType.fromFluid(fuelTankInternal.variant.fluid)
        if (fuelType != null) {
            currentFuelType = fuelType
            val consumePerTickMilliBuckets = FluidConstants.BUCKET * fuelType.mbPerSecond / 1000L / 20 * 1000
            fuelMbAccumulator += consumePerTickMilliBuckets
            val toConsume = fuelMbAccumulator / 1000
            fuelMbAccumulator %= 1000
            if (toConsume > 0L) {
                val consumed = fuelTankInternal.tryConsume(toConsume)
                if (consumed > 0L) {
                    markDirty()
                    return fuelType.heatPerTick
                } else {
                    currentFuelType = null
                }
            }
        }
        return 0L
    }

    override fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean =
        fuelTankInternal.amount > 0L && hasValidConsumer

    override fun getActiveState(state: BlockState): Boolean =
        state.get(FluidHeatGeneratorBlock.ACTIVE)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        processFuelContainers()
        if (fluidPipeProviderEnabled) {
            ejectFluidToNeighbors(world, pos, state)
        }
        tickHeatMachine(world, pos, state)
    }

    private fun processFuelContainers() {
        val fuelStack = getStack(FUEL_SLOT)
        when {
            fuelStack.item == Registries.ITEM.get(Identifier("ic2_120", "biofuel_bucket")) -> {
                val emptyBucket = ItemStack(Items.BUCKET)
                if (canInsertEmptyContainer(emptyBucket)) {
                    val inserted = fuelTankInternal.tryInsertFuel(ModFluids.BIOFUEL_STILL, FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyBucket)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
            fuelStack.item == Registries.ITEM.get(Identifier("ic2_120", "biofuel_cell")) -> {
                val emptyCell = ItemStack(Registries.ITEM.get(Identifier("ic2_120", "empty_cell")))
                if (canInsertEmptyContainer(emptyCell)) {
                    val inserted = fuelTankInternal.tryInsertFuel(ModFluids.BIOFUEL_STILL, FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyCell)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
            fuelStack.item == Registries.ITEM.get(Identifier("ic2_120", "fluid_cell")) -> {
                val fluid = fuelStack.getFluidCellVariant()?.fluid
                if (fluid != null && isSupportedFuelFluid(fluid)) {
                    val emptyCell = ItemStack(Registries.ITEM.get(Identifier("ic2_120", "empty_cell")))
                    if (canInsertEmptyContainer(emptyCell)) {
                        val inserted = fuelTankInternal.tryInsertFuel(fluid, FluidConstants.BUCKET)
                        if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyCell)) {
                            fuelStack.decrement(1)
                            if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                            markDirty()
                        }
                    }
                }
            }
        }
    }

    private fun ejectFluidToNeighbors(world: World, pos: BlockPos, state: BlockState) {
        if (fuelTankInternal.amount <= 0L || fuelTankInternal.variant.isBlank) return
        val front = state.get(Properties.HORIZONTAL_FACING)
        val ejectSide = fluidPipeProviderSide
        for (dir in Direction.values()) {
            if (dir == front) continue
            if (ejectSide != null && dir != ejectSide) continue
            val neighbor = FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
            val resource = fuelTankInternal.variant
            val maxPerTick = minOf(FluidConstants.BUCKET / 4, fuelTankInternal.amount)
            Transaction.openOuter().use { tx ->
                val extracted = fuelTankInternal.extract(resource, maxPerTick, tx)
                if (extracted <= 0L) return@use
                val accepted = neighbor.insert(resource, extracted, tx)
                if (accepted <= 0L) return@use
                if (accepted < extracted) {
                    fuelTankInternal.insert(resource, extracted - accepted, tx)
                }
                tx.commit()
            }
            if (fuelTankInternal.amount <= 0L) break
        }
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == getFrontFacing()) return null
        return fuelTank
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH

    private fun isSupportedFuelFluid(fluid: net.minecraft.fluid.Fluid): Boolean {
        return fluid == ModFluids.BIOFUEL_STILL || fluid == ModFluids.BIOFUEL_FLOWING
    }
}
