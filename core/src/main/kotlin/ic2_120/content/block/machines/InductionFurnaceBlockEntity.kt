package ic2_120.content.block.machines

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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = InductionFurnaceBlock::class)
class InductionFurnaceBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport,
    IRedstoneInverterUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = InductionFurnaceBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.startStop(
        startSoundId = "machine.furnace.induction.start",
        stopSoundId = "machine.furnace.induction.stop",
        volume = 0.5f,
        pitch = 1.0f,
        loopSoundId = "machine.furnace.induction.loop",
        loopIntervalTicks = 20
    )

    override fun getInventory(): net.minecraft.inventory.Inventory = this

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

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is EjectorUpgrade || it.item is PullingUpgrade }),
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is RedstoneInverterUpgrade }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_0), matcher = { isSmeltingInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_1), matcher = { isSmeltingInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData(this)

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
        SLOT_INPUT_0, SLOT_INPUT_1 -> isSmeltingInput(stack)
        SLOT_OUTPUT_0, SLOT_OUTPUT_1 -> false
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE
        SLOT_UPGRADE_0, SLOT_UPGRADE_1 -> stack.item is EjectorUpgrade || stack.item is PullingUpgrade
            || stack.item is RedstoneInverterUpgrade
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.induction_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        InductionFurnaceScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(InductionFurnaceSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(InductionFurnaceSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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
        var newActive = state.get(activeProperty)

        // Phase 1: completion — vanilla: if (heat == 0) newActive=false; if (progress >= 4000) { operate(); progress = 0; }
        if (heat == 0) newActive = false
        if (progress >= InductionFurnaceSync.PROGRESS_THRESHOLD) {
            operateBothSlots(world)
            progress = 0
            newActive = false
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
    private fun canOperate(world: World): Boolean =
        canOperateSlot(world, SLOT_INPUT_0, SLOT_OUTPUT_0) ||
        canOperateSlot(world, SLOT_INPUT_1, SLOT_OUTPUT_1)

    private fun canOperateSlot(world: World, inputSlot: Int, outputSlot: Int): Boolean {
        val input = getStack(inputSlot)
        if (input.isEmpty) return false
        val recipe = findSmeltingRecipe(world, input) ?: return false
        return canAcceptOutput(getStack(outputSlot), recipe)
    }

    /** Vanilla operate(): process both input/output pairs simultaneously (shared progress). */
    private fun operateBothSlots(world: World) {
        operateSlot(world, SLOT_INPUT_0, SLOT_OUTPUT_0)
        operateSlot(world, SLOT_INPUT_1, SLOT_OUTPUT_1)
    }

    private fun operateSlot(world: World, inputSlot: Int, outputSlot: Int) {
        val input = getStack(inputSlot)
        if (input.isEmpty) return
        val recipe = findSmeltingRecipe(world, input) ?: return
        val output = getStack(outputSlot)
        if (!canAcceptOutput(output, recipe)) return
        input.decrement(1)
        if (output.isEmpty) {
            setStack(outputSlot, recipe.copy())
        } else {
            output.increment(recipe.count)
        }
        markDirty()
    }

    private fun findSmeltingRecipe(world: World, input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        val inv = SimpleInventory(1).apply { setStack(0, input) }
        val match = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inv, world)
        return if (match.isPresent) {
            match.get().getOutput(world.registryManager).copy()
        } else null
    }

    private fun canAcceptOutput(currentOutput: ItemStack, recipeOutput: ItemStack): Boolean {
        return currentOutput.isEmpty ||
            (ItemStack.areItemsEqual(currentOutput, recipeOutput) &&
                currentOutput.count + recipeOutput.count <= currentOutput.maxCount)
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

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isSmeltingInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(1))
        return w.recipeManager.getFirstMatch(RecipeType.SMELTING, inv, w).isPresent
    }
}
