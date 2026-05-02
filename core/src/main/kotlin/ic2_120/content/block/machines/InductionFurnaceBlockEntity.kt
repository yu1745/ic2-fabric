package ic2_120.content.block.machines

import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.InductionFurnaceScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
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
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport,
    ExtendedScreenHandlerFactory {

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
        const val SLOT_UPGRADE_2 = 7
        const val SLOT_UPGRADE_3 = 8
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_0, SLOT_INPUT_1)
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
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_0), matcher = { isSmeltingInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_1), matcher = { isSmeltingInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = InductionFurnaceSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(INDUCTION_TIER + voltageTierBonus) }
    )

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
        SLOT_DISCHARGING -> isBatteryItem(stack)
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
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
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(InductionFurnaceSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(InductionFurnaceSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val isRedstonePowered = world.isReceivingRedstonePower(pos)
        val currentHeat = sync.heat

        if (isRedstonePowered) {
            if (currentHeat < InductionFurnaceSync.HEAT_MAX) {
                sync.heat = (currentHeat + InductionFurnaceSync.HEAT_CHANGE_PER_TICK)
                    .coerceAtMost(InductionFurnaceSync.HEAT_MAX)
                sync.consumeEnergy(InductionFurnaceSync.MAX_HEAT_ENERGY_PER_TICK)
            }
        } else {
            if (currentHeat > 0) {
                sync.heat = (currentHeat - InductionFurnaceSync.HEAT_CHANGE_PER_TICK)
                    .coerceAtLeast(0)
            }
        }

        val heat = sync.heat
        val isActive = processBothSlots(world, heat)

        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, isActive)
    }

    private fun processBothSlots(world: World, heat: Int): Boolean {
        var slot0Working = false
        var slot1Working = false

        if (heat >= InductionFurnaceSync.MIN_HEAT_THRESHOLD) {
            val input0 = getStack(SLOT_INPUT_0)
            if (!input0.isEmpty) {
                val recipe0 = findSmeltingRecipe(world, input0)
                if (recipe0 != null) {
                    val outputSlot0 = getStack(SLOT_OUTPUT_0)
                    if (canAcceptOutput(outputSlot0, recipe0)) {
                        slot0Working = processSlot0(heat, recipe0)
                    }
                }
            }

            val input1 = getStack(SLOT_INPUT_1)
            if (!input1.isEmpty) {
                val recipe1 = findSmeltingRecipe(world, input1)
                if (recipe1 != null) {
                    val outputSlot1 = getStack(SLOT_OUTPUT_1)
                    if (canAcceptOutput(outputSlot1, recipe1)) {
                        slot1Working = processSlot1(heat, recipe1)
                    }
                }
            }
        }

        if (heat < InductionFurnaceSync.MIN_HEAT_THRESHOLD) {
            if (sync.progressSlot0 != 0) sync.progressSlot0 = 0
            if (sync.progressSlot1 != 0) sync.progressSlot1 = 0
        }

        return slot0Working || slot1Working
    }

    private fun processSlot0(heat: Int, result: ItemStack): Boolean {
        val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
        val euPerOp = InductionFurnaceSync.EU_PER_OPERATION
        val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).coerceAtLeast(baseTicks)

        val progress = sync.progressSlot0
        if (progress >= progressNeeded) {
            val input = getStack(SLOT_INPUT_0)
            input.decrement(1)
            val out = getStack(SLOT_OUTPUT_0)
            if (out.isEmpty) {
                setStack(SLOT_OUTPUT_0, result.copy())
            } else {
                out.increment(result.count)
            }
            sync.progressSlot0 = 0
            markDirty()
            return false
        }

        val euPerTick = (euPerOp.toDouble() / progressNeeded).coerceAtLeast(1.0).toLong()
        if (sync.consumeEnergy(euPerTick) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progressSlot0 = progress + 1
            markDirty()
            return true
        }
        return false
    }

    private fun processSlot1(heat: Int, result: ItemStack): Boolean {
        val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
        val euPerOp = InductionFurnaceSync.EU_PER_OPERATION
        val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).coerceAtLeast(baseTicks)

        val progress = sync.progressSlot1
        if (progress >= progressNeeded) {
            val input = getStack(SLOT_INPUT_1)
            input.decrement(1)
            val out = getStack(SLOT_OUTPUT_1)
            if (out.isEmpty) {
                setStack(SLOT_OUTPUT_1, result.copy())
            } else {
                out.increment(result.count)
            }
            sync.progressSlot1 = 0
            markDirty()
            return false
        }

        val euPerTick = (euPerOp.toDouble() / progressNeeded).coerceAtLeast(1.0).toLong()
        if (sync.consumeEnergy(euPerTick) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progressSlot1 = progress + 1
            markDirty()
            return true
        }
        return false
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
        if (space <= 0L) return

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
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
