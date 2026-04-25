package ic2_120.content.block.machines

import ic2_120.content.sync.GeneratorSync
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.screen.GeneratorScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

/**
 * 火力发电机方块实体。燃料槽燃烧产生 EU，能量可被相邻方块提取。
 * 支持为电池充电，也可以使用电池为发电机供电。
 */
@ModBlockEntity(block = GeneratorBlock::class)
class GeneratorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    companion object {
        /** 发电机的能量等级（1级） */
        const val GENERATOR_TIER = 1
    }

    override val tier: Int = GENERATOR_TIER

    override val activeProperty: net.minecraft.state.property.BooleanProperty = GeneratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.loop(
        soundId = "generator.generator.loop",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    private val inventory = DefaultedList.ofSize(2, ItemStack.EMPTY)  // 0: 燃料槽, 1: 电池槽
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(FUEL_SLOT), matcher = { isValid(FUEL_SLOT, it) }),
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { isValid(BATTERY_SLOT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(FUEL_SLOT, BATTERY_SLOT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = GeneratorSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: net.minecraft.util.math.Direction.NORTH },
        { world?.time }
    )
    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0 }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        GeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = 2
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) {
            stack.count = 1
        }
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    /**
     * 实现 MachineBlockEntity 的 getInventory 方法
     */
    override fun getInventory(): net.minecraft.inventory.Inventory = this

    /**
     * 检查物品是否可以放入指定槽位
     * - 燃料槽：只允许燃料
     * - 电池槽：只允许电池物品，且最多1个
     */
    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            FUEL_SLOT -> {
                // 燃料槽：检查是否是有效燃料
                getFuelTime(stack) > 0
            }
            BATTERY_SLOT -> {
                // 充电槽：允许可充电电池或电动工具
                stack.canBeCharged()
            }
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        GeneratorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(GeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, GeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.burnTime = nbt.getInt("BurnTime")
        sync.totalBurnTime = nbt.getInt("TotalBurnTime")
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(GeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("BurnTime", sync.burnTime)
        nbt.putInt("TotalBurnTime", sync.totalBurnTime)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val fuelSlot = getStack(0)
        if (sync.burnTime <= 0 && !fuelSlot.isEmpty) {
            // 仅当至少能容纳 1 tick 发电量时才点燃新燃料，避免临界满电时整段空烧
            val euToAdd = GeneratorSync.EU_PER_BURN_TICK.toLong().coerceAtLeast(1L)
            val space = (GeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space >= euToAdd) {
                val burnTicks = getFuelTime(fuelSlot)
                if (burnTicks > 0) {
                    sync.totalBurnTime = burnTicks
                    sync.burnTime = burnTicks
                    val remainder = fuelSlot.item.getRecipeRemainder(fuelSlot)
                    fuelSlot.decrement(1)
                    if (fuelSlot.isEmpty && !remainder.isEmpty) setStack(0, remainder.copy())
                    else setStack(0, fuelSlot)
                    markDirty()
                }
            }
        }

        if (sync.burnTime > 0) {
            val euToAdd = (GeneratorSync.EU_PER_BURN_TICK).toLong().coerceAtLeast(1L)
            val space = (GeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            // 已点燃的燃料会持续燃烧；有剩余空间就写入能量，溢出则钳制到上限
            if (space > 0L) {
                val actualAdded = minOf(euToAdd, space)
                // 重要：使用统一发电入口追踪内部能量产生
                sync.generateEnergy(actualAdded)
            }
            sync.burnTime = (sync.burnTime - 1).coerceAtLeast(0)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            markDirty()
        }

        batteryCharger.tick()

        val active = sync.burnTime > 0
        setActiveState(world, pos, state, active)

        // 同步当前 tick 的实际输出
        sync.syncCurrentTickFlow()
    }

    /**
     * 发电机燃料燃烧时间（tick）。与 IC2 Experimental 对齐：原版熔炉时间 ÷ 4，
     * 使 1 煤 = 400 tick × 10 EU/t = 4000 EU，容量亦为 4000 EU。
     */
    private fun getFuelTime(stack: ItemStack): Int {
        if (stack.isEmpty) return 0
        val furnaceTicks = FuelRegistry.INSTANCE.get(stack.item) ?: return 0
        return (furnaceTicks / GeneratorSync.BURN_TICKS_DIVISOR).coerceAtLeast(1)
    }
}

