package ic2_120.content.screen

import ic2_120.content.block.nuclear.NuclearReactorBlock
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import ic2_120.content.network.NetworkManager
import ic2_120.content.network.ReactorLayoutLockPacket
import ic2_120.content.reactor.IBaseReactorComponent
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.ScreenFactory

/**
 * 核反应堆 ScreenHandler。
 * 槽位数量根据打开时的容量动态确定（27–81），竖排：3 列→9 列，每列 9 行。
 * 若在打开界面时添加/移除反应仓，需关闭重开以刷新槽位数量。
 *
 * 机器槽 GUI 坐标统一为 (0,0)，由客户端 [NuclearReactorScreen] Compose 锚点写回。
 *
 * 玩家栏纵坐标由格网与 [GRID_ROWS] 推导（[playerInvY] / [hotbarY]），不套用 [GuiSize] 固定分档。
 */
@ModScreenHandler(block = NuclearReactorBlock::class)
class NuclearReactorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    /** 打开时的反应堆槽位数量（27–81） */
    val reactorSlotCount: Int,
    /** 核反应堆方块实体 */
    val reactor: NuclearReactorBlockEntity? = null,
    /** 是否为热模式（热模式时显示 4 个流体槽） */
    val isThermalMode: Boolean = false,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(NuclearReactorScreenHandler::class.type(), syncId) {

    val sync = NuclearReactorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        val invSize = if (isThermalMode) NuclearReactorBlockEntity.INVENTORY_SIZE else reactorSlotCount
        checkSize(blockInventory, invSize)
        addProperties(propertyDelegate)

        // 槽位竖排：3 列→9 列，每列 9 行。index: col = i/9, row = i%9（几何由客户端 Compose 布局）
        // 反应堆槽位需要自定义 canInsert（布局锁定检查），手动添加
        for (index in 0 until reactorSlotCount) {
            val spec = itemStorage?.deriveSlotSpec(index) ?: REACTOR_SLOT_SPEC
            val handlerIndex = slots.size
            beSlotToHandlerIndex[index] = handlerIndex
            addSlot(object : PredicateSlot(blockInventory, index, 0, 0, spec) {
                override fun canInsert(stack: ItemStack): Boolean {
                    // 布局锁定：空槽不允许插入，只允许与锁定类型匹配的物品
                    if (reactor?.layoutLocked == true) {
                        val lockedItem = reactor.getLockedItemForSlot(index)
                        if (lockedItem == null) return false
                        if (stack.item !== lockedItem) return false
                    }
                    // 基础校验：IBaseReactorComponent
                    if (!super.canInsert(stack)) return false
                    return true
                }
            })
        }

        // 热模式：4 个流体槽（GUI 位置由客户端 Compose 布局）
        if (isThermalMode) {
            addTrackedSlot(blockInventory, NuclearReactorBlockEntity.SLOT_COOLANT_INPUT)
            addTrackedSlot(blockInventory, NuclearReactorBlockEntity.SLOT_COOLANT_OUTPUT)
            addTrackedSlot(blockInventory, NuclearReactorBlockEntity.SLOT_HOT_COOLANT_INPUT)
            addTrackedSlot(blockInventory, NuclearReactorBlockEntity.SLOT_HOT_COOLANT_OUTPUT)
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    /** 流体槽起始索引（仅热模式时有） */
    private val fluidSlotStart: Int get() = reactorSlotCount
    private val fluidSlotEnd: Int get() = if (isThermalMode) reactorSlotCount + 4 else reactorSlotCount

    /** 玩家主背包首槽在 [slots] 中的索引（客户端 Compose 锚点与 quickMove 共用） */
    val playerInventorySlotStart: Int get() = fluidSlotEnd

    private val playerInvStart: Int get() = playerInventorySlotStart

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, 0, 0, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, playerInvStart, slots.size, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in playerInvStart until slots.size -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    if (!insertItem(stackInSlot, playerInvStart, slots.size, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is NuclearReactorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_ID_TOGGLE_LOCK) return false
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? NuclearReactorBlockEntity ?: return@get
            be.toggleLayoutLock()
            // 发送锁数据同步包到客户端
            val packet = ReactorLayoutLockPacket(pos, be.lockedSlots.toMap())
            NetworkManager.sendToClient(player as ServerPlayerEntity, packet)
        })
        return true
    }

    /** 反应堆槽位列数（3–9） */
    val reactorCols: Int get() = (reactorSlotCount + 8) / 9

    /** 玩家背包 Y 偏移（用于 Screen 绘制） */
    val playerInvY: Int get() = if (isThermalMode) PLAYER_INV_Y_THERMAL else PLAYER_INV_Y_NT

    /** 快捷栏 Y 偏移 */
    val hotbarY: Int get() = if (isThermalMode) HOTBAR_Y_THERMAL else HOTBAR_Y_NT

    /** 槽位区域起始 Y（GUI 相对坐标），电力模式-1 */
    val slotGridY: Int get() = if (isThermalMode) 25 else 24

    companion object {
        const val BUTTON_ID_TOGGLE_LOCK = 100
        const val GRID_ROWS = 9
        /** 槽位区域起始 X（GUI 相对坐标） */
        const val SLOT_GRID_X = 26
        const val SLOT_SIZE = 18
        /** 固定界面框宽度（不随容量变化） */
        const val FRAME_WIDTH = 212
        /** 玩家背包 X 偏移 */
        const val PLAYER_INV_X = 26
        const val PLAYER_INV_Y_NT = 214
        const val PLAYER_INV_Y_THERMAL = 215
        const val HOTBAR_Y_NT = 272
        const val HOTBAR_Y_THERMAL = 273

        private val REACTOR_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> !stack.isEmpty && stack.item is IBaseReactorComponent }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): NuclearReactorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val capacity = buf.readVarInt().coerceIn(NuclearReactorSync.BASE_SLOTS, NuclearReactorBlockEntity.MAX_SLOTS)
            val isThermal = buf.readBoolean()
            val layoutLocked = buf.readBoolean()
            val lockSize = buf.readVarInt()
            val lockedSlots = mutableMapOf<Int, Item>()
            for (i in 0 until lockSize) {
                val slot = buf.readVarInt()
                val item = Registries.ITEM.get(buf.readIdentifier())
                lockedSlots[slot] = item
            }
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(NuclearReactorBlockEntity.INVENTORY_SIZE)
            val reactor = playerInventory.player.world.getBlockEntity(pos) as? NuclearReactorBlockEntity
            // 将锁数据写入 client BE，供槽位 canInsert 和 Screen 虚影渲染使用
            reactor?.let {
                it.layoutLocked = layoutLocked
                it.lockedSlots.clear()
                it.lockedSlots.putAll(lockedSlots)
            }
            return NuclearReactorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount), capacity, reactor, isThermal)
        }
    }
}
