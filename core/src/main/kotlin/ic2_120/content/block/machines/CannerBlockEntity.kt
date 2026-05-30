package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.CannerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.CfPack
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.recipes.solidcanner.SolidCannerRecipe
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.recipes.CannerMixingRecipes
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.screen.CannerScreenHandler
import ic2_120.content.sync.CannerSync
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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.BucketItem
import net.minecraft.recipe.input.SingleStackRecipeInput
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 流体/固体装罐机方块实体。
 * 双储罐：左罐（灌入来源/混合输入）、右罐（倒出目标/混合输出）。
 * 槽位：左输入、中材料、右输出、放电、4 升级
 * 模式化工作：
 * - 排出模式：左槽满容器 → 左液槽，空容器从右槽输出
 * - 灌入模式：左槽空容器/中槽泡沫喷枪或Cf背包 → 右液槽灌装，满容器从右槽输出
 * - 流体混合：左液槽 + 中槽固体 → 右液槽（配方见 CannerMixingRecipes）
 * - 固体装罐：左槽锡罐 + 中槽食物 → 右槽成品
 */
@ModBlockEntity(block = CannerBlock::class)
class CannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CannerBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.none()

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override val tier: Int = CannerSync.CANNER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
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

        private val tinCanItem by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "tin_can")) }

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> (be as CannerBlockEntity).getFluidStorageForSide(side) }, CannerBlockEntity::class.type())
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
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isValid(SLOT_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_MATERIAL), matcher = { isValid(SLOT_MATERIAL, it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT, SLOT_MATERIAL, SLOT_OUTPUT, SLOT_DISCHARGING),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
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
        override fun canInsert(variant: FluidVariant): Boolean = ModFluids.isFluid(variant.fluid)
        override fun canExtract(variant: FluidVariant): Boolean = true

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.leftFluidAmount = amount.toInt().coerceAtLeast(0)
            sync.leftFluidCapacity = TANK_CAPACITY.toInt()
            sync.leftFluidRawId = if (variant.isBlank) -1 else Registries.FLUID.getRawId(variant.fluid)
            markDirty()
        }
    }

    private val rightTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = true
        override fun canExtract(variant: FluidVariant): Boolean = ModFluids.isFluid(variant.fluid)

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.rightFluidAmount = amount.toInt().coerceAtLeast(0)
            sync.rightFluidCapacity = TANK_CAPACITY.toInt()
            sync.rightFluidRawId = if (variant.isBlank) -1 else Registries.FLUID.getRawId(variant.fluid)
            markDirty()
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

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> {
            if (stack.isEmpty || stack.item is IBatteryItem || stack.item is FoamSprayerItem || stack.item is CfPack) return false
            when (sync.getMode()) {
                CannerSync.Mode.BOTTLE_SOLID -> stack.item == tinCanItem || stack.item is EmptyFuelRodItem
                CannerSync.Mode.EMPTY_LIQUID, CannerSync.Mode.ENRICH_LIQUID -> cannerIsFilledFluidContainer(stack)
                CannerSync.Mode.BOTTLE_LIQUID -> cannerIsEmptyFluidContainer(stack)
            }
        }
        SLOT_MATERIAL -> {
            if (stack.isEmpty || stack.item is IBatteryItem) return false
            when (sync.getMode()) {
                CannerSync.Mode.EMPTY_LIQUID -> false
                CannerSync.Mode.BOTTLE_LIQUID -> stack.item is FoamSprayerItem || stack.item is CfPack
                else -> SolidCannerRecipe.slot1Ingredients(world!!).any { it.test(stack) } ||
                    CannerMixingRecipes.isMixingMaterial(stack.item)
            }
        }
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> !stack.isEmpty && stack.item is IBatteryItem
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.canner")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CannerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CannerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        leftTankInternal.amount = nbt.getLong(NBT_LEFT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val leftFluidTag = nbt.getCompound(NBT_LEFT_FLUID_VARIANT)
        leftTankInternal.variant = if (leftFluidTag.isEmpty) FluidVariant.blank() else FluidVariant.CODEC.decode(NbtOps.INSTANCE, leftFluidTag).result().map { it.first }.orElse(FluidVariant.blank())
        rightTankInternal.amount = nbt.getLong(NBT_RIGHT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val rightFluidTag = nbt.getCompound(NBT_RIGHT_FLUID_VARIANT)
        rightTankInternal.variant = if (rightFluidTag.isEmpty) FluidVariant.blank() else FluidVariant.CODEC.decode(NbtOps.INSTANCE, rightFluidTag).result().map { it.first }.orElse(FluidVariant.blank())
        sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
        sync.leftFluidCapacity = TANK_CAPACITY.toInt()
        sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(leftTankInternal.variant.fluid)
        sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
        sync.rightFluidCapacity = TANK_CAPACITY.toInt()
        sync.rightFluidRawId = if (rightTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(rightTankInternal.variant.fluid)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(CannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_LEFT_FLUID_AMOUNT, leftTankInternal.amount)
        if (!leftTankInternal.variant.isBlank) nbt.put(NBT_LEFT_FLUID_VARIANT, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, leftTankInternal.variant).result().orElse(NbtCompound()))
        nbt.putLong(NBT_RIGHT_FLUID_AMOUNT, rightTankInternal.amount)
        if (!rightTankInternal.variant.isBlank) nbt.put(NBT_RIGHT_FLUID_VARIANT, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, rightTankInternal.variant).result().orElse(NbtCompound()))
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return CombinedStorage(listOf(leftTankInputOnly, rightTankOutputOnly))
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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
        if (fluidPipeReceiverEnabled && mode == CannerSync.Mode.EMPTY_LIQUID) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, leftTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        val operating = when (sync.getMode()) {
            CannerSync.Mode.BOTTLE_SOLID -> trySolidCanning(getStack(SLOT_INPUT), getStack(SLOT_MATERIAL), getStack(SLOT_OUTPUT))
            CannerSync.Mode.EMPTY_LIQUID -> tryPourOutToLeft(getStack(SLOT_INPUT))
            CannerSync.Mode.BOTTLE_LIQUID -> resolveBottleLiquidTargetSlot() != null
            CannerSync.Mode.ENRICH_LIQUID -> tryMixing(getStack(SLOT_MATERIAL))
        }

        if (operating) {
            val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
            val need = (CannerSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
            if (sync.consumeEnergy(need) > 0L) {
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                sync.progress += progressIncrement
                if (sync.progress >= CannerSync.PROGRESS_MAX) {
                    completeCurrentOperationByMode()
                    sync.progress = 0
                }
                markDirty()
                setActiveState(world, pos, state, true)
            } else {
                setActiveState(world, pos, state, false)
            }
        } else {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    fun cycleMode() {
        sync.cycleMode()
        sync.progress = 0
        markDirty()
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
        sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(leftTankInternal.variant.fluid)
        sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
        sync.rightFluidRawId = if (rightTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(rightTankInternal.variant.fluid)
        sync.progress = 0
        markDirty()
        return beforeLeft != leftTankInternal.amount ||
            beforeRight != rightTankInternal.amount ||
            beforeLeftVariant != leftTankInternal.variant ||
            beforeRightVariant != rightTankInternal.variant
    }

    private fun trySolidCanning(container: ItemStack, material: ItemStack, outputSlot: ItemStack): Boolean {
        if (container.isEmpty || material.isEmpty) return false
        if (container.item != tinCanItem && container.item !is EmptyFuelRodItem) return false
        val recipeType = ModMachineRecipes.recipeType(SolidCannerRecipe::class)
        val match = world?.recipeManager?.getFirstMatch(recipeType, SingleStackRecipeInput(container.copyWithCount(1)), world) ?: return false
        if (match.isEmpty) return false
        val recipe = match.get().value
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
        val space = (TANK_CAPACITY - rightTankInternal.amount).coerceAtLeast(0L)
        if (space < FluidConstants.BUCKET) return false
        if (!rightTankInternal.variant.isBlank && rightTankInternal.variant.fluid != recipe.outputFluid) return false
        return true
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
        val emptyResult = getEmptyContainerFor(container) ?: return false
        val outputSlot = getStack(SLOT_OUTPUT)
        if (!canAcceptOutput(outputSlot, emptyResult)) return false
        return true
    }

    /**
     * 灌入模式：左槽为空桶/单元，灌装后满容器写入 SLOT_OUTPUT；
     * 或中槽为未满的建筑泡沫喷枪/Cf背包（从右液槽灌装）。
     */
    private fun resolveBottleLiquidTargetSlot(): Int? {
        val inputStack = getStack(SLOT_INPUT)
        if (canFillStackFromRightTank(inputStack) && canAcceptOutput(getStack(SLOT_OUTPUT), getFilledResult(inputStack))) return SLOT_INPUT
        val mat = getStack(SLOT_MATERIAL)
        if (mat.item is FoamSprayerItem && canFillStackFromRightTank(mat)) return SLOT_MATERIAL
        if (mat.item is CfPack && canFillStackFromRightTank(mat)) return SLOT_MATERIAL
        return null
    }

    private fun getFilledResult(emptyContainer: ItemStack): ItemStack = when (emptyContainer.item) {
        Items.BUCKET -> {
            val fluid = rightTankInternal.variant.fluid
            when (fluid) {
                Fluids.WATER, Fluids.FLOWING_WATER -> ItemStack(Items.WATER_BUCKET)
                Fluids.LAVA, Fluids.FLOWING_LAVA -> ItemStack(Items.LAVA_BUCKET)
                else -> ItemStack.EMPTY
            }
        }
        else -> fluidToFilledCellStack(rightTankInternal.variant.fluid)
    }

    private fun canFillStackFromRightTank(container: ItemStack): Boolean {
        if (container.isEmpty) return false
        if (rightTankInternal.amount < FluidConstants.BUCKET || rightTankInternal.variant.isBlank) return false
        val single = container.copyWithCount(1)
        val ctx = ContainerItemContext.withConstant(single)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return false
        if (!itemStorage.supportsInsertion()) return false
        val fluid = rightTankInternal.variant.fluid
        return when (container.item) {
            Items.BUCKET -> when (fluid) {
                Fluids.WATER, Fluids.FLOWING_WATER -> true
                Fluids.LAVA, Fluids.FLOWING_LAVA -> true
                else -> false
            }
            is FoamSprayerItem ->
                (fluid == ModFluids.CONSTRUCTION_FOAM_STILL || fluid == ModFluids.CONSTRUCTION_FOAM_FLOWING) &&
                    FoamSprayerItem.getFluidAmount(container) < FoamSprayerItem.CAPACITY_DROPLETS
            is CfPack ->
                (fluid == ModFluids.CONSTRUCTION_FOAM_STILL || fluid == ModFluids.CONSTRUCTION_FOAM_FLOWING) &&
                    CfPack.getFluidAmount(container) < CfPack.CAPACITY_DROPLETS
            else -> fluidToFilledCellStack(fluid).isEmpty.not()
        }
    }

    private fun getEmptyContainerFor(filled: ItemStack): ItemStack? = when (filled.item) {
        is BucketItem -> ItemStack(Items.BUCKET)
        is ModFluidCell -> ItemStack((filled.item as ModFluidCell).getEmptyCell())
        else -> {
            val path = Registries.ITEM.getId(filled.item).path
            if (path == "fluid_cell" || path.endsWith("_cell")) {
                ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")))
            } else null
        }
    }

    private fun canAcceptOutput(outputSlot: ItemStack, result: ItemStack): Boolean {
        if (outputSlot.isEmpty) return true
        return ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= result.maxCount
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
        val material = getStack(SLOT_MATERIAL)
        val recipe = CannerMixingRecipes.getRecipe(
            leftTankInternal.variant.fluid.takeIf { !leftTankInternal.variant.isBlank },
            leftTankInternal.amount,
            material
        ) ?: return
        val extractVariant = leftTankInternal.variant
        val outputVariant = FluidVariant.of(recipe.outputFluid)
        Transaction.openOuter().use { tx ->
            val extracted = leftTankInternal.extract(extractVariant, FluidConstants.BUCKET, tx)
            if (extracted > 0) {
                val inserted = rightTankInternal.insert(outputVariant, FluidConstants.BUCKET, tx)
                if (inserted > 0) {
                    tx.commit()
                    material.decrement(recipe.inputSolidCount)
                    if (material.isEmpty) setStack(SLOT_MATERIAL, ItemStack.EMPTY)
                    sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
                    sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(leftTankInternal.variant.fluid)
                    sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
                    sync.rightFluidRawId = if (rightTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(rightTankInternal.variant.fluid)
                }
            }
        }
    }

    private fun completeSolidCanning() {
        val container = getStack(SLOT_INPUT)
        val material = getStack(SLOT_MATERIAL)
        val outputSlot = getStack(SLOT_OUTPUT)
        val recipeType = ModMachineRecipes.recipeType(SolidCannerRecipe::class)
        val match = world?.recipeManager?.getFirstMatch(recipeType, SingleStackRecipeInput(container.copyWithCount(1)), world) ?: return
        if (match.isEmpty) return
        val recipe = match.get().value
        container.decrement(recipe.slot0Count)
        material.decrement(recipe.slot1Count)
        if (container.isEmpty) setStack(SLOT_INPUT, ItemStack.EMPTY)
        if (material.isEmpty) setStack(SLOT_MATERIAL, ItemStack.EMPTY)
        val result = recipe.output.copy()
        if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, result)
        else outputSlot.increment(result.count)
    }

    private fun completePourOutToLeft() {
        val container = getStack(SLOT_INPUT)
        if (container.isEmpty) return
        val emptyResult = getEmptyContainerFor(container) ?: return
        val single = container.copyWithCount(1)
        val ctx = ContainerItemContext.withConstant(single)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            for (view in itemStorage) {
                if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                    val extracted = view.extract(view.resource, FluidConstants.BUCKET, tx)
                    if (extracted > 0) {
                        val inserted = leftTankInternal.insert(FluidVariant.of(view.resource.fluid), extracted, tx)
                        if (inserted > 0) {
                            tx.commit()
                            container.decrement(1)
                            if (container.isEmpty) setStack(SLOT_INPUT, ItemStack.EMPTY)
                            val outputSlot = getStack(SLOT_OUTPUT)
                            if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, emptyResult)
                            else outputSlot.increment(emptyResult.count)
                            sync.leftFluidAmount = leftTankInternal.amount.toInt().coerceAtLeast(0)
                            sync.leftFluidRawId = if (leftTankInternal.variant.isBlank) -1 else Registries.FLUID.getRawId(leftTankInternal.variant.fluid)
                            return
                        }
                    }
                }
            }
        }
    }

    private fun completeFillFromRight() {
        val slot = resolveBottleLiquidTargetSlot() ?: return
        val container = getStack(slot)
        if (container.isEmpty) return
        val variant = rightTankInternal.variant
        if (variant.isBlank) return
        val fluid = variant.fluid

        if (container.item is FoamSprayerItem) {
            // withConstant 不修改传入的 ItemStack（Fabric 文档：内容仅为拷贝），必须直接改槽内堆叠的 NBT
            val before = FoamSprayerItem.getFluidAmount(container)
            val space = (FoamSprayerItem.CAPACITY_DROPLETS - before).coerceAtLeast(0L)
            val move = minOf(FluidConstants.BUCKET, space)
            if (move <= 0L) return
            Transaction.openOuter().use { tx ->
                val extracted = rightTankInternal.extract(variant, move, tx)
                if (extracted <= 0) return@use
                tx.commit()
                FoamSprayerItem.setFluidAmount(container, before + extracted)
            }
            setStack(slot, container)
            sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
            return
        }

        if (container.item is CfPack) {
            val before = CfPack.getFluidAmount(container)
            val space = (CfPack.CAPACITY_DROPLETS - before).coerceAtLeast(0L)
            val move = minOf(FluidConstants.BUCKET, space)
            if (move <= 0L) return
            Transaction.openOuter().use { tx ->
                val extracted = rightTankInternal.extract(variant, move, tx)
                if (extracted <= 0) return@use
                tx.commit()
                CfPack.setFluidAmount(container, before + extracted)
            }
            setStack(slot, container)
            sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
            return
        }

        val filledResult = when (container.item) {
            Items.BUCKET -> when (fluid) {
                Fluids.WATER, Fluids.FLOWING_WATER -> ItemStack(Items.WATER_BUCKET)
                Fluids.LAVA, Fluids.FLOWING_LAVA -> ItemStack(Items.LAVA_BUCKET)
                else -> return
            }
            else -> fluidToFilledCellStack(fluid)
        }
        if (filledResult.isEmpty) return
        val single = container.copyWithCount(1)
        val ctx = ContainerItemContext.withConstant(single)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            val inserted = itemStorage.insert(variant, FluidConstants.BUCKET, tx)
            if (inserted > 0) {
                val extracted = rightTankInternal.extract(variant, inserted, tx)
                if (extracted > 0) {
                    tx.commit()
                    if (slot == SLOT_INPUT) {
                        // 从左输入槽取空容器，灌装后放入输出槽
                        container.decrement(1)
                        if (container.isEmpty) setStack(SLOT_INPUT, ItemStack.EMPTY)
                        val output = getStack(SLOT_OUTPUT)
                        if (output.isEmpty) setStack(SLOT_OUTPUT, filledResult)
                        else output.increment(filledResult.count)
                    } else {
                        setStack(slot, filledResult)
                    }
                    sync.rightFluidAmount = rightTankInternal.amount.toInt().coerceAtLeast(0)
                }
            }
        }
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
}
