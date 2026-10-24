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
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.RegisterItemStorage
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos

@ModBlockEntity(block = PatternStorageBlock::class)
class PatternStorageBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Container, IRoutedSidedInventory, ExtendedScreenHandlerFactory {

    companion object {
        const val SLOT_CRYSTAL = 0
        const val INVENTORY_SIZE = 1
        private const val NBT_SELECTED_INDEX = "SelectedTemplateIndex"
    }

    private val inventory = NonNullList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
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

    override val routedItemStorage get() = itemStorage
    private val crystalMemoryId = ResourceLocation(Ic2_120.MOD_ID, "crystal_memory")

    private val templates = mutableListOf<UuTemplateEntry>()

    var selectedTemplateIndex: Int = -1
        private set

    constructor(pos: BlockPos, state: BlockState) : this(
        PatternStorageBlockEntity::class.type(),
        pos,
        state
    )

    fun getTemplatesSnapshot(): List<UuTemplateEntry> = templates.toList()

    /**
     * 当前识别的复制配方（旁边复制机实际使用的配方）：
     * 若水晶槽中有带配方的模式存储水晶，则直接使用水晶中的配方——不写入模板列表、不改变选中项，
     * 仅供自动化物流模组通过更换水晶来即时控制复制机的配方；水晶被取走或换成空水晶后，
     * 自动回落到模板列表中选中的配方。
     */
    fun getSelectedTemplate(): UuTemplateEntry? {
        val crystal = getStack(SLOT_CRYSTAL)
        if (isCrystalMemory(crystal)) {
            crystal.getUuTemplate()?.let { return it }
        }
        return templates.getOrNull(selectedTemplateIndex)
    }

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
        // 写入按钮语义不变：把模板列表中选中的配方写入水晶（不受水晶优先规则影响）
        val template = templates.getOrNull(selectedTemplateIndex) ?: return false
        stack.setUuTemplate(template)
        markDirtyAndSync()
        return true
    }

    fun removeSelectedTemplate() {
        val index = selectedTemplateIndex
        if (index !in templates.indices) return
        templates.removeAt(index)
        markDirtyAndSync()
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
            (world as? ServerLevel)?.chunkManager?.markForUpdate(pos)
        }
    }

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = ContainerHelper.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = ContainerHelper.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: Player): Boolean = Container.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_CRYSTAL -> stack.isEmpty || isCrystalMemory(stack)
        else -> false
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = if (slot == SLOT_CRYSTAL && !isCrystalMemory(stack)) ItemStack.EMPTY else stack
        if (inventory[slot].count > maxCountPerStack) inventory[slot].count = maxCountPerStack
        markDirtyAndSync()
    }

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(0)
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.pattern_storage")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        PatternStorageScreenHandler(syncId, playerInventory, this, pos, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), itemStorage)

    override fun readNbt(nbt: CompoundTag) {
        super.readNbt(nbt)
        ContainerHelper.readNbt(nbt, inventory)
        templates.clear()
        templates += decodeUuTemplateList(nbt.getList(UU_TEMPLATE_LIST_NBT_KEY, 10))
        selectedTemplateIndex = nbt.getInt(NBT_SELECTED_INDEX)
        if (templates.isEmpty()) selectedTemplateIndex = -1 else {
            selectedTemplateIndex = selectedTemplateIndex.coerceIn(0, templates.lastIndex)
        }
    }

    override fun writeNbt(nbt: CompoundTag) {
        super.writeNbt(nbt)
        ContainerHelper.writeNbt(nbt, inventory)
        nbt.put(UU_TEMPLATE_LIST_NBT_KEY, encodeUuTemplateList(templates))
        nbt.putInt(NBT_SELECTED_INDEX, selectedTemplateIndex)
    }

    override fun toInitialChunkDataNbt(): CompoundTag = createNbt()

    override fun toUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    private fun isCrystalMemory(stack: ItemStack): Boolean =
        !stack.isEmpty && BuiltInRegistries.ITEM.getId(stack.item) == crystalMemoryId
}
