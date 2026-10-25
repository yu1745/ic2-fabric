package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlockEntity
import ic2_120_advanced_solar_addon.content.sync.QuantumGeneratorSync
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.core.Direction
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.type
import ic2_120.compat.*

@ModScreenHandler(block = ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlock::class)
class QuantumGeneratorScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData
) : AbstractContainerMenu(QuantumGeneratorScreenHandler::class.type(), syncId) {

    val sync = QuantumGeneratorSync(
        schema = SyncedDataView(propertyDelegate),
        tier = QuantumGeneratorBlockEntity.DEFAULT_VARIABLE,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        addDataSlots(propertyDelegate)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9,
                    PLAYER_INV_X + col * SLOT_SPACING,
                    PLAYER_INV_Y + row * SLOT_SPACING))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col,
                HOTBAR_X + col * SLOT_SPACING,
                HOTBAR_Y))
        }
    }

    /**
     * 主背包↔快捷栏互搬：源区间与目标区间互斥（主背包 [0,27) ↔ 快捷栏 [27,36)），
     * 源槽永不落入 insertItem 目标区间，无自合并风险（与 Tank/Scanner 等同构实现一致）。
     * 本 GUI 无机器槽位，不需要 no-op。
     */
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.item
            stack = stackInSlot.copy()
            when {
                index in PLAYER_INV_START until HOTBAR_START -> {
                    if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
                index in HOTBAR_START until HOTBAR_END -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_START, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY)
            else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player.level().isClientSide) return true
        return context.evaluate({ world, pos ->
            val be = world.getBlockEntity(pos) as? QuantumGeneratorBlockEntity ?: return@evaluate false
            when (id) {
            BTN_EM_MINUS_100 -> be.addEnergyMac(-100)
            BTN_EM_MINUS_10  -> be.addEnergyMac(-10)
            BTN_EM_MINUS_1   -> be.addEnergyMac(-1)
            BTN_EM_PLUS_1    -> be.addEnergyMac(1)
            BTN_EM_PLUS_10   -> be.addEnergyMac(10)
            BTN_EM_PLUS_100  -> be.addEnergyMac(100)
            BTN_VAR_1   -> be.applyVariable(1)
            BTN_VAR_2   -> be.applyVariable(2)
            BTN_VAR_3   -> be.applyVariable(3)
            BTN_VAR_4   -> be.applyVariable(4)
            BTN_VAR_5   -> be.applyVariable(5)
            BTN_VAR_6   -> be.applyVariable(6)
            BTN_VAR_MAX -> be.applyVariable(8)
                else -> return@evaluate false
            }
            true
        }, false)
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 16
        const val SLOT_SPACING = 18 // 16 + 2px gap
        const val PLAYER_INV_START = 0
        const val HOTBAR_START = 27
        const val HOTBAR_END = 36

        private const val PLAYER_INV_X = 8
        private const val PLAYER_INV_Y = 110
        private const val HOTBAR_X = 8
        private const val HOTBAR_Y = 168

        const val BTN_EM_MINUS_100 = 0
        const val BTN_EM_MINUS_10  = 1
        const val BTN_EM_MINUS_1   = 2
        const val BTN_EM_PLUS_1    = 3
        const val BTN_EM_PLUS_10   = 4
        const val BTN_EM_PLUS_100  = 5
        const val BTN_VAR_1   = 10
        const val BTN_VAR_2   = 11
        const val BTN_VAR_3   = 12
        const val BTN_VAR_4   = 13
        const val BTN_VAR_5   = 14
        const val BTN_VAR_6   = 15
        const val BTN_VAR_MAX = 16

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): QuantumGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ContainerLevelAccess.create(playerInventory.player.level(), pos)
            val propertyDelegate = net.minecraft.world.inventory.SimpleContainerData(propertyCount)
            return QuantumGeneratorScreenHandler(syncId, playerInventory, context, propertyDelegate)
        }
    }
}
