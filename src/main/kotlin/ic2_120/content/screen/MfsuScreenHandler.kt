package ic2_120.content.screen

import ic2_120.content.sync.MfsuSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.MfsuBlock
import ic2_120.content.block.machines.MfsuBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Direction
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

/**
 * MFSU GUI 的 ScreenHandler。
 * 4个装备槽（头、胸、腿、脚）+ 玩家背包 + 能量同步。
 */
@ModScreenHandler(block = MfsuBlock::class)
class MfsuScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(MfsuScreenHandler::class), syncId) {

    /** 客户端仅用于 GUI 显示，getFacing 用占位即可（SIDED 仅在服务端 BlockEntity 使用）。 */
    val sync = MfsuSync(
        schema = SyncedDataView(propertyDelegate),
        getFacing = { Direction.NORTH }
    )

    // 装备槽位规则：每个槽只能放对应类型的装备
    private val helmetSlotSpec = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) &&
            item.tier <= 4 && // 4级或以下
            stack.item is net.minecraft.item.ArmorItem &&
            (stack.item as net.minecraft.item.ArmorItem).slotType == EquipmentSlot.HEAD
        },
        maxItemCount = 1
    )

    private val chestplateSlotSpec = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) &&
            item.tier <= 4 &&
            stack.item is net.minecraft.item.ArmorItem &&
            (stack.item as net.minecraft.item.ArmorItem).slotType == EquipmentSlot.CHEST
        },
        maxItemCount = 1
    )

    private val leggingsSlotSpec = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) &&
            item.tier <= 4 &&
            stack.item is net.minecraft.item.ArmorItem &&
            (stack.item as net.minecraft.item.ArmorItem).slotType == EquipmentSlot.LEGS
        },
        maxItemCount = 1
    )

    private val bootsSlotSpec = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) &&
            item.tier <= 4 &&
            stack.item is net.minecraft.item.ArmorItem &&
            (stack.item as net.minecraft.item.ArmorItem).slotType == EquipmentSlot.FEET
        },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, MfsuBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 装备槽（UI下方，横向）
        val armorSlotX = 8
        val armorSlotY = 55
        val armorSlotSpacing = 18
        addSlot(PredicateSlot(blockInventory, MfsuBlockEntity.SLOT_HELMET, armorSlotX, armorSlotY, helmetSlotSpec))
        addSlot(PredicateSlot(blockInventory, MfsuBlockEntity.SLOT_CHESTPLATE, armorSlotX + armorSlotSpacing, armorSlotY, chestplateSlotSpec))
        addSlot(PredicateSlot(blockInventory, MfsuBlockEntity.SLOT_LEGGINGS, armorSlotX + armorSlotSpacing * 2, armorSlotY, leggingsSlotSpec))
        addSlot(PredicateSlot(blockInventory, MfsuBlockEntity.SLOT_BOOTS, armorSlotX + armorSlotSpacing * 3, armorSlotY, bootsSlotSpec))

        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                in 0..3 -> {
                    // 装备槽 -> 玩家背包
                    if (!insertItem(stackInSlot, 4, 40, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in 4..40 -> {
                    // 玩家背包 -> 装备槽
                    if (!insertItem(stackInSlot, 0, 4, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is MfsuBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MfsuScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val ctx = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(MfsuBlockEntity.INVENTORY_SIZE)
            return MfsuScreenHandler(syncId, playerInventory, blockInv, ctx, ArrayPropertyDelegate(propertyCount))
        }
    }
}
