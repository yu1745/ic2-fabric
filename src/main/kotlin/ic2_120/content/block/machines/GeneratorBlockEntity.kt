package ic2_120.content.block.machines

import ic2_120.content.sync.GeneratorSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.screen.GeneratorScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.registry.FuelRegistry
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
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 火力发电机方块实体。燃料槽燃烧产生 EU，能量可被相邻方块提取。
 */
@ModBlockEntity(block = GeneratorBlock::class)
class GeneratorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)  // 0: 燃料槽

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = GeneratorSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: net.minecraft.util.math.Direction.NORTH },
        { world?.time }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(GeneratorBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = 1
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

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        GeneratorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(GeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, GeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.burnTime = nbt.getInt("BurnTime")
        sync.totalBurnTime = nbt.getInt("TotalBurnTime")
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(GeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("BurnTime", sync.burnTime)
        nbt.putInt("TotalBurnTime", sync.totalBurnTime)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val fuelSlot = getStack(0)
        if (sync.burnTime <= 0 && !fuelSlot.isEmpty) {
            val burnTicks = getFuelTime(fuelSlot)
            if (burnTicks > 0) {
                sync.totalBurnTime = burnTicks
                sync.burnTime = burnTicks
                val remainder = fuelSlot.item.getRecipeRemainder(fuelSlot)
                fuelSlot.decrement(1)
                if (fuelSlot.isEmpty && !remainder.isEmpty) setStack(0, remainder.copy())
                else setStack(0, fuelSlot)
                markDirty()
            }
        }

        if (sync.burnTime > 0) {
            val euToAdd = (GeneratorSync.EU_PER_BURN_TICK).toLong().coerceAtLeast(1L)
            val space = (GeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space >= euToAdd) {
                sync.amount = (sync.amount + euToAdd).coerceAtMost(GeneratorSync.ENERGY_CAPACITY)
                sync.burnTime = (sync.burnTime - 1).coerceAtLeast(0)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                markDirty()
            }
        }

        val active = sync.burnTime > 0
        if (state.get(GeneratorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(GeneratorBlock.ACTIVE, active))
        }
    }

    /**
     * 发电机燃料燃烧时间（tick）。与 IC2 Experimental 对齐：原版熔炉时间 ÷ 4，
     * 使 1 煤 = 400 tick × 10 EU/t = 4000 EU，容量亦为 4000 EU。
     */
    private fun getFuelTime(stack: ItemStack): Int {
        if (stack.isEmpty) return 0
        val furnaceTicks = FuelRegistry.INSTANCE.get(stack.item) ?: return 0
        return (furnaceTicks / GeneratorSync.BURN_TICKS_DIVISOR).coerceAtLeast(1)
    }
}
