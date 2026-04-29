package ic2_120.content.screen

import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
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
import ic2_120.registry.annotation.ScreenFactory

/**
 * 储电盒 GUI 的 ScreenHandler。
 * 四个等级（BatBox/CESU/MFE/MFSU）共用，根据方块类型动态配置槽位数量和规则。
 */
@ModScreenHandler(
    names = [
        "batbox", "cesu", "mfe", "mfsu",
        "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad"
    ]
)
class EnergyStorageScreenHandler(
    screenHandlerType: net.minecraft.screen.ScreenHandlerType<*>,
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate
) : ScreenHandler(screenHandlerType, syncId) {
    private val config = resolveConfig()
    private val machineSlotCount: Int = if (config.useEquipmentSlots) 5 else 1
    private val chargeSlotSpec = chargeSlotSpec(config.tier)
    private val equipmentSlotSpecs = listOf(
        equipmentSlotSpec(EquipmentSlot.HEAD, config.tier),
        equipmentSlotSpec(EquipmentSlot.CHEST, config.tier),
        equipmentSlotSpec(EquipmentSlot.LEGS, config.tier),
        equipmentSlotSpec(EquipmentSlot.FEET, config.tier)
    )
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

    private fun chargeSlotSpec(maxTier: Int): SlotSpec = SlotSpec(
        canInsert = { stack ->
            val item = stack.item
            (item is IBatteryItem || item is IElectricTool) && item.tier <= maxTier
        },
        maxItemCount = 1
    )

    private fun equipmentSlotSpec(slotType: EquipmentSlot, maxTier: Int): SlotSpec = SlotSpec(
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
        checkSize(blockInventory, machineSlotCount)
        addProperties(propertyDelegate)

        // 4 个装备槽（左侧，slot 1-4），MFE/MFSU 专用
        if (config.useEquipmentSlots) {
            addSlot(PredicateSlot(blockInventory, 1, 0, 0, equipmentSlotSpecs[0]))
            addSlot(PredicateSlot(blockInventory, 2, 0, 0, equipmentSlotSpecs[1]))
            addSlot(PredicateSlot(blockInventory, 3, 0, 0, equipmentSlotSpecs[2]))
            addSlot(PredicateSlot(blockInventory, 4, 0, 0, equipmentSlotSpecs[3]))
        }

        // 1 个充电槽（右侧，slot 0），所有等级都有
        addSlot(PredicateSlot(blockInventory, 0, 0, 0, chargeSlotSpec))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY

        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val movedFromMachine = index in machineSlotIndices()
            val moved = when {
                movedFromMachine -> {
                    if (!insertItem(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, true)) {
                        false
                    } else {
                        slot.onQuickTransfer(stackInSlot, stack)
                        true
                    }
                }
                index in playerInventorySlotStart..hotbarEnd -> {
                    SlotMoveHelper.insertIntoTargets(stackInSlot, machineSlotTargets())
                }
                else -> false
            }

            if (!moved) return ItemStack.EMPTY

            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    private fun machineSlotIndices(): IntArray {
        return if (config.useEquipmentSlots) intArrayOf(0, 1, 2, 3, 4) else intArrayOf(0)
    }

    private fun machineSlotTargets(): List<SlotTarget> {
        val targets = mutableListOf<SlotTarget>()
        if (config.useEquipmentSlots) {
            targets += SlotTarget(slots[0], equipmentSlotSpecs[0])
            targets += SlotTarget(slots[1], equipmentSlotSpecs[1])
            targets += SlotTarget(slots[2], equipmentSlotSpecs[2])
            targets += SlotTarget(slots[3], equipmentSlotSpecs[3])
            targets += SlotTarget(slots[4], chargeSlotSpec)
        } else {
            targets += SlotTarget(slots[0], chargeSlotSpec)
        }
        return targets
    }

    val playerInventorySlotStart: Int get() = machineSlotCount

    private val hotbarEnd: Int
        get() = playerInventorySlotStart + 35

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            block is ic2_120.content.block.storage.EnergyStorageBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): EnergyStorageScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val slotCount = buf.readVarInt()
            buf.readBoolean() // useEquipmentSlots, read but not stored (ScreenHandler gets config from context)
            val ctx = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(slotCount.coerceAtLeast(1))
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
