package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.ExtractorBlock
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.recipes.ExtractorRecipes
import ic2_120.content.screen.ExtractorScreenHandler
import ic2_120.content.sync.ExtractorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
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
) : BlockEntity(type, pos, state), Inventory, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, ExtendedScreenHandlerFactory {

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val EXTRACTOR_TIER = 1
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        val SLOT_UPGRADE_INDICES = intArrayOf(2, 3, 4, 5)
        const val INVENTORY_SIZE = 6
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ExtractorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(EXTRACTOR_TIER + voltageTierBonus) }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(ExtractorBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
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

        // 应用升级效果（加速、储能、高压）
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync, sync.getMaxInsertPerTick())

        val input = getStack(SLOT_INPUT)
        val result = ExtractorRecipes.getOutput(input) ?: run {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }
        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        if (sync.progress >= ExtractorSync.PROGRESS_MAX) {
            input.decrement(1)
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result)
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            return
        }

        val need = (ExtractorSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.amount >= need) {
            sync.amount = (sync.amount - need).coerceAtLeast(0L)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += progressIncrement
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
