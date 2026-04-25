package ic2_120.content.block.machines

import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.CreativeGeneratorScreenHandler
import ic2_120.content.sync.CreativeGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

/**
 * 创造模式发电机方块实体。无限生成 32 EU/t，支持电池充电。
 */
@ModBlockEntity(block = CreativeGeneratorBlock::class)
class CreativeGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator, ITieredMachine, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CreativeGeneratorBlock.ACTIVE

    companion object {
        /** 能量等级 1（LV） */
        const val GENERATOR_TIER = 1
        /** 电池槽索引 */
        const val BATTERY_SLOT = 0
        /** 物品栏大小 */
        const val INVENTORY_SIZE = 1
    }

    override val tier: Int = GENERATOR_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = CreativeGeneratorSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: net.minecraft.util.math.Direction.NORTH },
        { world?.time }
    )

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0L }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        CreativeGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.creative_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CreativeGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CreativeGeneratorSync.NBT_ENERGY_STORED)
            .coerceIn(0L, CreativeGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.totalGenerated = nbt.getInt(CreativeGeneratorSync.NBT_TOTAL_GENERATED)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CreativeGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(CreativeGeneratorSync.NBT_TOTAL_GENERATED, sync.totalGenerated)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 创造发电机无限生成能量（32 EU/t）
        val space = (CreativeGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        val toGenerate = if (space > 0L) minOf(CreativeGeneratorSync.GENERATION_RATE, space) else 0L
        val actualGenerated = sync.generateEnergy(toGenerate)
        sync.addGenerated(actualGenerated)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()

        // 为电池充电
        batteryCharger.tick()

        // 创造发电机永远处于激活状态
        setActiveState(world, pos, state, true)

        sync.syncCurrentTickFlow()
    }

    /**
     * 检查物品是否可以放入电池槽
     */
    fun canPlaceInSlot(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.canBeCharged()
    }
}
