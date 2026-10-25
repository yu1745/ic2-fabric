package ic2_120.content.block.machines

import ic2_120.compat.recipeManager
import ic2_120.compat.ContainerHelper26

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

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
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.level.Level
import ic2_120.content.upgrade.IValidInputItem

@ModBlockEntity(block = MaceratorBlock::class)
@ModMachineRecipeBinding(MaceratorRecipeSerializer::class)
class MaceratorBlockEntity(
    type: net.minecraft.world.level.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedMenuProvider<FriendlyByteBuf>, IValidInputItem {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = MaceratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.macerator.operate",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    override fun getInventory(): net.minecraft.world.Container = this

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

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        setChanged = { setChanged() }
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

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) {
            stack.count = 1
        }
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> isRecipeInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.macerator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        MaceratorScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLongOr(MaceratorSync.NBT_ENERGY_STORED, 0L)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloatOr("ProgressF", 0f).coerceIn(0f, MaceratorSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, MaceratorSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloatOr("EnergyDebt", 0f).coerceAtLeast(0f) else 0f
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
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

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
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

        val input = getItem(SLOT_INPUT)
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
        val outputSlot = getItem(SLOT_OUTPUT)
        val maxStack = result.maxStackSize
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.isSameItem(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (input.count < inputCount || !canAccept) {
            resetProgress()
            sync.syncCurrentTickFlow()
            return
        }

        if (progressF >= MaceratorSync.PROGRESS_MAX) {
            input.shrink(inputCount)
            if (outputSlot.isEmpty()) setItem(SLOT_OUTPUT, result.copy())
            else outputSlot.grow(result.count)
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
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
        setChanged()
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    // 配方缓存——安全处理多对一配方（inputCount > 1，如甘蔗需 8 个）：
    // MaceratorRecipe.matches 检查 stack.count >= inputCount，因此配方匹配结果依赖输入数量。
    // 缓存规则保证任何数量变化都正确失效：
    //   1. 非 null 缓存仅在 input.count >= recipe.inputCount 时有效（数量不足即重查）；
    //   2. null 缓存仅在输入数量未增加时有效（数量增加可能让此前数量不足的配方匹配）。
    // 这样既不会因“刚放入一个就缓存”锁死多对一机器，也避免了每 tick 重复查询 recipeManager。
    private var cachedRecipeItem: net.minecraft.world.item.Item? = null
    private var cachedRecipe: MaceratorRecipe? = null
    private var cachedRecipeQueryCount = 0
    private var cachedRecipeEpoch = -1
    private var cachedOutputRecipe: MaceratorRecipe? = null
    private var cachedOutputStack: ItemStack = ItemStack.EMPTY

    private fun maceratorRecipeFor(input: ItemStack, world: Level): MaceratorRecipe? {
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) {
            val cached = cachedRecipe
            if (cached != null && input.count >= cached.inputCount) return cached
            if (cached == null && input.count <= cachedRecipeQueryCount) return null
        }
        val recipeInventory = SingleRecipeInput(input)
        val recipe = world.recipeManager.getRecipeFor(getRecipeType<MaceratorRecipe>(), recipeInventory, world).orElse(null)?.value()
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
    private var cachedInputItem: net.minecraft.world.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val currentWorld = world ?: return true
        // 仅用于识别物品类型时，必须提供足够大的虚拟堆叠；部分配方需要多个输入。
        val recipeInventory = SingleRecipeInput(stack.copyWithCount(stack.maxStackSize))
        val found = currentWorld.recipeManager.getRecipeFor(getRecipeType<MaceratorRecipe>(), recipeInventory, currentWorld).isPresent
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
    }
    /** P1（2026-08-26）：pulling 候选快速否定——(配方类型, Item) 布尔缓存零分配作答。
     *  返回 false = 该 item 必然不被输入槽接受；true = 值得走完整 isValid 裁决。 */
    override fun isValidInputItem(item: net.minecraft.world.item.Item): Boolean {
        if (isBatteryItem(net.minecraft.world.item.ItemStack(item, 1))) return false
        val w = world as? net.minecraft.server.level.ServerLevel ?: return true
        return ic2_120.content.RecipeInputLookupCache.hasAnyRecipe(getRecipeType<MaceratorRecipe>(), item, w)
    }

}
