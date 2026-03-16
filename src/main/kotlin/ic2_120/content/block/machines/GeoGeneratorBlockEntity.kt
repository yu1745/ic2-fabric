package ic2_120.content.block.machines

import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.LavaCell
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.Ic2_120
import ic2_120.content.sync.GeoGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.fluid.Fluids
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries

/**
 * 地热发电机方块实体。
 * - 仅接受岩浆流体输入
 * - 内部流体缓存：8 桶
 * - 输出：20 EU/t（Tier 1）
 * - 1 桶岩浆燃烧 25 秒（500 ticks）
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
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator, IFluidPipeUpgradeSupport,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    // 流体管道升级支持属性（IFluidPipeUpgradeSupport 接口实现）
    override var fluidPipeProviderEnabled: Boolean = false  // 是否作为 provider 向管道输出流体
    override var fluidPipeReceiverEnabled: Boolean = false  // 是否作为 receiver 从管道接收流体
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null     // provider 流体过滤器（null = 不过滤）
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null    // receiver 流体过滤器（null = 不过滤）
    override var fluidPipeProviderSide: Direction? = null   // provider 工作面（null = 任意面）
    override var fluidPipeReceiverSide: Direction? = null   // receiver 工作面（null = 任意面）

    companion object {
        const val GENERATOR_TIER = 1
        private const val NBT_LAVA_AMOUNT = "LavaAmount"

        /** 地热发电机槽位：燃料输入、空容器输出、电池充电、4个升级槽 */
        const val FUEL_SLOT = 0
        const val EMPTY_CONTAINER_SLOT = 1
        const val BATTERY_SLOT = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 7

        @Volatile
        private var fluidLookupRegistered = false

        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = GeoGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    override val tier: Int = GENERATOR_TIER

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = GeoGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val lavaTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean = variant.fluid == Fluids.LAVA

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.lavaAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStoredLava(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.LAVA) else FluidVariant.blank()
            sync.lavaAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount

        fun hasAtLeastOneLavaBucket(): Boolean =
            amount >= FluidConstants.BUCKET && variant.fluid == Fluids.LAVA

        fun canAcceptFullBucket(): Boolean = (tankCapacity - amount) >= FluidConstants.BUCKET

        /** 尝试插入岩浆（用于岩浆桶倒入），返回实际插入量 */
        fun tryInsertLava(toInsert: Long): Long {
            if (toInsert <= 0L || (amount > 0L && variant.fluid != Fluids.LAVA)) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != Fluids.LAVA) variant = FluidVariant.of(Fluids.LAVA)
            sync.lavaAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }

        /** 内部消耗岩浆（不经过 extract，因 canExtract=false 会阻止外部抽取） */
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.LAVA) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.lavaAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

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

    override fun getInventory(): Inventory = this

    override fun size(): Int = INVENTORY_SIZE

    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)

    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun clear() {
        inventory.clear()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            FUEL_SLOT -> stack.item == Items.LAVA_BUCKET || stack.item is LavaCell
            EMPTY_CONTAINER_SLOT -> false  // 仅机器输出，玩家不可放入
            BATTERY_SLOT -> stack.item is IBatteryItem
            else -> false
        }
    }

    /** 尝试将空容器放入 EMPTY_CONTAINER_SLOT，可堆叠则合并。返回是否成功放入。 */
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

    /** 检查 EMPTY_CONTAINER_SLOT 是否可放入指定空容器（不修改库存） */
    private fun canInsertEmptyContainer(emptyStack: ItemStack): Boolean {
        if (emptyStack.isEmpty) return false
        val current = getStack(EMPTY_CONTAINER_SLOT)
        return if (current.isEmpty) {
            true
        } else {
            ItemStack.canCombine(current, emptyStack) && current.count + emptyStack.count <= current.maxCount
        }
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.geo_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ic2_120.content.screen.GeoGeneratorScreenHandler(
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
        sync.amount = nbt.getLong(GeoGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, GeoGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        lavaTankInternal.setStoredLava(nbt.getLong(NBT_LAVA_AMOUNT))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(GeoGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_LAVA_AMOUNT, lavaTankInternal.getStoredAmount())
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 应用流体管道升级
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        // 燃料槽位：岩浆桶或岩浆单元倒入储罐，空容器放入 EMPTY_CONTAINER_SLOT
        val fuelStack = getStack(FUEL_SLOT)
        when {
            fuelStack.item == Items.LAVA_BUCKET -> {
                val emptyBucket = ItemStack(Items.BUCKET)
                if (lavaTankInternal.canAcceptFullBucket() && canInsertEmptyContainer(emptyBucket)) {
                    val inserted = lavaTankInternal.tryInsertLava(FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyBucket)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
            fuelStack.item is LavaCell -> {
                // 岩浆单元 = 1 桶 = 1000 mB
                val emptyCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
                if (lavaTankInternal.canAcceptFullBucket() && canInsertEmptyContainer(emptyCell)) {
                    val inserted = lavaTankInternal.tryInsertLava(FluidConstants.BUCKET)
                    if (inserted >= FluidConstants.BUCKET && tryInsertEmptyContainer(emptyCell)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
        }

        // 按岩浆量消耗：每 tick 消耗 1 桶/500 = 2 mB 对应 20 EU
        val space = (GeoGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        if (space > 0L && lavaTankInternal.amount > 0L && lavaTankInternal.variant.fluid == Fluids.LAVA) {
            val consumePerTick = FluidConstants.BUCKET / GeoGeneratorSync.BURN_TICKS_PER_BUCKET
            val toConsume = minOf(consumePerTick, lavaTankInternal.amount, space * consumePerTick / GeoGeneratorSync.EU_PER_BURN_TICK)
            val consumed = lavaTankInternal.consumeInternal(toConsume)
            if (consumed > 0L) {
                val euToAdd = consumed * GeoGeneratorSync.EU_PER_BURN_TICK / consumePerTick
                sync.generateEnergy(minOf(euToAdd, space))
                markDirty()
            }
        }

        val charged = batteryCharger.tick()
        if (charged > 0L) {
            org.slf4j.LoggerFactory.getLogger("ic2_120/GeoGenerator")
                .debug("[GeoGenerator] tick 充电 {} EU, 剩余能量 {}", charged, sync.amount)
        }

        val active = sync.amount < GeoGeneratorSync.ENERGY_CAPACITY && lavaTankInternal.amount > 0L && lavaTankInternal.variant.fluid == Fluids.LAVA
        if (state.get(GeoGeneratorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(GeoGeneratorBlock.ACTIVE, active))
        }
        sync.syncCurrentTickFlow()
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == getFrontFacing()) return null
        return lavaTank
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
}
