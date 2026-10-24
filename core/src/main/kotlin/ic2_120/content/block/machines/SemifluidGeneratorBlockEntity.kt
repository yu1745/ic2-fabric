package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.Ic2_120
import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.fluid.FluidFuelRegistry
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
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.server.level.ServerLevel
import ic2_120.content.network.NetworkManager

@ModBlockEntity(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, IGenerator, IFluidPipeUpgradeSupport, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = SemifluidGeneratorBlock.ACTIVE

    /** 客户端：由网络包更新的燃料颜色 ARGB */
    @Volatile
    var clientFuelColorArgb: Int = 0xFFCC4400.toInt()
    private var fuelColorDirty = true

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

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

        /** 从 ModFluids 注册的 tint 颜色映射取色；无匹配则返回默认橙 */
        fun getFuelArgb(fluid: net.minecraft.world.level.material.Fluid?): Int {
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

    }

    override val tier: Int = GENERATOR_TIER
    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { canPlaceItem(BATTERY_SLOT, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(FUEL_SLOT), matcher = { canPlaceItem(FUEL_SLOT, it) })
        ),
        extractSlots = intArrayOf(FUEL_SLOT, EMPTY_CONTAINER_SLOT, BATTERY_SLOT, *SLOT_UPGRADE_INDICES),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = SemifluidGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val fuelTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean = FluidFuelRegistry.isSupported(variant.fluid) && !variant.isBlank

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.fuelAmount = amount.toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) BuiltInRegistries.FLUID.getId(variant.fluid) else -1
            fuelColorDirty = true
            setChanged()
        }

        fun setStoredFuel(newAmount: Long, fluid: net.minecraft.world.level.material.Fluid?) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L && fluid != null && FluidFuelRegistry.isSupported(fluid)) {
                FluidVariant.of(fluid)
            } else {
                FluidVariant.blank()
            }
            sync.fuelAmount = amount.toInt().coerceAtLeast(0)
            sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) BuiltInRegistries.FLUID.getId(variant.fluid) else -1
            fuelColorDirty = true
        }

        fun tryInsertFuel(fluid: net.minecraft.world.level.material.Fluid, toInsert: Long): Long {
            if (toInsert <= 0L || !FluidFuelRegistry.isSupported(fluid)) return 0L
            if (amount > 0L && variant.fluid != fluid) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.fluid != fluid) variant = FluidVariant.of(fluid)
                tx.commit()
                sync.fuelAmount = amount.toInt().coerceAtLeast(0)
                sync.fuelFluidRawId = BuiltInRegistries.FLUID.getId(variant.fluid)
                fuelColorDirty = true
                actual
            }
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || !FluidFuelRegistry.isSupported(variant.fluid)) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
                sync.fuelAmount = amount.toInt().coerceAtLeast(0)
                sync.fuelFluidRawId = if (amount > 0L && !variant.isBlank) BuiltInRegistries.FLUID.getId(variant.fluid) else -1
                fuelColorDirty = true
                actual
            }
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

    override fun getInventory(): Container = this
    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        setChanged()
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

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == FUEL_SLOT -> stack.isSemifluidFuel()
        slot == EMPTY_CONTAINER_SLOT -> false
        slot == BATTERY_SLOT -> stack.canBeCharged()
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    private fun tryInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getItem(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) {
            setItem(EMPTY_CONTAINER_SLOT, emptyStack.copy())
            true
        } else if (ItemStack.isSameItemSameComponents(current, emptyStack)) {
            val toAdd = minOf(emptyStack.count, current.maxStackSize - current.count)
            if (toAdd > 0) {
                current.grow(toAdd)
                setChanged()
                true
            } else false
        } else false
    }

    private fun canInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getItem(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) true
        else ItemStack.isSameItemSameComponents(current, emptyStack) && current.count + emptyStack.count <= current.maxStackSize
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.size)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.semifluid_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        SemifluidGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(SemifluidGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, SemifluidGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val fluidId = nbt.getString("FuelFluid")
        val fluid = if (fluidId.isNullOrBlank()) null else BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId))
        fuelTankInternal.setStoredFuel(nbt.getLong(NBT_FUEL_AMOUNT), fluid)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(SemifluidGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_FUEL_AMOUNT, fuelTankInternal.amount)
        if (fuelTankInternal.amount > 0L && !fuelTankInternal.variant.isBlank) {
            nbt.putString("FuelFluid", BuiltInRegistries.FLUID.getKey(fuelTankInternal.variant.fluid).toString())
        }
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
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
            NetworkManager.sendSemifluidGeneratorFuelState(world as ServerLevel, pos, color)
        }

        val fuelStack = getItem(FUEL_SLOT)
        // 油箱剩余空间不足 1 桶时跳过燃料处理，防止燃料单元被部分抽入后永不消耗（无限燃料 bug）
        val tankTotalCapacity = 8 * FluidConstants.BUCKET
        if (fuelTankInternal.amount <= tankTotalCapacity - FluidConstants.BUCKET) {
            val fuelFluid = fuelStack.getSemifluidFuelFluid()
            val fuelProfile = fuelFluid?.let { FluidFuelRegistry.getProfile(it) }
            if (fuelFluid != null && fuelProfile != null) {
                val emptyContainer = when (fuelStack.item) {
                    BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "biofuel_cell")),
                    BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "fluid_cell")) ->
                        ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "empty_cell")))
                    else -> fuelStack.item.getRecipeRemainder(fuelStack)
                }
                if (canInsertEmptyContainer(emptyContainer)) {
                    val inserted = fuelTankInternal.tryInsertFuel(fuelFluid, FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyContainer)) {
                        fuelStack.shrink(1)
                        if (fuelStack.isEmpty) setItem(FUEL_SLOT, ItemStack.EMPTY)
                        setChanged()
                    }
                }
            }
        }

        val space = (SemifluidGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        val fuelFluid = fuelTankInternal.variant.fluid
        val fuelProfile = FluidFuelRegistry.getProfile(fuelFluid)
        if (space > 0L && fuelTankInternal.amount > 0L && fuelProfile != null) {
            val consumePerTick = (FluidConstants.BUCKET * fuelProfile.euPerTick / fuelProfile.euPerBucket).coerceAtLeast(1L)
            val toConsume = minOf(consumePerTick, fuelTankInternal.amount, space * consumePerTick / fuelProfile.euPerTick)
            val consumed = fuelTankInternal.consumeInternal(toConsume)
            if (consumed > 0L) {
                val euToAdd = consumed * fuelProfile.euPerTick / consumePerTick
                sync.generateEnergy(minOf(euToAdd, space))
                setChanged()
            }
        }

        batteryCharger.tick()

        val active = sync.amount < SemifluidGeneratorSync.ENERGY_CAPACITY &&
            fuelTankInternal.amount > 0L &&
            FluidFuelRegistry.isSupported(fuelTankInternal.variant.fluid)
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return fuelTank
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH
}
