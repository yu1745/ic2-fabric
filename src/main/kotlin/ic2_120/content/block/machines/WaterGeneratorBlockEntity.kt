package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.WaterGeneratorBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.isWaterFuel
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.sync.WaterGeneratorSync
import ic2_120.content.screen.WaterGeneratorScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

/**
 * 水力发电机方块实体。
 *
 * 发电机制：
 * - 水罐（Fabric Transfer API）：1 桶容量，500 EU 总量，1 EU/t 速率
 * - 周围 3x3x3 水方块：每个水方块 +0.01 EU/t（常见水塔约 0.25 EU/t）
 *
 * 升级支持：
 * - 流体抽取升级：作为 receiver 从管道接收水
 */
@ModBlockEntity(block = WaterGeneratorBlock::class)
class WaterGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator, IFluidPipeUpgradeSupport, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    // 流体管道升级支持属性（IFluidPipeUpgradeSupport 接口实现）
    override var fluidPipeProviderEnabled: Boolean = false  // 是否作为 provider 向管道输出流体
    override var fluidPipeReceiverEnabled: Boolean = false  // 是否作为 receiver 从管道接收流体
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null     // provider 流体过滤器（null = 不过滤）
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null    // receiver 流体过滤器（null = 不过滤）
    override var fluidPipeProviderSide: Direction? = null   // provider 工作面（null = 任意面）
    override var fluidPipeReceiverSide: Direction? = null   // receiver 工作面（null = 任意面）

    companion object {
        const val GENERATOR_TIER = 1
        private const val NBT_WATER_AMOUNT = "WaterAmount"
        private const val NBT_WATER_ENV_ACCUM = "WaterEnvAccum"
        private const val NBT_TICK_OFFSET = "TickOffset"
        private const val NBT_CACHED_WATER_COUNT = "CachedWaterCount"
        /** 周围水方块检测间隔（tick） */
        private const val WATER_CHECK_INTERVAL = 20

        private val random = Random.create()

        /** 水力发电机槽位：燃料输入、空容器输出、电池充电、4个升级槽 */
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

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = WaterGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    override val tier: Int = GENERATOR_TIER

    override val activeProperty: net.minecraft.state.property.BooleanProperty = WaterGeneratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.loop(
        soundId = "generator.water.loop",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

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

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = WaterGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val waterTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean = variant.fluid == Fluids.WATER

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStoredWater(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount

        /** 尝试插入水，返回实际插入量 */
        fun tryInsertWater(toInsert: Long): Long {
            if (toInsert <= 0L || (amount > 0L && variant.fluid != Fluids.WATER)) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != Fluids.WATER) variant = FluidVariant.of(Fluids.WATER)
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }

        /** 内部消耗水 */
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.WATER) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    val waterTank: Storage<FluidVariant> = waterTankInternal

    /** 周围水方块发电累积器（百分之一 EU 单位，满 100 时产生 1 EU） */
    private var waterEnvAccum: Int = 0

    /** 构造时随机 0..19，使各机器水检测分散到不同 tick，避免集中卡顿 */
    private var tickOffset: Int = 0

    /** 缓存的周围水方块数量，每 20 tick 更新一次 */
    private var cachedWaterCount: Int = 0

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0L }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        WaterGeneratorBlockEntity::class.type(),
        pos,
        state
    ) {
        tickOffset = random.nextBetween(0, WATER_CHECK_INTERVAL - 1)
    }

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
            FUEL_SLOT -> stack.isWaterFuel()
            EMPTY_CONTAINER_SLOT -> false
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == FUEL_SLOT -> stack.isWaterFuel()
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

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.water_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WaterGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(WaterGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, WaterGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        waterTankInternal.setStoredWater(nbt.getLong(NBT_WATER_AMOUNT))
        waterEnvAccum = nbt.getInt(NBT_WATER_ENV_ACCUM).coerceIn(0, 99)
        tickOffset = nbt.getInt(NBT_TICK_OFFSET).coerceIn(0, WATER_CHECK_INTERVAL - 1)
        cachedWaterCount = nbt.getInt(NBT_CACHED_WATER_COUNT).coerceAtLeast(0)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(WaterGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_WATER_AMOUNT, waterTankInternal.amount)
        nbt.putInt(NBT_WATER_ENV_ACCUM, waterEnvAccum)
        nbt.putInt(NBT_TICK_OFFSET, tickOffset)
        nbt.putInt(NBT_CACHED_WATER_COUNT, cachedWaterCount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 应用流体管道升级
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, waterTankInternal, fluidPipeProviderFilter, fluidPipeProviderSide, state.get(Properties.HORIZONTAL_FACING))
        }

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        // 发电：水罐 1 EU/t + 周围水方块 0.01 EU/t 每块（必须在燃料处理之前，否则插入逻辑会干扰发电）
        val space = (WaterGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        if (space > 0L) {
            var euToAdd = 0L

            // 水罐：按流体量消耗，1 桶 = 500 EU = 500 ticks
            if (waterTankInternal.amount > 0L && waterTankInternal.variant.fluid == Fluids.WATER) {
                val consumePerTick = FluidConstants.BUCKET / WaterGeneratorSync.BURN_TICKS_PER_BUCKET
                val toConsume = minOf(consumePerTick, waterTankInternal.amount, space * consumePerTick / WaterGeneratorSync.EU_PER_BURN_TICK)
                val consumed = waterTankInternal.consumeInternal(toConsume)
                if (consumed > 0L) {
                    euToAdd += consumed * WaterGeneratorSync.EU_PER_BURN_TICK / consumePerTick
                }
            }

            // 周围 3x3x3 水方块：每块 0.01 EU/t，每 20 tick 检测一次（带随机偏移分散负载）
            if ((world.time + tickOffset) % WATER_CHECK_INTERVAL == 0L) {
                cachedWaterCount = countWaterBlocks(world, pos)
            }
            waterEnvAccum += cachedWaterCount * WaterGeneratorSync.EU_PER_TICK_PER_WATER_BLOCK_CENT
            while (waterEnvAccum >= 100 && euToAdd + 1 <= space) {
                waterEnvAccum -= 100
                euToAdd += 1
            }

            if (euToAdd > 0L) {
                sync.generateEnergy(euToAdd)
                sync.energy = sync.amount.toInt().coerceAtLeast(0)
                markDirty()
            }
        }

        batteryCharger.tick()

        // 燃料槽：水桶、水单元、通用流体单元（NBT 为水）倒入水罐（必须在发电之后处理，否则会干扰发电）
        // 水罐仅 1 桶容量，必须等水罐空才能倒入整桶
        val fuelStack = getStack(FUEL_SLOT)
        val emptyCell = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell"))
        val canAcceptFullBucket = waterTankInternal.amount <= 0L
        when {
            fuelStack.item == Items.WATER_BUCKET && canAcceptFullBucket -> {
                val inserted = waterTankInternal.tryInsertWater(FluidConstants.BUCKET)
                if (inserted >= FluidConstants.BUCKET) {
                    val emptyBucket = ItemStack(Items.BUCKET)
                    if (tryInsertEmptyContainer(emptyBucket)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
            fuelStack.isWaterFuel() && canAcceptFullBucket -> {
                val inserted = waterTankInternal.tryInsertWater(FluidConstants.BUCKET)
                if (inserted >= FluidConstants.BUCKET) {
                    val emptyCellStack = ItemStack(emptyCell)
                    if (tryInsertEmptyContainer(emptyCellStack)) {
                        fuelStack.decrement(1)
                        if (fuelStack.isEmpty) setStack(FUEL_SLOT, ItemStack.EMPTY)
                        markDirty()
                    }
                }
            }
        }

        val active = waterTankInternal.amount > 0L && waterTankInternal.variant.fluid == Fluids.WATER ||
            waterEnvAccum >= 100 || cachedWaterCount > 0
        setActiveState(world, pos, state, active)
        // 同步当前 tick 的实际输出/输入
        sync.syncCurrentTickFlow()

    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == (world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH)) return null
        return waterTank
    }

    private fun countWaterBlocks(world: World, center: BlockPos): Int {
        var count = 0
        for (x in (center.x - 1)..(center.x + 1)) {
            for (y in (center.y - 1)..(center.y + 1)) {
                for (z in (center.z - 1)..(center.z + 1)) {
                    if (x == center.x && y == center.y && z == center.z) continue
                    val p = BlockPos(x, y, z)
                    if (world.isInBuildLimit(p)) {
                        val blockState = world.getBlockState(p)
                        if (blockState.isOf(Blocks.WATER)) {
                            count++
                        }
                    }
                }
            }
        }
        return count
    }
}


