package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.CropSeedData
import ic2_120.content.item.CropnalyzerItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.InteractionHand

@ModScreenHandler(name = "cropnalyzer", mode = ScreenHandlerMode.HANDHELD)
class CropnalyzerScreenHandler(
    syncId: Int,
    private val playerInventory: Inventory,
    private val hand: InteractionHand,
    private val itemInventory: Container = SimpleContainer(2),
    private val propertyDelegate: ContainerData = SimpleContainerData(PROPERTY_COUNT)
) : AbstractContainerMenu(CropnalyzerScreenHandler::class.type(), syncId) {

    private val seedInputSpec = SlotSpec(
        canInsert = { it.item is CropSeedBagItem },
        maxItemCount = 1
    )

    private val seedOutputSpec = SlotSpec(
        canInsert = { false },
        canTake = { true },
        maxItemCount = 1
    )

    val energy: Int get() = propertyDelegate.get(0).coerceAtLeast(0)
    val energyCapacity: Int get() = propertyDelegate.get(1).coerceAtLeast(1)

    init {
        addDataSlots(propertyDelegate)

        addSlot(PredicateSlot(itemInventory, SLOT_INPUT, 8, 7, seedInputSpec))
        addSlot(PredicateSlot(itemInventory, SLOT_OUTPUT, 41, 7, seedOutputSpec))

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

   override fun clickMenuButton(player: Player, id: Int): Boolean {
       if (id != BUTTON_ID_SCAN) return false

       val scanner = getScannerStack(player) ?: return true
       val seed = itemInventory.getStack(SLOT_INPUT)
       if (seed.item !is CropSeedBagItem) {
            player.sendMessage(net.minecraft.network.chat.Component.translatable("gui.ic2_120.cropnalyzer.insert_seed_bag").withStyle(net.minecraft.ChatFormatting.RED), true)
            return true
        }

        val type = CropSeedData.readType(seed)
        if (type == null) {
            player.sendMessage(net.minecraft.network.chat.Component.translatable("gui.ic2_120.cropnalyzer.no_valid_crop_data").withStyle(net.minecraft.ChatFormatting.RED), true)
            return true
        }

        if (!itemInventory.getStack(SLOT_OUTPUT).isEmpty) {
            player.sendMessage(net.minecraft.network.chat.Component.translatable("gui.ic2_120.cropnalyzer.output_occupied").withStyle(net.minecraft.ChatFormatting.RED), true)
            return true
        }

       val tool = scanner.item as? IElectricTool ?: return true
       val currentEnergy = tool.getEnergy(scanner)
       val currentLevel = CropSeedData.readScanLevel(seed)
      val scanCost = CropnalyzerItem.energyForScanLevel(currentLevel)
       if (scanCost <= 0L) {
            player.sendMessage(net.minecraft.network.chat.Component.translatable("gui.ic2_120.cropnalyzer.no_valid_crop_data").withStyle(net.minecraft.ChatFormatting.RED), true)
            return true
        }
        if (currentEnergy < scanCost) {
            player.sendMessage(net.minecraft.network.chat.Component.translatable("gui.ic2_120.status_no_energy").withStyle(net.minecraft.ChatFormatting.RED), true)
            return true
        }

       tool.setEnergy(scanner, currentEnergy - scanCost)

       val stats = CropSeedData.readStats(seed)
       CropSeedData.write(seed, type, stats, currentLevel + 1)
       itemInventory.setStack(SLOT_OUTPUT, seed.copy())
       itemInventory.setStack(SLOT_INPUT, ItemStack.EMPTY)
       itemInventory.setChanged()

       refreshEnergyState()
        sendContentUpdates()
        player.sendMessage(CropnalyzerItem.buildResultMessage(type, stats, currentLevel + 1), false)
        return true
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var moved = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot .hasItem()) return ItemStack.EMPTY

        val stack = slot.stack
        moved = stack.copy()
        if (index == SLOT_INDEX_INPUT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
        } else if (index == SLOT_INDEX_OUTPUT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
        } else if (index in PLAYER_INV_START until HOTBAR_END) {
            if (stack.item is CropSeedBagItem) {
                if (!moveItemStackTo(stack, SLOT_INDEX_INPUT, SLOT_INDEX_INPUT + 1, false)) return ItemStack.EMPTY
            } else {
                return ItemStack.EMPTY
            }
        } else {
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.stack = ItemStack.EMPTY else slot.setChanged()
        slot.onTake(player, stack)
        return moved
    }

    override fun stillValid(player: Player): Boolean {
        val scanner = getScannerStack(player) ?: return false
        return scanner.item is CropnalyzerItem
    }

    override fun onClosed(player: Player) {
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

    private fun getScannerStack(player: Player): ItemStack? {
        val stack = player.getItemInHand(hand)
        return stack.takeIf { it.item is CropnalyzerItem }
    }

    companion object {
        const val BUTTON_ID_SCAN = 0

        private const val SLOT_INPUT = 0
        private const val SLOT_OUTPUT = 1
        const val SLOT_INDEX_INPUT = 0
        const val SLOT_INDEX_OUTPUT = 1
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 38
        private const val PROPERTY_COUNT = 2

    }
}
