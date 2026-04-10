package ic2_120.content.block.machines

import ic2_120.content.sync.ElectricFurnaceSync
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.ElectricFurnaceScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 电力熔炉方块实体。提供输入/输出槽位并实现简单 GUI。
 * [sync] 继承 TickLimitedEnergyStorage，即同步数据又即 Energy API 存储。
 */
@ModBlockEntity(block = ElectricFurnaceBlock::class)
class ElectricFurnaceBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, Storage<ItemVariant>, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = ElectricFurnaceBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.startStop(
        startSoundId = "machine.furnace.electric.start",
        stopSoundId = "machine.furnace.electric.stop",
        volume = 0.5f,
        pitch = 1.0f,
        loopSoundId = "machine.furnace.electric.loop",
        loopIntervalTicks = 20
    )

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = 1

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_DISCHARGING = 2
        const val INVENTORY_SIZE = 3
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)  // 0: 输入, 1: 输出, 2: 放电
    private var storedExperience: Float = 0f
    private val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isSmeltingInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ElectricFurnaceSync(syncedData) { world?.time }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < ElectricFurnaceSync.ENERGY_CAPACITY }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ElectricFurnaceBlockEntity::class.type(),
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
    override fun canPlayerUse(player: PlayerEntity): Boolean =
        Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> isSmeltingInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack)
        else -> false
    }

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
        itemStorage.insert(resource, maxAmount, transaction)

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
        itemStorage.extract(resource, maxAmount, transaction)

    override fun iterator(): MutableIterator<StorageView<ItemVariant>> = itemStorage.iterator()

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("block.ic2_120.electric_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ElectricFurnaceScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ElectricFurnaceSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        storedExperience = nbt.getFloat(FurnaceExperienceHelper.NBT_EXPERIENCE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ElectricFurnaceSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat(FurnaceExperienceHelper.NBT_EXPERIENCE, storedExperience)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.experienceDisplay = (storedExperience * 10).toInt()
        pullEnergyFromNeighbors(world, pos, sync)

        // 从放电槽提取能量
        extractFromDischargingSlot()

        val input = getStack(0)
        if (input.isEmpty()) {
            if (sync.progress != 0) {
                sync.progress = 0
            }
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val inputInv = SimpleInventory(1).apply { setStack(0, input) }
        val match = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inputInv, world)
        if (match.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val recipe = match.get()
        val result = recipe.getOutput(world.registryManager).copy()
        val outputSlot = getStack(1)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.progress >= ElectricFurnaceSync.PROGRESS_MAX) {
            input.decrement(1)
            if (outputSlot.isEmpty()) setStack(1, result.copy())
            else outputSlot.increment(result.count)
            storedExperience += FurnaceExperienceHelper.getExperienceFromRecipe(recipe)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 内部消耗：直接扣减 amount（不经过 extract，故对外 MAX_EXTRACT=0 仍生效，电缆无法拉电）
        val need = ElectricFurnaceSync.ENERGY_PER_TICK
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += 1
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }
        sync.syncCurrentTickFlow()
    }

    fun dropStoredExperience() {
        val world = this.world as? ServerWorld ?: return
        FurnaceExperienceHelper.dropExperience(world, pos, storedExperience)
        storedExperience = 0f
        markDirty()
    }

    /**
     * 从放电槽提取能量（如果需要）
     */
    private fun extractFromDischargingSlot() {
        val space = (ElectricFurnaceSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, ElectricFurnaceSync.MAX_EXTRACT)
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isSmeltingInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(1))
        return w.recipeManager.getFirstMatch(RecipeType.SMELTING, inv, w).isPresent
    }
}











