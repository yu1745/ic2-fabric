package ic2_120.content.block.machines

import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.WaterCell
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.recipes.orewashing.OreWashingRecipe
import ic2_120.content.recipes.orewashing.OreWashingRecipeSerializer
import ic2_120.content.screen.OreWashingPlantScreenHandler
import ic2_120.content.sync.OreWashingPlantSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

import net.minecraft.recipe.RecipeManager
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.Ic2_120
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 洗矿机方块实体。
 * - 输入槽1：粉碎矿石
 * - 输入槽2：水桶/水单元
 * - 输出槽1：纯净的粉碎矿石
 * - 输出槽2：石粉
 * - 输出槽3：小撮金属粉（2个）
 * - 输出槽4：空桶/空单元
 * - 电池槽：放电
 * - 升级槽：4个
 * - 流体：水（内部储罐8桶）
 * - 每次加工消耗：1桶水 + 8000 EU（2000 ticks @ 4 EU/t）
 *
 * 升级支持：
 * - 加速、储能、高压升级
 * - 流体抽取升级：作为 receiver 从管道接收水
 */
@ModBlockEntity(block = OreWashingPlantBlock::class)
@ModMachineRecipeBinding(OreWashingRecipeSerializer::class)
class OreWashingPlantBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = OreWashingPlantBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    // 流体管道升级支持属性（IFluidPipeUpgradeSupport 接口实现）
    override var fluidPipeProviderEnabled: Boolean = false  // 是否作为 provider 向管道输出流体
    override var fluidPipeReceiverEnabled: Boolean = false  // 是否作为 receiver 从管道接收流体
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null     // provider 流体过滤器（null = 不过滤）
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null    // receiver 流体过滤器（null = 不过滤）
    override var fluidPipeProviderSide: net.minecraft.util.math.Direction? = null    // provider 工作面（null = 任意面）
    override var fluidPipeReceiverSide: net.minecraft.util.math.Direction? = null    // receiver 工作面（null = 任意面）

    override val tier: Int = ORE_WASHING_PLANT_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val ORE_WASHING_PLANT_TIER = 1
        const val SLOT_INPUT_ORE = 0        // 粉碎矿石输入
        const val SLOT_INPUT_WATER = 1      // 水桶/水单元输入
        const val SLOT_OUTPUT_1 = 2         // 纯净的粉碎矿石
        const val SLOT_OUTPUT_2 = 3         // 石粉
        const val SLOT_OUTPUT_3 = 4         // 小撮金属粉
        const val SLOT_OUTPUT_EMPTY = 5     // 空桶/空单元
        const val SLOT_DISCHARGING = 6
        const val SLOT_UPGRADE_0 = 7
        const val SLOT_UPGRADE_1 = 8
        const val SLOT_UPGRADE_2 = 9
        const val SLOT_UPGRADE_3 = 10
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3, SLOT_OUTPUT_EMPTY)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_ORE)
        const val INVENTORY_SIZE = 11
        private const val NBT_WATER_AMOUNT = "WaterAmount"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = OreWashingPlantBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
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
            ItemInsertRoute(intArrayOf(SLOT_INPUT_ORE), matcher = { isCrushedOreInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_WATER), matcher = { isWaterInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3, SLOT_OUTPUT_EMPTY),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = OreWashingPlantSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ORE_WASHING_PLANT_TIER + voltageTierBonus) }
    )

    // 水储罐（8桶容量）
    private val waterTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == Fluids.WATER || variant.fluid == Fluids.FLOWING_WATER

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        fun getTankCapacity(): Long = tankCapacity

        fun setStoredWater(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount

        fun hasAtLeastOneBucket(): Boolean = amount >= FluidConstants.BUCKET && variant.fluid == Fluids.WATER

        /** 内部消耗水（用于加工） */
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.WATER) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    val waterTank: Storage<FluidVariant> = waterTankInternal

    // 当前加工配方的缓存（用于计算水消耗）
    private var currentRecipe: OreWashingRecipe? = null

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { ORE_WASHING_PLANT_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        OreWashingPlantBlockEntity::class.type(),
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
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_ORE -> isCrushedOreInput(stack)
        SLOT_INPUT_WATER -> isWaterInput(stack)
        SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3, SLOT_OUTPUT_EMPTY -> false
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

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.ore_washing_plant")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        OreWashingPlantScreenHandler(syncId, playerInventory, this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(OreWashingPlantSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        waterTankInternal.setStoredWater(nbt.getLong(NBT_WATER_AMOUNT))
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(OreWashingPlantSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_WATER_AMOUNT, waterTankInternal.getStoredAmount())
    }

    private fun getFluidStorageForSide(side: net.minecraft.util.math.Direction?): Storage<FluidVariant>? {
        // 前方不提供流体存储
        val facing = world?.getBlockState(pos)?.get(OreWashingPlantBlock.ACTIVE)
        val front = when (facing) {
            true -> net.minecraft.util.math.Direction.SOUTH
            false -> net.minecraft.util.math.Direction.NORTH
            else -> null
        }
        if (side == front) return null
        return waterTank
    }

    /**
     * 获取当前输入的配方
     */
    private fun getRecipeForInput(input: ItemStack): OreWashingRecipe? {
        if (input.isEmpty) return null

        val inv = OreWashingRecipe.Input(input)
        val recipeManager = world?.recipeManager ?: return null

        return recipeManager.getFirstMatch(getRecipeType<OreWashingRecipe>(), inv, world ?: return null).map { it.value }.orElse(null)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 应用升级效果
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, waterTankInternal, fluidPipeProviderFilter, fluidPipeProviderSide)
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        // 从相邻方块或导线提取能量
        pullEnergyFromNeighbors(world, pos, sync)

        // 从放电槽提取能量
        extractFromDischargingSlot()

        // 处理水输入槽（水桶/水单元 -> 储罐）
        extractFromWaterInputSlot()

        // 检查输入物品
        val input = getStack(SLOT_INPUT_ORE)
        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            currentRecipe = null
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 检查配方
        val recipe = getRecipeForInput(input) ?: run {
            if (sync.progress != 0) sync.progress = 0
            currentRecipe = null
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 缓存当前配方
        currentRecipe = recipe

        // 获取所有输出（补齐到3个）
        val allOutputs = OreWashingRecipe.getAllOutputs(recipe)
        val output1 = allOutputs[0]
        val output2 = allOutputs[1]
        val output3 = allOutputs[2]

        // 检查输出槽是否有空间
        val output1Slot = getStack(SLOT_OUTPUT_1)
        val output2Slot = getStack(SLOT_OUTPUT_2)
        val output3Slot = getStack(SLOT_OUTPUT_3)
        val outputEmptySlot = getStack(SLOT_OUTPUT_EMPTY)

        val canAccept1 = output1Slot.isEmpty() ||
            (ItemStack.areItemsEqual(output1Slot, output1) && output1Slot.count + output1.count <= output1.maxCount)
        val canAccept2 = output2Slot.isEmpty() ||
            (ItemStack.areItemsEqual(output2Slot, output2) && output2Slot.count + output2.count <= output2.maxCount)
        val canAccept3 = output3Slot.isEmpty() ||
            (ItemStack.areItemsEqual(output3Slot, output3) && output3Slot.count + output3.count <= output3.maxCount)

        if (!canAccept1 || !canAccept2 || !canAccept3) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 加工完成
        if (sync.progress >= OreWashingPlantSync.PROGRESS_MAX) {
            input.decrement(1)
            // 放入输出槽
            if (output1Slot.isEmpty()) setStack(SLOT_OUTPUT_1, output1)
            else if (!output1.isEmpty) output1Slot.increment(output1.count)

            if (output2Slot.isEmpty()) setStack(SLOT_OUTPUT_2, output2)
            else if (!output2.isEmpty) output2Slot.increment(output2.count)

            if (output3Slot.isEmpty()) setStack(SLOT_OUTPUT_3, output3)
            else if (!output3.isEmpty) output3Slot.increment(output3.count)

            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 消耗能量并增加进度
        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        val need = (OreWashingPlantSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            val currentProgress = sync.progress.coerceIn(0, OreWashingPlantSync.PROGRESS_MAX)
            val nextProgress = (currentProgress + progressIncrement).coerceAtMost(OreWashingPlantSync.PROGRESS_MAX)
            val waterNeed = waterNeededForProgressRange(currentProgress, nextProgress)
            if (waterTankInternal.consumeInternal(waterNeed) < waterNeed) {
                setActiveState(world, pos, state, false)
                sync.syncCurrentTickFlow()
                return
            }
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = nextProgress
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

    private fun extractFromWaterInputSlot() {
        val waterInput = getStack(SLOT_INPUT_WATER)
        if (waterInput.isEmpty) return

        // 检查储罐是否有空间
        val space = (waterTankInternal.getTankCapacity() - waterTankInternal.amount).coerceAtLeast(0L)
        if (space < FluidConstants.BUCKET) return

        val emptySlot = getStack(SLOT_OUTPUT_EMPTY)
        val emptyStack = when (waterInput.item) {
            Items.WATER_BUCKET -> ItemStack(Items.BUCKET)
            is WaterCell -> {
                val cellId = Identifier.of(Ic2_120.MOD_ID, "empty_cell")
                ItemStack(Registries.ITEM.get(cellId))
            }
            else -> return
        }

        // 检查空容器槽是否有空间
        val canAcceptEmpty = emptySlot.isEmpty() ||
            (ItemStack.areItemsEqual(emptySlot, emptyStack) && emptySlot.count + 1 <= emptyStack.maxCount)

        if (!canAcceptEmpty) return

        // 插入水到储罐
        val inserted = waterTankInternal.amount + FluidConstants.BUCKET
        waterTankInternal.amount = inserted.coerceAtMost(waterTankInternal.getTankCapacity())
        if (waterTankInternal.amount > 0L && waterTankInternal.variant.isBlank) {
            waterTankInternal.variant = FluidVariant.of(Fluids.WATER)
        }
        sync.waterAmountMb = (waterTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

        // 消耗水输入
        waterInput.decrement(1)
        if (waterInput.isEmpty) setStack(SLOT_INPUT_WATER, ItemStack.EMPTY)

        // 放入空容器
        if (emptySlot.isEmpty) setStack(SLOT_OUTPUT_EMPTY, emptyStack)
        else emptySlot.increment(1)

        markDirty()
    }

    private fun waterNeededForProgressRange(fromProgress: Int, toProgress: Int): Long {
        if (toProgress <= fromProgress) return 0L

        // 使用当前配方的水消耗量，默认1000mB（1桶）
        val perOperation = currentRecipe?.waterConsumptionMb ?: 1000L

        val max = OreWashingPlantSync.PROGRESS_MAX.toLong().coerceAtLeast(1L)
        val before = perOperation * fromProgress.toLong() / max
        val after = perOperation * toProgress.toLong() / max
        return (after - before).coerceAtLeast(0L)
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isCrushedOreInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || stack.item is IBatteryItem) return false
        val item = stack.item
        return item.toString().contains("crushed") ||
            Registries.ITEM.getId(item).path.contains("crushed")
    }

    private fun isWaterInput(stack: ItemStack): Boolean =
        !stack.isEmpty && (stack.item == Items.WATER_BUCKET || stack.item is WaterCell)
}
