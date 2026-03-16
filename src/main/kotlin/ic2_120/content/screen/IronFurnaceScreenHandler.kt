package ic2_120.content.screen

import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.content.sync.IronFurnaceSync
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
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
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.recipe.RecipeType

@ModScreenHandler(block = IronFurnaceBlock::class)
class IronFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(IronFurnaceScreenHandler::class.type(), syncId) {

    val sync = IronFurnaceSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, 3)
        addProperties(propertyDelegate)

        // 机器槽位
        addSlot(PredicateSlot(blockInventory, IronFurnaceBlockEntity.SLOT_INPUT, INPUT_SLOT_X, INPUT_SLOT_Y, INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, IronFurnaceBlockEntity.SLOT_FUEL, FUEL_SLOT_X, FUEL_SLOT_Y, FUEL_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, IronFurnaceBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y, OUTPUT_SLOT_SPEC))

        // 玩家物品栏
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
            when {
                // 输出槽 -> 玩家物品栏
                index == IronFurnaceBlockEntity.SLOT_OUTPUT -> {
                    if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 燃料槽 -> 玩家物品栏
                index == IronFurnaceBlockEntity.SLOT_FUEL -> {
                    if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                }
                // 输入槽 -> 玩家物品栏
                index == IronFurnaceBlockEntity.SLOT_INPUT -> {
                    if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                }
                // 玩家物品栏 -> 机器槽位
                index in 3..38 -> {
                    // 优先输出槽，然后输入槽，最后燃料槽
                    val movedToOutput = insertItem(stackInSlot, IronFurnaceBlockEntity.SLOT_OUTPUT, IronFurnaceBlockEntity.SLOT_OUTPUT + 1, false)
                    if (!movedToOutput) {
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[IronFurnaceBlockEntity.SLOT_INPUT], INPUT_SLOT_SPEC),
                                SlotTarget(slots[IronFurnaceBlockEntity.SLOT_FUEL], FUEL_SLOT_SPEC)
                            )
                        )
                        if (!moved) {
                            return ItemStack.EMPTY
                        }
                    }
                }
                else -> if (!insertItem(stackInSlot, 3, 39, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is IronFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val INPUT_SLOT_X = 56
        const val INPUT_SLOT_Y = 17
        const val FUEL_SLOT_X = 56
        const val FUEL_SLOT_Y = 53
        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y = 35
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_SIZE = 18

        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                // 输入槽：检查是否有烧制配方
                // 注意：这里在客户端无法检查，简单允许所有物品
                // 实际的验证会在服务器端的 canPlaceInSlot 中进行
                !stack.isEmpty
            }
        )

        private val FUEL_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                !stack.isEmpty && ((FuelRegistry.INSTANCE.get(stack.item) ?: 0) > 0 ||
                    stack.item == net.minecraft.item.Items.LAVA_BUCKET)
            }
        )

        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> false }  // 输出槽不能手动插入
        )

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): IronFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(3)
            return IronFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
