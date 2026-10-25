package ic2_120.content.block.machines

import ic2_120.compat.recipeManager
import ic2_120.compat.ContainerHelper26

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.RecipeCacheEpoch
import ic2_120.content.item.IBlockCuttingBlade
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.recipes.blockcutter.BlockCutterRecipe
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.sync.BlockCutterSync
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.BlockCutterScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyDebtAccounting
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.energy.charge.BatteryDischargerComponent
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
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.level.Level
import ic2_120.content.upgrade.IValidInputItem

@ModBlockEntity(block = BlockCutterBlock::class)
@ModMachineRecipeBinding(BlockCutterRecipeSerializer::class)
class BlockCutterBlockEntity(
    type: net.minecraft.world.level.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedMenuProvider<FriendlyByteBuf>, IValidInputItem {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = BlockCutterBlock.ACTIVE

    override fun getInventory(): net.minecraft.world.Container = this

    override val tier: Int = BLOCK_CUTTER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

    companion object {
        const val BLOCK_CUTTER_TIER = 1
        const val SLOT_BLADE = 0
        const val SLOT_INPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_OUTPUT = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT)
        const val INVENTORY_SIZE = 8
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_BLADE), matcher = { it.item is IBlockCuttingBlade }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = BlockCutterSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(BLOCK_CUTTER_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        BlockCutterBlockEntity::class.type(),
        pos,
        state
    )

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if ((slot == SLOT_BLADE || slot == SLOT_DISCHARGING) && stack.count > 1) {
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
        SLOT_BLADE -> !stack.isEmpty && stack.item is IBlockCuttingBlade
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

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.block_cutter")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        BlockCutterScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLongOr(BlockCutterSync.NBT_ENERGY_STORED, 0L)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloatOr("ProgressF", 0f).coerceIn(0f, BlockCutterSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, BlockCutterSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloatOr("EnergyDebt", 0f).coerceAtLeast(0f) else 0f
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(BlockCutterSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
    }

    /** 锯片硬度，无锯片或非锯片时为 -1f */
    fun getBladeHardness(): Float {
        val blade = getItem(SLOT_BLADE)
        if (blade.isEmpty) return -1f
        return (blade.item as? IBlockCuttingBlade)?.getBladeHardness(blade) ?: -1f
    }

    /** 锯片是否足够硬（或无配方时不检查） */
    fun isBladeTooWeak(): Boolean {
        val bladeHardness = getBladeHardness()
        if (bladeHardness < 0f) return true
        val input = getItem(SLOT_INPUT)
        if (input.isEmpty) return false
        val recipe = getRecipeForInput(input) ?: return false
        return !recipe.isBladeSufficient(bladeHardness)
    }

    // 配方缓存——安全处理双配方系统（同一输入可有 inputCount=1/2 两个配方）：
    // BlockCutterRecipe.matches 检查 stack.count >= inputCount，匹配结果依赖输入数量。
    // 规则：非 null 缓存仅在 input.count >= recipe.inputCount 时有效；
    // null 缓存仅在输入数量未增加时有效（数量增加可能让此前不足量的配方匹配）。
    private var cachedRecipeItem: net.minecraft.world.item.Item? = null
    private var cachedRecipe: BlockCutterRecipe? = null
    private var cachedRecipeQueryCount = 0
    private var cachedRecipeEpoch = -1

    /**
     * 获取当前输入的配方（不考虑刀片硬度）
     */
    private fun getRecipeForInput(input: ItemStack): BlockCutterRecipe? {
        if (input.isEmpty) return null
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) {
            val cached = cachedRecipe
            if (cached != null && input.count >= cached.inputCount) return cached
            if (cached == null && input.count <= cachedRecipeQueryCount) return null
        }
        val w = world ?: return null
        val inv = SingleRecipeInput(input)
        val recipe = w.recipeManager.getRecipeFor(getRecipeType<BlockCutterRecipe>(), inv, w).orElse(null)?.value()
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipe = recipe
        cachedRecipeQueryCount = input.count
        return recipe
    }

    /**
     * 获取配方（检查刀片硬度）
     */
    private fun getRecipe(): BlockCutterRecipe? {
        val bladeHardness = getBladeHardness()
        if (bladeHardness < 0f) return null

        val input = getItem(SLOT_INPUT)
        if (input.isEmpty) return null

        val recipe = getRecipeForInput(input) ?: return null

        // 检查刀片硬度
        if (!recipe.isBladeSufficient(bladeHardness)) return null

        return recipe
    }

    /** 获取材料硬度（用于锯片检查） */
    fun getMaterialHardness(): Float {
        val input = getItem(SLOT_INPUT)
        if (input.isEmpty) return 0f

        val recipe = getRecipeForInput(input)
        return recipe?.materialHardness ?: 0f
    }

    /** 重置全部进度状态（刀片过弱/输入空/无配方/输出堵时调用）。 */
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

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        sync.bladeTooWeak = if (isBladeTooWeak()) 1 else 0

        val bladeHardness = getBladeHardness()
        if (bladeHardness < 0f) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val input = getItem(SLOT_INPUT)
        if (input.isEmpty) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val recipe = getRecipe() ?: run {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val inputCount = recipe.inputCount
        if (input.count < inputCount) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val result = recipe.output
        val outputSlot = getItem(SLOT_OUTPUT)
        val maxStack = result.maxStackSize
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.isSameItem(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (progressF >= BlockCutterSync.PROGRESS_MAX) {
            input.shrink(inputCount)
            if (outputSlot.isEmpty()) setItem(SLOT_OUTPUT, result.copy())
            else outputSlot.grow(result.count)
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
            setChanged()
            sync.syncCurrentTickFlow()
            return
        }

        // 耗能记账：1.6^n 按浮点累计，取整数部分消费、余数结转（EU 本身是整数）
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, BlockCutterSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
        val need = energyDebtF.toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            energyDebtF -= need
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
            progressF = (progressF + speedMultiplier).coerceAtMost(BlockCutterSync.PROGRESS_MAX.toFloat())
            sync.progress = progressF.toInt()
            setChanged()
            setActiveState(world, pos, state, true)
        } else {
            // 停顿期间不累计债务：防恢复供电后 need 超容量 all-or-nothing 卡死
            energyDebtF = 0f
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

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

    // 类型判定缓存：isRecipeInput 用 maxCount 查询（仅依赖物品类型），按 item 缓存安全。
    private var cachedInputItem: net.minecraft.world.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack) || stack.item is IUpgradeItem || stack.item is IBlockCuttingBlade) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val w = world ?: return true
        val inv = SingleRecipeInput(stack.copyWithCount(stack.maxStackSize))
        val found = w.recipeManager.getRecipeFor(getRecipeType<BlockCutterRecipe>(), inv, w).isPresent
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
    }
    /** P1（2026-08-26）：pulling 候选快速否定——(配方类型, Item) 布尔缓存零分配作答。
     *  返回 false = 该 item 必然不被输入槽接受；true = 值得走完整 isValid 裁决。 */
    override fun isValidInputItem(item: net.minecraft.world.item.Item): Boolean {
        if (isBatteryItem(net.minecraft.world.item.ItemStack(item, 1)) || net.minecraft.world.item.ItemStack(item, 1).item is IUpgradeItem || net.minecraft.world.item.ItemStack(item, 1).item is IBlockCuttingBlade) return false
        val w = world as? net.minecraft.server.level.ServerLevel ?: return true
        return ic2_120.content.RecipeInputLookupCache.hasAnyRecipe(getRecipeType<BlockCutterRecipe>(), item, w)
    }

}
