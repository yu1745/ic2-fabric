package ic2_120.content.block.storage

import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.player.PlayerEntity

import net.minecraft.screen.ScreenHandler
import net.minecraft.registry.RegistryWrapper

/**
 * 储物箱 BlockEntity
 *
 * 类似潜影盒，破坏时保留物品。
 * 不同材质的储物箱容量不同：
 * - 木质储物箱: 27 格
 * - 铁质储物箱: 45 格
 * - 青铜储物箱: 45 格
 * - 钢制储物箱: 63 格
 * - 铱储物箱: 126 格
 */
@ModBlockEntity(
    name = "storage_box",
    blocks = [
        WoodenStorageBoxBlock::class,
        IronStorageBoxBlock::class,
        BronzeStorageBoxBlock::class,
        SteelStorageBoxBlock::class,
        IridiumStorageBoxBlock::class
    ]
)
class StorageBoxBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(StorageBoxBlockEntity::class.type(), pos, state), Inventory, ExtendedScreenHandlerFactory {

    companion object {
        /** 木质储物箱容量 */
        private const val WOODEN_CAPACITY = 27

        /** 青铜储物箱容量 */
        private const val BRONZE_CAPACITY = 45

        /** 铁质储物箱容量 */
        private const val IRON_CAPACITY = 45

        /** 钢制储物箱容量 */
        private const val STEEL_CAPACITY = 63

        /** 铱储物箱容量 */
        private const val IRIDIUM_CAPACITY = 126

        /** NBT 键 */
        private const val INVENTORY_KEY = "Inventory"
        private const val ITEMS_KEY = "Items"
    }

    /** 物品栏内容 */
    private var inventory: DefaultedList<ItemStack> = createInventory()

    /** 根据方块类型获取对应容量 */
    private fun getCapacity(): Int {
        val blockId = cachedState.block.toString()
        return when {
            blockId.contains("wooden_storage_box") -> WOODEN_CAPACITY
            blockId.contains("iron_storage_box") -> IRON_CAPACITY
            blockId.contains("bronze_storage_box") -> BRONZE_CAPACITY
            blockId.contains("steel_storage_box") -> STEEL_CAPACITY
            blockId.contains("iridium_storage_box") -> IRIDIUM_CAPACITY
            else -> WOODEN_CAPACITY
        }
    }

    /** 创建物品栏 */
    private fun createInventory(): DefaultedList<ItemStack> {
        return DefaultedList.ofSize(getCapacity(), ItemStack.EMPTY)
    }

    /** 获取物品栏（只读） - 外部访问用 */
    fun getInventory(): DefaultedList<ItemStack> = inventory

    /** 获取物品栏大小 */
    override fun size(): Int = inventory.size

    /** 判断物品栏是否为空 */
    override fun isEmpty(): Boolean {
        for (stack in inventory) {
            if (!stack.isEmpty) return false
        }
        return true
    }

    /** 获取指定槽位的物品 */
    override fun getStack(slot: Int): ItemStack {
        return if (slot >= 0 && slot < inventory.size) {
            inventory[slot]
        } else {
            ItemStack.EMPTY
        }
    }

    /** 从物品栏移除物品（玩家取物品） */
    override fun removeStack(slot: Int, amount: Int): ItemStack {
        if (slot >= 0 && slot < inventory.size) {
            val stack = inventory[slot]
            val removed = stack.split(amount)
            if (stack.isEmpty) {
                inventory[slot] = ItemStack.EMPTY
            }
            markDirty()
            return removed
        }
        return ItemStack.EMPTY
    }

    /** 从物品栏移除整个槽位的物品 */
    override fun removeStack(slot: Int): ItemStack {
        if (slot >= 0 && slot < inventory.size) {
            val stack = inventory[slot]
            inventory[slot] = ItemStack.EMPTY
            markDirty()
            return stack
        }
        return ItemStack.EMPTY
    }

    /** 设置指定槽位的物品 */
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot >= 0 && slot < inventory.size) {
            inventory[slot] = stack
            markDirty()
        }
    }

    /** 标记物品栏已更改 */
    override fun markDirty() {
        super.markDirty()
    }

    /** 检查玩家是否可以使用此物品栏 */
    override fun canPlayerUse(player: PlayerEntity): Boolean {
        return if (world == null || pos == null) false
        else player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
    }

    /** 清空物品栏 */
    override fun clear() {
        inventory.clear()
        markDirty()
    }

    // ========== ExtendedScreenHandlerFactory 实现 ==========

    /** 创建菜单标题 */
    override fun getDisplayName(): Text {
        val block = cachedState.block
        return Text.translatable(block.translationKey)
    }

    /** 创建 ScreenHandler */
    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return StorageBoxScreenHandler.create(syncId, playerInventory, this)
    }

    /** 写入数据到 PacketByteBuf（客户端 GUI 打开时使用） */
    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        // 保存物品栏到 NBT
        val inventoryNbt = NbtCompound()
        Inventories.writeNbt(inventoryNbt, inventory)
        nbt.put(INVENTORY_KEY, inventoryNbt)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        // 从 NBT 读取物品栏
        val inventoryNbt = nbt.getCompound(INVENTORY_KEY)
        if (inventoryNbt.size == 0) {
            // 兼容旧格式（直接存储在 Items 键）
            val itemsNbt = nbt.getList(ITEMS_KEY, 10)
            if (itemsNbt.size != 0) {
                inventory = createInventory()
                Inventories.readNbt(inventoryNbt, inventory)
            }
        } else {
            inventory = createInventory()
            Inventories.readNbt(inventoryNbt, inventory)
        }
    }
}
