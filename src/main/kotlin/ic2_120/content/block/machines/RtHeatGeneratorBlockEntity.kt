package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.RtHeatGeneratorBlock
import ic2_120.content.screen.RtHeatGeneratorScreenHandler
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.sync.RtHeatGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 放射性同位素温差加热机（RTG）：
 * - 使用 RTG 燃料靶丸（rtg_pellet）产生热量，靶丸无限耐久不消耗
 * - 6 个燃料槽
 * - 发热量由靶丸数量决定：1→2, 2→4, 3→8, 4→16, 5→32, 6→64 HU/t
 * - 仅背面单面传热
 */
@ModBlockEntity(block = RtHeatGeneratorBlock::class)
class RtHeatGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatGeneratorBlockEntityBase(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    companion object {
        /** 燃料槽数量（6 个靶丸槽位） */
        const val FUEL_SLOT_COUNT = 6
        /** 燃料槽 0..5 */
        const val FUEL_SLOT_START = 0
        const val FUEL_SLOT_END = 5
        const val INVENTORY_SIZE = FUEL_SLOT_COUNT

        private val RTG_PELLET_ITEM = lazy {
            Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rtg_pellet"))
        }

        fun isRtgPellet(stack: ItemStack): Boolean =
            !stack.isEmpty && stack.item == RTG_PELLET_ITEM.value

        /** 根据靶丸数量计算发热量（HU/t）：1→2, 2→4, 3→8, 4→16, 5→32, 6→64 */
        fun huPerTickFromPelletCount(count: Int): Long {
            val c = count.coerceIn(0, 6)
            return if (c == 0) 0L else (1 shl c).toLong()
        }
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData(this)
    override val heatFlow = HeatFlowSync(syncedData, this)
    val sync = RtHeatGeneratorSync(syncedData, heatFlow)

    constructor(pos: BlockPos, state: BlockState) : this(
        RtHeatGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    /** 统计燃料槽中靶丸数量 */
    fun countPelletSlots(): Int =
        (FUEL_SLOT_START..FUEL_SLOT_END).count { !getStack(it).isEmpty }

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.rt_heat_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        RtHeatGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot in FUEL_SLOT_START..FUEL_SLOT_END && stack.count > 1) {
            stack.count = 1
        }
        inventory[slot] = stack
        markDirty()
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
    }

    override fun generateHeat(world: World, pos: BlockPos, state: BlockState): Long {
        // 燃料不消耗，直接根据靶丸数量计算发热量
        return huPerTickFromPelletCount(countPelletSlots())
    }

    override fun syncAdditionalData() {
        // 无需同步额外数据
    }

    override fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean =
        generatedHeat > 0 && hasValidConsumer

    override fun getActiveState(state: BlockState): Boolean =
        state.get(RtHeatGeneratorBlock.ACTIVE)

    override fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        world.setBlockState(pos, state.with(RtHeatGeneratorBlock.ACTIVE, active))
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        tickHeatMachine(world, pos, state)
    }
}
