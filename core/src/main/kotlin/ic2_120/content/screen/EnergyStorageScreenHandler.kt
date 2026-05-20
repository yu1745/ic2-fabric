package ic2_120.content.screen

import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.ModScreenHandler
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
    propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(screenHandlerType, syncId) {
    private val config = resolveConfig()
    private val machineSlotCount: Int = config.slotCount
    val sync = EnergyStorageSync(
        schema = SyncedDataView(propertyDelegate),
        getFacing = { Direction.NORTH },
        tier = config.tier,
        capacity = config.capacity
    )

    val useEquipmentSlots: Boolean get() = config.useEquipmentSlots

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun resolveConfig(): EnergyStorageConfig {
        return context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path) ?: EnergyStorageConfig.BATBOX
        }, EnergyStorageConfig.BATBOX)
    }

    init {
        checkSize(blockInventory, machineSlotCount)
        addProperties(propertyDelegate)

        // 1 个充电槽，所有等级都有（blockInventory slot 0）
        addTrackedSlot(blockInventory, 0, 56, 17)

        // 1 个供能槽，所有等级都有（blockInventory 最后一个 slot）
        addTrackedSlot(blockInventory, machineSlotCount - 1, 56, 53)

        // 4 个玩家装备槽（横列，1px 间距），MFE/MFSU 专用
        if (config.useEquipmentSlots) {
            addSlot(Slot(playerInventory, 39, 8, 84))   // 头盔
            addSlot(Slot(playerInventory, 38, 26, 84))  // 胸甲
            addSlot(Slot(playerInventory, 37, 44, 84))  // 护腿
            addSlot(Slot(playerInventory, 36, 62, 84))  // 靴子
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 172))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY

        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                useEquipmentSlots && index in machineSlotCount until machineSlotCount + 4 -> {
                    if (!insertItem(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, true)) return ItemStack.EMPTY
                }
                index in playerInventorySlotStart..hotbarEnd -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    if (!insertItem(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, false)) return ItemStack.EMPTY
                }
            }

            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    val playerInventorySlotStart: Int get() = machineSlotCount + (if (config.useEquipmentSlots) 4 else 0)

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
