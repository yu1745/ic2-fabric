package ic2_120.content.block.storage

import ic2_120.content.block.*
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
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
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 储电盒方块实体基类。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
abstract class EnergyStorageBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    val config: EnergyStorageConfig
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory, ITieredMachine {

    override val tier: Int get() = config.tier

    private val inventory = DefaultedList.ofSize(config.slotCount, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = EnergyStorageSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time },
        config.tier,
        config.capacity
    )

    private val chargerComponents = mutableListOf<BatteryChargerComponent>()

    init {
        for (slot in 0 until config.slotCount) {
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

    override fun size(): Int = config.slotCount
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
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
        buf.writeVarInt(5) // always 5 slots: 1 charge + 4 equipment
        buf.writeBoolean(config.useEquipmentSlots)
    }

    override fun getDisplayName(): Text = Text.translatable(containerTranslationKey)

    abstract val containerTranslationKey: String

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        val blockId = Registries.BLOCK.getId(world!!.getBlockState(pos).block)
        val screenHandlerType = Registries.SCREEN_HANDLER.get(Identifier(blockId.namespace, blockId.path))
            ?: error("ScreenHandler type not found for $blockId")
        @Suppress("UNCHECKED_CAST")
        return ic2_120.content.screen.EnergyStorageScreenHandler(
            screenHandlerType as net.minecraft.screen.ScreenHandlerType<net.minecraft.screen.ScreenHandler>,
            syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData
        )
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(EnergyStorageSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(EnergyStorageSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        for (charger in chargerComponents) {
            charger.tick()
        }

        sync.syncCurrentTickFlow()
    }

    // ============== Concrete BlockEntities ==============

    @ModBlockEntity(block = BatBoxBlock::class)
    class BatBoxBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.BATBOX) {
        constructor(pos: BlockPos, state: BlockState) : this(BatBoxBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.batbox"
    }

    @ModBlockEntity(block = CesuBlock::class)
    class CesuBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.CESU) {
        constructor(pos: BlockPos, state: BlockState) : this(CesuBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.cesu"
    }

    @ModBlockEntity(block = MfeBlock::class)
    class MfeBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.MFE) {
        constructor(pos: BlockPos, state: BlockState) : this(MfeBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.mfe"
    }

    @ModBlockEntity(block = MfsuBlock::class)
    class MfsuBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.MFSU) {
        constructor(pos: BlockPos, state: BlockState) : this(MfsuBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.mfsu"
    }
}
