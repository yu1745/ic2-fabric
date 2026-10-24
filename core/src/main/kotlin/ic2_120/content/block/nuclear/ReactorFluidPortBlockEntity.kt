import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

﻿package ic2_120.content.block.nuclear

import net.minecraft.core.HolderLookup
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.ContainerData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 反应堆流体接口方块实体。
 * 提供 1 个升级槽，用于安装流体抽入/弹出升级。
 * 实际的流体存储在中心反应堆中。
 */
@ModBlockEntity(block = ReactorFluidPortBlock::class)
class ReactorFluidPortBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Container, IRoutedSidedInventory, IFluidPipeUpgradeSupport,
    ExtendedScreenHandlerFactory {

    // 次构造函数：仅接受 pos 和 state，自动获取 type
    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorFluidPortBlockEntity::class.type(),
        pos,
        state
    )

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    // 用于同步数据到客户端的PropertyDelegate
    val propertyDelegate = object : ContainerData {
        private val data = IntArray(4) // 可根据需要调整大小

        override fun get(index: Int): Int = data[index]
        override fun set(index: Int, value: Int) {
            data[index] = value
        }
        override fun size(): Int = data.size
    }

    companion object {
        const val INVENTORY_SIZE = 1  // 只有一个升级槽
        val SLOT_UPGRADE_INDICES = intArrayOf(0)

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = ReactorFluidPortBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity(
                { be, side -> be.getFluidStorageForSide(side) },
                type
            )
            fluidLookupRegistered = true
        }
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { _, stack -> stack.item is IUpgradeItem },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem })
        ),
        extractSlots = SLOT_UPGRADE_INDICES,
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    // Container 实现
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    // 获取中心反应堆
    private fun getCentralReactor(): NuclearReactorBlockEntity? {
        val w = world ?: return null
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val be = w.getBlockEntity(pos.add(dx, dy, dz))
                    if (be is NuclearReactorBlockEntity) return be
                }
            }
        }
        return null
    }

    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant> {
        val reactor = getCentralReactor() ?: return Storage.empty()
        // 流体接口统一暴露反应堆的组合存储（冷却液输入 + 热冷却液输出）。
        // 不按接口方块的接触面拆分，避免出现“只有某个面能拿到热冷却液”的问题。
        return reactor.getFluidStorageForSide(null)
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.reactor_fluid_port")

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(propertyDelegate.size())
    }

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu? {
        return ic2_120.content.screen.ReactorFluidPortScreenHandler(
            syncId,
            playerInventory,
            this,
            ContainerLevelAccess.create(world ?: return null, pos),
            propertyDelegate,
            itemStorage
        )
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        // 应用流体升级
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    }
}
