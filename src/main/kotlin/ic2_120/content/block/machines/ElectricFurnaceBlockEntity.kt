package ic2_120.content.block.machines

import ic2_120.content.sync.ElectricFurnaceSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.ElectricFurnaceScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 电力熔炉方块实体。提供输入/输出槽位并实现简单 GUI。
 * [sync] 继承 TickLimitedEnergyStorage，即同步数据又即 Energy API 存储。
 */
@ModBlockEntity(block = ElectricFurnaceBlock::class)
class ElectricFurnaceBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ITieredMachine, ExtendedScreenHandlerFactory {

    override val tier: Int = 1

    private val inventory = DefaultedList.ofSize(2, ItemStack.EMPTY)  // 0: 输入, 1: 输出

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ElectricFurnaceSync(syncedData) { world?.time }

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(ElectricFurnaceBlockEntity::class),
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
    override fun markDirty() {
        super.markDirty()
    }
    override fun canPlayerUse(player: PlayerEntity): Boolean =
        Inventory.canPlayerUse(this, player)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("block.ic2_120.electric_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ElectricFurnaceScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ElectricFurnaceSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ElectricFurnaceSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        pullEnergyFromNeighbors(world, pos, sync)

        val input = getStack(0)
        if (input.isEmpty()) {
            if (sync.progress != 0) {
                sync.progress = 0
            }
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val inputInv = SimpleInventory(1).apply { setStack(0, input) }
        val match = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inputInv, world)
        if (match.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val recipe = match.get()
        val result = recipe.getOutput(world.registryManager).copy()
        val outputSlot = getStack(1)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.progress >= ElectricFurnaceSync.PROGRESS_MAX) {
            input.decrement(1)
            if (outputSlot.isEmpty()) setStack(1, result)
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 内部消耗：直接扣减 amount（不经过 extract，故对外 MAX_EXTRACT=0 仍生效，电缆无法拉电）
        val need = ElectricFurnaceSync.ENERGY_PER_TICK
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += 1
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }
        sync.syncCurrentTickFlow()
    }

    /** 根据是否在工作更新方块的 active 状态，配合 blockstate 切换模型。 */
    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(ElectricFurnaceBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(ElectricFurnaceBlock.ACTIVE, active))
        }
    }
}










