package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.screen.PatternStorageScreenHandler
import ic2_120.content.uu.UU_TEMPLATE_LIST_NBT_KEY
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.content.uu.decodeUuTemplateList
import ic2_120.content.uu.encodeUuTemplateList
import ic2_120.content.uu.getUuTemplate
import ic2_120.content.uu.setUuTemplate
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos

@ModBlockEntity(block = PatternStorageBlock::class)
class PatternStorageBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    companion object {
        const val SLOT_CRYSTAL = 0
        const val INVENTORY_SIZE = 1
        private const val NBT_SELECTED_INDEX = "SelectedTemplateIndex"
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_CRYSTAL), matcher = { it.isEmpty || isCrystalMemory(it) })
        ),
        extractSlots = intArrayOf(SLOT_CRYSTAL),
        markDirty = { markDirty() }
    )
    private val crystalMemoryId = Identifier(Ic2_120.MOD_ID, "crystal_memory")

    private val templates = mutableListOf<UuTemplateEntry>()

    var selectedTemplateIndex: Int = -1
        private set

    constructor(pos: BlockPos, state: BlockState) : this(
        PatternStorageBlockEntity::class.type(),
        pos,
        state
    )

    fun getTemplatesSnapshot(): List<UuTemplateEntry> = templates.toList()

    fun getSelectedTemplate(): UuTemplateEntry? =
        templates.getOrNull(selectedTemplateIndex)

    fun addOrSelectTemplate(entry: UuTemplateEntry): Boolean {
        val existingIndex = templates.indexOfFirst { it.itemId == entry.itemId }
        if (existingIndex >= 0) {
            templates[existingIndex] = entry
            selectedTemplateIndex = existingIndex
        } else {
            templates += entry
            selectedTemplateIndex = templates.lastIndex
        }
        markDirtyAndSync()
        return true
    }

    fun selectTemplate(index: Int): Boolean {
        if (index !in templates.indices) return false
        selectedTemplateIndex = index
        markDirtyAndSync()
        return true
    }

    fun importTemplateFromCrystal(): Boolean {
        val stack = getStack(SLOT_CRYSTAL)
        if (!isCrystalMemory(stack)) return false
        val template = stack.getUuTemplate() ?: return false
        return addOrSelectTemplate(template)
    }

    fun exportSelectedTemplateToCrystal(): Boolean {
        val stack = getStack(SLOT_CRYSTAL)
        if (!isCrystalMemory(stack)) return false
        val template = getSelectedTemplate() ?: return false
        stack.setUuTemplate(template)
        markDirtyAndSync()
        return true
    }

    fun getTemplateCount(): Int = templates.size

    private fun markDirtyAndSync() {
        if (templates.isEmpty()) {
            selectedTemplateIndex = -1
        } else {
            selectedTemplateIndex = selectedTemplateIndex.coerceIn(0, templates.lastIndex)
        }
        markDirty()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
        }
    }

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_CRYSTAL -> stack.isEmpty || isCrystalMemory(stack)
        else -> false
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = if (slot == SLOT_CRYSTAL && !isCrystalMemory(stack)) ItemStack.EMPTY else stack
        if (inventory[slot].count > maxCountPerStack) inventory[slot].count = maxCountPerStack
        markDirtyAndSync()
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(0)
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.pattern_storage")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        PatternStorageScreenHandler(syncId, playerInventory, this, pos, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos))

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        templates.clear()
        templates += decodeUuTemplateList(nbt.getList(UU_TEMPLATE_LIST_NBT_KEY, 10))
        selectedTemplateIndex = nbt.getInt(NBT_SELECTED_INDEX)
        if (templates.isEmpty()) selectedTemplateIndex = -1 else {
            selectedTemplateIndex = selectedTemplateIndex.coerceIn(0, templates.lastIndex)
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        nbt.put(UU_TEMPLATE_LIST_NBT_KEY, encodeUuTemplateList(templates))
        nbt.putInt(NBT_SELECTED_INDEX, selectedTemplateIndex)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    private fun isCrystalMemory(stack: ItemStack): Boolean =
        !stack.isEmpty && Registries.ITEM.getId(stack.item) == crystalMemoryId
}
