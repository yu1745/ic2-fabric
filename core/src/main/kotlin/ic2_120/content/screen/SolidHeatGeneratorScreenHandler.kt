package ic2_120.content.screen

import ic2_120.content.block.SolidHeatGeneratorBlock
import ic2_120.content.block.machines.SolidHeatGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.sync.SolidHeatGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = SolidHeatGeneratorBlock::class, inventorySize = SolidHeatGeneratorBlockEntity.INVENTORY_SIZE)
class SolidHeatGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SolidHeatGeneratorScreenHandler::class.type(), syncId) {

    private val syncedView = SyncedDataView(propertyDelegate)
    private val heatFlow = HeatFlowSync(
        syncedView,
        object : HeatFlowSync.HeatProducer {
            override fun getLastGeneratedHeat(): Long = 0L
            override fun getLastOutputHeat(): Long = 0L
        }
    )
    val sync = SolidHeatGeneratorSync(syncedView, heatFlow)

    init {
        checkSize(blockInventory, SolidHeatGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        addSlot(PredicateSlot(blockInventory, SolidHeatGeneratorBlockEntity.SLOT_FUEL, 80, 44, FUEL_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SolidHeatGeneratorBlockEntity.SLOT_OUTPUT, 113, 44, OUTPUT_SLOT_SPEC))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 83 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 141))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SolidHeatGeneratorBlockEntity.SLOT_FUEL ||
                index == SolidHeatGeneratorBlockEntity.SLOT_OUTPUT -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    if (FUEL_SLOT_SPEC.canInsert(stackInSlot) && insertItem(stackInSlot, 0, 1, false)) return stack
                    // 原兜底：向玩家背包区间 insertItem 已删除——目标区间含源槽自身，
                    // 会与自身合并导致数量翻倍（物品复制，同 ChunkLoader bug）。
                    // 燃料槽插不进时物品留在原位，下方 count 相等检查会返回 EMPTY。
                }
                else -> {
                    // 守卫：仅机器槽可落此（玩家槽已被区间检查全覆盖）。若未来条件收窄使玩家槽落入，
                    // 直接拒绝而非向含源槽的玩家区间 insertItem（同实例自合并会复制物品）。
                    if (index >= PLAYER_INV_START) return ItemStack.EMPTY
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is SolidHeatGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 38

        private val FUEL_SLOT_SPEC = SlotSpec(canInsert = { stack ->
            !stack.isEmpty && (FuelRegistry.INSTANCE.get(stack.item) ?: 0) > 0
        })
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false })

    }
}
