package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.ExtractorBlock
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.recipes.ExtractorRecipes
import ic2_120.content.screen.ExtractorScreenHandler
import ic2_120.content.sync.ExtractorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = ExtractorBlock::class)
class ExtractorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    private val inventory = DefaultedList.ofSize(2, ItemStack.EMPTY)  // 0: 输入, 1: 输出

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ExtractorSync(syncedData) { world?.time }

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(ExtractorBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = 2
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.extractor")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ExtractorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ExtractorSync.NBT_ENERGY_STORED)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ExtractorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        pullEnergyFromNeighbors(world, pos, sync, ExtractorSync.MAX_INSERT)

        val input = getStack(0)
        val result = ExtractorRecipes.getOutput(input) ?: run {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }
        val outputSlot = getStack(1)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }

        if (sync.progress >= ExtractorSync.PROGRESS_MAX) {
            input.decrement(1)
            if (outputSlot.isEmpty()) setStack(1, result)
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            return
        }

        val need = ExtractorSync.ENERGY_PER_TICK
        if (sync.amount >= need) {
            sync.amount = (sync.amount - need).coerceAtLeast(0L)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += 1
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(ExtractorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(ExtractorBlock.ACTIVE, active))
        }
    }
}
