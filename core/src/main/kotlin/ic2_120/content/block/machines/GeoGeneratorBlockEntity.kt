package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.LavaCell
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.canBeCharged
import ic2_120.Ic2_120
import ic2_120.content.sync.GeoGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.content.fluid.ModFluids
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
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.core.registries.BuiltInRegistries

/**
 * 地热发电机方块实体。
 * - 仅接受岩浆流体输入
 * - 内部流体缓存：8 桶
 * - 输出：20 EU/t（Tier 1）
 * - 1 桶岩浆燃烧 60 秒（1200 ticks）
 * - 按岩浆量消耗，电满时暂停（不消耗岩浆）
 *
 * 升级支持：
 * - 流体抽取升级：作为 receiver 从管道接收岩浆
 */
@ModBlockEntity(block = GeoGeneratorBlock::class)
class GeoGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    companion object {
        const val GENERATOR_TIER = 1
        private const val NBT_LAVA_AMOUNT = "LavaAmount"
        private const val NBT_LAVA_CONSUMPTION_REMAINDER = "LavaConsumptionRemainder"

        const val FUEL_SLOT = 0
        const val EMPTY_CONTAINER_SLOT = 1
        const val BATTERY_SLOT = 2
        const val INVENTORY_SIZE = 3

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = GeoGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    override val tier: Int = GENERATOR_TIER

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = GeoGeneratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.loop(
        soundId = "generator.geothermal.loop",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { canPlaceItem(BATTERY_SLOT, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(FUEL_SLOT), matcher = { canPlaceItem(FUEL_SLOT, it) })
        ),
        extractSlots = intArrayOf(FUEL_SLOT, EMPTY_CONTAINER_SLOT, BATTERY_SLOT),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = GeoGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val lavaTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean = variant.fluid == Fluids.LAVA && !variant.isBlank

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.lavaAmount = amount.toInt().coerceAtLeast(0)
            setChanged()
        }

        fun setStoredLava(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.LAVA) else FluidVariant.blank()
            sync.lavaAmount = amount.toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount

        fun hasAtLeastOneLavaBucket(): Boolean =
            amount >= FluidConstants.BUCKET && variant.fluid == Fluids.LAVA

        fun canAcceptFullBucket(): Boolean = (tankCapacity - amount) >= FluidConstants.BUCKET

