package ic2_120.content.block.machines

import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.recipes.RecyclerRecipes
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.RecyclerSync
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.RecyclerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.RecyclerScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.sound.SoundCategory
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
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.random.Random

/**
 * ================================================================================
 * ⚠️ 回收机 - 不使用原生配方系统
 * ================================================================================
 *
 * 本机器使用 RecyclerRecipes 的黑名单机制，而非传统的配方系统。
 *
 * 原因说明：
 * - 回收机可处理几乎所有物品（黑名单除外）
 * - 使用概率性输出（不一定每次都产生废料）
 * - 这是动态计算逻辑，不适合静态配方 JSON
 *
 * 详细说明请参阅：ic2_120.content.recipes.RecyclerRecipes
 *
 * @see ic2_120.content.recipes.RecyclerRecipes
 * ================================================================================
 */
@ModBlockEntity(block = RecyclerBlock::class)
class RecyclerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = RecyclerBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.recycler.operate", volume = 0.5f, pitch = 1.0f, intervalTicks = 20
    )

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = RECYCLER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val RECYCLER_TIER = 1
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT)
        const val INVENTORY_SIZE = 7
        private val SCRAP_ID = Identifier("ic2_120", "scrap")
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecyclerInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = RecyclerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(RECYCLER_TIER + voltageTierBonus) }
    )

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { RECYCLER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        RecyclerBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) {
            stack.count = 1
        }
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() {
        super.markDirty()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> isRecyclerInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack)
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.recycler")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        RecyclerScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(RecyclerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(RecyclerSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 应用升级效果
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        // 从相邻导线提取能量
        pullEnergyFromNeighbors(world, pos, sync)

        // 从电池槽提取能量
        extractFromDischargingSlot()

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 检查是否可回收
        if (!RecyclerRecipes.canRecycle(input)) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 检查输出槽是否有空间（废料最多堆叠 64）
        val scrapItem = getScrapItem()
        if (scrapItem == null) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }
        val outputSlot = getStack(SLOT_OUTPUT)
        val canAcceptScrap = outputSlot.isEmpty ||
                (outputSlot.item == scrapItem && outputSlot.count < outputSlot.maxCount)

        if (!canAcceptScrap) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        if (sync.progress >= RecyclerSync.PROGRESS_MAX) {
            // 消耗输入物品
            input.decrement(1)

            // 1/8 概率产出废料
            val roll = Random.nextInt(RecyclerSync.SCRAP_CHANCE_DENOMINATOR)
            if (roll < RecyclerSync.SCRAP_CHANCE_NUMERATOR) {
                val newCount = outputSlot.count + 1
                val scrapStack = ItemStack(scrapItem, newCount)
                setStack(SLOT_OUTPUT, scrapStack)
            }

            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val need = (RecyclerSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += progressIncrement
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    private fun getScrapItem(): Item? {
        val item = Registries.ITEM.get(SCRAP_ID)
        return if (item == net.minecraft.item.Items.AIR) null else item
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isRecyclerInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        return RecyclerRecipes.canRecycle(stack)
    }
}

