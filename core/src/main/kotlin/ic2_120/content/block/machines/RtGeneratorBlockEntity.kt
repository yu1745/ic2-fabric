package ic2_120.content.block.machines

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
import ic2_120.registry.type
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
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = RtGeneratorBlock.ACTIVE

    companion object {
        const val GENERATOR_TIER = 1
        const val FUEL_SLOT_COUNT = 6
        const val FUEL_SLOT_START = 0
        const val FUEL_SLOT_END = 5

        private val RTG_PELLET_ITEM = lazy {
            Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rtg_pellet"))
        }

        fun isRtgPellet(stack: ItemStack): Boolean =
            !stack.isEmpty && stack.item == RTG_PELLET_ITEM.value
    }

    override val tier: Int = GENERATOR_TIER

    private val inventory = DefaultedList.ofSize(6, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> canPlaceInSlot(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute((FUEL_SLOT_START..FUEL_SLOT_END).toList().toIntArray(), matcher = { isRtgPellet(it) }, maxPerSlot = 1)
        ),
        extractSlots = IntArray(6) { it },
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = RtGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    constructor(pos: BlockPos, state: BlockState) : this(
        RtGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    override fun size(): Int = 6

    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)

    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot in FUEL_SLOT_START..FUEL_SLOT_END && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun clear() {
        inventory.clear()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return slot in FUEL_SLOT_START..FUEL_SLOT_END && isRtgPellet(stack)
    }

    /** 统计燃料槽中靶丸数量 */
    fun countPelletSlots(): Int =
        (FUEL_SLOT_START..FUEL_SLOT_END).count { !getStack(it).isEmpty }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("container.ic2_120.rt_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        RtGeneratorScreenHandler(
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
        sync.amount = nbt.getLong(RtGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, RtGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(RtGeneratorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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
                markDirty()
            }
        }

        val active = pelletCount > 0
        setActiveState(world, pos, state, active)
        // 同步当前 tick 的实际输出/输入
        sync.syncCurrentTickFlow()

    }
}