        /** 尝试插入岩浆（用于岩浆桶倒入），返回实际插入量（droplets） */
        fun tryInsertLava(toInsert: Long): Long {
            if (toInsert <= 0L || (amount > 0L && variant.fluid != Fluids.LAVA)) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.fluid != Fluids.LAVA) variant = FluidVariant.of(Fluids.LAVA)
                tx.commit()
                sync.lavaAmount = amount.toInt().coerceAtLeast(0)
                actual
            }
        }

        /** 内部消耗岩浆（不经过 extract，因 canExtract=false 会阻止外部抽取），返回实际消耗量（droplets） */
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.LAVA) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
                sync.lavaAmount = amount.toInt().coerceAtLeast(0)
                actual
            }
        }
    }

    /** 用余数累加处理 1200 tick / 81000 droplets 的非整数每 tick 消耗。 */
    private var lavaConsumptionRemainder: Long = 0L

    val lavaTank: Storage<FluidVariant> = lavaTankInternal

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0L }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        GeoGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this

    override fun getContainerSize(): Int = INVENTORY_SIZE

    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)

    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        setChanged()
    }

    override fun clearContent() {
        inventory.clear()
    }

    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            FUEL_SLOT -> stack.item == Items.LAVA_BUCKET || stack.item is LavaCell
            EMPTY_CONTAINER_SLOT -> false  // 仅机器输出，玩家不可放入
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == FUEL_SLOT -> stack.item == Items.LAVA_BUCKET || stack.item is LavaCell
        slot == EMPTY_CONTAINER_SLOT -> false
        slot == BATTERY_SLOT -> stack.canBeCharged()
        else -> false
    }

    /** 尝试将空容器放入 EMPTY_CONTAINER_SLOT，可堆叠则合并。返回是否成功放入。 */
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

    /** 检查 EMPTY_CONTAINER_SLOT 是否可放入指定空容器（不修改库存） */
    private fun canInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getItem(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) {
            true
        } else {
            ItemStack.isSameItemSameComponents(current, emptyStack) && current.count + emptyStack.count <= current.maxStackSize
        }
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.geo_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        ic2_120.content.screen.GeoGeneratorScreenHandler(
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
        sync.amount = nbt.getLong(GeoGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, GeoGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        lavaTankInternal.setStoredLava(nbt.getLong(NBT_LAVA_AMOUNT))
        lavaConsumptionRemainder = nbt.getLong(NBT_LAVA_CONSUMPTION_REMAINDER).coerceIn(0L, GeoGeneratorSync.BURN_TICKS_PER_BUCKET - 1L)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(GeoGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_LAVA_AMOUNT, lavaTankInternal.getStoredAmount())
        nbt.putLong(NBT_LAVA_CONSUMPTION_REMAINDER, lavaConsumptionRemainder)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        // 燃料槽位：岩浆桶或岩浆单元倒入储罐，空容器放入 EMPTY_CONTAINER_SLOT
        val fuelStack = getItem(FUEL_SLOT)
        when {
            fuelStack.item == Items.LAVA_BUCKET -> {
                val emptyBucket = ItemStack(Items.BUCKET)
                if (lavaTankInternal.canAcceptFullBucket() && canInsertEmptyContainer(emptyBucket)) {
                    val inserted = lavaTankInternal.tryInsertLava(FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyBucket)) {
                        fuelStack.shrink(1)
                        if (fuelStack.isEmpty) setItem(FUEL_SLOT, ItemStack.EMPTY)
                        setChanged()
                    }
                }
            }
            fuelStack.item is LavaCell -> {
                // 岩浆单元 = 1 桶 = 1000 mB
                val emptyCell = ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "empty_cell")))
                if (lavaTankInternal.canAcceptFullBucket() && canInsertEmptyContainer(emptyCell)) {
                    val inserted = lavaTankInternal.tryInsertLava(FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyCell)) {
                        fuelStack.shrink(1)
                        if (fuelStack.isEmpty) setItem(FUEL_SLOT, ItemStack.EMPTY)
                        setChanged()
                    }
                }
            }
        }

        // 按岩浆量消耗：每桶 1200 tick，使用余数累加避免 81000 / 1200 的整数除法损失。
        val space = (GeoGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        if (space > 0L && lavaTankInternal.amount > 0L && lavaTankInternal.variant.fluid == Fluids.LAVA) {
            val numerator = FluidConstants.BUCKET + lavaConsumptionRemainder
            val consumePerTick = numerator / GeoGeneratorSync.BURN_TICKS_PER_BUCKET
            lavaConsumptionRemainder = numerator % GeoGeneratorSync.BURN_TICKS_PER_BUCKET
            val toConsume = minOf(consumePerTick, lavaTankInternal.amount, space * consumePerTick / GeoGeneratorSync.EU_PER_BURN_TICK)
            val consumed = lavaTankInternal.consumeInternal(toConsume)
            if (consumed > 0L) {
                val euToAdd = consumed * GeoGeneratorSync.EU_PER_BURN_TICK / consumePerTick
                sync.generateEnergy(minOf(euToAdd, space))
                setChanged()
            }
        }

        val charged = batteryCharger.tick()
        if (charged > 0L) {
            org.slf4j.LoggerFactory.getLogger("ic2_120/GeoGenerator")
                .debug("[GeoGenerator] tick 充电 {} EU, 剩余能量 {}", charged, sync.amount)
        }

        val active = sync.amount < GeoGeneratorSync.ENERGY_CAPACITY && lavaTankInternal.amount > 0L && lavaTankInternal.variant.fluid == Fluids.LAVA
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return lavaTank
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH
}
