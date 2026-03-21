package ic2_120.content.screen

import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

/**
 * 储电盒 GUI 的 ScreenHandler。
 * 四个等级（BatBox/CESU/MFE/MFSU）共用，根据方块类型动态配置槽位数量和规则。
 */
@ModScreenHandler(names = ["batbox", "cesu", "mfe", "mfsu"])
class EnergyStorageScreenHandler(
    screenHandlerType: net.minecraft.screen.ScreenHandlerType<*>,
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate
) : ScreenHandler(screenHandlerType, syncId) {

    private val config = resolveConfig()
    private val slotCount: Int = config.slotCount

    val sync = EnergyStorageSync(
        schema = SyncedDataView(propertyDelegate),
        getFacing = { Direction.NORTH },
        tier = config.tier,
        capacity = config.capacity
    )

    val useEquipmentSlots: Boolean get() = config.useEquipmentSlots

    private fun resolveConfig(): EnergyStorageConfig {
        return context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path) ?: EnergyStorageConfig.BATBOX
        }, EnergyStorageConfig.BATBOX)
    }

    private fun chargeSlotSpec(maxTier: Int) = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) && item.tier <= maxTier
        },
        maxItemCount = 1
    )

    private fun equipmentSlotSpec(slotType: EquipmentSlot, maxTier: Int) = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) &&
                item.tier <= maxTier &&
                stack.item is net.minecraft.item.ArmorItem &&
                (stack.item as net.minecraft.item.ArmorItem).slotType == slotType
        },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, 5)
        addProperties(propertyDelegate)

        val slotY = 55
        val slotSpacing = 18

        // 4 个装备槽（左侧，slot 1-4），MFE/MFSU 专用
        addSlot(PredicateSlot(blockInventory, 1, 8, slotY, equipmentSlotSpec(EquipmentSlot.HEAD, config.tier)))
        addSlot(PredicateSlot(blockInventory, 2, 8 + slotSpacing, slotY, equipmentSlotSpec(EquipmentSlot.CHEST, config.tier)))
        addSlot(PredicateSlot(blockInventory, 3, 8 + slotSpacing * 2, slotY, equipmentSlotSpec(EquipmentSlot.LEGS, config.tier)))
        addSlot(PredicateSlot(blockInventory, 4, 8 + slotSpacing * 3, slotY, equipmentSlotSpec(EquipmentSlot.FEET, config.tier)))

        // 1 个充电槽（右侧，slot 0），所有等级都有
        addSlot(PredicateSlot(blockInventory, 0, 8 + slotSpacing * 4, slotY, chargeSlotSpec(config.tier)))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val playerInvStart = 5
            val hotbarEnd = 5 + 35
            when (index) {
                in 0..4 -> {
                    // block slot -> player inventory
                    if (!insertItem(stackInSlot, playerInvStart, hotbarEnd + 1, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in playerInvStart..hotbarEnd -> {
                    // player inventory -> block (only charge slot 0 accepts items for non-MFE tiers)
                    // limit stack size to 1 for charge/equipment slots
                    val limited = stackInSlot.copyWithCount(1)
                    if (!insertItem(limited, 0, 5, false)) return ItemStack.EMPTY
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
            val block = world.getBlockState(pos).block
            block is ic2_120.content.block.storage.EnergyStorageBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_SIZE = 18

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): EnergyStorageScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val slotCount = buf.readVarInt()
            buf.readBoolean() // useEquipmentSlots, read but not stored (ScreenHandler gets config from context)
            val ctx = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(5)
            val blockId = Registries.BLOCK.getId(playerInventory.player.world.getBlockState(pos).block)
            val screenHandlerType = Registries.SCREEN_HANDLER.get(Identifier(blockId.namespace, blockId.path))
                ?: error("ScreenHandler type not found for $blockId")
            @Suppress("UNCHECKED_CAST")
            return EnergyStorageScreenHandler(
                screenHandlerType as net.minecraft.screen.ScreenHandlerType<net.minecraft.screen.ScreenHandler>,
                syncId, playerInventory, blockInv, ctx, ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}
