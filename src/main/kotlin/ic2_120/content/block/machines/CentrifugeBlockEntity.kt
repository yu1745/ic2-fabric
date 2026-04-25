package ic2_120.content.block.machines

import ic2_120.content.block.CentrifugeBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.recipes.centrifuge.CentrifugeRecipe
import ic2_120.content.recipes.centrifuge.CentrifugeRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.screen.CentrifugeScreenHandler
import ic2_120.content.sync.CentrifugeSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
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
import net.minecraft.nbt.NbtCompound

import net.minecraft.recipe.RecipeManager
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

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
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CentrifugeBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = CentrifugeSync.CENTRIFUGE_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

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
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = CentrifugeSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CentrifugeSync.CENTRIFUGE_TIER + voltageTierBonus) }
    )

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
        SLOT_DISCHARGING -> isBatteryItem(stack)
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: RegistryByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.centrifuge")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CentrifugeScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CentrifugeSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.heat = nbt.getInt("Heat").coerceIn(0, CentrifugeSync.HEAT_MAX)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CentrifugeSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val input = getStack(SLOT_INPUT)
        val hasRedstone = world.isReceivingRedstonePower(pos)

        // 热量逻辑：有红石时加热至 5000；无红石时降至当前配方最低热量
        val recipe = getRecipe()
        val targetHeat = when {
            hasRedstone -> CentrifugeSync.HEAT_MAX
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
        if (!hasRedstone && sync.heat > targetHeat) {
            sync.heat = (sync.heat - CentrifugeSync.HEAT_RATE_PER_TICK).coerceAtLeast(targetHeat)
            markDirty()
        }

        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val r = recipe ?: run {
            if (sync.progress != 0) sync.progress = 0
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
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.progress >= CentrifugeSync.PROGRESS_MAX) {
            input.decrement(r.inputCount)
            for (i in r.outputs.indices) {
                val out = r.outputs[i].copy()
                val slotIdx = SLOT_OUTPUT_1 + i
                val existing = getStack(slotIdx)
                if (existing.isEmpty) setStack(slotIdx, out)
                else existing.increment(out.count)
            }
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        val need = (CentrifugeSync.ENERGY_PER_TICK_PROCESSING * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += progressIncrement
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
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
        if (space <= 0L) return
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    /**
     * 获取当前输入的配方
     */
    private fun getRecipe(): CentrifugeRecipe? {
        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) return null

        val inv = SimpleInventory(input)
        val recipeManager = world?.recipeManager ?: return null

        val optionalRecipe = recipeManager.getFirstMatch(
            getRecipeType<CentrifugeRecipe>(),
            inv,
            world ?: return null
        )

        return optionalRecipe.orElse(null)
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(1))
        return w.recipeManager.getFirstMatch(getRecipeType<CentrifugeRecipe>(), inv, w).isPresent
    }
}
