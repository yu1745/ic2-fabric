package ic2_120.content.block.nuclear

import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.PropertyDelegate
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

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
) : BlockEntity(type, pos, state), Inventory, IFluidPipeUpgradeSupport,
    ExtendedScreenHandlerFactory {

    // 次构造函数：仅接受 pos 和 state，自动获取 type
    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorFluidPortBlockEntity::class.type(),
        pos,
        state
    )

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    // 用于同步数据到客户端的PropertyDelegate
    val propertyDelegate = object : PropertyDelegate {
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

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    // Inventory 实现
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

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

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.reactor_fluid_port")

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(propertyDelegate.size())
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler? {
        return ic2_120.content.screen.ReactorFluidPortScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world ?: return null, pos),
            propertyDelegate
        )
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        // 应用流体升级
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    }
}
