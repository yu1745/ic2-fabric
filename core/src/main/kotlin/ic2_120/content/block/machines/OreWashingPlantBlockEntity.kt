package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.RecipeCacheEpoch
import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.fluid.ModFluids
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.WaterCell
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.recipes.orewashing.OreWashingRecipe
import ic2_120.content.recipes.orewashing.OreWashingRecipeSerializer
import ic2_120.content.screen.OreWashingPlantScreenHandler
import ic2_120.content.sync.OreWashingPlantSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyDebtAccounting
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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.Ic2_120
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

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
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = OreWashingPlantBlock.ACTIVE

    override fun getInventory(): net.minecraft.world.Container = this

    // 流体管道升级支持属性（IFluidPipeUpgradeSupport 接口实现）
    override var fluidPipeProviderEnabled: Boolean = false  // 是否作为 provider 向管道输出流体
    override var fluidPipeReceiverEnabled: Boolean = false  // 是否作为 receiver 从管道接收流体
    override var fluidPipeProviderFilter: net.minecraft.world.level.material.Fluid? = null     // provider 流体过滤器（null = 不过滤）
    override var fluidPipeReceiverFilter: net.minecraft.world.level.material.Fluid? = null    // receiver 流体过滤器（null = 不过滤）
    override var fluidPipeProviderSides: MutableSet<net.minecraft.core.Direction> = mutableSetOf()    // provider 工作面（null = 任意面）
    override var fluidPipeReceiverSides: MutableSet<net.minecraft.core.Direction> = mutableSetOf()    // receiver 工作面（null = 任意面）
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override val tier: Int = ORE_WASHING_PLANT_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

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

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_ORE), matcher = { isCrushedOreInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_WATER), matcher = { isWaterInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3, SLOT_OUTPUT_EMPTY),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = OreWashingPlantSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ORE_WASHING_PLANT_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    // 水储罐（8 桶 = 648,000 droplets）
    private val waterTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        override fun canInsert(variant: FluidVariant): Boolean =
            (variant.fluid == Fluids.WATER || variant.fluid == Fluids.FLOWING_WATER) && !variant.isBlank

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            setChanged()
        }

        fun setStoredWater(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount

        fun remainingSpace(): Long = tankCapacity - amount

        fun hasAtLeastOneBucket(): Boolean = amount >= FluidConstants.BUCKET && variant.fluid == Fluids.WATER

        /** 内部消耗水（用于加工），返回实际消耗的 droplet 数 */
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.WATER) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
                sync.waterAmount = amount.toInt().coerceAtLeast(0)
                actual
            }
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
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_ORE -> isCrushedOreInput(stack)
        SLOT_INPUT_WATER -> isWaterInput(stack)
        SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3, SLOT_OUTPUT_EMPTY -> false
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

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.ore_washing_plant")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        OreWashingPlantScreenHandler(syncId, playerInventory, this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(OreWashingPlantSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloat("ProgressF").coerceIn(0f, OreWashingPlantSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, OreWashingPlantSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloat("EnergyDebt").coerceAtLeast(0f) else 0f
        waterTankInternal.setStoredWater(nbt.getLong(NBT_WATER_AMOUNT))
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(OreWashingPlantSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
        nbt.putLong(NBT_WATER_AMOUNT, waterTankInternal.getStoredAmount())
    }

    private fun getFluidStorageForSide(side: net.minecraft.core.Direction?): Storage<FluidVariant>? {
        // 前方不提供流体存储
        val facing = world?.getBlockState(pos)?.getValue(OreWashingPlantBlock.ACTIVE)
        val front = when (facing) {
            true -> net.minecraft.core.Direction.SOUTH
            false -> net.minecraft.core.Direction.NORTH
            else -> null
        }
        if (side == front) return null
        return waterTank
    }

    // 配方缓存：OreWashingRecipe.matches 只检查物品类型（不看数量），结果仅依赖
    // item，因此按 item 缓存（含 null）安全，避免每 tick 重复查询 recipeManager。
    private var cachedRecipeItem: net.minecraft.world.item.Item? = null
    private var cachedRecipe: OreWashingRecipe? = null
    private var cachedRecipeEpoch = -1

    /**
     * 获取当前输入的配方
     */
    private fun getRecipeForInput(input: ItemStack): OreWashingRecipe? {
        if (input.isEmpty) return null
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) return cachedRecipe

        val inv = OreWashingRecipe.Input(input)
        val recipeManager = world?.recipeManager ?: return null

        val recipe = recipeManager.getRecipeFor(getRecipeType<OreWashingRecipe>(), inv, world ?: return null).orElse(null)?.value
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipe = recipe
        return recipe
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

        // 应用升级效果
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, waterTankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, waterTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        // 从相邻方块或导线提取能量
        adjacentEnergyTransfer.tick()

        // 从放电槽提取能量
        extractFromDischargingSlot()

        // 处理水输入槽（水桶/水单元 -> 储罐）
        extractFromWaterInputSlot()

        // 检查输入物品
        val input = getItem(SLOT_INPUT_ORE)
        if (input.isEmpty) {
            resetProgress()
            currentRecipe = null
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 检查配方
        val recipe = getRecipeForInput(input) ?: run {
            resetProgress()
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
        val output1Slot = getItem(SLOT_OUTPUT_1)
        val output2Slot = getItem(SLOT_OUTPUT_2)
        val output3Slot = getItem(SLOT_OUTPUT_3)
        val outputEmptySlot = getItem(SLOT_OUTPUT_EMPTY)

        val canAccept1 = output1Slot.isEmpty() ||
            (ItemStack.isSameItem(output1Slot, output1) && output1Slot.count + output1.count <= output1.maxStackSize)
        val canAccept2 = output2Slot.isEmpty() ||
            (ItemStack.isSameItem(output2Slot, output2) && output2Slot.count + output2.count <= output2.maxStackSize)
        val canAccept3 = output3Slot.isEmpty() ||
            (ItemStack.isSameItem(output3Slot, output3) && output3Slot.count + output3.count <= output3.maxStackSize)

        if (!canAccept1 || !canAccept2 || !canAccept3) {
            resetProgress()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 加工完成
        if (progressF >= OreWashingPlantSync.PROGRESS_MAX) {
            input.shrink(1)
            // 放入输出槽
            if (output1Slot.isEmpty()) setItem(SLOT_OUTPUT_1, output1)
            else if (!output1.isEmpty) output1Slot.grow(output1.count)

            if (output2Slot.isEmpty()) setItem(SLOT_OUTPUT_2, output2)
            else if (!output2.isEmpty) output2Slot.grow(output2.count)

            if (output3Slot.isEmpty()) setItem(SLOT_OUTPUT_3, output3)
            else if (!output3.isEmpty) output3Slot.grow(output3.count)

            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
            setChanged()
            sync.syncCurrentTickFlow()
            return
        }

       // 消耗能量并增加进度（浮点记账：1.6^n 小数余数跨 tick 结转）
        val currentProgress = progressF.toInt()
        val nextProgressF = (progressF + speedMultiplier).coerceAtMost(OreWashingPlantSync.PROGRESS_MAX.toFloat())
        val nextProgress = nextProgressF.toInt()
        val waterNeed = waterNeededForProgressRange(currentProgress, nextProgress)
        // 先检查水是否充足，不足则不消耗能量
        if (waterTankInternal.getStoredAmount() < waterNeed) {
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, OreWashingPlantSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
        val need = energyDebtF.toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            energyDebtF -= need
            waterTankInternal.consumeInternal(waterNeed)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            progressF = nextProgressF
            sync.progress = nextProgress
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

    private fun extractFromWaterInputSlot() {
        val waterInput = getItem(SLOT_INPUT_WATER)
        if (waterInput.isEmpty) return

        // 检查储罐是否有空间
        val space = waterTankInternal.remainingSpace().coerceAtLeast(0L)
        if (space < FluidConstants.BUCKET) return

        val emptySlot = getItem(SLOT_OUTPUT_EMPTY)
        val emptyStack = when (waterInput.item) {
            Items.WATER_BUCKET -> ItemStack(Items.BUCKET)
            is WaterCell -> {
                val cellId = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "empty_cell")
                ItemStack(BuiltInRegistries.ITEM.get(cellId))
            }
            else -> return
        }

        // 检查空容器槽是否有空间
        val canAcceptEmpty = emptySlot.isEmpty() ||
            (ItemStack.isSameItem(emptySlot, emptyStack) && emptySlot.count + 1 <= emptyStack.maxStackSize)

        if (!canAcceptEmpty) return

        // 插入水到储罐（1 桶 = 1000mB）
        waterTankInternal.setStoredWater(waterTankInternal.amount + FluidConstants.BUCKET)

        // 消耗水输入
        waterInput.shrink(1)
        if (waterInput.isEmpty) setItem(SLOT_INPUT_WATER, ItemStack.EMPTY)

        // 放入空容器
        if (emptySlot.isEmpty) setItem(SLOT_OUTPUT_EMPTY, emptyStack)
        else emptySlot.grow(1)

        setChanged()
    }

    private fun waterNeededForProgressRange(fromProgress: Int, toProgress: Int): Long {
        if (toProgress <= fromProgress) return 0L

        // 使用当前配方的水消耗量，默认 1 桶 = 81000 droplets
        val perOperation = currentRecipe?.waterConsumptionDroplets ?: FluidConstants.BUCKET

        val max = OreWashingPlantSync.PROGRESS_MAX.toLong().coerceAtLeast(1L)
        val before = perOperation * fromProgress.toLong() / max
        val after = perOperation * toProgress.toLong() / max
        return (after - before).coerceAtLeast(0L)
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    private fun isCrushedOreInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || stack.item is IBatteryItem) return false
        return getRecipeForInput(stack) != null
    }

    private fun isWaterInput(stack: ItemStack): Boolean =
        !stack.isEmpty && (stack.item == Items.WATER_BUCKET || stack.item is WaterCell)
}
