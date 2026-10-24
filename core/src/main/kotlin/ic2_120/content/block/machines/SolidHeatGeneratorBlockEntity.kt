package ic2_120.content.block.machines

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.pos
import ic2_120.compat.world
import ic2_120.content.block.SolidHeatGeneratorBlock
import ic2_120.content.screen.SolidHeatGeneratorScreenHandler
import ic2_120.content.sync.SolidHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.RegisterItemStorage
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * 固体加热机：
 * - 行为类似火力发电机，但产 HU
 * - 20 HU/t
 * - 煤炭/木炭：每个 8000 HU（即 400 ticks）
 */
@ModBlockEntity(block = SolidHeatGeneratorBlock::class)
class SolidHeatGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatGeneratorBlockEntityBase(type, pos, state), Container, IRoutedSidedInventory, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = SolidHeatGeneratorBlock.ACTIVE

    companion object {
        const val SLOT_FUEL = 0
        const val SLOT_OUTPUT = 1
        const val INVENTORY_SIZE = 2
        private const val HU_PER_TICK = 20L
        private const val SOLID_HEAT_BURN_DIVISOR = 4 // 1600(煤炭熔炉tick) -> 400(本机tick)
    }

    override val tier: Int = 1
    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_FUEL), matcher = { canPlaceItem(SLOT_FUEL, it) })
        ),
        extractSlots = intArrayOf(SLOT_FUEL, SLOT_OUTPUT),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    private var burnTime = 0
    private var burnTotal = 0
    val syncedData = SyncedData()
    override val heatFlow = HeatFlowSync(syncedData, this)
    val sync = SolidHeatGeneratorSync(syncedData, heatFlow)

    constructor(pos: BlockPos, state: BlockState) : this(
        SolidHeatGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.size)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.solid_heat_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        SolidHeatGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData
        )

    override fun getInventory(): Container = this
    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun stillValid(player: net.minecraft.world.entity.player.Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        setChanged()
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_FUEL -> !stack.isEmpty && getFuelTimeForSolidHeat(stack) > 0
        else -> false
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        burnTime = nbt.getInt("BurnTime").coerceAtLeast(0)
        burnTotal = nbt.getInt("BurnTotal").coerceAtLeast(0)
        sync.burnTime = burnTime
        sync.burnTotal = burnTotal
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putInt("BurnTime", burnTime)
        nbt.putInt("BurnTotal", burnTotal)
    }

    override fun generateHeat(world: Level, pos: BlockPos, state: BlockState): Long {
        if (burnTime <= 0) {
            val fuel = getItem(SLOT_FUEL)
            val fuelTicks = getFuelTimeForSolidHeat(fuel)
            if (!fuel.isEmpty && fuelTicks > 0) {
                burnTime = fuelTicks
                burnTotal = fuelTicks
                fuel.shrink(1)
                if (fuel.isEmpty) setItem(SLOT_FUEL, ItemStack.EMPTY)
                setChanged()
            }
        }

        var generatedThisTick = 0L
        if (burnTime > 0) {
            generatedThisTick = HU_PER_TICK
            burnTime--
        }
        return generatedThisTick
    }

    override fun syncAdditionalData() {
        sync.burnTime = burnTime
        sync.burnTotal = burnTotal
    }

    override fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean =
        burnTime > 0 && hasValidConsumer

    override fun getActiveState(state: BlockState): Boolean =
        state.getValue(SolidHeatGeneratorBlock.ACTIVE)

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        tickHeatMachine(world, pos, state)
    }

    private fun getFuelTimeForSolidHeat(stack: ItemStack): Int {
        if (stack.isEmpty) return 0
        val item = stack.item
        val isSupported = item == Items.COAL ||
            item == Items.CHARCOAL ||
            item == net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ic2_120", "coke"))
        if (!isSupported) return 0
        val furnaceTicks = FuelRegistry.INSTANCE.get(item) ?: return 0
        return (furnaceTicks / SOLID_HEAT_BURN_DIVISOR).coerceAtLeast(1)
    }
}
