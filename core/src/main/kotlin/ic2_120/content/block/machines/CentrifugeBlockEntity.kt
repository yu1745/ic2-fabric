package ic2_120.content.block.machines

import ic2_120.content.RecipeCacheEpoch
import ic2_120.content.block.CentrifugeBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.recipes.centrifuge.CentrifugeRecipe
import ic2_120.content.recipes.centrifuge.CentrifugeRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.screen.CentrifugeScreenHandler
import ic2_120.content.sync.CentrifugeSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyDebtAccounting
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SimpleInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeManager
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.content.upgrade.IValidInputItem

/**
 * 热能离心机方块实体。
 *
 * 槽位：输入、3 输出、放电、4 升级
 * 热量逻辑：有红石信号时加热至 5000 并维持；无红石时降至当前配方最低热量
 * 加热 1 EU/t 不受超频影响；加工 48 EU/t 受超频影响
 */
@ModBlockEntity(block = CentrifugeBlock::class)
@ModMachineRecipeBinding(CentrifugeRecipeSerializer::class)
class CentrifugeBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport,
    IRedstoneInverterUpgradeSupport, ExtendedScreenHandlerFactory, IValidInputItem {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CentrifugeBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = CentrifugeSync.CENTRIFUGE_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var redstoneInverted: Boolean = false

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务（加工阶段 1.6^n 的小数余数跨 tick 结转，停顿期间清零）。 */
    private var energyDebtF = 0f

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT_1 = 1
        const val SLOT_OUTPUT_2 = 2
        const val SLOT_OUTPUT_3 = 3
        const val SLOT_DISCHARGING = 4
        const val SLOT_UPGRADE_0 = 5
        const val SLOT_UPGRADE_1 = 6
        const val SLOT_UPGRADE_2 = 7
        const val SLOT_UPGRADE_3 = 8
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT)
        const val INVENTORY_SIZE = 9
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
        extractSlots = intArrayOf(SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = CentrifugeSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CentrifugeSync.CENTRIFUGE_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { CentrifugeSync.CENTRIFUGE_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        CentrifugeBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
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
        SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3 -> false
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.centrifuge")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CentrifugeScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CentrifugeSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloat("ProgressF").coerceIn(0f, CentrifugeSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, CentrifugeSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloat("EnergyDebt").coerceAtLeast(0f) else 0f
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CentrifugeSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
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

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        val input = getStack(SLOT_INPUT)
        val redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)

        // 热量逻辑：有红石时加热至 5000；无红石时降至当前配方最低热量
        val recipe = getRecipe()
        val targetHeat = when {
            redstoneAllowsRun -> CentrifugeSync.HEAT_MAX
            recipe != null -> recipe.minHeat
            else -> 0
        }

        // 加热：1 EU/t，不受超频影响
        if (sync.heat < targetHeat && sync.amount >= CentrifugeSync.ENERGY_PER_TICK_HEATING) {
            val heatSpace = (targetHeat - sync.heat).coerceAtMost(CentrifugeSync.HEAT_RATE_PER_TICK)
            if (sync.consumeEnergy(CentrifugeSync.ENERGY_PER_TICK_HEATING) > 0L) {
                sync.heat = (sync.heat + heatSpace).coerceAtMost(CentrifugeSync.HEAT_MAX)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                markDirty()
            }
        }
        // 无红石时热量衰减至 targetHeat
        if (!redstoneAllowsRun && sync.heat > targetHeat) {
            sync.heat = (sync.heat - CentrifugeSync.HEAT_RATE_PER_TICK).coerceAtLeast(targetHeat)
            markDirty()
        }

        if (input.isEmpty) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val r = recipe ?: run {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.heat < r.minHeat) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (!canAcceptOutputs(r.outputs)) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (progressF >= CentrifugeSync.PROGRESS_MAX) {
            input.decrement(r.inputCount)
            for (i in r.outputs.indices) {
                val out = r.outputs[i].copy()
                val slotIdx = SLOT_OUTPUT_1 + i
                val existing = getStack(slotIdx)
                if (existing.isEmpty) setStack(slotIdx, out)
                else existing.increment(out.count)
            }
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
            markDirty()
            // 完工不振荡（2026-08-26 perf）：下 tick 早退分支自然关灯
            sync.syncCurrentTickFlow()
            return
        }

        // 加工阶段耗能记账：1.6^n 按浮点累计，取整数部分消费、余数结转（EU 本身是整数）
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, CentrifugeSync.ENERGY_PER_TICK_PROCESSING * energyMultiplier, sync.getEffectiveCapacity())
        val need = energyDebtF.toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            energyDebtF -= need
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
            progressF = (progressF + speedMultiplier).coerceAtMost(CentrifugeSync.PROGRESS_MAX.toFloat())
            sync.progress = progressF.toInt()
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            // 停顿期间不累计债务：防恢复供电后 need 超容量 all-or-nothing 卡死
            energyDebtF = 0f
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun canAcceptOutputs(outputs: List<ItemStack>): Boolean {
        for (i in outputs.indices) {
            val out = outputs[i]
            val slotIdx = SLOT_OUTPUT_1 + i
            val existing = getStack(slotIdx)
            val canAccept = existing.isEmpty ||
                (ItemStack.areItemsEqual(existing, out) && existing.count + out.count <= out.maxCount)
            if (!canAccept) return false
        }
        return true
    }

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

    // 配方缓存——安全处理多对一配方（inputCount > 1）：
    // CentrifugeRecipe.matches 检查 stack.count >= inputCount，匹配结果依赖输入数量。
    // 规则：非 null 缓存仅在 input.count >= recipe.inputCount 时有效；
    // null 缓存仅在输入数量未增加时有效（数量增加可能让此前不足量的配方匹配）。
    private var cachedRecipeItem: net.minecraft.item.Item? = null
    private var cachedRecipe: CentrifugeRecipe? = null
    private var cachedRecipeQueryCount = 0
    private var cachedRecipeEpoch = -1

    /**
     * 获取当前输入的配方
     */
    private fun getRecipe(): CentrifugeRecipe? {
        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) return null

        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) {
            val cached = cachedRecipe
            if (cached != null && input.count >= cached.inputCount) return cached
            if (cached == null && input.count <= cachedRecipeQueryCount) return null
        }

        val inv = SimpleInventory(input)
        val recipeManager = world?.recipeManager ?: return null

        val optionalRecipe = recipeManager.getFirstMatch(
            getRecipeType<CentrifugeRecipe>(),
            inv,
            world ?: return null
        )

        val recipe = optionalRecipe.orElse(null)
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipe = recipe
        cachedRecipeQueryCount = input.count
        return recipe
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    // 类型判定缓存：isRecipeInput 始终用 maxCount 查询（仅依赖物品类型），按 item 缓存安全。
    private var cachedInputItem: net.minecraft.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(stack.maxCount))
        val found = w.recipeManager.getFirstMatch(getRecipeType<CentrifugeRecipe>(), inv, w).isPresent
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
    }
    /** P1（2026-08-26）：pulling 候选快速否定——(配方类型, Item) 布尔缓存零分配作答。
     *  返回 false = 该 item 必然不被输入槽接受；true = 值得走完整 isValid 裁决。 */
    override fun isValidInputItem(item: net.minecraft.item.Item): Boolean {
        if (isBatteryItem(net.minecraft.item.ItemStack(item, 1))) return false
        val w = world as? net.minecraft.server.world.ServerWorld ?: return true
        return ic2_120.content.RecipeInputLookupCache.hasAnyRecipe(getRecipeType<CentrifugeRecipe>(), item, w)
    }

}
