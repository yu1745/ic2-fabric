package ic2_120.content.block.machines

import ic2_120.content.recipes.getRecipeType
import ic2_120.content.recipes.metalformer.MetalFormerRecipe
import ic2_120.content.recipes.metalformer.MetalFormerRecipeSerializer
import ic2_120.content.sync.MetalFormerSync
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.MetalFormerScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
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
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = MetalFormerBlock::class)
@ModMachineRecipeBinding(MetalFormerRecipeSerializer::class)
class MetalFormerBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, Storage<ItemVariant>, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = MetalFormerBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = METAL_FORMER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val METAL_FORMER_TIER = 1
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 7
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = MetalFormerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(METAL_FORMER_TIER + voltageTierBonus) }
    )
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { METAL_FORMER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        MetalFormerBlockEntity::class.type(),
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
        SLOT_INPUT -> isRecipeInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack)
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
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

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.metal_former")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MetalFormerScreenHandler(
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
        sync.amount = nbt.getLong(MetalFormerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.setMode(MetalFormerSync.Mode.fromId(nbt.getInt(MetalFormerSync.NBT_MODE)))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MetalFormerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(MetalFormerSync.NBT_MODE, sync.mode)
    }

    /**
     * 切换加工模式（由 GUI 按钮调用）
     */
    fun cycleMode() {
        sync.cycleMode()
        sync.progress = 0  // 切换模式时重置进度
        markDirty()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 应用升级效果（加速、储能、高压等）
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
//        println("voltageTierBonus: $voltageTierBonus")
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        // 从相邻方块或导线提取能量（maxPull 随高压升级提高）
        pullEnergyFromNeighbors(world, pos, sync)

        // 从放电槽提取能量
        extractFromDischargingSlot()

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val currentMode = sync.getMode()

        // 根据当前模式获取对应的配方类
        val recipeType = when (currentMode) {
            MetalFormerSync.Mode.ROLLING -> ic2_120.content.recipes.metalformer.RollingRecipe::class
            MetalFormerSync.Mode.CUTTING -> ic2_120.content.recipes.metalformer.CuttingRecipe::class
            MetalFormerSync.Mode.EXTRUDING -> ic2_120.content.recipes.metalformer.ExtrudingRecipe::class
        }

        // 通过 RecipeManager 查找配方
        val recipeManager = world?.recipeManager ?: return
        val recipeInput = MetalFormerRecipe.Input(input)
        // 遍历所有匹配该 RecipeType 的配方，找到当前模式对应的
        val recipe = recipeManager.listAllOfType(getRecipeType<MetalFormerRecipe>())
            .filterIsInstance(recipeType.java)
            .firstOrNull { it.matches(recipeInput, world) }
        val result = recipe?.let { MetalFormerRecipe.getOutput(it) } ?: run {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
                (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        if (sync.progress >= MetalFormerSync.PROGRESS_MAX) {
            // 消耗输入物品
            input.decrement(1)

            // 输出结果
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result.copy())
            else outputSlot.increment(result.count)

            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val need = (MetalFormerSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += progressIncrement
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }

        // 同步当前 tick 的实际输入/耗能
        sync.syncCurrentTickFlow()
    }

    /**
     * 从放电槽提取能量（如果需要）
     */
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

    private fun isInputItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item !is IBatteryItem

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (!isInputItem(stack)) return false
        val currentWorld = world ?: return true
        val input = MetalFormerRecipe.Input(stack.copyWithCount(1))
        return currentWorld.recipeManager
            .listAllOfType(getRecipeType<MetalFormerRecipe>())
            .any { it.matches(input, currentWorld) }
    }

}
