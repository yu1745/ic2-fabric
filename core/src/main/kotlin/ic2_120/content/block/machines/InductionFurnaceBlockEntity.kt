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
import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.InductionFurnaceScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.EjectorUpgrade
import ic2_120.content.item.PullingUpgrade
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.RedstoneInverterUpgrade
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.content.upgrade.IValidInputItem

@ModBlockEntity(block = InductionFurnaceBlock::class)
class InductionFurnaceBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport,
    IRedstoneInverterUpgradeSupport, ExtendedMenuProvider<FriendlyByteBuf>, IValidInputItem {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = InductionFurnaceBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.startStop(
        startSoundId = "machine.furnace.induction.start",
        stopSoundId = "machine.furnace.induction.stop",
        volume = 0.5f,
        pitch = 1.0f,
        loopSoundId = "machine.furnace.induction.loop",
        loopIntervalTicks = 20
    )

    override fun getInventory(): net.minecraft.world.Container = this

    override val tier: Int = INDUCTION_TIER

    override var capacityBonus: Long = 0L
    override var redstoneInverted: Boolean = false
    override var voltageTierBonus: Int = 0

    companion object {
        const val INDUCTION_TIER = 2
        const val SLOT_INPUT_0 = 0
        const val SLOT_INPUT_1 = 1
        const val SLOT_OUTPUT_0 = 2
        const val SLOT_OUTPUT_1 = 3
        const val SLOT_DISCHARGING = 4
        const val SLOT_UPGRADE_0 = 5
        const val SLOT_UPGRADE_1 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_0, SLOT_INPUT_1)
        const val INVENTORY_SIZE = 7
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is EjectorUpgrade || it.item is PullingUpgrade }),
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is RedstoneInverterUpgrade }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_0), matcher = { isSmeltingInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_1), matcher = { isSmeltingInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = InductionFurnaceSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(INDUCTION_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { INDUCTION_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        InductionFurnaceBlockEntity::class.type(),
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
        SLOT_INPUT_0, SLOT_INPUT_1 -> isSmeltingInput(stack)
        SLOT_OUTPUT_0, SLOT_OUTPUT_1 -> false
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
        SLOT_UPGRADE_0, SLOT_UPGRADE_1 -> stack.item is EjectorUpgrade || stack.item is PullingUpgrade
            || stack.item is RedstoneInverterUpgrade
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.induction_furnace")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        InductionFurnaceScreenHandler(
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
        sync.amount = nbt.getLongOr(InductionFurnaceSync.NBT_ENERGY_STORED, 0L)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBooleanOr("RedstoneInverted", false) else false
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(InductionFurnaceSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

       val isRedstonePowered = RedstoneControlComponent.canRun(world, pos, this)
        var heat = sync.heat
        var progress = sync.progress
        var newActive = state.getValue(activeProperty)

        // Phase 1: completion — vanilla: if (heat == 0) newActive=false; if (progress >= 4000) { operate(); progress = 0; }
        if (heat == 0) newActive = false
        if (progress >= InductionFurnaceSync.PROGRESS_THRESHOLD) {
            operateBothSlots(world)
            progress = 0
            // 完工不振荡（2026-08-26 perf）：不置 false，见 ElectricFurnace 同款注释
        }

        // Phase 2: heating / cooling
        // vanilla: if ((canOperate || redstone) && energy.useEnergy(1)) heat++; else heat -= min(heat, 4);
        val canOperate = canOperate(world)
        if ((canOperate || isRedstonePowered) && sync.consumeEnergy(InductionFurnaceSync.HEAT_ENERGY_PER_TICK) > 0L) {
            if (heat < InductionFurnaceSync.HEAT_MAX) heat += InductionFurnaceSync.HEAT_GAIN_PER_TICK
            newActive = true
        } else {
            heat -= minOf(heat, InductionFurnaceSync.HEAT_LOSS_PER_TICK)
        }
        sync.heat = heat

        // Phase 3: state machine — determines newActive (sound/active transitions)
        if (!newActive || progress == 0) {
            if (!canOperate) {
                progress = 0
            } else if (sync.amount >= InductionFurnaceSync.SMELT_ENERGY_PER_TICK) {
                newActive = true
            }
        } else if (!canOperate || sync.amount < InductionFurnaceSync.SMELT_ENERGY_PER_TICK) {
            if (!canOperate) progress = 0
            newActive = false
        }

        // Phase 4: smelting
        // vanilla: if (newActive && canOperate) { progress += heat / 30; energy.useEnergy(15); }
        if (newActive && canOperate) {
            progress += heat / 30
            sync.consumeEnergy(InductionFurnaceSync.SMELT_ENERGY_PER_TICK)
        }

        sync.progress = progress
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, newActive)
    }

    /** Vanilla canOperate(): either slot has a valid recipe with room for output. */
    private fun canOperate(world: Level): Boolean =
        canOperateSlot(world, SLOT_INPUT_0, SLOT_OUTPUT_0) ||
        canOperateSlot(world, SLOT_INPUT_1, SLOT_OUTPUT_1)

    private fun canOperateSlot(world: Level, inputSlot: Int, outputSlot: Int): Boolean {
        val input = getItem(inputSlot)
        if (input.isEmpty) return false
        val recipe = findSmeltingRecipe(world, input) ?: return false
        return canAcceptOutput(getItem(outputSlot), recipe)
    }

    /** Vanilla operate(): process both input/output pairs simultaneously (shared progress). */
    private fun operateBothSlots(world: Level) {
        operateSlot(world, SLOT_INPUT_0, SLOT_OUTPUT_0)
        operateSlot(world, SLOT_INPUT_1, SLOT_OUTPUT_1)
    }

    private fun operateSlot(world: Level, inputSlot: Int, outputSlot: Int) {
        val input = getItem(inputSlot)
        if (input.isEmpty) return
        val recipe = findSmeltingRecipe(world, input) ?: return
        val output = getItem(outputSlot)
        if (!canAcceptOutput(output, recipe)) return
        input.shrink(1)
        if (output.isEmpty) {
            setItem(outputSlot, recipe.copy())
        } else {
            output.grow(recipe.count)
        }
        setChanged()
    }

    // 配方缓存：SMELTING 配方 matches 只看物品类型（不看数量），按 item 缓存
    // （含 null）安全，避免每 tick 重复查询 recipeManager。
    private var cachedRecipeItem: net.minecraft.world.item.Item? = null
    private var cachedRecipeOutput: ItemStack? = null
    private var cachedRecipeEpoch = -1

    private fun findSmeltingRecipe(world: Level, input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) return cachedRecipeOutput
        val inv = net.minecraft.world.item.crafting.SingleRecipeInput(input)
        val match = world.recipeManager.getRecipeFor(RecipeType.SMELTING, inv, world)
        val result = if (match.isPresent) match.get().value.assemble(inv).copy() else null
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipeOutput = result
        return result
    }

    private fun canAcceptOutput(currentOutput: ItemStack, recipeOutput: ItemStack): Boolean {
        return currentOutput.isEmpty ||
            (ItemStack.isSameItem(currentOutput, recipeOutput) &&
                currentOutput.count + recipeOutput.count <= currentOutput.maxStackSize)
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

    // 类型判定缓存：isSmeltingInput 用 copyWithCount(1) 查询（配方只看物品类型），
    // 结果仅依赖 item，按 item 缓存安全。
    private var cachedInputItem: net.minecraft.world.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isSmeltingInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val w = world ?: return true
        val inv = net.minecraft.world.item.crafting.SingleRecipeInput(stack.copyWithCount(1))
        val found = w.recipeManager.getRecipeFor(RecipeType.SMELTING, inv, w).isPresent
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
        return ic2_120.content.RecipeInputLookupCache.hasAnyRecipe(RecipeType.SMELTING, item, w)
    }

}
