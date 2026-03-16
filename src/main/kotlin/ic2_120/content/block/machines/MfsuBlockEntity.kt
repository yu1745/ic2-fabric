package ic2_120.content.block.machines

import ic2_120.content.sync.MfsuSync
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MfsuBlock
import ic2_120.content.screen.MfsuScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.EquipmentSlot
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
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * MFSU 方块实体。仅能量存储，通过 Energy API 与电网交互。
 * 添加装备槽，可给护甲充电。
 */
@ModBlockEntity(block = MfsuBlock::class)
class MfsuBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory, ITieredMachine {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(MfsuBlockEntity::class.java)
        const val SLOT_HELMET = 0
        const val SLOT_CHESTPLATE = 1
        const val SLOT_LEGGINGS = 2
        const val SLOT_BOOTS = 3
        const val INVENTORY_SIZE = 4
    }

    override val tier: Int = 4

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = MfsuSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time },
        tier = tier
    )

    // 充电组件列表（4个装备槽）
    private val chargerComponents = mutableListOf<BatteryChargerComponent>()

    constructor(pos: BlockPos, state: BlockState) : this(
        MfsuBlockEntity::class.type(),
        pos,
        state
    ) {
        // 初始化4个装备槽的充电组件
        for (slot in 0 until INVENTORY_SIZE) {
            chargerComponents.add(
                BatteryChargerComponent(
                    inventory = this,
                    batterySlot = slot,
                    machineTierProvider = { tier },
                    machineEnergyProvider = { sync.amount },
                    extractEnergy = { requested -> sync.consumeEnergy(requested) },
                    canChargeNow = { true }
                )
            )
        }
    }

    // Inventory 接口实现
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() {
        super.markDirty()
    }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("container.ic2_120.mfsu")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MfsuScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(MfsuSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MfsuSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 给4个装备槽的装备充电
        for (charger in chargerComponents) {
            charger.tick()
        }

        // 在 tick 结束时同步当前 tick 的实际输入/输出
        sync.syncCurrentTickFlow()
    }
}

