package ic2_120.content.block.machines

import ic2_120.content.block.SolidHeatGeneratorBlock
import ic2_120.content.screen.SolidHeatGeneratorScreenHandler
import ic2_120.content.sync.SolidHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

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
) : HeatGeneratorBlockEntityBase(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    companion object {
        private const val SLOT_FUEL = 0
        private const val INVENTORY_SIZE = 1
        private const val HU_PER_TICK = 20L
        private const val HEAT_PER_ITEM = 8_000L
        private const val BURN_TICKS_PER_ITEM = (HEAT_PER_ITEM / HU_PER_TICK).toInt()
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private var burnTime = 0
    private var burnTotal = 0
    val syncedData = SyncedData(this)
    override val heatFlow = HeatFlowSync(syncedData, this)
    val sync = SolidHeatGeneratorSync(syncedData, heatFlow)

    constructor(pos: BlockPos, state: BlockState) : this(
        SolidHeatGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.solid_heat_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SolidHeatGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: net.minecraft.entity.player.PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        markDirty()
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        burnTime = nbt.getInt("BurnTime").coerceAtLeast(0)
        burnTotal = nbt.getInt("BurnTotal").coerceAtLeast(0)
        sync.burnTime = burnTime
        sync.burnTotal = burnTotal
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putInt("BurnTime", burnTime)
        nbt.putInt("BurnTotal", burnTotal)
    }

    override fun generateHeat(world: World, pos: BlockPos, state: BlockState): Long {
        if (burnTime <= 0) {
            val fuel = getStack(SLOT_FUEL)
            if (!fuel.isEmpty && isSupportedFuel(fuel)) {
                burnTime = BURN_TICKS_PER_ITEM
                burnTotal = BURN_TICKS_PER_ITEM
                fuel.decrement(1)
                if (fuel.isEmpty) setStack(SLOT_FUEL, ItemStack.EMPTY)
                markDirty()
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
        state.get(SolidHeatGeneratorBlock.ACTIVE)

    override fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        world.setBlockState(pos, state.with(SolidHeatGeneratorBlock.ACTIVE, active))
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        tickHeatMachine(world, pos, state)
    }

    private fun isSupportedFuel(stack: ItemStack): Boolean =
        stack.item == Items.COAL || stack.item == Items.CHARCOAL
}
