package ic2_120.content.block.machines

import ic2_120.compat.ContainerHelper26

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.TradeOMatBlock
import ic2_120.content.block.WirelessTradeOMatBlock
import ic2_120.content.screen.TradeOMatScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
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
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 交易机方块实体基类。
 *
 * 自动交易方块。拥有者设置 demand（需求模板）和 offer（供给模板），
 * 玩家在 input 槽放入匹配 demand 的物品后，机器从相邻库存取出 offer 物品放入 output。
 *
 * 槽位：
 * - SLOT_DEMAND：需求模板（拥有者设置）
 * - SLOT_OFFER：供给模板（拥有者设置）
 * - SLOT_INPUT：买家放入（仅接受匹配 demand 的物品）
 * - SLOT_OUTPUT：买家取出（交易结果）
 */
abstract class TradeOMatBlockEntityBase(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, ExtendedMenuProvider<FriendlyByteBuf> {

    override fun getInventory(): Container = this
    override val tier: Int = 1

    companion object {
        const val SLOT_DEMAND = 0
        const val SLOT_OFFER = 1
        const val SLOT_INPUT = 2
        const val SLOT_OUTPUT = 3
        const val INVENTORY_SIZE = 4

        private const val NBT_TOTAL_TRADES = "TotalTrades"
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData()

    var totalTrades: Int = 0

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
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
        SLOT_DEMAND, SLOT_OFFER -> true
        SLOT_INPUT -> matchesDemand(stack)
        SLOT_OUTPUT -> false
        else -> false
    }

    private fun matchesDemand(stack: ItemStack): Boolean {
        val demand = getItem(SLOT_DEMAND)
        if (demand.isEmpty) return false
        return ItemStack.isSameItem(demand, stack)
    }

    fun isOwner(player: Player): Boolean {
        val uuid = ownerUuid ?: return true
        return player.uuid == uuid || player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
    }

    /** 返回一个 ScreenHandlerFactory，携带 isOwner 标志。 */
    fun getScreenHandlerFactory(player: Player): ExtendedMenuProvider<FriendlyByteBuf> {
        val owner = isOwner(player)
        val be = this
        return object : ExtendedMenuProvider<FriendlyByteBuf> {
            override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                    buf.writeBlockPos(pos)
                    buf.writeBoolean(owner)
                return buf
            }

            override fun getDisplayName(): Component = be.getDisplayName()

            override fun createMenu(syncId: Int, playerInventory: Inventory, p: Player): AbstractContainerMenu =
                TradeOMatScreenHandler(syncId, playerInventory, be,
                    ContainerLevelAccess.create(be.world!!, be.pos), owner)
        }
    }

    abstract override fun getDisplayName(): Component

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        TradeOMatScreenHandler(syncId, playerInventory, this,
            ContainerLevelAccess.create(world!!, pos), player?.let { isOwner(it) } ?: false)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeBoolean(isOwner(player))
        return buf
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        totalTrades = nbt.getIntOr(NBT_TOTAL_TRADES, 0)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putInt(NBT_TOTAL_TRADES, totalTrades)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        tryTrade(world, pos)
    }

    private fun tryTrade(world: Level, pos: BlockPos) {
        val demand = getItem(SLOT_DEMAND)
        val offer = getItem(SLOT_OFFER)
        val input = getItem(SLOT_INPUT)
        val output = getItem(SLOT_OUTPUT)

        if (demand.isEmpty || offer.isEmpty || input.isEmpty) {
            setActiveState(world, pos, world.getBlockState(pos), false)
            return
        }
        if (!ItemStack.isSameItem(demand, input) || input.count < demand.count) {
            setActiveState(world, pos, world.getBlockState(pos), false)
            return
        }

        val offerStack = offer.copy()
        if (output.isEmpty) {
            // ok
        } else if (!ItemStack.isSameItem(output, offerStack) || output.count + offerStack.count > offerStack.maxStackSize) {
            setActiveState(world, pos, world.getBlockState(pos), false)
            return
        }

        val offerVariant = ItemVariant.of(offerStack)
        val offerAmount = offerStack.count.toLong()
        val fetched = fetchFromAdjacent(world, pos, offerVariant, offerAmount)
        if (fetched < offerAmount) {
            setActiveState(world, pos, world.getBlockState(pos), false)
            return
        }

        input.shrink(demand.count)
        if (input.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)

        val demandVariant = ItemVariant.of(demand)
        distributeToAdjacent(world, pos, demandVariant, demand.count.toLong())

        if (output.isEmpty) setItem(SLOT_OUTPUT, offerStack.copy())
        else output.grow(offerStack.count)

        totalTrades++
        setChanged()
        setActiveState(world, pos, world.getBlockState(pos), true)
    }

    private fun fetchFromAdjacent(world: Level, pos: BlockPos, variant: ItemVariant, amount: Long): Long {
        var remaining = amount
        for (dir in Direction.entries) {
            if (remaining <= 0) break
            val storage = ItemStorage.SIDED.find(world, pos.relative(dir), dir.opposite) ?: continue
            Transaction.openOuter().use { tx ->
                val extracted = storage.extract(variant, remaining, tx)
                if (extracted > 0) {
                    tx.commit()
                    remaining -= extracted
                }
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

@ModBlockEntity(block = TradeOMatBlock::class)
class TradeOMatBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : TradeOMatBlockEntityBase(type, pos, state) {

    override val activeProperty = TradeOMatBlock.ACTIVE

    constructor(pos: BlockPos, state: BlockState) : this(
        TradeOMatBlockEntity::class.type(),
        pos,
        state
    )

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.trade_o_mat")
}

// TODO: 无线交易机 BlockEntity 待 TradingMarket 系统实现后启用
//@ModBlockEntity(block = WirelessTradeOMatBlock::class)
class WirelessTradeOMatBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : TradeOMatBlockEntityBase(type, pos, state) {

    override val activeProperty = WirelessTradeOMatBlock.ACTIVE

    constructor(pos: BlockPos, state: BlockState) : this(
        WirelessTradeOMatBlockEntity::class.type(),
        pos,
        state
    )

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.wireless_trade_o_mat")
}
