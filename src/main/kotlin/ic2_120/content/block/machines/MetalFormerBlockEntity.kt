package ic2_120.content.block.machines

import ic2_120.content.recipes.MetalFormerRecipes
import ic2_120.content.sync.MetalFormerSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.screen.MetalFormerScreenHandler
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

@ModBlockEntity(block = MetalFormerBlock::class)
class MetalFormerBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    companion object {
        const val METAL_FORMER_TIER = 1
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_UPGRADE = 3
        const val INVENTORY_SIZE = 4
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = MetalFormerSync(syncedData) { world?.time }
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { METAL_FORMER_TIER },
        canDischargeNow = { sync.amount < MetalFormerSync.ENERGY_CAPACITY }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(MetalFormerBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) {
            stack.count = 1
        }
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

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.metal_former")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MetalFormerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(MetalFormerSync.NBT_ENERGY_STORED)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.setMode(MetalFormerSync.Mode.fromId(nbt.getInt(MetalFormerSync.NBT_MODE)))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MetalFormerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(MetalFormerSync.NBT_MODE, sync.mode)
    }

    /**
     * 切换加工模式（由 GUI 按钮调用）
     */
    fun cycleMode() {
        sync.cycleMode()
        sync.progress = 0  // 切换模式时重置进度
        markDirty()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 从相邻方块或物品栏电池提取能量
        pullEnergyFromNeighbors(world, pos, sync, MetalFormerSync.MAX_INSERT)

        // 从放电槽提取能量
        extractFromDischargingSlot()

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }

        val currentMode = sync.getMode()

        // 检查配方（支持双输入配方）
        val secondaryInput = if (needsSecondaryInput(currentMode)) getStack(SLOT_UPGRADE) else null
        val result = MetalFormerRecipes.getOutput(currentMode, input, secondaryInput) ?: run {
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

        if (sync.progress >= MetalFormerSync.PROGRESS_MAX) {
            // 消耗输入物品
            input.decrement(1)

            // 消耗次要输入物品（如果有）
            if (secondaryInput != null && !secondaryInput.isEmpty) {
                secondaryInput.decrement(1)
            }

            // 输出结果
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result)
            else outputSlot.increment(result.count)

            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            return
        }

        val need = MetalFormerSync.ENERGY_PER_TICK
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

    /**
     * 检查当前模式是否需要次要输入（升级槽）
     * 某些挤压模式配方需要两个输入
     */
    private fun needsSecondaryInput(mode: MetalFormerSync.Mode): Boolean {
        return mode == MetalFormerSync.Mode.EXTRUDING
    }

    /**
     * 从放电槽提取能量（如果需要）
     */
    private fun extractFromDischargingSlot() {
        val space = (MetalFormerSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, MetalFormerSync.MAX_INSERT)
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        sync.amount = (sync.amount + extracted).coerceAtMost(MetalFormerSync.ENERGY_CAPACITY)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(MetalFormerBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(MetalFormerBlock.ACTIVE, active))
        }
    }
}
