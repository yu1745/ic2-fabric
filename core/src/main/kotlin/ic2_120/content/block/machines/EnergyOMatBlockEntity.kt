package ic2_120.content.block.machines

import ic2_120.compat.ContainerHelper26

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.EnergyOMatBlock
import ic2_120.content.energy.EnergyTier
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.EnergyOMatScreenHandler
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.syncs.SyncedData
import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 能源交易机方块实体。
 *
 * 玩家放入需求物品（匹配 demand 模板），物品被送入拥有者的相邻库存，
 * 机器累计 paidFor（EU 额度）。拥有者的电网从非正面输入 EU（受 paidFor 限制），
 * 正面输出 EU 供买家取用。
 *
 * 槽位：
 * - SLOT_DEMAND：需求模板（拥有者可设置）
 * - SLOT_INPUT：买家放入（匹配 demand）
 * - SLOT_CHARGE：充电槽（从 buffer 给电池充电）
 * - SLOT_UPGRADE：升级槽（储能、高压）
 */
@ModBlockEntity(block = EnergyOMatBlock::class)
class EnergyOMatBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport,
    ExtendedMenuProvider<FriendlyByteBuf> {

    override val activeProperty = EnergyOMatBlock.ACTIVE
    override fun getInventory(): Container = this
    override val tier: Int = 1

    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val SLOT_DEMAND = 0
        const val SLOT_INPUT = 1
        const val SLOT_CHARGE = 2
        const val SLOT_UPGRADE = 3
        const val INVENTORY_SIZE = 4

        private const val NBT_EU_OFFER = "EuOffer"
        private const val NBT_PAID_FOR = "PaidFor"
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)

    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_UPGRADE), matcher = { it.item is ic2_120.content.item.IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_CHARGE), matcher = { it.item is IBatteryItem || it.item is IElectricTool }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { matchesDemand(it) })
        ),
        extractSlots = intArrayOf(SLOT_CHARGE),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = EnergyOMatSync(
        syncedData,
        { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(1 + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val charger = BatteryChargerComponent(
        inventory = this,
        batterySlot = SLOT_CHARGE,
        machineTierProvider = { 1 + voltageTierBonus },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.extractEnergy(requested) }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        EnergyOMatBlockEntity::class.type(),
        pos,
        state
    )

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == SLOT_CHARGE && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_DEMAND -> true
        SLOT_INPUT -> matchesDemand(stack)
        SLOT_CHARGE -> stack.item is IBatteryItem || stack.item is IElectricTool
        SLOT_UPGRADE -> stack.item is ic2_120.content.item.IUpgradeItem
        else -> false
    }

    private fun matchesDemand(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val demand = getItem(SLOT_DEMAND)
        if (demand.isEmpty) return false
        return ItemStack.isSameItem(demand, stack)
    }

    fun isOwner(player: Player): Boolean {
        val uuid = ownerUuid ?: return true
        return player.uuid == uuid || player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
    }

    fun getScreenHandlerFactory(player: Player): ExtendedMenuProvider<FriendlyByteBuf> {
        val owner = isOwner(player)
        val be = this
        return object : ExtendedMenuProvider<FriendlyByteBuf> {
            override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                    buf.writeBlockPos(pos)
                    buf.writeVarInt(syncedData.count)
                    buf.writeBoolean(owner)
                return buf
            }
            override fun getDisplayName(): Component = be.getDisplayName()
            override fun createMenu(syncId: Int, playerInventory: Inventory, p: Player): AbstractContainerMenu =
                EnergyOMatScreenHandler(syncId, playerInventory, be,
                    ContainerLevelAccess.create(be.world!!, be.pos), be.syncedData, owner)
        }
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.energy_o_mat")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        EnergyOMatScreenHandler(syncId, playerInventory, this,
            ContainerLevelAccess.create(world!!, pos), syncedData, player?.let { isOwner(it) } ?: false)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.count)
            buf.writeBoolean(isOwner(player))
        return buf
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLongOr(EnergyOMatSync.NBT_ENERGY_STORED, 0L)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.euOffer = nbt.getIntOr(NBT_EU_OFFER, 0).coerceAtLeast(100)
        sync.paidFor = nbt.getIntOr(NBT_PAID_FOR, 0).coerceAtLeast(0)

    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(EnergyOMatSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_EU_OFFER, sync.euOffer)
        nbt.putInt(NBT_PAID_FOR, sync.paidFor)
    }

    fun adjustEuOffer(delta: Int) {
        sync.euOffer = (sync.euOffer + delta).coerceAtLeast(100)
        setChanged()
    }

    private var prevTickAmount: Long = 0L

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 应用升级
        EnergyStorageUpgradeComponent.apply(this, intArrayOf(SLOT_UPGRADE), this)
        TransformerUpgradeComponent.apply(this, intArrayOf(SLOT_UPGRADE), this)

        adjacentEnergyTransfer.tick()

        // 正面输出后扣减 paidFor（买家通过电缆从正面拉走的能量）
        if (sync.amount < prevTickAmount) {
            val consumed = (prevTickAmount - sync.amount)
            sync.paidFor = (sync.paidFor - consumed.toInt().coerceAtLeast(0)).coerceAtLeast(0)
        }
        prevTickAmount = sync.amount

        // 交易：检查 input 是否匹配 demand
        processTrade(world, pos)

        // 充电：从 buffer 给电池充电
        charger.tick()

        // 更新 active 状态
        val isActive = sync.amount > 0 || sync.paidFor > 0
        setActiveState(world, pos, state, isActive)
    }

    private fun processTrade(world: Level, pos: BlockPos) {
        val demand = getItem(SLOT_DEMAND)
        val input = getItem(SLOT_INPUT)
        if (demand.isEmpty || input.isEmpty) return
        if (!ItemStack.isSameItem(demand, input) || input.count < demand.count) return

        // 模拟：demand 物品能否送入相邻库存
        val demandVariant = ItemVariant.of(demand)
        val demandAmount = demand.count.toLong()
        val canDistribute = simulateDistribute(world, pos, demandVariant, demandAmount)
        if (canDistribute < demandAmount) return

        // 实际执行：消耗 input，送入相邻库存
        input.shrink(demand.count)
        if (input.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
        distributeToAdjacent(world, pos, demandVariant, demandAmount)

        // 累计 EU 额度
        sync.paidFor += sync.euOffer
        setChanged()
    }

    private fun simulateDistribute(world: Level, pos: BlockPos, variant: ItemVariant, amount: Long): Long {
        var remaining = amount
        for (dir in Direction.entries) {
            if (remaining <= 0) break
            val storage = ItemStorage.SIDED.find(world, pos.relative(dir), dir.opposite) ?: continue
            Transaction.openOuter().use { tx ->
                remaining -= storage.insert(variant, remaining, tx)
            }
        }
        return amount - remaining
    }

    private fun distributeToAdjacent(world: Level, pos: BlockPos, variant: ItemVariant, amount: Long) {
        var remaining = amount
        for (dir in Direction.entries) {
            if (remaining <= 0) break
            val storage = ItemStorage.SIDED.find(world, pos.relative(dir), dir.opposite) ?: continue
            Transaction.openOuter().use { tx ->
                val inserted = storage.insert(variant, remaining, tx)
                if (inserted > 0) {
                    tx.commit()
                    remaining -= inserted
                }
            }
        }
    }
}

