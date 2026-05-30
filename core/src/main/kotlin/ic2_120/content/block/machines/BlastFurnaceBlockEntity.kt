package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.heat.IHeatConsumer
import ic2_120.content.item.AirCell
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.getFluidCellVariant
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
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
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
 * 每钢锭空气消耗随温度线性递减（单位 droplets）：
 * - 1401→1500：486,000→421,200 droplets（显示约 6000→5200 mB）
 * - 1501→1600：421,119→380,700 droplets（显示约 5199→4700 mB）
 * - 1601→1700：380,619→340,200 droplets（显示约 4699→4200 mB）
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
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = BlastFurnaceBlock.ACTIVE
    override fun getInventory(): Inventory = this
    override val tier: Int = 1

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

    // 当前钢锭已消耗的空气 droplet 数（用于滞后耗尽模型）
    private var airConsumedThisSteel: Long = 0L

    // 压缩空气储罐 (8 BUCKET = 648,000 droplets)
    private val airTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = FluidConstants.BUCKET * BlastFurnaceSync.AIR_TANK_BUCKETS

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canExtract(variant: FluidVariant): Boolean = false
        override fun canInsert(variant: FluidVariant): Boolean = ModFluids.isCompressedAir(variant.fluid)

        override fun onFinalCommit() {
            sync.airAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        fun tryConsumeAir(droplets: Long): Long {
            if (droplets <= 0L || amount <= 0L) return 0L
            val actual = minOf(droplets, amount)
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.airAmount = amount.toInt().coerceAtLeast(0)
            return actual
        }

        fun tryFillAir(droplets: Long): Long {
            if (droplets <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(droplets, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.COMPRESSED_AIR_STILL)
            sync.airAmount = amount.toInt().coerceAtLeast(0)
            return actual
        }

        fun remainingSpace(): Long = tankCapacity - amount

        fun setStoredAir(droplets: Long) {
            amount = droplets.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.COMPRESSED_AIR_STILL) else FluidVariant.blank()
            sync.airAmount = amount.toInt().coerceAtLeast(0)
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
        SLOT_AIR_INPUT -> !stack.isEmpty && (stack.item is AirCell || isCompressedAirFluidCell(stack))
        SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY -> false
        SLOT_UPGRADE_0, SLOT_UPGRADE_1 -> stack.item is EjectorUpgrade || stack.item is PullingUpgrade
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.blast_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        BlastFurnaceScreenHandler(syncId, playerInventory, this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        temperature = nbt.getInt(NBT_TEMPERATURE).coerceIn(0, BlastFurnaceSync.TEMP_MAX)
        tempProgress = nbt.getInt(NBT_TEMP_PROGRESS).coerceAtLeast(0)
        tempDecayProgress = nbt.getInt(NBT_TEMP_DECAY).coerceAtLeast(0)
        sync.temperature = temperature
        airTankInternal.setStoredAir(nbt.getLong(NBT_AIR_AMOUNT).coerceAtLeast(0L))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
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
        return recipeManager.getFirstMatch(getRecipeType<BlastFurnaceRecipe>(), inv, world ?: return null).orElse(null)
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
                    airConsumedThisSteel = 0L
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
            if (!consumeAirForTick(progressMax)) {
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
                airConsumedThisSteel = 0L
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
            airConsumedThisSteel = 0L

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

    /** 按当前温度 / 进度消耗压缩空气，返回是否成功消耗（可用于推进进度） */
    private fun consumeAirForTick(progressMax: Int): Boolean {
        val airPerSteel = BlastFurnaceSync.getAirPerSteelDroplets(temperature).toLong()
        // 当前进度应消耗的 droplet 总量
        val expected = airPerSteel * sync.progress / progressMax
        val need = expected - airConsumedThisSteel
        if (need <= 0L) return true  // 已消耗足够

        val consumed = airTankInternal.tryConsumeAir(need)
        if (consumed < need) return false  // 空气不足

        airConsumedThisSteel += consumed
        return true
    }

    private fun fillAirTankFromSlot() {
        if (airTankInternal.remainingSpace() < FluidConstants.BUCKET) return
        val airSlot = getStack(SLOT_AIR_INPUT)
        if (airSlot.isEmpty) return
        val airItem = airSlot.item

        // AirCell / FluidCellItem[compressed_air]：消耗 1 个，返还 empty_cell
        if (airItem is AirCell || airItem is FluidCellItem) {
            if (airItem is FluidCellItem && !isCompressedAirFluidCell(airSlot)) return
            val emptySlot = getStack(SLOT_OUTPUT_EMPTY)
            val emptyCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
            val canAcceptEmpty = emptySlot.isEmpty ||
                (ItemStack.areItemsEqual(emptySlot, emptyCell) && emptySlot.count + 1 <= emptyCell.maxCount)
            if (!canAcceptEmpty) return

            airSlot.decrement(1)
            if (airSlot.isEmpty) setStack(SLOT_AIR_INPUT, ItemStack.EMPTY)
            if (emptySlot.isEmpty) setStack(SLOT_OUTPUT_EMPTY, emptyCell)
            else emptySlot.increment(1)
            airTankInternal.tryFillAir(FluidConstants.BUCKET)
            return
        }

        // 任意 Fabric Transfer API 流体容器：尝试提取 1 桶 compressed_air
        val ctx = ContainerItemContext.withConstant(airSlot)
        val storage = ctx.find(FluidStorage.ITEM) ?: return
        Transaction.openOuter().use { tx ->
            for (view in storage) {
                if (view.isResourceBlank || view.amount < FluidConstants.BUCKET) continue
                if (!ModFluids.isCompressedAir(view.resource.fluid)) continue
                val extracted = view.extract(view.resource, FluidConstants.BUCKET, tx)
                if (extracted < FluidConstants.BUCKET) continue
                tx.commit()
                airSlot.decrement(1)
                val remaining = ctx.itemVariant.toStack(ctx.amount.toInt().coerceAtLeast(0))
                setStack(SLOT_AIR_INPUT, if (airSlot.isEmpty) ItemStack.EMPTY else remaining)
                airTankInternal.tryFillAir(FluidConstants.BUCKET)
                return
            }
        }
    }

    private fun isCompressedAirFluidCell(stack: ItemStack): Boolean {
        // 先检查本模组 fluid_cell NBT
        if (stack.item is FluidCellItem) {
            val variant = stack.getFluidCellVariant() ?: return false
            return ModFluids.isCompressedAir(variant.fluid)
        }
        // 再检查任意 Fabric Transfer API 流体容器
        val storage = FluidStorage.ITEM.find(stack, null) ?: return false
        for (view in storage) {
            if (!view.isResourceBlank && view.amount >= FluidConstants.BUCKET && ModFluids.isCompressedAir(view.resource.fluid)) {
                return true
            }
        }
        return false
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
