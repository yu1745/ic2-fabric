package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.heat.IHeatConsumer
import ic2_120.content.item.AirCell
import ic2_120.content.item.EjectorUpgrade
import ic2_120.content.item.PullingUpgrade
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipe
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.screen.BlastFurnaceScreenHandler
import ic2_120.content.sync.BlastFurnaceSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.type
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
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

/**
 * 高炉方块实体。
 *
 * 从背面接收热量（HU），消耗铁质材料与压缩空气，产出钢锭和炉渣。
 *
 * HU 储值上限 1,000 HU。温度 0–1,700。
 *
 * 升温消耗 HU：
 * - 0–1400：100 HU/tick
 * - 1401–1500：80 HU/tick
 * - 1501–1600：60 HU/tick
 * - 1601–1700：40 HU/tick
 *
 * 工作条件：温度 > 1400。工作时温度冻结，持续消耗对应温度段的 HU 维持。
 *
 * 每钢锭固定消耗 6,000 mB 压缩空气，单 tick 消耗量随温度升高而加快：
 * - 1401–1500：8,400 ticks → 约 0.71 mB/tick
 * - 1501–1600：6,000 ticks → 1.00 mB/tick
 * - 1601–1700：4,000 ticks → 1.50 mB/tick
 *
 * 槽位：
 * - SLOT_INPUT：铁质材料
 * - SLOT_AIR_INPUT：压缩空气单元（填充储罐用）
 * - SLOT_OUTPUT_STEEL：钢锭输出
 * - SLOT_OUTPUT_SLAG：炉渣输出
 * - SLOT_OUTPUT_EMPTY：空单元输出
 * - SLOT_UPGRADE_0/1：升级槽（2 个，无限制）
 */
