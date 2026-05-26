package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlockEntity
import ic2_120_advanced_solar_addon.content.sync.QuantumGeneratorSync
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.Direction
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.type

@ModScreenHandler(block = ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlock::class)
class QuantumGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(QuantumGeneratorScreenHandler::class.type(), syncId) {

    val sync = QuantumGeneratorSync(
        schema = SyncedDataView(propertyDelegate),
        tier = QuantumGeneratorBlockEntity.DEFAULT_VARIABLE,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        addProperties(propertyDelegate)

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

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index in PLAYER_INV_START until HOTBAR_START -> {
                    if (!insertItem(stackInSlot, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
                index in HOTBAR_START until HOTBAR_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_START, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (player.world.isClient) return true
        // 使用双参数版本 get(BiFunction, defaultValue)，返回 T? 而不是 Optional<T>
        val be = context.get({ world, pos -> world.getBlockEntity(pos) }, null)
            as? QuantumGeneratorBlockEntity ?: return false

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
            else -> return false
        }
        return true
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
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
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): QuantumGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val propertyDelegate = net.minecraft.screen.ArrayPropertyDelegate(propertyCount)
            return QuantumGeneratorScreenHandler(syncId, playerInventory, context, propertyDelegate)
        }
    }
}
