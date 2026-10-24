package ic2_120.content.screen

import ic2_120.content.block.SteamGeneratorBlock
import ic2_120.content.block.machines.SteamGeneratorBlockEntity
import ic2_120.content.sync.SteamGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext

/**
 * 蒸汽发生器 ScreenHandler — 对齐 ic2_origin ContainerSteamGenerator。
 * 无物品槽位，仅同步数据 + 按钮事件。
 */
@ModScreenHandler(block = SteamGeneratorBlock::class, inventorySize = SteamGeneratorBlockEntity.INVENTORY_SIZE)
class SteamGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SteamGeneratorScreenHandler::class.type(), syncId) {

    val sync = SteamGeneratorSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, 0)
        addProperties(propertyDelegate)
        // 无槽位 — 对齐 ic2_origin ContainerSteamGenerator
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is SteamGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? SteamGeneratorBlockEntity ?: return@get
            val event = BUTTON_EVENTS.getOrNull(id) ?: return@get
            be.onNetworkEvent(event)
            be.markDirty()
        }, false)
        return true
    }

    companion object {
        /**
         * 按钮事件值 — 对齐 ic2_origin onNetworkEvent。
         * [-2000,2000] → inputMB, 超出 → pressure
         */
        val BUTTON_EVENTS = intArrayOf(
            -1000, -100, -10, -1,  // inputMB dec (IDs 0-3)
            +1, +10, +100, +1000,  // inputMB inc (IDs 4-7)
            -2100, -2010, -2001,   // pressure dec (IDs 8-10)
            +2001, +2010, +2100    // pressure inc (IDs 11-13)
        )

    }
}
