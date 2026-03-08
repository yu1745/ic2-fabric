package ic2_120.content.block.storage

import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.registry.BlockEntityTypeStore
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler

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
 *
 * 注意：此 BlockEntity 不使用 @ModBlockEntity 注解，需要通过 [register] 方法手动注册。
 */
class StorageBoxBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(STORAGE_BOX_TYPE, pos, state), Inventory, ExtendedScreenHandlerFactory {

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

        /** BlockEntityType 实例，由 register 方法初始化 */
        private lateinit var STORAGE_BOX_TYPE: BlockEntityType<StorageBoxBlockEntity>

        /**
         * 手动注册储物箱 BlockEntityType
         *
         * 必须在所有储物箱方块注册完成后调用（一个 BE 类型关联多个方块）。
         */
        fun register(modId: String) {
            val blockIds = listOf(
                Identifier(modId, "wooden_storage_box"),
                Identifier(modId, "iron_storage_box"),
                Identifier(modId, "bronze_storage_box"),
                Identifier(modId, "steel_storage_box"),
                Identifier(modId, "iridium_storage_box")
            )

            val blocks = blockIds.mapNotNull { id ->
                Registries.BLOCK.getOrEmpty(id).orElse(null)
            }

            require(blocks.size == blockIds.size) {
                "注册储物箱 BlockEntity 失败：找不到部分储物箱方块。需要: ${blockIds.joinToString()}"
            }

            val factory = FabricBlockEntityTypeBuilder.Factory { pos: BlockPos, state: BlockState ->
                StorageBoxBlockEntity(pos, state)
            }

            @Suppress("UNCHECKED_CAST")
            val type = FabricBlockEntityTypeBuilder.create(factory, *blocks.toTypedArray())
                .build() as BlockEntityType<StorageBoxBlockEntity>

            STORAGE_BOX_TYPE = type

            val id = Identifier(modId, "storage_box")
            Registry.register(Registries.BLOCK_ENTITY_TYPE, id, type)
            BlockEntityTypeStore.registerType(StorageBoxBlockEntity::class, type)

            // 为了兼容 getType 方法，也注册到 ModBlockEntities
            ic2_120.content.ModBlockEntities.getType(StorageBoxBlockEntity::class)
        }
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

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        // 保存物品栏到 NBT
        val inventoryNbt = NbtCompound()
        Inventories.writeNbt(inventoryNbt, inventory)
        nbt.put(INVENTORY_KEY, inventoryNbt)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
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
