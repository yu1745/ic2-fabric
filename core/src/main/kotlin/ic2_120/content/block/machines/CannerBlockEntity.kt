package ic2_120.content.block.machines

import net.minecraft.core.registries.BuiltInRegistries

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.RecipeCacheEpoch
import ic2_120.Ic2_120
import ic2_120.content.block.CannerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.recipes.solidcanner.SolidCannerRecipe
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.recipes.CannerMixingRecipes
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.screen.CannerScreenHandler
import ic2_120.content.sync.CannerSync
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
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
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
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 流体/固体装罐机方块实体。
 * 双储罐：左罐（灌入来源/混合输入）、右罐（倒出目标/混合输出）。
 * 槽位：左输入、中材料、右输出、放电、4 升级
 * 模式化工作：
 * - 排出模式：左槽满容器 → 左液槽，空容器从右槽输出
 * - 灌入模式：左槽空容器/中槽泡沫喷枪或Cf背包 → 右液槽灌装，满容器从右槽输出
 * - 流体混合：左液槽 + 中槽固体 → 优先灌入左槽空容器，余液进入右液槽
 * - 固体装罐：左槽锡罐 + 中槽食物 → 右槽成品
 */
@ModBlockEntity(block = CannerBlock::class)
class CannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = CannerBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.none()

    override fun getInventory(): net.minecraft.world.Container = this

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.world.level.material.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override val tier: Int = CannerSync.CANNER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

    companion object {

        // fabric 1.21.1：FluidVariant.fromNbt/toNbt 已移除，用 CODEC + NbtOps 桥接
        fun encodeFluidVariant(variant: net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant): net.minecraft.nbt.CompoundTag {
            val tag = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, variant)
                .getOrThrow()
            return if (tag is net.minecraft.nbt.CompoundTag) tag else net.minecraft.nbt.CompoundTag()
        }

        fun decodeFluidVariant(tag: net.minecraft.nbt.CompoundTag): net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant {
            if (tag.isEmpty) return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank()
            return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag)
                .result()
                .orElse(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank())
        }

        const val SLOT_INPUT = 0      // 左液槽顶部（通用输入：满容器/空容器/锡罐/空燃料棒）
        const val SLOT_MATERIAL = 1    // 中间槽（固体材料/泡沫喷枪/Cf背包）
        const val SLOT_OUTPUT = 2      // 右液槽底部（通用输出：满容器/空容器/成品罐头）
        const val SLOT_DISCHARGING = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT, SLOT_MATERIAL)
        const val INVENTORY_SIZE = 8

        private const val NBT_LEFT_FLUID_AMOUNT = "LeftFluidAmount"
        private const val NBT_LEFT_FLUID_VARIANT = "LeftFluidVariant"
        private const val NBT_RIGHT_FLUID_AMOUNT = "RightFluidAmount"
        private const val NBT_RIGHT_FLUID_VARIANT = "RightFluidVariant"
        private const val TANK_CAPACITY_BUCKETS = 8
        private val TANK_CAPACITY = FluidConstants.BUCKET * TANK_CAPACITY_BUCKETS

        private val tinCanItem by lazy { BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "tin_can")) }

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> (be as CannerBlockEntity).getFluidStorageForSide(side) }, CannerBlockEntity::class.type())
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
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { canPlaceItem(SLOT_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_MATERIAL), matcher = { canPlaceItem(SLOT_MATERIAL, it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_INPUT, SLOT_MATERIAL, SLOT_OUTPUT, SLOT_DISCHARGING),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = CannerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CannerSync.CANNER_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val leftTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = !variant.isBlank
        override fun canExtract(variant: FluidVariant): Boolean = true

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.leftFluidAmount = amount.toInt().coerceAtLeast(0)
            sync.leftFluidCapacity = TANK_CAPACITY.toInt()
            sync.leftFluidRawId = if (variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(variant.fluid)
            setChanged()
        }
    }

    private val rightTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = true
        override fun canExtract(variant: FluidVariant): Boolean = !variant.isBlank

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.rightFluidAmount = amount.toInt().coerceAtLeast(0)
            sync.rightFluidCapacity = TANK_CAPACITY.toInt()
            sync.rightFluidRawId = if (variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(variant.fluid)
            setChanged()
        }
    }

    val fluidTank: Storage<FluidVariant> = CombinedStorage(listOf(leftTankInternal, rightTankInternal))

    /** 对外（管道网络）只允许输入的左槽包装。 */
    private val leftTankInputOnly = object : Storage<FluidVariant> {
        override fun insert(variant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
            leftTankInternal.insert(variant, maxAmount, transaction)
        override fun extract(variant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = false
        override fun iterator(): MutableIterator<StorageView<FluidVariant>> = mutableListOf<StorageView<FluidVariant>>().iterator()
    }

    /** 对外（管道网络）只允许输出的右槽包装。 */
    private val rightTankOutputOnly = object : Storage<FluidVariant> {
        override fun insert(variant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
        override fun extract(variant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
            rightTankInternal.extract(variant, maxAmount, transaction)
        override fun supportsInsertion(): Boolean = false
        override fun supportsExtraction(): Boolean = true
        override fun iterator(): MutableIterator<StorageView<FluidVariant>> = rightTankInternal.iterator()
    }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { CannerSync.CANNER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(CannerBlockEntity::class.type(), pos, state)

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
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

    private fun cannerIsFilledFluidContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.item == Items.WATER_BUCKET || stack.item == Items.LAVA_BUCKET) return true
        val ctx = ContainerItemContext.withConstant(stack)
        val fluidItemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        for (view in fluidItemStorage) {
            if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) return true
        }
        return false
    }

    private fun cannerIsEmptyFluidContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.item == Items.BUCKET) return true
        val ctx = ContainerItemContext.withConstant(stack)
        val fluidItemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        return fluidItemStorage.supportsInsertion()
    }

    fun getLeftFluidAmount() = leftTankInternal.amount
    fun getRightFluidAmount() = rightTankInternal.amount

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> {
            if (stack.isEmpty || stack.item is IBatteryItem) return false
            when (sync.getMode()) {
                CannerSync.Mode.BOTTLE_SOLID -> stack.item == tinCanItem || stack.item is EmptyFuelRodItem
                CannerSync.Mode.EMPTY_LIQUID -> cannerIsFilledFluidContainer(stack)
                CannerSync.Mode.ENRICH_LIQUID ->
                    cannerIsFilledFluidContainer(stack) || cannerIsEmptyFluidContainer(stack)
                CannerSync.Mode.BOTTLE_LIQUID -> cannerIsEmptyFluidContainer(stack)
            }
        }
        SLOT_MATERIAL -> {
            if (stack.isEmpty || stack.item is IBatteryItem) return false
            when (sync.getMode()) {
                CannerSync.Mode.EMPTY_LIQUID -> false
                CannerSync.Mode.BOTTLE_LIQUID, CannerSync.Mode.EMPTY_LIQUID -> false
                else -> SolidCannerRecipe.slot1Ingredients(world!!).any { it.test(stack) } ||
                    CannerMixingRecipes.isMixingMaterial(stack.item)
            }
        }
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.count)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.canner")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        CannerScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CannerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloat("ProgressF").coerceIn(0f, CannerSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, CannerSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloat("EnergyDebt").coerceAtLeast(0f) else 0f
        leftTankInternal.amount = nbt.getLong(NBT_LEFT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val leftFluidTag = nbt.getCompound(NBT_LEFT_FLUID_VARIANT)
        leftTankInternal.variant = if (leftFluidTag.isEmpty) FluidVariant.blank() else decodeFluidVariant(leftFluidTag)
        rightTankInternal.amount = nbt.getLong(NBT_RIGHT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val rightFluidTag = nbt.getCompound(NBT_RIGHT_FLUID_VARIANT)
        rightTankInternal.variant = if (rightFluidTag.isEmpty) FluidVariant.blank() else decodeFluidVariant(rightFluidTag)
        sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
        sync.leftFluidCapacity = TANK_CAPACITY.toInt()
        sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(leftTankInternal.variant.fluid)
        sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
        sync.rightFluidCapacity = TANK_CAPACITY.toInt()
        sync.rightFluidRawId = if (rightTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(rightTankInternal.variant.fluid)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(CannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
        nbt.putLong(NBT_LEFT_FLUID_AMOUNT, leftTankInternal.amount)
        if (!leftTankInternal.variant.isBlank) nbt.put(NBT_LEFT_FLUID_VARIANT, encodeFluidVariant(leftTankInternal.variant))
        nbt.putLong(NBT_RIGHT_FLUID_AMOUNT, rightTankInternal.amount)
        if (!rightTankInternal.variant.isBlank) nbt.put(NBT_RIGHT_FLUID_VARIANT, encodeFluidVariant(rightTankInternal.variant))
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return CombinedStorage(listOf(leftTankInputOnly, rightTankOutputOnly))
    }

    /** 重置全部进度状态（操作不可用/模式切换/换罐时调用）。 */
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

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        val mode = sync.getMode()
        if (fluidPipeProviderEnabled && mode != CannerSync.Mode.BOTTLE_SOLID && mode != CannerSync.Mode.EMPTY_LIQUID) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, rightTankInternal, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }
        if (fluidPipeReceiverEnabled && (mode == CannerSync.Mode.EMPTY_LIQUID || mode == CannerSync.Mode.ENRICH_LIQUID)) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, leftTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        val operating = when (sync.getMode()) {
            CannerSync.Mode.BOTTLE_SOLID -> trySolidCanning(getItem(SLOT_INPUT), getItem(SLOT_MATERIAL), getItem(SLOT_OUTPUT))
            CannerSync.Mode.EMPTY_LIQUID -> tryPourOutToLeft(getItem(SLOT_INPUT))
            CannerSync.Mode.BOTTLE_LIQUID -> resolveBottleLiquidTargetSlot() != null
            CannerSync.Mode.ENRICH_LIQUID -> tryMixing(getItem(SLOT_MATERIAL))
        }

        if (operating) {
            // 耗能记账：1.6^n 按浮点累计，取整数部分消费、余数结转（EU 本身是整数）
            energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, CannerSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
            val need = energyDebtF.toLong().coerceAtLeast(1L)
            if (sync.consumeEnergy(need) > 0L) {
                energyDebtF -= need
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
                progressF = (progressF + speedMultiplier).coerceAtMost(CannerSync.PROGRESS_MAX.toFloat())
                sync.progress = progressF.toInt()
                if (progressF >= CannerSync.PROGRESS_MAX) {
                    completeCurrentOperationByMode()
                    progressF = 0f
                    energyDebtF = 0f
                    sync.progress = 0
                }
                setChanged()
                setActiveState(world, pos, state, true)
            } else {
                // 停顿期间不累计债务：防恢复供电后 need 超容量 all-or-nothing 卡死
                energyDebtF = 0f
                setActiveState(world, pos, state, false)
            }
        } else {
            resetProgress()
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    fun cycleMode() {
        sync.cycleMode()
        resetProgress()
        setChanged()
    }

    fun swapTanks(): Boolean {
        val beforeLeft = leftTankInternal.amount
        val beforeRight = rightTankInternal.amount
        val beforeLeftVariant = leftTankInternal.variant
        val beforeRightVariant = rightTankInternal.variant
        Transaction.openOuter().use { tx ->
            val leftVariant = leftTankInternal.variant
            val rightVariant = rightTankInternal.variant
            val leftAmount = leftTankInternal.amount
            val rightAmount = rightTankInternal.amount

            if (!leftVariant.isBlank && leftAmount > 0) {
                leftTankInternal.extract(leftVariant, leftAmount, tx)
            }
            if (!rightVariant.isBlank && rightAmount > 0) {
                rightTankInternal.extract(rightVariant, rightAmount, tx)
            }
            if (!rightVariant.isBlank && rightAmount > 0) {
                leftTankInternal.insert(rightVariant, rightAmount, tx)
            }
            if (!leftVariant.isBlank && leftAmount > 0) {
                rightTankInternal.insert(leftVariant, leftAmount, tx)
            }
            tx.commit()
        }
        sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
        sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(leftTankInternal.variant.fluid)
        sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
        sync.rightFluidRawId = if (rightTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(rightTankInternal.variant.fluid)
        resetProgress()
        setChanged()
        return beforeLeft != leftTankInternal.amount ||
            beforeRight != rightTankInternal.amount ||
            beforeLeftVariant != leftTankInternal.variant ||
            beforeRightVariant != rightTankInternal.variant
    }

    // 双输入数量安全缓存（SolidCannerRecipe.matches 检查 slot0/slot1 数量）：
    // 非 null 仅当两槽数量均 >= 对应 slotXCount；null 仅当两槽数量均未增加
    // （任一槽增加都可能让此前数量不足的配方匹配）。
    private var cachedContainerItem: net.minecraft.world.item.Item? = null
    private var cachedMaterialItem: net.minecraft.world.item.Item? = null
    private var cachedContainerCount = 0
    private var cachedMaterialCount = 0
    private var cachedSolidCannerRecipe: SolidCannerRecipe? = null
    private var cachedRecipeEpoch = -1

    private fun solidCanningRecipeFor(container: ItemStack, material: ItemStack): SolidCannerRecipe? {
        val world = world ?: return null
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedContainerItem === container.item && cachedMaterialItem === material.item) {
            val r = cachedSolidCannerRecipe
            if (r != null && container.count >= r.slot0Count && material.count >= r.slot1Count) return r
            if (r == null && container.count <= cachedContainerCount && material.count <= cachedMaterialCount) return null
        }
        val recipeType = ModMachineRecipes.recipeType(SolidCannerRecipe::class)
        // 配方匹配包含 slot0/slot1 的数量要求，不能将输入堆叠压成 1 个。
        val recipeInventory = SolidCannerRecipe.Input(container, material)
        val holder = world.recipeManager.getRecipeFor(recipeType, recipeInventory, world).orElse(null) ?: return null
        val recipe: SolidCannerRecipe = holder.value
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedContainerItem = container.item
        cachedMaterialItem = material.item
        cachedContainerCount = container.count
        cachedMaterialCount = material.count
        cachedSolidCannerRecipe = recipe
        return recipe
    }

    private fun trySolidCanning(container: ItemStack, material: ItemStack, outputSlot: ItemStack): Boolean {
        if (container.isEmpty || material.isEmpty) return false
        if (container.item != tinCanItem && container.item !is EmptyFuelRodItem) return false
        val recipe = solidCanningRecipeFor(container, material) ?: return false
        if (container.count < recipe.slot0Count || material.count < recipe.slot1Count) return false
        if (!canAcceptOutput(outputSlot, recipe.output.copy())) return false
        return true
    }

    private fun tryMixing(material: ItemStack): Boolean {
        if (material.isEmpty) return false
        val recipe = CannerMixingRecipes.getRecipe(
            leftTankInternal.variant.fluid.takeIf { !leftTankInternal.variant.isBlank },
            leftTankInternal.amount,
            material
        ) ?: return false
        return planMixingOutput(FluidVariant.of(recipe.outputFluid)) != null
    }

    private data class MixingOutputPlan(
        val containerFluidAmount: Long,
        val filledContainer: ItemStack,
        val containerFullAfterFill: Boolean,
        val tankFluidAmount: Long
    )

    /**
     * 原版 EnrichLiquid 语义：产物先尝试灌入容器槽，只有余液进入右液槽。
     * 此处只模拟 Transfer API 操作，用于在开始读条前同时验证容器输出和水槽容量。
     */
    private fun planMixingOutput(outputVariant: FluidVariant): MixingOutputPlan? {
        var containerFluidAmount = 0L
        var filledContainer = ItemStack.EMPTY
        var containerFullAfterFill = false
        val container = getItem(SLOT_INPUT)

        if (!container.isEmpty) {
            @Suppress("DEPRECATION")
            val ctx = ContainerItemContext.withConstant(container.copyWithCount(1))
            val containerStorage = ctx.find(FluidStorage.ITEM)
            if (containerStorage != null && containerStorage.supportsInsertion()) {
                Transaction.openOuter().use { tx ->
                    containerFluidAmount = containerStorage.insert(outputVariant, FluidConstants.BUCKET, tx)
                    if (containerFluidAmount > 0) {
                        filledContainer = ctx.itemVariant.toStack(1)
                        containerFullAfterFill = containerStorage.insert(outputVariant, 1L, tx) == 0L
                    }
                }
                if (containerFullAfterFill &&
                    !canAcceptOutput(getItem(SLOT_OUTPUT), filledContainer)
                ) return null
            }
        }

        val tankFluidAmount = FluidConstants.BUCKET - containerFluidAmount
        Transaction.openOuter().use { tx ->
            if (rightTankInternal.insert(outputVariant, tankFluidAmount, tx) != tankFluidAmount) return null
        }
        return MixingOutputPlan(
            containerFluidAmount,
            filledContainer,
            containerFullAfterFill,
            tankFluidAmount
        )
    }

    private fun tryPourOutToLeft(container: ItemStack): Boolean {
        if (container.isEmpty) return false
        val single = container.copyWithCount(1)
        val ctx = ContainerItemContext.withConstant(single)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        var canExtract = false
        for (view in itemStorage) {
            if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                canExtract = true
                break
            }
        }
        if (!canExtract) return false
        val space = (TANK_CAPACITY - leftTankInternal.amount).coerceAtLeast(0L)
        if (space < FluidConstants.BUCKET) return false
        if (leftTankInternal.amount > 0 && !leftTankInternal.variant.isBlank) {
            var containerFluidMatches = false
            for (view in itemStorage) {
                if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                    if (view.resource.fluid != leftTankInternal.variant.fluid) return false
                    containerFluidMatches = true
                }
            }
            if (!containerFluidMatches) return false
        }
        val outputSlot = getItem(SLOT_OUTPUT)
        return true
    }

    /** 灌入模式统一处理左槽中所有支持 Fabric Transfer API 的流体容器。 */
    private fun resolveBottleLiquidTargetSlot(): Int? {
        val inputStack = getItem(SLOT_INPUT)
        return if (planContainerFill(inputStack, rightTankInternal.variant, rightTankInternal.amount) != null) {
            SLOT_INPUT
        } else {
            null
        }
    }

    private data class ContainerFillPlan(
        val amount: Long,
        val result: ItemStack,
        val fullAfterFill: Boolean
    )

    private fun planContainerFill(
        container: ItemStack,
        variant: FluidVariant,
        available: Long
    ): ContainerFillPlan? {
        if (container.isEmpty || variant.isBlank || available <= 0L) return null
        @Suppress("DEPRECATION")
        val ctx = ContainerItemContext.withConstant(container.copyWithCount(1))
        val storage = ctx.find(FluidStorage.ITEM) ?: return null
        if (!storage.supportsInsertion()) return null

        Transaction.openOuter().use { tx ->
            val inserted = storage.insert(variant, minOf(FluidConstants.BUCKET, available), tx)
            if (inserted <= 0L) return null
            val result = ctx.itemVariant.toStack(1)
            val fullAfterFill = storage.insert(variant, 1L, tx) == 0L
            if (fullAfterFill && !canAcceptOutput(getItem(SLOT_OUTPUT), result)) return null
            return ContainerFillPlan(inserted, result, fullAfterFill)
        }
    }

    private fun canAcceptOutput(outputSlot: ItemStack, result: ItemStack): Boolean {
        if (outputSlot.isEmpty) return true
        return ItemStack.isSameItem(outputSlot, result) && outputSlot.count + result.count <= result.maxStackSize
    }

    private fun completeCurrentOperationByMode() {
        when (sync.getMode()) {
            CannerSync.Mode.BOTTLE_SOLID -> completeSolidCanning()
            CannerSync.Mode.EMPTY_LIQUID -> completePourOutToLeft()
            CannerSync.Mode.BOTTLE_LIQUID -> completeFillFromRight()
            CannerSync.Mode.ENRICH_LIQUID -> completeMixing()
        }
    }

    private fun completeMixing() {
        val material = getItem(SLOT_MATERIAL)
        val recipe = CannerMixingRecipes.getRecipe(
            leftTankInternal.variant.fluid.takeIf { !leftTankInternal.variant.isBlank },
            leftTankInternal.amount,
            material
        ) ?: return
        val extractVariant = leftTankInternal.variant
        val outputVariant = FluidVariant.of(recipe.outputFluid)
        val plan = planMixingOutput(outputVariant) ?: return
        val container = getItem(SLOT_INPUT)
        val outputSlot = getItem(SLOT_OUTPUT)

        Transaction.openOuter().use { tx ->
            val extracted = leftTankInternal.extract(extractVariant, FluidConstants.BUCKET, tx)
            if (extracted != FluidConstants.BUCKET) return@use

            var filledContainer = ItemStack.EMPTY
            if (plan.containerFluidAmount > 0) {
                if (container.isEmpty) return@use
                @Suppress("DEPRECATION")
                val ctx = ContainerItemContext.withConstant(container.copyWithCount(1))
                val containerStorage = ctx.find(FluidStorage.ITEM) ?: return@use
                val inserted = containerStorage.insert(outputVariant, plan.containerFluidAmount, tx)
                if (inserted != plan.containerFluidAmount) return@use
                filledContainer = ctx.itemVariant.toStack(1)
                if (plan.containerFullAfterFill &&
                    !canAcceptOutput(outputSlot, filledContainer)
                ) return@use
            }

            if (rightTankInternal.insert(outputVariant, plan.tankFluidAmount, tx) != plan.tankFluidAmount) return@use
            tx.commit()

            material.shrink(recipe.inputSolidCount)
            if (material.isEmpty) setItem(SLOT_MATERIAL, ItemStack.EMPTY)
            if (!filledContainer.isEmpty) {
                container.shrink(1)
                if (container.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
                if (plan.containerFullAfterFill) {
                    if (outputSlot.isEmpty) setItem(SLOT_OUTPUT, filledContainer)
                    else outputSlot.grow(filledContainer.count)
                } else {
                    setItem(SLOT_INPUT, filledContainer)
                }
            }
            sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
            sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(leftTankInternal.variant.fluid)
            sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
            sync.rightFluidRawId = if (rightTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(rightTankInternal.variant.fluid)
        }
    }

    private fun completeSolidCanning() {
        val container = getItem(SLOT_INPUT)
        val material = getItem(SLOT_MATERIAL)
        val outputSlot = getItem(SLOT_OUTPUT)
        val recipe = solidCanningRecipeFor(container, material) ?: return
        container.shrink(recipe.slot0Count)
        material.shrink(recipe.slot1Count)
        if (container.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
        if (material.isEmpty) setItem(SLOT_MATERIAL, ItemStack.EMPTY)
        val result = recipe.output.copy()
        if (outputSlot.isEmpty) setItem(SLOT_OUTPUT, result)
        else outputSlot.grow(result.count)
    }

    private fun completePourOutToLeft() {
        val container = getItem(SLOT_INPUT)
        if (container.isEmpty) return
        val single = container.copyWithCount(1)
        @Suppress("DEPRECATION")
        val ctx = ContainerItemContext.withConstant(single)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            for (view in itemStorage) {
                if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                    // FullItemFluidStorage is a live view: extracting swaps the full
                    // container for its empty variant, which immediately makes
                    // view.resource blank. Keep the fluid before mutating the context.
                    val containedFluid = view.resource
                    val extracted = view.extract(containedFluid, FluidConstants.BUCKET, tx)
                    if (extracted > 0) {
                        val inserted = leftTankInternal.insert(containedFluid, extracted, tx)
                        if (inserted > 0) {
                            val emptyResult = ctx.itemVariant.toStack(1)
                            val outputSlot = getItem(SLOT_OUTPUT)
                            if (!canAcceptOutput(outputSlot, emptyResult)) return@use
                            tx.commit()
                            container.shrink(1)
                            if (container.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
                            if (outputSlot.isEmpty) setItem(SLOT_OUTPUT, emptyResult)
                            else outputSlot.grow(1)
                            sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
                            sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else BuiltInRegistries.FLUID.getId(leftTankInternal.variant.fluid)
                            return
                        }
                    }
                }
            }
        }
    }

    private fun completeFillFromRight() {
        val slot = resolveBottleLiquidTargetSlot() ?: return
        val container = getItem(slot)
        if (container.isEmpty) return
        val variant = rightTankInternal.variant
        if (variant.isBlank) return
        val plan = planContainerFill(container, variant, rightTankInternal.amount) ?: return
        val single = container.copyWithCount(1)
        @Suppress("DEPRECATION")
        val ctx = ContainerItemContext.withConstant(single)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            val inserted = itemStorage.insert(variant, plan.amount, tx)
            if (inserted != plan.amount) return@use
            val extracted = rightTankInternal.extract(variant, inserted, tx)
            if (extracted != inserted) return@use
            val filled = ctx.itemVariant.toStack(1)
            if (plan.fullAfterFill && !canAcceptOutput(getItem(SLOT_OUTPUT), filled)) return@use
            tx.commit()

            container.shrink(1)
            if (container.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
            if (plan.fullAfterFill) {
                val output = getItem(SLOT_OUTPUT)
                if (output.isEmpty) setItem(SLOT_OUTPUT, filled)
                else output.grow(filled.count)
            } else {
                setItem(SLOT_INPUT, filled)
            }
            sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
        }
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
}
