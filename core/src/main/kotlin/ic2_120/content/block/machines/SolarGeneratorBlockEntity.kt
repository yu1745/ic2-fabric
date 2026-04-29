package ic2_120.content.block.machines

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.screen.SolarGeneratorScreenHandler
import ic2_120.content.sync.SolarGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 太阳能发电机方块实体。
 *
 * 发电条件（全部满足）：
 * - 主世界
 * - 白天 6:20~17:45
 * - 正上方无非透明方块遮挡
 * - 无雨雪
 */
@ModBlockEntity(block = SolarGeneratorBlock::class)
class SolarGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = SolarGeneratorBlock.ACTIVE

    companion object {
        const val GENERATOR_TIER = 1
        const val BATTERY_SLOT = 0

        /** 6:20 AM ≈ 333 ticks（0 = 6:00 AM，20 分钟 ≈ 333 ticks） */
        private const val DAY_START_TICK = 333
        /** 17:45 PM = 11750 ticks（11*1000 + 750） */
        private const val DAY_END_TICK = 11750
    }

    override val tier: Int = GENERATOR_TIER

    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)  // 仅电池槽
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { isValid(BATTERY_SLOT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(BATTERY_SLOT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = SolarGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
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
        SolarGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    override fun size(): Int = 1
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.solar_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SolarGeneratorScreenHandler(
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
        sync.amount = nbt.getLong(SolarGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, SolarGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(SolarGeneratorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val canGenerate = canGenerate(world, pos)
        sync.isGenerating = if (canGenerate) 1 else 0

        if (canGenerate) {
            val space = (SolarGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space > 0L) {
                val euToAdd = minOf(SolarGeneratorSync.EU_PER_TICK, space)
                sync.generateEnergy(euToAdd)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                markDirty()
            }
        }

        batteryCharger.tick()

        val active = canGenerate
        setActiveState(world, pos, state, active)
        // 同步当前 tick 的实际输出/输入
        sync.syncCurrentTickFlow()

    }

    /**
     * 检查是否满足发电条件。
     */
    private fun canGenerate(world: World, pos: BlockPos): Boolean {
        if (world.isClient) return false

        // 仅主世界
        if (world.registryKey != World.OVERWORLD) return false

        // 下雨或下雪不发电
        if (world.isRaining) return false

        // 白天 6:20~17:45
        val time = world.timeOfDay % 24000
        if (time < DAY_START_TICK || time > DAY_END_TICK) return false

        // 正上方无非透明方块遮挡
        if (!hasSkyAccess(world, pos)) return false

        return true
    }

    /**
     * 检查正上方是否有天空可见（无非透明方块遮挡）。
     */
    private fun hasSkyAccess(world: World, basePos: BlockPos): Boolean {
        val topY = world.topY
        var y = basePos.y + 1
        while (y < topY) {
            val pos = BlockPos(basePos.x, y, basePos.z)
            val blockState = world.getBlockState(pos)
            if (blockState.isOpaqueFullCube(world, pos)) return false
            y++
        }
        return true
    }
}


