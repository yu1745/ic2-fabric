package ic2_120.content.block.storage

import ic2_120.content.block.*
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.EnergiumDust
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.item.energy.chargePlayerInventoryPerItemLimit
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
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
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Box
import net.minecraft.world.World

/**
 * 储电盒方块实体基类。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
abstract class EnergyStorageBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    val config: EnergyStorageConfig
) : BlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ExtendedScreenHandlerFactory, ITieredMachine {

    override val tier: Int get() = config.tier

    private val fuelSlotIndex: Int = config.slotCount - 1

    private val inventory = DefaultedList.ofSize(config.slotCount, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = buildList {
            add(
                ItemInsertRoute(intArrayOf(0), matcher = { stack ->
                    val item = stack.item
                    (item is IBatteryItem || item is IElectricTool) && item.tier <= config.tier
                }, maxPerSlot = 1)
            )
            add(
                ItemInsertRoute(intArrayOf(fuelSlotIndex), matcher = { stack ->
                    stack.isOf(Items.REDSTONE) || stack.item is EnergiumDust
                }, maxPerSlot = 64)
            )
        },
        extractSlots = IntArray(config.slotCount) { it },
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = EnergyStorageSync(
        syncedData,
        {
            world?.getBlockState(pos)?.let { state ->
                state.getOrEmpty(Properties.FACING)
                    .orElse(state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH))
            } ?: Direction.NORTH
        },
        { world?.time },
        config.tier,
        config.capacity
    )
    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val chargerComponents = mutableListOf<BatteryChargerComponent>()

   init {
       for (slot in 0 until config.slotCount) {
           if (slot == fuelSlotIndex) continue
           chargerComponents.add(
               BatteryChargerComponent(
                   inventory = this,
                   batterySlot = slot,
                   machineTierProvider = { tier },
                    // 充电模式返回机器当前储能（可输出量），放电模式返回剩余容量（可接收量）
                    machineEnergyProvider = {
                        if (sync.chargeMode == EnergyStorageSync.MODE_DISCHARGE)
                            config.capacity - sync.amount
                        else
                            sync.amount
                    },
                    extractEnergy = { requested -> sync.extractEnergy(requested) },
                    insertEnergy = { requested -> sync.insertEnergy(requested) },
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

    override fun isValid(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (slot == fuelSlotIndex) {
            return stack.isOf(Items.REDSTONE) || stack.item is EnergiumDust
        }
        val item = stack.item
        return (item is IBatteryItem || item is IElectricTool) && item.tier <= config.tier
    }

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeVarInt(config.slotCount)
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
            syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage
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

    open fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        consumeFuel()

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()

       var chargedThisTick = 0L
       for (charger in chargerComponents) {
           chargedThisTick += if (sync.chargeMode == EnergyStorageSync.MODE_DISCHARGE) {
               charger.discharge()
           } else {
               charger.tick()
           }
       }

       if (config.chargePlayersAbove) {
           chargedThisTick += chargePlayersAbove(world, pos)
       }

       updateActiveState(world, pos, chargedThisTick > 0L)

       sync.syncCurrentTickFlow()
   }

    private fun consumeFuel() {
        val fuelStack = inventory[fuelSlotIndex]
        if (fuelStack.isEmpty) return

        val energyPerItem = when {
            fuelStack.isOf(Items.REDSTONE) -> 800L
            fuelStack.item is EnergiumDust -> 16000L
            else -> return
        }

        val remainingCapacity = config.capacity - sync.amount
        if (remainingCapacity < energyPerItem) return

        fuelStack.decrement(1)
        sync.amount += energyPerItem
        markDirty()
    }

    private fun chargePlayersAbove(world: World, pos: BlockPos): Long {
        val area = Box(
            pos.x.toDouble(),
            pos.y.toDouble() + 1.0,
            pos.z.toDouble(),
            pos.x.toDouble() + 1.0,
            pos.y.toDouble() + 2.9,
            pos.z.toDouble() + 1.0
        )
        val players = world.getNonSpectatingEntities(PlayerEntity::class.java, area)
        var charged = 0L
        for (player in players) {
            charged += chargePlayerInventoryPerItemLimit(
                player = player,
                machineTier = tier,
                machineEnergyProvider = { sync.amount },
                extractEnergy = { requested -> sync.extractEnergy(requested) }
            )
        }
        return charged
    }

   private fun updateActiveState(world: World, pos: BlockPos, active: Boolean) {
       val current = world.getBlockState(pos)
       if (!current.contains(EnergyStorageBlock.ACTIVE)) return
       if (current.get(EnergyStorageBlock.ACTIVE) == active) return
       world.setBlockState(pos, current.with(EnergyStorageBlock.ACTIVE, active), Block.NOTIFY_LISTENERS)
   }

    fun toggleChargeMode() {
        sync.chargeMode = if (sync.chargeMode == EnergyStorageSync.MODE_DISCHARGE)
            EnergyStorageSync.MODE_CHARGE
        else
            EnergyStorageSync.MODE_DISCHARGE
        markDirty()
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

    @ModBlockEntity(block = BatBoxChargepadBlock::class)
    class BatBoxChargepadBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.BATBOX_CHARGEPAD) {
        constructor(pos: BlockPos, state: BlockState) : this(BatBoxChargepadBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.batbox_chargepad"
    }

    @ModBlockEntity(block = CesuChargepadBlock::class)
    class CesuChargepadBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.CESU_CHARGEPAD) {
        constructor(pos: BlockPos, state: BlockState) : this(CesuChargepadBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.cesu_chargepad"
    }

    @ModBlockEntity(block = MfeChargepadBlock::class)
    class MfeChargepadBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.MFE_CHARGEPAD) {
        constructor(pos: BlockPos, state: BlockState) : this(MfeChargepadBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.mfe_chargepad"
    }

    @ModBlockEntity(block = MfsuChargepadBlock::class)
    class MfsuChargepadBlockEntity(
        type: BlockEntityType<*>,
        pos: BlockPos,
        state: BlockState
    ) : EnergyStorageBlockEntity(type, pos, state, EnergyStorageConfig.MFSU_CHARGEPAD) {
        constructor(pos: BlockPos, state: BlockState) : this(MfsuChargepadBlockEntity::class.type(), pos, state)
        override val containerTranslationKey: String = "container.ic2_120.mfsu_chargepad"
    }
}