/**
 * 能源交易机的能量同步数据。
 *
 * - 正面（facing）：仅输出 EU（买家拉电）
 * - 其他面：仅输入 EU（拥有者的电网注入），受 paidFor 限制
 * - 外部注入时自动扣减 paidFor
 */
class EnergyOMatSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long? = { null },
    capacityBonusProvider: () -> Long = { 0L },
    maxInsertPerTickProvider: (() -> Long)? = null
) : UpgradeableTickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    capacityBonusProvider,
    EnergyTier.euPerTickFromTier(1),
    EnergyTier.euPerTickFromTier(1),
    currentTickProvider,
    maxInsertPerTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    var euOffer by schema.int("EuOffer", default = 1000)
    var paidFor by schema.int("PaidFor", default = 0)

    override fun getSideMaxInsert(side: Direction?): Long {
        if (side == null) return 0L
        val facing = getFacing()
        if (side == facing) return 0L  // 正面不接收
        // 非正面正常接收，不受 paidFor 限制
        val transformerMax = getEffectiveMaxInsertPerTick()
        val space = (getEffectiveCapacity() - amount).coerceAtLeast(0L)
        return minOf(transformerMax, space)
    }

    override fun getSideMaxExtract(side: Direction?): Long {
        if (side == null) return 0L
        val facing = getFacing()
        // 正面输出受 paidFor 额度限制
        if (side != facing) return 0L
        return minOf(paidFor.toLong(), amount)
    }


    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
