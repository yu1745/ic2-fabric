package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.CannerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.recipes.CannerMixingRecipes
import ic2_120.content.recipes.SolidCannerRecipes
import ic2_120.content.screen.CannerScreenHandler
import ic2_120.content.sync.CannerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.BucketItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 流体/固体装罐机方块实体。
 * 双储罐：左侧（灌入来源/混合输入）、右侧（倒出目标/混合输出）。
 * 槽位：左上槽(A)、中间材料槽(B)、右上槽(C)、放电、4 升级
 * 模式化工作：
 * - 单元->左槽：A(流体单元) -> 左液槽
 * - 右槽->单元：右液槽 -> C(空单元)；未满时亦可将建筑泡沫喷枪放在 **B(中间槽)** 从右液槽灌装建筑泡沫
 * - 流体混合：左液槽 + B(固体) -> 右液槽（配方见 CannerMixingRecipes）
 *   - 水 + 8×青金石粉 → 冷却液；蒸馏水 + 1×青金石粉 → 冷却液
 *   - 水 + 1×糠 → 生物质；水 + 建筑泡沫粉 → 建筑泡沫；水 + 1×蛤蛤粉 → 除草剂
 * - 固体装罐：A(锡罐) + B(食物) -> C(输出)
 */
@ModBlockEntity(block = CannerBlock::class)
class CannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IFluidPipeUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CannerBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.none()

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    override val tier: Int = CannerSync.CANNER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val SLOT_CONTAINER = 0   // 左上槽（流体单元输入/固体装罐输入1）
        const val SLOT_MATERIAL = 1    // 中间槽（混合固体/固体装罐输入2）
        const val SLOT_OUTPUT = 2      // 右上槽（流体单元IO/固体装罐输出）
        const val SLOT_DISCHARGING = 3
        val SLOT_UPGRADE_INDICES = intArrayOf(4, 5, 6, 7)
        const val INVENTORY_SIZE = 8
        private const val NBT_LEFT_FLUID_AMOUNT = "LeftFluidAmount"
        private const val NBT_LEFT_FLUID_VARIANT = "LeftFluidVariant"
        private const val NBT_RIGHT_FLUID_AMOUNT = "RightFluidAmount"
        private const val NBT_RIGHT_FLUID_VARIANT = "RightFluidVariant"
        private const val TANK_CAPACITY_BUCKETS = 10
        private val TANK_CAPACITY = FluidConstants.BUCKET * TANK_CAPACITY_BUCKETS

        private val tinCanItem by lazy { Registries.ITEM.get(Identifier("ic2_120", "tin_can")) }

        @Volatile
        private var fluidLookupRegistered = false

        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> (be as CannerBlockEntity).getFluidStorageForSide(side) }, CannerBlockEntity::class.type())
            fluidLookupRegistered = true
        }
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = CannerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CannerSync.CANNER_TIER + voltageTierBonus) }
    )

    private val leftTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = true
        override fun canExtract(variant: FluidVariant): Boolean = true
        override fun onFinalCommit() {
            sync.leftFluidAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.leftFluidCapacityMb = (TANK_CAPACITY * 1000L / FluidConstants.BUCKET).toInt()
            markDirty()
        }
    }

    private val rightTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant): Boolean = true
        override fun canExtract(variant: FluidVariant): Boolean = true
        override fun onFinalCommit() {
            sync.rightFluidAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            sync.rightFluidCapacityMb = (TANK_CAPACITY * 1000L / FluidConstants.BUCKET).toInt()
            markDirty()
        }
    }

    val fluidTank: Storage<FluidVariant> = CombinedStorage(listOf(leftTankInternal, rightTankInternal))

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

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.canner")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CannerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CannerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        leftTankInternal.amount = nbt.getLong(NBT_LEFT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val leftFluidTag = nbt.getCompound(NBT_LEFT_FLUID_VARIANT)
        leftTankInternal.variant = if (leftFluidTag.isEmpty) FluidVariant.blank() else FluidVariant.fromNbt(leftFluidTag)
        rightTankInternal.amount = nbt.getLong(NBT_RIGHT_FLUID_AMOUNT).coerceIn(0L, TANK_CAPACITY)
        val rightFluidTag = nbt.getCompound(NBT_RIGHT_FLUID_VARIANT)
        rightTankInternal.variant = if (rightFluidTag.isEmpty) FluidVariant.blank() else FluidVariant.fromNbt(rightFluidTag)
        sync.leftFluidAmountMb = (leftTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        sync.leftFluidCapacityMb = (TANK_CAPACITY * 1000L / FluidConstants.BUCKET).toInt()
        sync.rightFluidAmountMb = (rightTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        sync.rightFluidCapacityMb = (TANK_CAPACITY * 1000L / FluidConstants.BUCKET).toInt()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_LEFT_FLUID_AMOUNT, leftTankInternal.amount)
        if (!leftTankInternal.variant.isBlank) nbt.put(NBT_LEFT_FLUID_VARIANT, leftTankInternal.variant.toNbt())
        nbt.putLong(NBT_RIGHT_FLUID_AMOUNT, rightTankInternal.amount)
        if (!rightTankInternal.variant.isBlank) nbt.put(NBT_RIGHT_FLUID_VARIANT, rightTankInternal.variant.toNbt())
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val facing = world?.getBlockState(pos)?.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        if (side == facing) return null
        return fluidTank
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val operating = when (sync.getMode()) {
            CannerSync.Mode.BOTTLE_SOLID -> trySolidCanning(getStack(SLOT_CONTAINER), getStack(SLOT_MATERIAL), getStack(SLOT_OUTPUT))
            CannerSync.Mode.EMPTY_LIQUID -> tryPourOutToLeft(getStack(SLOT_CONTAINER))
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
        sync.leftFluidAmountMb = (leftTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        sync.rightFluidAmountMb = (rightTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        sync.progress = 0
        markDirty()
        return beforeLeft != leftTankInternal.amount ||
            beforeRight != rightTankInternal.amount ||
            beforeLeftVariant != leftTankInternal.variant ||
            beforeRightVariant != rightTankInternal.variant
    }

    private fun trySolidCanning(container: ItemStack, material: ItemStack, outputSlot: ItemStack): Boolean {
        if (container.isEmpty || material.isEmpty) return false
        if (container.item != tinCanItem) return false
        val (recipe, result) = SolidCannerRecipes.getOutput(container, material) ?: return false
        if (!canAcceptOutput(outputSlot, result)) return false
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
        if (container.count != 1) return false
        val ctx = ContainerItemContext.withConstant(container)
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
        if (getEmptyContainerFor(container) == null) return false
        return true
    }

    /**
     * 灌入模式：右上槽为空单元/桶等，或**中间槽**为未满的建筑泡沫喷枪（从右液槽灌装）。
     */
    private fun resolveBottleLiquidTargetSlot(): Int? {
        val out = getStack(SLOT_OUTPUT)
        if (canFillStackFromRightTank(out)) return SLOT_OUTPUT
        val mat = getStack(SLOT_MATERIAL)
        if (mat.item is FoamSprayerItem && canFillStackFromRightTank(mat)) return SLOT_MATERIAL
        return null
    }

    private fun canFillStackFromRightTank(container: ItemStack): Boolean {
        if (container.isEmpty) return false
        if (container.count != 1) return false
        if (rightTankInternal.amount < FluidConstants.BUCKET || rightTankInternal.variant.isBlank) return false
        val ctx = ContainerItemContext.withConstant(container)
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
            else -> fluidToFilledCellStack(fluid).isEmpty.not()
        }
    }

    private fun getEmptyContainerFor(filled: ItemStack): ItemStack? = when (filled.item) {
        is BucketItem -> ItemStack(Items.BUCKET)
        is ModFluidCell -> ItemStack((filled.item as ModFluidCell).getEmptyCell())
        else -> {
            val path = Registries.ITEM.getId(filled.item).path
            if (path == "fluid_cell" || path.endsWith("_cell")) {
                ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
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
                    sync.leftFluidAmountMb = (leftTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
                    sync.rightFluidAmountMb = (rightTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
                }
            }
        }
    }

    private fun completeSolidCanning() {
        val container = getStack(SLOT_CONTAINER)
        val material = getStack(SLOT_MATERIAL)
        val outputSlot = getStack(SLOT_OUTPUT)
        val (recipe, result) = SolidCannerRecipes.getOutput(container, material) ?: return
        container.decrement(recipe.tinCanInputCount)
        material.decrement(recipe.foodInputCount)
        if (container.isEmpty) setStack(SLOT_CONTAINER, ItemStack.EMPTY)
        if (material.isEmpty) setStack(SLOT_MATERIAL, ItemStack.EMPTY)
        if (outputSlot.isEmpty) setStack(SLOT_OUTPUT, result)
        else outputSlot.increment(result.count)
    }

    private fun completePourOutToLeft() {
        val container = getStack(SLOT_CONTAINER)
        if (container.count != 1) return
        val emptyResult = getEmptyContainerFor(container) ?: return
        val ctx = ContainerItemContext.withConstant(container)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            for (view in itemStorage) {
                if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) {
                    val extracted = view.extract(view.resource, FluidConstants.BUCKET, tx)
                    if (extracted > 0) {
                        val inserted = leftTankInternal.insert(FluidVariant.of(view.resource.fluid), extracted, tx)
                        if (inserted > 0) {
                            tx.commit()
                            setStack(SLOT_CONTAINER, emptyResult)
                            sync.leftFluidAmountMb = (leftTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
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
        if (container.count != 1) return
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
            sync.rightFluidAmountMb = (rightTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
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
        val ctx = ContainerItemContext.withConstant(container)
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            val inserted = itemStorage.insert(variant, FluidConstants.BUCKET, tx)
            if (inserted > 0) {
                val extracted = rightTankInternal.extract(variant, inserted, tx)
                if (extracted > 0) {
                    tx.commit()
                    setStack(slot, filledResult)
                    sync.rightFluidAmountMb = (rightTankInternal.amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
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

