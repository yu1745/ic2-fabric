package ic2_120.content.block.machines

import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.StirlingGeneratorScreenHandler
import ic2_120.content.sync.StirlingGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.heat.IHeatConsumer
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

@ModBlockEntity(block = StirlingGeneratorBlock::class)
class StirlingGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Container, IGenerator, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = StirlingGeneratorBlock.ACTIVE

    companion object {
        const val STIRLING_TIER = 2
        const val INVENTORY_SIZE = 0

        private const val NBT_HEAT_BUFFERED = "HeatBuffered"

        const val HU_PER_EU = 2L
        const val MAX_OUTPUT_EU_PER_TICK = 50L
        const val MAX_HEAT_PER_TICK = MAX_OUTPUT_EU_PER_TICK * HU_PER_EU
    }

    override val tier: Int = STIRLING_TIER
    private val inventory = NonNullList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = StirlingGeneratorSync(
        syncedData,
        { world?.getBlockState(pos)?.get(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time }
    )
    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private var heatBuffered: Long = 0L

    constructor(pos: BlockPos, state: BlockState) : this(
        StirlingGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = ContainerHelper.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = ContainerHelper.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: Player): Boolean = Container.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {}

    override fun isValid(slot: Int, stack: ItemStack): Boolean = false

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.stirling_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        StirlingGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: CompoundTag) {
        super.readNbt(nbt)
        ContainerHelper.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(StirlingGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, StirlingGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        heatBuffered = nbt.getLong(NBT_HEAT_BUFFERED)
        sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    override fun writeNbt(nbt: CompoundTag) {
        super.writeNbt(nbt)
        ContainerHelper.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(StirlingGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_HEAT_BUFFERED, heatBuffered)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        val toAdd = hu.coerceAtMost(MAX_HEAT_PER_TICK - heatBuffered)
        if (toAdd <= 0L) {
            return 0L
        }
        heatBuffered += toAdd
        sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        markDirty()
        return toAdd
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        adjacentEnergyTransfer.tick()
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        val euToGenerate = (heatBuffered / HU_PER_EU).coerceAtMost(MAX_OUTPUT_EU_PER_TICK)
        if (euToGenerate > 0L) {
            val space = (StirlingGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            val actualGenerate = minOf(euToGenerate, space)
            if (actualGenerate > 0L) {
                sync.generateEnergy(actualGenerate)
                heatBuffered -= actualGenerate * HU_PER_EU
                sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                markDirty()
            }
        }
        val active = sync.amount < StirlingGeneratorSync.ENERGY_CAPACITY &&
            heatBuffered > 0L &&
            hasValidHeatSource()
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun hasValidHeatSource(): Boolean {
        val world = world ?: return false
        val myFace = getHeatTransferFace()
        val neighborPos = pos.offset(myFace)
        val neighbor = world.getBlockEntity(neighborPos) as? ic2_120.content.heat.IHeatNode ?: return false
        val neighborFace = neighbor.getHeatTransferFace()
        return neighborFace == myFace.opposite
    }

    fun onNeighborUpdate() {
        markDirty()
    }
}
