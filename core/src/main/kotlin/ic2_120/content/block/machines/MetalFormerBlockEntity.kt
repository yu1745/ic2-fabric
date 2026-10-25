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
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.recipes.metalformer.MetalFormerRecipe
import ic2_120.content.recipes.metalformer.MetalFormerRecipeSerializer
import ic2_120.content.sync.MetalFormerSync
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
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.MetalFormerScreenHandler
import ic2_120.content.syncs.SyncedData
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
import net.minecraft.world.level.Level

@ModBlockEntity(block = MetalFormerBlock::class)
@ModMachineRecipeBinding(MetalFormerRecipeSerializer::class)
class MetalFormerBlockEntity(
    type: net.minecraft.world.level.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedMenuProvider<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = MetalFormerBlock.ACTIVE

    override fun getInventory(): net.minecraft.world.Container = this

    override val tier: Int = METAL_FORMER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

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
    val sync = MetalFormerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(METAL_FORMER_TIER + voltageTierBonus) }
    )
    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
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
    override fun setChanged() {
        super.setChanged()
    }

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

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.metal_former")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        MetalFormerScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLongOr(MetalFormerSync.NBT_ENERGY_STORED, 0L)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloatOr("ProgressF", 0f).coerceIn(0f, MetalFormerSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, MetalFormerSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloatOr("EnergyDebt", 0f).coerceAtLeast(0f) else 0f
        sync.setMode(MetalFormerSync.Mode.fromId(nbt.getIntOr(MetalFormerSync.NBT_MODE, 0)))
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(MetalFormerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
        nbt.putInt(MetalFormerSync.NBT_MODE, sync.mode)
    }

    /**
     * 切换加工模式（由 GUI 按钮调用）
     */
    fun cycleMode() {
        sync.cycleMode()
        resetProgress()  // 切换模式时重置进度（含浮点记账）
        setChanged()
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
//        println("voltageTierBonus: $voltageTierBonus")
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()

        // 从放电槽提取能量
        extractFromDischargingSlot()

        val input = getItem(SLOT_INPUT)
        if (input.isEmpty) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val currentMode = sync.getMode()
        val recipe = findRecipe(world, input, currentMode)
        val result = recipe?.let { MetalFormerRecipe.getOutput(it) } ?: run {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

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

        if (progressF >= MetalFormerSync.PROGRESS_MAX) {
            // 消耗输入物品
            input.shrink(1)

            // 输出结果
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
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, MetalFormerSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
        val need = energyDebtF.toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            energyDebtF -= need
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
            progressF = (progressF + speedMultiplier).coerceAtMost(MetalFormerSync.PROGRESS_MAX.toFloat())
            sync.progress = progressF.toInt()
            setChanged()
            setActiveState(world, pos, state, true)
        } else {
            // 停顿期间不累计债务：防恢复供电后 need 超容量 all-or-nothing 卡死
            energyDebtF = 0f
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

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        setChanged()
    }

    private fun isInputItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item !is IBatteryItem

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    // 配方缓存：MetalFormerRecipe.matches 只检查物品类型（不看数量），且结果同时
    // 依赖输入物品与当前模式（rolling/cutting/extruding），因此按 (item, mode) 缓存。
    // listAllOfType 每 tick 全量遍历所有金属成型配方，命中缓存可完全跳过。
    private var cachedQueryItem: net.minecraft.world.item.Item? = null
    private var cachedQueryMode: MetalFormerSync.Mode? = null
    private var cachedRecipe: MetalFormerRecipe? = null
    private var cachedRecipeEpoch = -1

    private fun findRecipe(world: Level, input: ItemStack, mode: MetalFormerSync.Mode): MetalFormerRecipe? {
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedQueryItem === input.item && cachedQueryMode == mode) return cachedRecipe

        // 根据当前模式获取对应的配方类
        val recipeType = when (mode) {
            MetalFormerSync.Mode.ROLLING -> ic2_120.content.recipes.metalformer.RollingRecipe::class
            MetalFormerSync.Mode.CUTTING -> ic2_120.content.recipes.metalformer.CuttingRecipe::class
            MetalFormerSync.Mode.EXTRUDING -> ic2_120.content.recipes.metalformer.ExtrudingRecipe::class
        }

        // 通过 RecipeManager 查找配方
        val recipeManager = world.recipeManager ?: return null
        val recipeInput = MetalFormerRecipe.Input(input)
        val recipe = recipeManager.recipes
            .map { it.value }
            .filterIsInstance<MetalFormerRecipe>()
            .firstOrNull { recipeType.isInstance(it) && it.matches(recipeInput, world) }
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedQueryItem = input.item
        cachedQueryMode = mode
        cachedRecipe = recipe
        return recipe
    }

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (!isInputItem(stack)) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val currentWorld = world ?: return true
        val input = MetalFormerRecipe.Input(stack.copyWithCount(1))
        val found = currentWorld.recipeManager.recipes
            .any { (it.value as? MetalFormerRecipe)?.matches(input, currentWorld) == true }
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
    }

    // 类型判定缓存：isRecipeInput 用 copyWithCount(1) 查询（配方只看物品类型），
    // 结果仅依赖 item，按 item 缓存安全。
    private var cachedInputItem: net.minecraft.world.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

}
