package ic2_120.content.screen

import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.CropSeedData
import ic2_120.content.item.CropnalyzerItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Hand

@ModScreenHandler(name = "cropnalyzer")
class CropnalyzerScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val hand: Hand,
    private val itemInventory: Inventory = SimpleInventory(1),
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(CropnalyzerScreenHandler::class.type(), syncId) {

    private val seedSlotSpec = SlotSpec(
        canInsert = { it.item is CropSeedBagItem },
        maxItemCount = 1
    )

    val energy: Int get() = propertyDelegate.get(0).coerceAtLeast(0)
    val energyCapacity: Int get() = propertyDelegate.get(1).coerceAtLeast(1)

    init {
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(itemInventory, SLOT_SEED, 0, 0, seedSlotSpec))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }

        refreshEnergyState()
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_ID_SCAN) return false

        val scanner = getScannerStack(player) ?: return true
        val seed = itemInventory.getStack(SLOT_SEED)
        if (seed.item !is CropSeedBagItem) {
            player.sendMessage(net.minecraft.text.Text.translatable("gui.ic2_120.cropnalyzer.insert_seed_bag").formatted(net.minecraft.util.Formatting.RED), true)
            return true
        }

        val type = CropSeedData.readType(seed)
        if (type == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("gui.ic2_120.cropnalyzer.no_valid_crop_data").formatted(net.minecraft.util.Formatting.RED), true)
            return true
        }

        val tool = scanner.item as? IElectricTool ?: return true
        val currentEnergy = tool.getEnergy(scanner)
        if (currentEnergy < CropnalyzerItem.ENERGY_PER_SCAN) {
            player.sendMessage(net.minecraft.text.Text.translatable("gui.ic2_120.status_no_energy").formatted(net.minecraft.util.Formatting.RED), true)
            return true
        }

        tool.setEnergy(scanner, currentEnergy - CropnalyzerItem.ENERGY_PER_SCAN)

        val oldLevel = CropSeedData.readScanLevel(seed)
        val newLevel = (oldLevel + 1).coerceAtMost(4)
        val stats = CropSeedData.readStats(seed)
        CropSeedData.write(seed, type, stats, newLevel)
        itemInventory.markDirty()

        refreshEnergyState()
        sendContentUpdates()
        player.sendMessage(CropnalyzerItem.buildResultMessage(type, stats, newLevel), false)
        return true
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var moved = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return ItemStack.EMPTY

        val stack = slot.stack
        moved = stack.copy()
        if (index == SLOT_INDEX_SEED) {
            if (!insertItem(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
        } else if (index in PLAYER_INV_START until HOTBAR_END) {
            if (stack.item is CropSeedBagItem) {
                if (!insertItem(stack, SLOT_INDEX_SEED, SLOT_INDEX_SEED + 1, false)) return ItemStack.EMPTY
            } else {
                return ItemStack.EMPTY
            }
        } else {
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        slot.onTakeItem(player, stack)
        return moved
    }

    override fun canUse(player: PlayerEntity): Boolean {
        val scanner = getScannerStack(player) ?: return false
        return scanner.item is CropnalyzerItem
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        dropInventory(player, itemInventory)
    }

    private fun refreshEnergyState() {
        val scanner = getScannerStack(playerInventory.player)
        val tool = scanner?.item as? IElectricTool
        val energyValue = tool?.getEnergy(scanner)?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0
        val capValue = tool?.getMaxEnergy()?.coerceIn(1L, Int.MAX_VALUE.toLong())?.toInt()
            ?: 10_000
        propertyDelegate.set(0, energyValue)
        propertyDelegate.set(1, capValue)
    }

    private fun getScannerStack(player: PlayerEntity): ItemStack? {
        val stack = player.getStackInHand(hand)
        return stack.takeIf { it.item is CropnalyzerItem }
    }

    companion object {
        const val BUTTON_ID_SCAN = 0

        private const val SLOT_SEED = 0
        const val SLOT_INDEX_SEED = 0
        const val PLAYER_INV_START = 1
        const val HOTBAR_END = 37
        private const val PROPERTY_COUNT = 2

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CropnalyzerScreenHandler {
            val hand = buf.readEnumConstant(Hand::class.java)
            return CropnalyzerScreenHandler(syncId, playerInventory, hand)
        }
    }
}