@ModBlockEntity(block = BlastFurnaceBlock::class)
@ModMachineRecipeBinding(BlastFurnaceRecipeSerializer::class)
class BlastFurnaceBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = BlastFurnaceBlock.ACTIVE
    override fun getInventory(): Inventory = this
    override val tier: Int = 1

    // IFluidPipeUpgradeSupport
    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_AIR_INPUT = 1
        const val SLOT_OUTPUT_STEEL = 2
        const val SLOT_OUTPUT_SLAG = 3
        const val SLOT_OUTPUT_EMPTY = 4
        const val SLOT_UPGRADE_0 = 5
        const val SLOT_UPGRADE_1 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT, SLOT_AIR_INPUT)
        const val INVENTORY_SIZE = 7

        private const val NBT_HU_RECEIVED = "HuReceived"
        private const val NBT_TEMPERATURE = "Temperature"
        private const val NBT_TEMP_PROGRESS = "TempProgress"
        private const val NBT_TEMP_DECAY = "TempDecay"
        private const val NBT_AIR_AMOUNT = "AirAmount"

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val t = BlastFurnaceBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, t)
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
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is EjectorUpgrade || it.item is PullingUpgrade }),
            ItemInsertRoute(intArrayOf(SLOT_AIR_INPUT), matcher = { !it.isEmpty && it.item is AirCell }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isBlastRecipeInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)
    val sync = BlastFurnaceSync(syncedData)

    private var huReceivedThisTick: Long = 0L
    private var temperature: Int = 0
    private var tempProgress: Int = 0
    private var tempDecayProgress: Int = 0
    private var heatReceivedLastTick: Boolean = false

    // 空气消耗累加器（微 mB，即千分之一 mB），用于分 tick 精确消耗
    private var airAccumulatedNeed: Long = 0L

    // 压缩空气储罐 (8,000 mB)
    private val airTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * 8

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canExtract(variant: FluidVariant): Boolean = false
        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == ModFluids.COMPRESSED_AIR_STILL || variant.fluid == ModFluids.COMPRESSED_AIR_FLOWING

        override fun onFinalCommit() {
            sync.airAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        fun tryConsumeAir(mlToConsume: Long): Long {
            val toConsume = mlToConsume * FluidConstants.BUCKET / 1000L
            if (toConsume <= 0L || amount <= 0L) return 0L
            val actual = minOf(toConsume, amount)
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.airAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual * 1000L / FluidConstants.BUCKET
        }

        fun tryFillAir(mlToInsert: Long): Long {
            val toInsert = mlToInsert * FluidConstants.BUCKET / 1000L
            if (toInsert <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.COMPRESSED_AIR_STILL)
            sync.airAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual * 1000L / FluidConstants.BUCKET
        }

        fun getRemainingSpaceMl(): Long {
            val space = tankCapacity - amount
            return space * 1000L / FluidConstants.BUCKET
        }

        fun setStoredAir(droplets: Long) {
            amount = droplets.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.COMPRESSED_AIR_STILL) else FluidVariant.blank()
            sync.airAmountMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }
    }

    val airTank: Storage<FluidVariant> = airTankInternal

    constructor(pos: BlockPos, state: BlockState) : this(
        BlastFurnaceBlockEntity::class.type(),
        pos,
        state
    )

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        if (side == getHeatTransferFace()) return null
        return airTank
    }

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
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
        SLOT_INPUT -> isBlastRecipeInput(stack)
        SLOT_AIR_INPUT -> !stack.isEmpty && stack.item is AirCell
        SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY -> false
        SLOT_UPGRADE_0, SLOT_UPGRADE_1 -> stack.item is EjectorUpgrade || stack.item is PullingUpgrade
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.blast_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        BlastFurnaceScreenHandler(syncId, playerInventory, this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        temperature = nbt.getInt(NBT_TEMPERATURE).coerceIn(0, BlastFurnaceSync.TEMP_MAX)
        tempProgress = nbt.getInt(NBT_TEMP_PROGRESS).coerceAtLeast(0)
        tempDecayProgress = nbt.getInt(NBT_TEMP_DECAY).coerceAtLeast(0)
        sync.temperature = temperature
        airTankInternal.setStoredAir(nbt.getLong(NBT_AIR_AMOUNT).coerceAtLeast(0L))
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_HU_RECEIVED, huReceivedThisTick)
        nbt.putInt(NBT_TEMPERATURE, temperature)
        nbt.putInt(NBT_TEMP_PROGRESS, tempProgress)
        nbt.putInt(NBT_TEMP_DECAY, tempDecayProgress)
        nbt.putLong(NBT_AIR_AMOUNT, airTankInternal.amount)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        huReceivedThisTick += hu
        heatReceivedLastTick = true
        markDirty()
        return hu
    }

    private fun getRecipeForInput(input: ItemStack): BlastFurnaceRecipe? {
        if (input.isEmpty) return null
        val inv = BlastFurnaceRecipe.Input(input)
        val recipeManager = world?.recipeManager ?: return null
        return recipeManager.getFirstMatch(getRecipeType<BlastFurnaceRecipe>(), inv, world ?: return null)
            .map { it.value }
            .orElse(null)
    }

    /** 当前温度下每 tick 升温 / 维持所需 HU */
    private fun getHuPerTick(temp: Int): Int = when {
        temp >= 1601 -> 40
        temp >= 1501 -> 60
        temp >= 1401 -> 80
        else -> 100
    }

    /** 当前温度下每升高 1 温度值所需 tick 数 */
    private fun getTicksPerTemp(temp: Int): Int = when {
        temp >= 1601 -> 56   // 2.8s
        temp >= 1501 -> 32   // 1.6s
        temp >= 1401 -> 20   // 1.0s
        else -> 14           // 0.7s (0–1400)
    }

    /** 当前温度下 HU 不足时每下降 1 温度值所需 tick 数 */
    private fun getTicksPerDecay(temp: Int): Int = when {
        temp >= 1601 -> 6    // 0.3s
        temp >= 1501 -> 10   // 0.5s
        temp >= 1401 -> 16   // 0.8s
        else -> 20           // 1.0s (0–1400)
    }

    /** 当前温度下的加工总 tick 数（三段线性插值） */
    private fun getProgressMax(temp: Int): Int = BlastFurnaceSync.getProgressMax(temp)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.huInput = huReceivedThisTick.toInt()
        sync.temperature = temperature
        sync.heatInput = if (heatReceivedLastTick) 1 else 0

        val input = getStack(SLOT_INPUT)
        val outputSteel = getStack(SLOT_OUTPUT_STEEL)
        val outputSlag = getStack(SLOT_OUTPUT_SLAG)

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(
                world, pos, airTankInternal, fluidPipeProviderFilter, fluidPipeProviderSides,
                blockedFace = getHeatTransferFace()
            )
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        fillAirTankFromSlot()

        val huPerTick = getHuPerTick(temperature)
        val progressMax = getProgressMax(temperature)

        val hasInput = !input.isEmpty && getRecipeForInput(input) != null
        val canAcceptOutput = canAcceptOutput(outputSteel, outputSlag)
        val canWork = temperature >= BlastFurnaceSync.TEMP_WORK_MIN && hasInput && canAcceptOutput

        if (canWork) {
            // HU 不足维持当前温度：暂停工作，触发温度衰减
            if (huReceivedThisTick < huPerTick) {
                applyTemperatureDecay()
                if (temperature < BlastFurnaceSync.TEMP_WORK_MIN && sync.progress != 0) {
                    sync.progress = 0
                    airAccumulatedNeed = 0L
                }
                setActiveState(world, pos, state, false)
                return
            }

            // HU 充足，始终消耗 HU 维持温度（即使空气不足或产物满）
            tempDecayProgress = 0

            // 无材料：重置进度，仅消耗 HU
            if (!hasInput) {
                if (sync.progress != 0) sync.progress = 0
                markDirty()
                setActiveState(world, pos, state, false)
                return
            }

            // 产物满：仅消耗 HU
            if (!canAcceptOutput) {
                markDirty()
                setActiveState(world, pos, state, false)
                return
            }

            // 消耗压缩空气
            val consumedAir = consumeAirForTick(progressMax)
            if (consumedAir <= 0L) {
                // 空气不足：仅消耗 HU，不推进进度
                markDirty()
                setActiveState(world, pos, state, false)
                return
            }

            // 推进进度
            val nextProgress = sync.progress + 1
            if (nextProgress >= progressMax) {
                val recipe = getRecipeForInput(input) ?: return
                val steelOutput = BlastFurnaceRecipe.getSteelOutput(recipe)
                val slagOutput = BlastFurnaceRecipe.getSlagOutput(recipe)
                input.decrement(1)
                if (outputSteel.isEmpty) setStack(SLOT_OUTPUT_STEEL, steelOutput)
                else outputSteel.increment(steelOutput.count)
                if (outputSlag.isEmpty) setStack(SLOT_OUTPUT_SLAG, slagOutput)
                else outputSlag.increment(slagOutput.count)
                sync.progress = 0
                airAccumulatedNeed = 0L
                markDirty()
                setActiveState(world, pos, state, false)
                return
            }

            sync.progress = nextProgress
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            // 不工作
            if (sync.progress != 0) sync.progress = 0
            airAccumulatedNeed = 0L

            if (huReceivedThisTick >= huPerTick && temperature < BlastFurnaceSync.TEMP_MAX) {
                // HU 充足：累积温度进度，达到阈值后温度 +1
                tempDecayProgress = 0
                tempProgress++
                val ticksPerTemp = getTicksPerTemp(temperature)
                if (tempProgress >= ticksPerTemp) {
                    temperature++
                    tempProgress = 0
                    sync.temperature = temperature
                }
                markDirty()
            } else {
                // HU 不足：温度衰减
                applyTemperatureDecay()
            }
            setActiveState(world, pos, state, false)
        }

        huReceivedThisTick = 0L
        heatReceivedLastTick = false
    }

    /** 按当前温度 / 进度消耗压缩空气，返回实际消耗的 mB 数 */
    private fun consumeAirForTick(progressMax: Int): Long {
        val airPerSteel = BlastFurnaceSync.getAirPerSteelMb(temperature).toLong()
        // 目标累计消耗量（微 mB）：进度每前进 1，消耗 airPerSteel * 1000 / progressMax 微 mB
        val targetNeed = (sync.progress + 1).toLong() * airPerSteel * 1000L / progressMax
        val needThisTick = targetNeed - airAccumulatedNeed
        if (needThisTick <= 0L) return 1L // 至少消耗 1 mB

        var consumedTotal = 0L
        var remaining = needThisTick

        while (remaining >= 1000L) {
            val mB = (remaining / 1000L).coerceAtMost(10L) // 单次最多 10 mB
            val c = airTankInternal.tryConsumeAir(mB)
            if (c <= 0L) {
                // 空气不足，回退已消耗的部分
                airAccumulatedNeed += consumedTotal * 1000L
                return 0L
            }
            consumedTotal += c
            remaining -= c * 1000L
        }

        airAccumulatedNeed = targetNeed
        return consumedTotal
    }

    private fun fillAirTankFromSlot() {
        if (airTankInternal.getRemainingSpaceMl() < 1000L) return
        val airSlot = getStack(SLOT_AIR_INPUT)
        if (airSlot.isEmpty || airSlot.item !is AirCell) return
        val emptySlot = getStack(SLOT_OUTPUT_EMPTY)
        val emptyCell = ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")))
        val canAcceptEmpty = emptySlot.isEmpty ||
            (ItemStack.areItemsEqual(emptySlot, emptyCell) && emptySlot.count + 1 <= emptyCell.maxCount)
        if (!canAcceptEmpty) return

        airSlot.decrement(1)
        if (airSlot.isEmpty) setStack(SLOT_AIR_INPUT, ItemStack.EMPTY)
        if (emptySlot.isEmpty) setStack(SLOT_OUTPUT_EMPTY, emptyCell)
        else emptySlot.increment(1)

        airTankInternal.tryFillAir(1000L)
    }

    /** HU 不足时温度衰减，达到阈值后温度 -1。温度到 0 后不再衰减。 */
    private fun applyTemperatureDecay() {
        if (temperature <= 0) return
        tempDecayProgress++
        val ticksPerDecay = getTicksPerDecay(temperature)
        if (tempDecayProgress >= ticksPerDecay) {
            temperature--
            tempDecayProgress = 0
            tempProgress = 0
            sync.temperature = temperature
            markDirty()
        }
    }

    private fun canAcceptOutput(steel: ItemStack, slag: ItemStack): Boolean {
        val recipe = getRecipeForInput(getStack(SLOT_INPUT)) ?: return true
        val steelOut = BlastFurnaceRecipe.getSteelOutput(recipe)
        val slagOut = BlastFurnaceRecipe.getSlagOutput(recipe)
        val steelOk = steel.isEmpty || (ItemStack.areItemsEqual(steel, steelOut) && steel.count + steelOut.count <= steel.maxCount)
        val slagOk = slag.isEmpty || (ItemStack.areItemsEqual(slag, slagOut) && slag.count + slagOut.count <= slag.maxCount)
        return steelOk && slagOk
    }

    private fun isBlastRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return getRecipeForInput(stack) != null
    }
}
