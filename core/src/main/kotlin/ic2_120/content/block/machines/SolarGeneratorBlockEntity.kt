package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.screen.SolarGeneratorScreenHandler
import ic2_120.content.sync.SolarGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 太阳能发电机方块实体。
 *
 * 发电条件（全部满足）：
 * - 主世界（白天 6:20~17:45）或末地（永远白天）
* - 正上方无非透明方块遮挡
* - 无雨雪
*/
@ModBlockEntity(block = SolarGeneratorBlock::class)
class SolarGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = SolarGeneratorBlock.ACTIVE

    companion object {
        const val INVENTORY_SIZE = 1
        const val GENERATOR_TIER = 1
        const val BATTERY_SLOT = 0

        /** 6:20 AM ≈ 333 ticks（0 = 6:00 AM，20 分钟 ≈ 333 ticks） */
        private const val DAY_START_TICK = 333
        /** 17:45 PM = 11750 ticks（11*1000 + 750） */
        private const val DAY_END_TICK = 11750
    }

    override val tier: Int = GENERATOR_TIER

    private val inventory = NonNullList.withSize(1, ItemStack.EMPTY)  // 仅电池槽
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { canPlaceItem(BATTERY_SLOT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(BATTERY_SLOT),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = SolarGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

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

    override fun getInventory(): Container = this

    override fun getContainerSize(): Int = 1
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.solar_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        SolarGeneratorScreenHandler(
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
        sync.amount = nbt.getLong(SolarGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, SolarGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(SolarGeneratorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val canGenerate = canGenerate(world, pos)
        sync.isGenerating = if (canGenerate) 1 else 0

        if (canGenerate) {
            val space = (SolarGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space > 0L) {
                val euToAdd = minOf(SolarGeneratorSync.EU_PER_TICK, space)
                sync.generateEnergy(euToAdd)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                setChanged()
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
    private fun canGenerate(world: Level, pos: BlockPos): Boolean {
        if (world.isClient) return false

        return when (world.dimension()) {
            // 末地永远白天、无天气变化，仅检查天空可见性即可
            Level.END -> hasSkyAccess(world, pos)
            // 主世界需要白天、无雨雪、天空可见
            Level.OVERWORLD -> {
                // 下雨或下雪不发电
                if (world.isRaining) return false
                // 白天 6:20~17:45
                val time = world.dayTime % 24000
                if (time < DAY_START_TICK || time > DAY_END_TICK) return false
                // 正上方无非透明方块遮挡
                hasSkyAccess(world, pos)
            }
            else -> false
        }
    }

    /**
     * 检查正上方是否有天空可见（无非透明方块遮挡）。
     */
    private fun hasSkyAccess(world: Level, basePos: BlockPos): Boolean {
        val topY = world.maxBuildHeight
        var y = basePos.y + 1
        while (y < topY) {
            val pos = BlockPos(basePos.x, y, basePos.z)
            val blockState = world.getBlockState(pos)
            if (blockState.isSolidRender(world, pos)) return false
            y++
        }
        return true
    }
}
