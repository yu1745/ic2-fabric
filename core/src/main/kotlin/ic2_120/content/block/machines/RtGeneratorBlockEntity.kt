package ic2_120.content.block.machines

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.Ic2_120
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.RtGeneratorBlock
import ic2_120.content.screen.RtGeneratorScreenHandler
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.sync.RtGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
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

/**
 * 放射性同位素温差发电机方块实体。
 *
 * - 需放入放射性同位素燃料靶丸（rtg_pellet），靶丸无限耐久不消耗
 * - 发电量由靶丸数量决定：1→1, 2→2, 3→4, 4→8, 5→16, 6→32 EU/t
 * - 能量缓冲 20000 EU
 */
@ModBlockEntity(block = RtGeneratorBlock::class)
class RtGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = RtGeneratorBlock.ACTIVE

    companion object {
        const val INVENTORY_SIZE = 6
        const val GENERATOR_TIER = 1
        const val FUEL_SLOT_COUNT = 6
        const val FUEL_SLOT_START = 0
        const val FUEL_SLOT_END = 5

        private val RTG_PELLET_ITEM = lazy {
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rtg_pellet"))
        }

        fun isRtgPellet(stack: ItemStack): Boolean =
            !stack.isEmpty && stack.item == RTG_PELLET_ITEM.value
    }

    override val tier: Int = GENERATOR_TIER

    private val inventory = NonNullList.withSize(6, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceInSlot(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute((FUEL_SLOT_START..FUEL_SLOT_END).toList().toIntArray(), matcher = { isRtgPellet(it) }, maxPerSlot = 1)
        ),
        extractSlots = IntArray(6) { it },
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = RtGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    constructor(pos: BlockPos, state: BlockState) : this(
        RtGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this

    override fun size(): Int = 6

    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)

    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot in FUEL_SLOT_START..FUEL_SLOT_END && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        setChanged()
    }

    override fun clear() {
        inventory.clear()
    }

    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return slot in FUEL_SLOT_START..FUEL_SLOT_END && isRtgPellet(stack)
    }

    /** 统计燃料槽中靶丸数量 */
    fun countPelletSlots(): Int =
        (FUEL_SLOT_START..FUEL_SLOT_END).count { !getStack(it).isEmpty }

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("container.ic2_120.rt_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        RtGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(RtGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, RtGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(RtGeneratorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        adjacentEnergyTransfer.tick()

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val pelletCount = countPelletSlots()
        val euPerTick = RtGeneratorSync.euPerTickFromPelletCount(pelletCount).toLong()

        if (euPerTick > 0L) {
            val space = (RtGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space > 0L) {
                val euToAdd = minOf(euPerTick, space)
                sync.generateEnergy(euToAdd)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                setChanged()
            }
        }

        val active = pelletCount > 0
        setActiveState(world, pos, state, active)
        // 同步当前 tick 的实际输出/输入
        sync.syncCurrentTickFlow()

    }
}

