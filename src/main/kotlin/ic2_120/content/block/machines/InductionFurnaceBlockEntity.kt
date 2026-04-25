package ic2_120.content.block.machines

import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.InductionFurnaceScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
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

import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

/**
 * 感应炉方块实体。支持双槽同时烧制，热量机制控制加工速度。
 *
 * 热量机制：
 * - 持续红石信号 → 热量上升（消耗 1 EU/t 维持热量）
 * - 无红石信号 → 热量衰减（不消耗额外能量）
 * - 热量越高，加工速度越快（0% = 不加工，100% = 全速）
 *
 * 每个物品在热量 100% 时总能耗为 150 EU + 加热能耗，与热量无关。
 */
@ModBlockEntity(block = InductionFurnaceBlock::class)
class InductionFurnaceBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, ExtendedScreenHandlerFactory {

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

    companion object {
        const val INDUCTION_TIER = 2  // MV
        const val SLOT_INPUT_0 = 0
        const val SLOT_INPUT_1 = 1
        const val SLOT_OUTPUT_0 = 2
        const val SLOT_OUTPUT_1 = 3
        const val SLOT_DISCHARGING = 4
        const val INVENTORY_SIZE = 5
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_0), matcher = { isSmeltingInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_1), matcher = { isSmeltingInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_0, SLOT_OUTPUT_1),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = InductionFurnaceSync(syncedData) { world?.time }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < sync.capacity }
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
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
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

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(InductionFurnaceSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(InductionFurnaceSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 从相邻导线或电池槽提取能量
        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        // === 热量管理 ===
        val isRedstonePowered = world.isReceivingRedstonePower(pos)
        val currentHeat = sync.heat

        if (isRedstonePowered) {
            // 有红石信号：热量上升，最高 100%
            if (currentHeat < InductionFurnaceSync.HEAT_MAX) {
                sync.heat = (currentHeat + InductionFurnaceSync.HEAT_CHANGE_PER_TICK)
                    .coerceAtMost(InductionFurnaceSync.HEAT_MAX)
                // 维持热量消耗 1 EU/t
                sync.consumeEnergy(InductionFurnaceSync.MAX_HEAT_ENERGY_PER_TICK)
            }
        } else {
            // 无红石信号：热量衰减，最低 0%
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

    /**
     * 处理两个槽位。
     * 返回是否有任意槽位正在加工。
     */
    private fun processBothSlots(world: World, heat: Int): Boolean {
        var slot0Working = false
        var slot1Working = false

        if (heat >= InductionFurnaceSync.MIN_HEAT_THRESHOLD) {
            // 槽 0
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

            // 槽 1
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

        // 如果热量低于阈值，重置进度
        if (heat < InductionFurnaceSync.MIN_HEAT_THRESHOLD) {
            if (sync.progressSlot0 != 0) sync.progressSlot0 = 0
            if (sync.progressSlot1 != 0) sync.progressSlot1 = 0
        }

        return slot0Working || slot1Working
    }

    /** 处理槽 0 */
    private fun processSlot0(heat: Int, result: ItemStack): Boolean {
        val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
        val euPerOp = InductionFurnaceSync.EU_PER_OPERATION
        val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).coerceAtLeast(baseTicks)

        val progress = sync.progressSlot0
        if (progress >= progressNeeded) {
            // 加工完成
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

        // 每 tick 消耗能量（每个物品总能耗固定为 euPerOp）
        val euPerTick = (euPerOp.toDouble() / progressNeeded).coerceAtLeast(1.0).toLong()
        if (sync.consumeEnergy(euPerTick) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progressSlot0 = progress + 1
            markDirty()
            return true
        }
        return false
    }

    /** 处理槽 1 */
    private fun processSlot1(heat: Int, result: ItemStack): Boolean {
        val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
        val euPerOp = InductionFurnaceSync.EU_PER_OPERATION
        val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).coerceAtLeast(baseTicks)

        val progress = sync.progressSlot1
        if (progress >= progressNeeded) {
            // 加工完成
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

    /**
     * 从放电槽提取能量（如果需要）
     */
    private fun extractFromDischargingSlot() {
        val space = (sync.capacity - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, InductionFurnaceSync.MAX_INSERT)
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

