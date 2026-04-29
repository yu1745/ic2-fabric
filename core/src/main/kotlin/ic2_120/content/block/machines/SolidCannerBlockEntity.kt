package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.SolidCannerBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.recipes.solidcanner.SolidCannerRecipe
import ic2_120.content.recipes.solidcanner.SolidCannerRecipeSerializer
import ic2_120.content.screen.SolidCannerScreenHandler
import ic2_120.content.sync.SolidCannerSync
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
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.EmptyTinCanItem
import ic2_120.content.item.FilledTinCanItem
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.registry.instance
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
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
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.recipe.input.RecipeInput
import net.minecraft.nbt.NbtCompound

import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 固体装罐机方块实体。
 * 槽位：锡罐、食物、输出、放电、4 升级
 */
@ModBlockEntity(block = SolidCannerBlock::class)
@ModMachineRecipeBinding(SolidCannerRecipeSerializer::class)
class SolidCannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = SolidCannerBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = SolidCannerSync.SOLID_CANNER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val SOLID_CANNER_TIER = 1
        const val SLOT_TIN_CAN = 0
        const val SLOT_FOOD = 1
        const val SLOT_OUTPUT = 2
        const val SLOT_DISCHARGING = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_TIN_CAN, SLOT_FOOD)
        const val INVENTORY_SIZE = 8
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
            ItemInsertRoute(intArrayOf(SLOT_TIN_CAN), matcher = { isContainerInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_FOOD), matcher = { isFoodInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = SolidCannerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(SOLID_CANNER_TIER + voltageTierBonus) }
    )

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { SOLID_CANNER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        SolidCannerBlockEntity::class.type(),
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
        SLOT_TIN_CAN -> isContainerInput(stack)
        SLOT_FOOD -> isFoodInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack)
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.solid_canner")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SolidCannerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)

        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(SolidCannerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(SolidCannerSync.NBT_ENERGY_STORED, sync.amount)
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

        val tinCan = getStack(SLOT_TIN_CAN)
        val food = getStack(SLOT_FOOD)
        if (tinCan.isEmpty || food.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val recipeInput = object : RecipeInput {
            override fun getStackInSlot(slot: Int) = when (slot) {
                0 -> tinCan.copyWithCount(1)
                1 -> food.copyWithCount(1)
                else -> ItemStack.EMPTY
            }
            override fun getSize() = 2
            override fun isEmpty() = false
        }
        val match = world.recipeManager.getFirstMatch(getRecipeType<SolidCannerRecipe>(), recipeInput, world)

        val result: ItemStack
        val slot0InputCount: Int
        val slot1InputCount: Int

        if (match.isPresent) {
            val recipe = match.get().value()
            result = recipe.output.copy()
            slot0InputCount = recipe.slot0Count
            slot1InputCount = recipe.slot1Count
        } else if (tinCan.item is EmptyTinCanItem && food.get(DataComponentTypes.FOOD) != null) {
            val canCount = food.get(DataComponentTypes.FOOD)!!.nutrition.coerceAtLeast(1)
            result = ItemStack(FilledTinCanItem::class.instance(), canCount)
            slot0InputCount = canCount
            slot1InputCount = 1
        } else {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }
        if (tinCan.count < slot0InputCount || food.count < slot1InputCount) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.progress >= SolidCannerSync.PROGRESS_MAX) {
            tinCan.decrement(slot0InputCount)
            food.decrement(slot1InputCount)
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result.copy())
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        val need = (SolidCannerSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
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

    private fun isContainerInput(stack: ItemStack): Boolean =
        !stack.isEmpty && (
            stack.item == Registries.ITEM.get(Identifier.of("ic2_120", "tin_can")) ||
            stack.item is EmptyFuelRodItem
        )

    private fun isFoodInput(stack: ItemStack): Boolean =
        !stack.isEmpty && stack.item !is IBatteryItem && stack.item !is IUpgradeItem &&
            (world?.recipeManager?.values()?.any { it is SolidCannerRecipe && it.slot1Ingredient.test(stack) } == true
             || stack.get(DataComponentTypes.FOOD) != null)
}
