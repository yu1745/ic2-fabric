package ic2_120.content.screen

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.world

import ic2_120.compat.stack

import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.ChargepadBlock
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.entity.EquipmentSlot
import ic2_120.registry.annotation.ScreenFactory

/**
 * 储电盒 GUI 的 AbstractContainerMenu。
 * 四个等级（BatBox/CESU/MFE/MFSU）共用，根据方块类型动态配置槽位数量和规则。
 */
@ModScreenHandler(
    names = [
        "batbox", "cesu", "mfe", "mfsu",
        "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad"
    ]
)
class EnergyStorageScreenHandler(
    screenHandlerType: net.minecraft.world.inventory.MenuType<*>,
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    val context: ContainerLevelAccess,
    propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(screenHandlerType, syncId) {
    private val config = resolveConfig()
    private val machineSlotCount: Int = config.slotCount
    val sync = EnergyStorageSync(
        schema = SyncedDataView(propertyDelegate),
        getFacing = {
            context.evaluate({ world, pos ->
                val state = world.getBlockState(pos)
                state.getOptionalValue(BlockStateProperties.FACING)
                    .orElse(state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH))
            }, Direction.NORTH)
        },
        tier = config.tier,
        capacity = config.capacity
    )

    val useEquipmentSlots: Boolean get() = config.useEquipmentSlots

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun resolveConfig(): EnergyStorageConfig {
        return context.evaluate({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = BuiltInRegistries.BLOCK.getKey(block)
            EnergyStorageConfig.fromBlockPath(id.path) ?: EnergyStorageConfig.BATBOX
        }, EnergyStorageConfig.BATBOX)
    }

    init {
        checkContainerSize(blockInventory, machineSlotCount)
        addDataSlots(propertyDelegate)

        // 1 个充电槽，所有等级都有（blockInventory slot 0）
        addTrackedSlot(blockInventory, 0, 56, 17)

        // 1 个供能槽，所有等级都有（blockInventory 最后一个 slot）
        addTrackedSlot(blockInventory, machineSlotCount - 1, 56, 53)

        // 4 个玩家装备槽（横列，1px 间距），MFE/MFSU 专用
        if (config.useEquipmentSlots) {
            addSlot(armorSlot(playerInventory, 39, 8, 84, EquipmentSlot.HEAD))   // 头盔
            addSlot(armorSlot(playerInventory, 38, 26, 84, EquipmentSlot.CHEST))  // 胸甲
            addSlot(armorSlot(playerInventory, 37, 44, 84, EquipmentSlot.LEGS))   // 护腿
            addSlot(armorSlot(playerInventory, 36, 62, 84, EquipmentSlot.FEET))   // 靴子
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 172))
        }
    }

    private fun addTrackedSlot(inventory: Container, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    /**
     * MFE/MFSU 的装备槽直接映射到玩家护甲栏，必须同时校验物品类型和对应部位。
     * 普通 Slot 的 canInsert 默认恒为 true，会导致任意物品被塞进护甲栏。
     */
    private fun armorSlot(
        inventory: Inventory,
        index: Int,
        x: Int,
        y: Int,
        equipmentSlot: EquipmentSlot
    ): Slot = object : Slot(inventory, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            val item = stack.item
            return item is ArmorItem && item.type.slot == equipmentSlot
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY

        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!moveItemStackTo(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                useEquipmentSlots && index in machineSlotCount until machineSlotCount + 4 -> {
                    if (!moveItemStackTo(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, true)) return ItemStack.EMPTY
                }
                index in playerInventorySlotStart..hotbarEnd -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    if (!moveItemStackTo(stackInSlot, playerInventorySlotStart, hotbarEnd + 1, false)) return ItemStack.EMPTY
                }
            }

            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY)
            else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

   val playerInventorySlotStart: Int get() = machineSlotCount + (if (config.useEquipmentSlots) 4 else 0)

   private val hotbarEnd: Int
       get() = playerInventorySlotStart + 35

   override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            val block = world.getBlockState(pos).block
            (block is ic2_120.content.block.storage.EnergyStorageBlock || block is ChargepadBlock) &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    /** 是否为可切红石模式的储电箱（非充电座）。 */
    val supportsRedstoneMode: Boolean get() = config.emitRedstoneWhenNotFull

    /** 当前红石模式（两端统一经 SyncedData 属性：服务端读 BE 本体，客户端读属性同步值）。 */
    val currentRedstoneMode: Int
        get() = sync.redstoneMode.coerceIn(0, 6)

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id != BUTTON_REDSTONE_MODE) return super.clickMenuButton(player, id)
        context.execute { world, pos ->
            val be = world.getBlockEntity(pos) as? EnergyStorageBlockEntity ?: return@execute
            be.cycleRedstoneMode()
        }
        return true
    }

   companion object {
       const val SLOT_SIZE = 18
        /** GUI 红石模式循环切换按钮。 */
        const val BUTTON_REDSTONE_MODE = 100
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): EnergyStorageScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val slotCount = buf.readVarInt()
            buf.readBoolean() // useEquipmentSlots, read but not stored (AbstractContainerMenu gets config from context)
            val ctx = ContainerLevelAccess.create(playerInventory.player.level(), pos)
            val blockInv = SimpleContainer(slotCount.coerceAtLeast(1))
            val blockId = BuiltInRegistries.BLOCK.getKey(playerInventory.player.level().getBlockState(pos).block) ?: error("block not registered")
            val screenHandlerType = BuiltInRegistries.MENU.get(ResourceLocation.fromNamespaceAndPath(blockId.namespace, blockId.path))
                ?: error("AbstractContainerMenu type not found for $blockId")
            @Suppress("UNCHECKED_CAST")
            return EnergyStorageScreenHandler(
                screenHandlerType as net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.AbstractContainerMenu>,
                syncId, playerInventory, blockInv, ctx, SimpleContainerData(propertyCount)
            )
        }
    }
}
