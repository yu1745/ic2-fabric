package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.recipes.ModTags
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.screen.SemifluidGeneratorScreenHandler
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
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
import net.minecraft.server.world.ServerWorld
import ic2_120.content.network.NetworkManager

@ModBlockEntity(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, IGenerator, IFluidPipeUpgradeSupport, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = SemifluidGeneratorBlock.ACTIVE

    /** 客户端：由网络包更新的燃料颜色 ARGB */
    @Volatile
    var clientFuelColorArgb: Int = 0xFFCC4400.toInt()
    private var fuelColorDirty = true

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    data class FuelProfile(
        val euPerBucket: Long,
        val euPerTick: Long
    )

    companion object {
        const val GENERATOR_TIER = 1
        private const val NBT_FUEL_AMOUNT = "FuelAmount"

        const val FUEL_SLOT = 0
        const val EMPTY_CONTAINER_SLOT = 1
        const val BATTERY_SLOT = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 7

        private val BIOFUEL_PROFILE = FuelProfile(euPerBucket = 32_000L, euPerTick = 16L)
        private val CREOSOTE_PROFILE = FuelProfile(euPerBucket = 3_200L, euPerTick = 8L)

        /** 从 ModFluids 注册的 tint 颜色映射取色；无匹配则返回默认橙 */
        fun getFuelArgb(fluid: net.minecraft.fluid.Fluid?): Int {
            return when (fluid) {
                null -> 0xFFCC4400.toInt()
                else -> ModFluids.getFluidTintOrNull(fluid) ?: 0xFFCC4400.toInt()
            }
        }

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = SemifluidGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }

        fun isSupportedFuelFluid(fluid: net.minecraft.fluid.Fluid): Boolean = getFuelProfile(fluid) != null

        fun getFuelProfile(fluid: net.minecraft.fluid.Fluid): FuelProfile? {
            // Fluid.isIn(TagKey) 与 Fluid.getRegistryEntry() 在 1.20.1 均已 @Deprecated，
            // 改用未废弃的 FluidState.isIn（其内部委托 RegistryEntry.isIn）。
            val state = fluid.defaultState
            return when {
                state.isIn(ModTags.Compat.Fluids.SEMIFLUID_BIOFUEL_EQUIVALENT) -> BIOFUEL_PROFILE
                state.isIn(ModTags.Compat.Fluids.SEMIFLUID_CREOSOTE_EQUIVALENT) -> CREOSOTE_PROFILE
                else -> null
            }
        }
    }

    override val tier: Int = GENERATOR_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { isValid(BATTERY_SLOT, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(FUEL_SLOT), matcher = { isValid(FUEL_SLOT, it) })
        ),
        extractSlots = intArrayOf(FUEL_SLOT, EMPTY_CONTAINER_SLOT, BATTERY_SLOT, *SLOT_UPGRADE_INDICES),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = SemifluidGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val fuelTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean = isSupportedFuelFluid(variant.fluid) && ModFluids.isFluid(variant.fluid)

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.fuelAmount = amount.toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) Registries.FLUID.getRawId(variant.fluid) else -1
            fuelColorDirty = true
            markDirty()
        }

        fun setStoredFuel(newAmount: Long, fluid: net.minecraft.fluid.Fluid?) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L && fluid != null && isSupportedFuelFluid(fluid)) {
                FluidVariant.of(fluid)
            } else {
                FluidVariant.blank()
            }
            sync.fuelAmount = amount.toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) Registries.FLUID.getRawId(variant.fluid) else -1
            fuelColorDirty = true
        }

        fun tryInsertFuel(fluid: net.minecraft.fluid.Fluid, toInsert: Long): Long {
            if (toInsert <= 0L || !isSupportedFuelFluid(fluid)) return 0L
            if (amount > 0L && variant.fluid != fluid) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != fluid) variant = FluidVariant.of(fluid)
            sync.fuelAmount = amount.toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = Registries.FLUID.getRawId(variant.fluid)
            fuelColorDirty = true
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || !isSupportedFuelFluid(variant.fluid)) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.fuelAmount = amount.toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) Registries.FLUID.getRawId(variant.fluid) else -1
            fuelColorDirty = true
            return actual
        }
    }

    val fuelTank: Storage<FluidVariant> = fuelTankInternal

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0L }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        SemifluidGeneratorBlockEntity::class.type(),
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
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            FUEL_SLOT -> stack.isSemifluidFuel()
            EMPTY_CONTAINER_SLOT -> false
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == FUEL_SLOT -> stack.isSemifluidFuel()
        slot == EMPTY_CONTAINER_SLOT -> false
        slot == BATTERY_SLOT -> stack.canBeCharged()
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
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

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.semifluid_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SemifluidGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(SemifluidGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, SemifluidGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val fluidId = nbt.getString("FuelFluid")
        val fluid = if (fluidId.isNullOrBlank()) null else Registries.FLUID.get(Identifier(fluidId))
        fuelTankInternal.setStoredFuel(nbt.getLong(NBT_FUEL_AMOUNT), fluid)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(SemifluidGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_FUEL_AMOUNT, fuelTankInternal.amount)
        if (fuelTankInternal.amount > 0L && !fuelTankInternal.variant.isBlank) {
            nbt.putString("FuelFluid", Registries.FLUID.getId(fuelTankInternal.variant.fluid).toString())
        }
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, fuelTankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, fuelTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        // 燃料颜色变化时通过网络包同步到客户端
        if (fuelColorDirty) {
            fuelColorDirty = false
            val color = getFuelArgb(if (fuelTankInternal.amount > 0L) fuelTankInternal.variant.fluid else null)
            NetworkManager.sendSemifluidGeneratorFuelState(world as ServerWorld, pos, color)
        }

        val fuelStack = getStack(FUEL_SLOT)
        // 油箱剩余空间不足 1 桶时跳过燃料处理，防止燃料单元被部分抽入后永不消耗（无限燃料 bug）
        val tankTotalCapacity = 8 * FluidConstants.BUCKET
        if (fuelTankInternal.amount <= tankTotalCapacity - FluidConstants.BUCKET) {
            val fuelFluid = fuelStack.getSemifluidFuelFluid()
            val fuelProfile = fuelFluid?.let { getFuelProfile(it) }
            if (fuelFluid != null && fuelProfile != null) {
                val emptyContainer = when (fuelStack.item) {
                    Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "biofuel_cell")),
                    Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) ->
                        ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
                    else -> fuelStack.item.getRecipeRemainder(fuelStack)
                }
                if (canInsertEmptyContainer(emptyContainer)) {
                    val inserted = fuelTankInternal.tryInsertFuel(fuelFluid, FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyContainer)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
        }

        val space = (SemifluidGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        val fuelFluid = fuelTankInternal.variant.fluid
        val fuelProfile = getFuelProfile(fuelFluid)
        if (space > 0L && fuelTankInternal.amount > 0L && fuelProfile != null) {
            val consumePerTick = (FluidConstants.BUCKET * fuelProfile.euPerTick / fuelProfile.euPerBucket).coerceAtLeast(1L)
            val toConsume = minOf(consumePerTick, fuelTankInternal.amount, space * consumePerTick / fuelProfile.euPerTick)
            val consumed = fuelTankInternal.consumeInternal(toConsume)
            if (consumed > 0L) {
                val euToAdd = consumed * fuelProfile.euPerTick / consumePerTick
                sync.generateEnergy(minOf(euToAdd, space))
                markDirty()
            }
        }

        batteryCharger.tick()

        val active = sync.amount < SemifluidGeneratorSync.ENERGY_CAPACITY &&
            fuelTankInternal.amount > 0L &&
            getFuelProfile(fuelTankInternal.variant.fluid) != null
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return fuelTank
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
}
