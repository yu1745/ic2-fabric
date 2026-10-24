package ic2_120.content.block.storage

import ic2_120.content.block.*
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.energy.EnergyTier
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

    private companion object {
        const val CHARGE_SLOT = 0
        const val DISCHARGE_SLOT = 1
    }

    private val inventory = DefaultedList.ofSize(config.slotCount, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = buildList {
            add(
                ItemInsertRoute(intArrayOf(CHARGE_SLOT), matcher = { stack ->
                    val item = stack.item
                    when (item) {
                        is IBatteryItem -> item.canCharge && item.tier <= config.tier
                        is IElectricTool -> item.tier <= config.tier
                        else -> false
                    }
                }, maxPerSlot = 1)
            )
            add(
                ItemInsertRoute(intArrayOf(DISCHARGE_SLOT), matcher = { stack ->
                    when (val item = stack.item) {
                        is IBatteryItem -> item.tier <= config.tier
                        else -> stack.isOf(Items.REDSTONE) || item is EnergiumDust
                    }
                })
            )
        },
        extractSlots = IntArray(config.slotCount) { it },
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
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

    private val chargeComponent = BatteryChargerComponent(
        inventory = this,
        batterySlot = CHARGE_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.extractEnergy(requested) },
        canChargeNow = { true }
    )

    private val dischargeComponent = BatteryChargerComponent(
        inventory = this,
        batterySlot = DISCHARGE_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { config.capacity - sync.amount },
        extractEnergy = { 0L },
        insertEnergy = { requested -> sync.insertEnergy(requested) },
        canChargeNow = { true }
    )

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
        if (slot == DISCHARGE_SLOT) {
            return when (val item = stack.item) {
                is IBatteryItem -> item.tier <= config.tier
                else -> stack.isOf(Items.REDSTONE) || item is EnergiumDust
            }
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

    /**
     * 红石模式（对齐原版 IC2 TileEntityElectricBlock 的 7 档循环）：
     * 0 不输出（默认）；1 满电时输出；2 部分充电时输出；3 未满电时输出；
     * 4 空电时输出；5 收到红石时不输出能量；6 收到红石且未满时不输出能量。
     * 仅储电箱（非充电座）可切换；充电座恒为 0。
     * 真值存在 sync.redstoneMode（SyncedData 属性：自动 NBT 持久化 + 属性同步到客户端 GUI）。
     */
    val redstoneMode: Int
        get() = if (config.emitRedstoneWhenNotFull) sync.redstoneMode.coerceIn(0, 6) else 0

    /** 循环切换红石模式（服务端调用；模式系统入口，含邻居刷新与聊天提示）。 */
    fun cycleRedstoneMode(): Boolean {
        if (!config.emitRedstoneWhenNotFull) return false
        setRedstoneMode((redstoneMode + 1) % 7)
        return true
    }

    fun setRedstoneMode(mode: Int) {
        if (!config.emitRedstoneWhenNotFull) return
        sync.redstoneMode = mode.coerceIn(0, 6)
        markDirty()
        // 输出电平可能随模式变化，立即刷新邻居
        world?.updateNeighborsAlways(pos, cachedState.block)
        // 原版行为：切换时聊天栏提示当前模式名
        world?.let { w ->
            val players = w.getNonSpectatingEntities(PlayerEntity::class.java, net.minecraft.util.math.Box(pos).expand(8.0))
            for (p in players) {
                p.sendMessage(Text.translatable("gui.ic2_120.eu_storage.redstone_mode$redstoneMode"), true)
            }
        }
    }

    /** 当前模式是否应输出红石信号（0/5/6 档不发射）。 */
    val emitsRedstoneNow: Boolean
        get() = when (redstoneMode) {
            1 -> sync.amount >= capacityMinusBuffer
            2 -> sync.amount > 0 && sync.amount < config.capacity
            3 -> sync.amount < config.capacity
            4 -> sync.amount <= outputPower
            else -> false
        }

    /** 原版阈値：容量 - 20×输出功率（output = EnergyTier.euPerTickFromTier(tier)）。 */
    private val capacityMinusBuffer: Long get() = config.capacity - outputPower * 20
    private val outputPower: Long get() = EnergyTier.euPerTickFromTier(config.tier)

    /** 红石输出翻转时通知邻居，让红石线/比较器即时响应充放电状态变化。 */
    private var lastRedstoneEmit = false

    open fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        consumeFuel()

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()

       var chargedThisTick = chargeComponent.tick()
       chargedThisTick += dischargeComponent.discharge()

       if (config.chargePlayersAbove) {
           chargedThisTick += chargePlayersAbove(world, pos)
       }

       updateActiveState(world, pos, chargedThisTick > 0L)

       if (config.emitRedstoneWhenNotFull) {
           val emit = emitsRedstoneNow
           if (emit != lastRedstoneEmit) {
               lastRedstoneEmit = emit
               world.updateNeighborsAlways(pos, state.block)
           }
       }

       sync.syncCurrentTickFlow()
   }

    private fun consumeFuel() {
        val fuelStack = inventory[DISCHARGE_SLOT]
        val energyPerItem = when {
            fuelStack.isOf(Items.REDSTONE) -> 800L
            fuelStack.item is EnergiumDust -> 16_000L
            else -> return
        }

        if (config.capacity - sync.amount < energyPerItem) return
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
