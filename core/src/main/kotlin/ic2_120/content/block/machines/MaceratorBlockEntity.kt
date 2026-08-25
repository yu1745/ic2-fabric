package ic2_120.content.block.machines

import ic2_120.content.RecipeCacheEpoch
import ic2_120.content.sync.MaceratorSync
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyDebtAccounting
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.screen.MaceratorScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.recipes.macerator.MaceratorRecipe
import ic2_120.content.recipes.macerator.MaceratorRecipeSerializer
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = MaceratorBlock::class)
@ModMachineRecipeBinding(MaceratorRecipeSerializer::class)
class MaceratorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = MaceratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.macerator.operate",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = MACERATOR_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

    companion object {
        const val MACERATOR_TIER = 1
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
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = MaceratorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(MACERATOR_TIER + voltageTierBonus) }
    )
    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { MACERATOR_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        MaceratorBlockEntity::class.type(),
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
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> isRecipeInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.macerator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MaceratorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(MaceratorSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloat("ProgressF").coerceIn(0f, MaceratorSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, MaceratorSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloat("EnergyDebt").coerceAtLeast(0f) else 0f
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MaceratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
    }

    /** 重置全部进度状态（输入空/无配方/输出堵时调用）。 */
    private fun resetProgress() {
        if (progressF != 0f || energyDebtF != 0f || sync.progress != 0) {
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
        }
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 应用升级效果（加速、储能、高压等）
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()

        // 从放电槽提取能量
        extractFromDischargingSlot()

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty()) {
            resetProgress()
            sync.syncCurrentTickFlow()
            return
        }

        val recipe = maceratorRecipeFor(input, world)
        if (recipe == null) {
            resetProgress()
            sync.syncCurrentTickFlow()
            return
        }
        val result = maceratorOutputFor(recipe)
        val inputCount = recipe.inputCount
        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (input.count < inputCount || !canAccept) {
            resetProgress()
            sync.syncCurrentTickFlow()
            return
        }

        if (progressF >= MaceratorSync.PROGRESS_MAX) {
            input.decrement(inputCount)
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result.copy())
            else outputSlot.increment(result.count)
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
            setActiveState(world, pos, state, false)
            markDirtyThrottled()
            sync.syncCurrentTickFlow()
            return
        }

        // 耗能记账：1.6^n 按浮点累计，取整数部分消费、余数结转（EU 本身是整数）
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, MaceratorSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
        val need = energyDebtF.toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            energyDebtF -= need
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
            progressF = (progressF + speedMultiplier).coerceAtMost(MaceratorSync.PROGRESS_MAX.toFloat())
            sync.progress = progressF.toInt()
            markDirtyThrottled()
        }

        val active = sync.progress > 0
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }
    /**
     * 从放电槽提取能量（如果需要）
     */
    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    // 配方缓存——安全处理多对一配方（inputCount > 1，如甘蔗需 8 个）：
    // MaceratorRecipe.matches 检查 stack.count >= inputCount，因此配方匹配结果依赖输入数量。
    // 缓存规则保证任何数量变化都正确失效：
    //   1. 非 null 缓存仅在 input.count >= recipe.inputCount 时有效（数量不足即重查）；
    //   2. null 缓存仅在输入数量未增加时有效（数量增加可能让此前数量不足的配方匹配）。
    // 这样既不会因“刚放入一个就缓存”锁死多对一机器，也避免了每 tick 重复查询 recipeManager。
    private var cachedRecipeItem: net.minecraft.item.Item? = null
    private var cachedRecipe: MaceratorRecipe? = null
    private var cachedRecipeQueryCount = 0
    private var cachedRecipeEpoch = -1
    private var cachedOutputRecipe: MaceratorRecipe? = null
    private var cachedOutputStack: ItemStack = ItemStack.EMPTY

    private fun maceratorRecipeFor(input: ItemStack, world: World): MaceratorRecipe? {
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) {
            val cached = cachedRecipe
            if (cached != null && input.count >= cached.inputCount) return cached
            if (cached == null && input.count <= cachedRecipeQueryCount) return null
        }
        val recipeInventory = SimpleInventory(input)
        val recipe = world.recipeManager.getFirstMatch(getRecipeType<MaceratorRecipe>(), recipeInventory, world).orElse(null)
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipe = recipe
        cachedRecipeQueryCount = input.count
        return recipe
    }

    /** Recipe outputs are immutable data after datapack load; avoid copying one every tick. */
    private fun maceratorOutputFor(recipe: MaceratorRecipe): ItemStack {
        if (recipe !== cachedOutputRecipe) {
            cachedOutputRecipe = recipe
            cachedOutputStack = recipe.output.copy()
        }
        return cachedOutputStack
    }

    // 类型判定缓存：isRecipeInput 始终用 maxCount 查询（仅依赖物品类型，与数量无关），
    // 因此按 item 缓存布尔结果是安全的，且可命中 isValid / 物流路由 matcher 的重复调用。
    private var cachedInputItem: net.minecraft.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val currentWorld = world ?: return true
        // 仅用于识别物品类型时，必须提供足够大的虚拟堆叠；部分配方需要多个输入。
        val recipeInventory = SimpleInventory(stack.copyWithCount(stack.maxCount))
        val found = currentWorld.recipeManager.getFirstMatch(getRecipeType<MaceratorRecipe>(), recipeInventory, currentWorld).isPresent
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
    }
}
