package ic2_120.content.block.storage

import ic2_120.compat.orCreateNbt
import ic2_120.compat.world
import ic2_120.compat.ContainerHelper26

import ic2_120.compat.LegacyBlockEntity26

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import ic2_120.compat.cachedState
import ic2_120.compat.pos

import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.network.FriendlyByteBuf
import io.netty.buffer.Unpooled

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
) : LegacyBlockEntity26(StorageBoxBlockEntity::class.type(), pos, state), Container, ExtendedMenuProvider<FriendlyByteBuf> {

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
        private const val INVENTORY_KEY = "Container"
        private const val ITEMS_KEY = "Items"
    }

    /** 物品栏内容 */
    private var inventory: NonNullList<ItemStack> = createInventory()

    /**
     * 对外暴露的 Fabric ItemStorage。
     *
     * 仅用 `implements Container` 时，物品栏会被 FFFAPI（ForgifiedFabricAPI）的
     * `ItemStorage.SIDED` fallback（vanilla Container 兜底）处理；但该 fallback 在
     * `TransferApiForgeCompat` 的 capability 桥（`getCapability(ITEM_HANDLER)` 反查
     * `ItemStorage.SIDED.find()`）置 `COMPUTING_CAPABILITY_LOCK` 时会被显式跳过，导致
     * AE2 总线 / Create 溜槽等走 Forge 能力的物流拿不到储物箱能力。
     *
     * 改为走和其他机器（RoutedItemStorage + @RegisterItemStorage）相同的显式
     * `registerForBlockEntity` 路径后，能力分发不经过上述 fallback，故不受该锁影响，
     * 所有走能力 / 走 Fabric Storage 的物流（AE2、溜槽、管道）都能读写储物箱。
     * `direction = null` 表示无视侧面、全向暴露（与储物箱无方向语义一致）。
     */
    @RegisterItemStorage
    val itemStorage: Storage<ItemVariant> = ContainerStorage.of(this, null)

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
    private fun createInventory(): NonNullList<ItemStack> {
        return NonNullList.withSize(getCapacity(), ItemStack.EMPTY)
    }

    /** 获取物品栏（只读） - 外部访问用 */
    fun getInventory(): NonNullList<ItemStack> = inventory

    /** 创造模式破坏时由方块临时关闭掉落。 */
    var shouldDropOnBreak = true

    override fun preRemoveSideEffects(pos: BlockPos, state: BlockState) {
        // Keep contents in one packed block item, as a shulker box does.
        val level = world as? net.minecraft.server.level.ServerLevel ?: return
        if (!shouldDropOnBreak) return
        val stack = ItemStack(state.block.asItem())
        val saved = saveWithFullMetadata(level.registryAccess())
        if (!saved.isEmpty) {
            stack.orCreateNbt = stack.orCreateNbt.apply { put("BlockEntityTag", saved) }
        }
        net.minecraft.world.level.block.Block.popResource(level, pos, stack)
    }

    /** 获取物品栏大小 */
    override fun getContainerSize(): Int = inventory.size

    /** 判断物品栏是否为空 */
    override fun isEmpty(): Boolean {
        for (stack in inventory) {
            if (!stack.isEmpty) return false
        }
        return true
    }

    /** 获取指定槽位的物品 */
    override fun getItem(slot: Int): ItemStack {
        return if (slot >= 0 && slot < inventory.size) {
            inventory[slot]
        } else {
            ItemStack.EMPTY
        }
    }

    /** 从物品栏移除物品（玩家取物品） */
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (slot >= 0 && slot < inventory.size) {
            val stack = inventory[slot]
            val removed = stack.split(amount)
            if (stack.isEmpty) {
                inventory[slot] = ItemStack.EMPTY
            }
            setChanged()
            return removed
        }
        return ItemStack.EMPTY
    }

    /** 从物品栏移除整个槽位的物品 */
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        if (slot >= 0 && slot < inventory.size) {
            val stack = inventory[slot]
            inventory[slot] = ItemStack.EMPTY
            setChanged()
            return stack
        }
        return ItemStack.EMPTY
    }

    /** 设置指定槽位的物品 */
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot >= 0 && slot < inventory.size) {
            inventory[slot] = stack
            setChanged()
        }
    }

    /** 标记物品栏已更改 */
    override fun setChanged() {
        super.setChanged()
    }

    /** 检查玩家是否可以使用此物品栏 */
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    /** 清空物品栏 */
    override fun clearContent() {
        inventory.clear()
        setChanged()
    }

    // ========== ExtendedMenuProvider<FriendlyByteBuf> 实现 ==========

    /** 创建菜单标题 */
    override fun getDisplayName(): Component {
        val block = cachedState.block
        return Component.translatable(block.descriptionId)
    }

    /** 创建 AbstractContainerMenu */
    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return StorageBoxScreenHandler.create(syncId, playerInventory, this)
    }

    /** 写入数据到 FriendlyByteBuf（客户端 GUI 打开时使用） */
    override fun getScreenOpeningData(player: net.minecraft.server.level.ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        return buf
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        // 保存物品栏到 NBT
        val inventoryNbt = CompoundTag()
        ContainerHelper26.saveAllItems(inventoryNbt, inventory, lookup)
        nbt.put(INVENTORY_KEY, inventoryNbt)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        // 从 NBT 读取物品栏
        val inventoryNbt = nbt.getCompoundOrEmpty(INVENTORY_KEY)
        if (inventoryNbt.size() == 0) {
            // 兼容旧格式（直接存储在 Items 键）
            val itemsNbt = nbt.getListOrEmpty(ITEMS_KEY)
            if (itemsNbt.size != 0) {
                inventory = createInventory()
                ContainerHelper26.loadAllItems(nbt, inventory, lookup)
            }
        } else {
            inventory = createInventory()
            ContainerHelper26.loadAllItems(inventoryNbt, inventory, lookup)
        }
    }
}
