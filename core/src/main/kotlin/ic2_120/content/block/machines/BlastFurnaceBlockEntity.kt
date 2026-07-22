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
import ic2_120.content.storage.IRoutedSidedInventory
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
 * HU 缓存上限 1,280 HU。温度 0–1,700。
 *
 * 纯 HU 累积温度模型：温度变化完全由 HU 流入/流出驱动，不再使用固定 tick 计数表。
 * 每 tick 净HU = 缓存 HU − 散热(T)，散热随温度线性增长（见 [getDissipation]）：
 * - T = 0..1401：散热 0 → 50 HU/t
 * - T = 1402..1700：散热 50 → 100 HU/t
 * 净HU > 0 累积到 [HEAT_PER_DEGREE] 升 1 度（速度 ∝ 净HU）；< 0 反向累积降 1 度；= 0 稳态。
 * 稳态温度完全由 HU 输入决定：50 HU/t → 1401，100 HU/t → 1700，中间线性。
 *
 * 温度逻辑每 tick 独立运行，工作时不再冻结温度。
 * 工作条件：温度 ≥ 1401 且有材料、产物可接收、压缩空气充足。炼钢仅消耗压缩空气，不额外消耗 HU。
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
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, IRoutedSidedInventory, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

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
        private const val NBT_HEAT_ACCUM = "HeatAccum"
        private const val NBT_AIR_AMOUNT = "AirAmount"
        private const val NBT_HU_BUFFER = "HuBuffer"
        private const val NBT_AIR_CONSUMED = "AirConsumed"

        /** HU 缓存容量：1280 HU，覆盖最高 100 HU/tick 散热的多个 tick 窗口 */
        const val HU_BUFFER_CAPACITY: Long = 1280L

        /**
         * 每升高 1 度需要的净 HU 总量（纯 HU 累积模型）。
         * 升温速度 = 净HU/t ÷ HEAT_PER_DEGREE 度/tick，严格正比于 HU 输入。
         */
        const val HEAT_PER_DEGREE: Int = 100

        /** 散热定点精度倍数（散热值 ×1000 存为整数，避免浮点） */
        private const val DISSIPATION_SCALE = 1000

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

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData(this)
    val sync = BlastFurnaceSync(syncedData)

    private var huReceivedThisTick: Long = 0L
    private var huBuffer: Long = 0L  // HU 缓存，上限 HU_BUFFER_CAPACITY (1280)
    private var temperature: Int = 0
    /** 升温/降温累积进度（单位 = 净HU × DISSIPATION_SCALE，正值趋近 +HEAT_PER_DEGREE*SCALE 升温，负值趋近 -... 降温） */
    private var heatAccum: Long = 0L
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
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
                sync.airAmount = amount.toInt().coerceAtLeast(0)
                actual
            }
        }

        fun tryFillAir(droplets: Long): Long {
            if (droplets <= 0L) return 0L
            val space = tankCapacity - amount
            val actual = minOf(droplets, space)
            if (actual <= 0L) return 0L
            return Transaction.openOuter().use { tx ->
                updateSnapshots(tx)
                amount += actual
                if (variant.isBlank) variant = FluidVariant.of(ModFluids.COMPRESSED_AIR_STILL)
                tx.commit()
                sync.airAmount = amount.toInt().coerceAtLeast(0)
                actual
            }
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
        heatAccum = nbt.getLong(NBT_HEAT_ACCUM)
        huBuffer = nbt.getLong(NBT_HU_BUFFER).coerceIn(0L, HU_BUFFER_CAPACITY)
        sync.temperature = temperature
        airTankInternal.setStoredAir(nbt.getLong(NBT_AIR_AMOUNT).coerceAtLeast(0L))
        // 优先从 NBT 恢复；旧存档没有此 key 时从 progress 反推，避免 reload 后 need 暴涨
        airConsumedThisSteel = if (nbt.contains(NBT_AIR_CONSUMED)) {
            nbt.getLong(NBT_AIR_CONSUMED).coerceAtLeast(0L)
        } else {
            val progressMax = getProgressMax(temperature)
            if (progressMax > 0) {
                BlastFurnaceSync.getAirPerSteelDroplets(temperature).toLong() * sync.progress / progressMax
            } else 0L
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_HU_RECEIVED, huReceivedThisTick)
        nbt.putLong(NBT_HU_BUFFER, huBuffer)
        nbt.putInt(NBT_TEMPERATURE, temperature)
        nbt.putLong(NBT_HEAT_ACCUM, heatAccum)
        nbt.putLong(NBT_AIR_AMOUNT, airTankInternal.amount)
        nbt.putLong(NBT_AIR_CONSUMED, airConsumedThisSteel)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        val space = (HU_BUFFER_CAPACITY - huBuffer).coerceAtLeast(0L)
        if (space <= 0L) {
            // 缓存满，不接受热量
            heatReceivedLastTick = true
            return 0L
        }
        val accepted = minOf(hu, space)
        huBuffer += accepted
        huReceivedThisTick += accepted
        heatReceivedLastTick = true
        markDirty()
        return accepted
    }

    private fun getRecipeForInput(input: ItemStack): BlastFurnaceRecipe? {
        if (input.isEmpty) return null
        val inv = BlastFurnaceRecipe.Input(input)
        val recipeManager = world?.recipeManager ?: return null
        return recipeManager.getFirstMatch(getRecipeType<BlastFurnaceRecipe>(), inv, world ?: return null).orElse(null)
    }

    /**
     * 当前温度下的散热（维持需求），返回 ×[DISSIPATION_SCALE] 的定点整数。
     *
     * 两段线性，穿过稳态锚点 (50 HU/t, 1401) 与 (100 HU/t, 1700)：
     * - T = 0..1401：0 → 50 HU/t（线性，T=0 无散热，便于冷启动）
     * - T = 1402..1700：50 → 100 HU/t（线性）
     *
     * 稳态温度（散热 = HU 输入）：50 HU/t → 1401，100 HU/t → 1700，中间线性。
     */
    private fun getDissipation(temp: Int): Long {
        val t = temp.coerceIn(0, BlastFurnaceSync.TEMP_MAX)
        return if (t <= BlastFurnaceSync.TEMP_WORK_MIN) {
            50L * DISSIPATION_SCALE * t / BlastFurnaceSync.TEMP_WORK_MIN
        } else {
            50L * DISSIPATION_SCALE + (t - BlastFurnaceSync.TEMP_WORK_MIN).toLong() * 50L * DISSIPATION_SCALE / (BlastFurnaceSync.TEMP_MAX - BlastFurnaceSync.TEMP_WORK_MIN)
        }
    }

    /** 当前温度下的加工总 tick 数（三段线性插值） */
    private fun getProgressMax(temp: Int): Int = BlastFurnaceSync.getProgressMax(temp)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.huInput = huBuffer.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.temperature = temperature
        sync.heatInput = if (heatReceivedLastTick) 1 else 0

        val input = getStack(SLOT_INPUT)
        val outputSteel = getStack(SLOT_OUTPUT_STEEL)
        val outputSlag = getStack(SLOT_OUTPUT_SLAG)

        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        fillAirTankFromSlot()

        // 当前温度下的散热（维持需求，定点 ×DISSIPATION_SCALE）
        val dissipation = getDissipation(temperature)
        // 每度累积阈值（定点）：HEAT_PER_DEGREE × SCALE
        val accumThreshold = HEAT_PER_DEGREE.toLong() * DISSIPATION_SCALE

        // ---- 温度逻辑（纯 HU 累积模型，每 tick 独立运行，与是否工作无关）----
        // 每个 HU 只能用一次：先扣散热（维持），剩余的全部转升温累积。
        // 这样缓存不会因"净HU用缓存总量算"而重复累积（曾导致秒升 1700 的 bug）。
        // 缓存（huBuffer）仅用于吸收输入波动：本 tick 输入 + 上轮结余，扣散热后若有剩余则全用于升温。
        // 散热随温度线性增长（见 getDissipation），稳态温度完全由 HU 输入决定：
        //   50 HU/t → 1401，100 HU/t → 1700，中间线性。
        val bufferScaled = huBuffer * DISSIPATION_SCALE  // 缓存 HU 折算为定点
        val bufferAfterDissipation = bufferScaled - dissipation  // 扣散热后的剩余（定点）
        if (bufferAfterDissipation >= 0L) {
            // 缓存足以覆盖散热：扣散热，剩余全用于升温累积，清空缓存（不重复计算）
            huBuffer = 0L
            sync.warmActive = 1
            if (bufferAfterDissipation > 0L && temperature < BlastFurnaceSync.TEMP_MAX) {
                heatAccum += bufferAfterDissipation
                while (heatAccum >= accumThreshold && temperature < BlastFurnaceSync.TEMP_MAX) {
                    heatAccum -= accumThreshold
                    temperature++
                    sync.temperature = temperature
                }
            }
            // bufferAfterDissipation == 0：稳态，温度不变
        } else {
            // 缓存不足以覆盖散热：扣空缓存，反向累积降温
            huBuffer = 0L
            sync.warmActive = 0
            if (temperature > 0) {
                val deficitScaled = -bufferAfterDissipation  // 散热超出缓存的部分（正定点值）
                heatAccum -= deficitScaled
                while (heatAccum <= -accumThreshold && temperature > 0) {
                    heatAccum += accumThreshold
                    temperature--
                    sync.temperature = temperature
                }
                // 温度跌破工作阈值时清空炼钢进度
                if (temperature < BlastFurnaceSync.TEMP_WORK_MIN && sync.progress != 0) {
                    sync.progress = 0
                    airConsumedThisSteel = 0L
                }
            }
        }

        // ---- 炼钢逻辑（独立于温度逻辑）----
        // HU 只用于升温/维持（见上方），工作仅消耗压缩空气。
        // progressMax 在温度逻辑之后计算，确保用更新后的温度（避免温度刚跨过 1401 时 progressMax=0 导致除零）。
        val progressMax = getProgressMax(temperature)
        val hasInput = !input.isEmpty && getRecipeForInput(input) != null
        val canAcceptOutput = canAcceptOutput(outputSteel, outputSlag)
        val canWork = temperature >= BlastFurnaceSync.TEMP_WORK_MIN && hasInput && canAcceptOutput

        if (canWork) {
            // 消耗压缩空气，空气不足则不推进进度（不产出）
            if (!consumeAirForTick(progressMax)) {
                markDirty()
                setActiveState(world, pos, state, false)
                sync.huInput = huBuffer.toInt()
                huReceivedThisTick = 0L
                heatReceivedLastTick = false
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
                setActiveState(world, pos, state, true)
                sync.huInput = huBuffer.toInt()
                huReceivedThisTick = 0L
                heatReceivedLastTick = false
                return
            }

            sync.progress = nextProgress
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            // 温度不够 / 无材料 / 产物满：重置进度，不工作
            if (sync.progress != 0) sync.progress = 0
            airConsumedThisSteel = 0L
            setActiveState(world, pos, state, false)
        }

        markDirty()
        sync.huInput = huBuffer.toInt()
        huReceivedThisTick = 0L
        heatReceivedLastTick = false
    }

    /** 按当前温度 / 进度消耗压缩空气，返回是否成功消耗（可用于推进进度） */
    private fun consumeAirForTick(progressMax: Int): Boolean {
        if (progressMax <= 0) return false  // 防御：温度低于工作阈值时不应调用
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
        val storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) ?: return false
        for (view in storage) {
            if (!view.isResourceBlank && view.amount >= FluidConstants.BUCKET && ModFluids.isCompressedAir(view.resource.fluid)) {
                return true
            }
        }
        return false
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
