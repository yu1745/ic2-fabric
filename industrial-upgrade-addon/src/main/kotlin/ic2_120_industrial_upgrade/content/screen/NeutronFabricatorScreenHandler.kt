package ic2_120_industrial_upgrade.content.screen

import ic2_120.content.item.EmptyCell
import ic2_120.content.item.FluidCellItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.block.NeutronFabricatorBlockEntity
import ic2_120_industrial_upgrade.content.sync.NeutronFabricatorSync
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.core.Direction

@ModScreenHandler(names = ["neutron_fabricator"])
class NeutronFabricatorScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    private val context: ContainerLevelAccess,
    propertyDelegate: ContainerData,
    blockInventory: Container? = null,
    private val machineSlotCount: Int = 0
) : AbstractContainerMenu(NeutronFabricatorScreenHandler::class.type(), syncId) {

    /** 客户端通过 SyncedDataView 包装 propertyDelegate 读取同步字段，供 Screen 渲染使用 */
    val sync = NeutronFabricatorSync(
        schema = SyncedDataView(propertyDelegate),
        capacity = 1L,
        tier = 1,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    /** 升级槽：仅接受 UpgradeItemRegistry 注册且本机实现了对应接口的升级（与物质生成机一致） */
    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.evaluate({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    companion object {
        private const val SLOT_SIZE = 18
        // 升级槽：贴图左侧 152 列，纵向 8/26/44/62（对齐物质生成机的 4 个升级格子）
        private const val UPGRADE_SLOT_X = 152
        private val UPGRADE_SLOT_YS = intArrayOf(8, 26, 44, 62)
        // 容器输入/输出槽：贴图右侧（125 列）
        private const val CONTAINER_INPUT_X = 125
        private const val CONTAINER_INPUT_Y = 23
        private const val CONTAINER_OUTPUT_Y = 59
        // 玩家背包
        private const val PLAYER_INV_X = 8
        private const val PLAYER_INV_Y = 84
        private const val HOTBAR_Y = 142

        /** 容器输入槽：接受空桶 / 空单元 / 流体单元（具体是否可装由 BlockEntity 服务端逻辑判定） */
        private val containerInputSpec = SlotSpec(
            canInsert = { stack ->
                val item = stack.item
                item == net.minecraft.world.item.Items.BUCKET ||
                    item is EmptyCell ||
                    item is FluidCellItem
            }
        )

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): NeutronFabricatorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ContainerLevelAccess.create(playerInventory.player.level(), pos)
            val propertyDelegate = SimpleContainerData(propertyCount)
            // 客户端用临时 SimpleContainer 占位（真实数据由服务端同步）
            val inv = SimpleContainer(NeutronFabricatorBlockEntity.INVENTORY_SIZE)
            return NeutronFabricatorScreenHandler(syncId, playerInventory, context, propertyDelegate, inv, NeutronFabricatorBlockEntity.INVENTORY_SIZE)
        }
    }

    init {
        addDataSlots(propertyDelegate)

        if (blockInventory != null) {
            checkContainerSize(blockInventory, NeutronFabricatorBlockEntity.INVENTORY_SIZE)
            // 升级槽（BlockEntity slot 0-3）：贴图左侧 152 列，纵向 8/26/44/62
            for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
                addSlot(PredicateSlot(blockInventory, NeutronFabricatorBlockEntity.SLOT_UPGRADE_INDICES[i], UPGRADE_SLOT_X, UPGRADE_SLOT_YS[i], upgradeSlotSpec))
            }
            // 容器输入槽（BlockEntity slot 4）：125, 23
            addSlot(PredicateSlot(blockInventory, NeutronFabricatorBlockEntity.SLOT_CONTAINER_INPUT, CONTAINER_INPUT_X, CONTAINER_INPUT_Y, containerInputSpec))
            // 容器输出槽（BlockEntity slot 5）：125, 59 —— 禁物流插入（仅配方产物落槽），否则 shift+左键可污染输出
            addSlot(PredicateSlot(blockInventory, NeutronFabricatorBlockEntity.SLOT_CONTAINER_OUTPUT, CONTAINER_INPUT_X, CONTAINER_OUTPUT_Y, SlotSpec(canInsert = { false })))
        }
        // 玩家背包（3×9）
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * SLOT_SIZE, PLAYER_INV_Y + row * SLOT_SIZE))
            }
        }
        // 快捷栏（9）
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_SIZE, HOTBAR_Y))
        }
    }

    private val hotbarEnd: Int get() = machineSlotCount + 36

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.item
            stack = stackInSlot.copy()
            if (index < machineSlotCount) {
                // 机器槽 → 玩家背包
                if (!moveItemStackTo(stackInSlot, machineSlotCount, hotbarEnd, true)) return ItemStack.EMPTY
            } else {
                // 玩家背包 → 机器槽（由 PredicateSlot 的 spec 自动判定能否放入对应槽）
                if (!moveItemStackTo(stackInSlot, 0, machineSlotCount, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
}
