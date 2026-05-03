package ic2_120.content.block.nuclear

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.block.IGenerator
import ic2_120.content.block.IOwned
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.ReactorHeatVentBase
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.reactor.IBaseReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IReactorComponent
import ic2_120.content.network.NetworkManager
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.SlotHeatEnergyInfo
import ic2_120.content.screen.NuclearReactorScreenHandler
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.IRedstoneControlSupport
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.AbstractFireBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Box
// import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 核反应堆方块实体。
 *
 * 多方块结构：中心为核反应堆，六面可各接触 0 或 1 个核反应仓。
 * 容量 = 27 + 相邻反应仓数 * 9，最大 81 格。
 * 所有 NBT 存在反应堆 BE 上。
 * 电力等级 5，视为发电机（IGenerator），不实现电池充电。
 */
@ModBlockEntity(block = NuclearReactorBlock::class)
class NuclearReactorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, IGenerator, ITieredMachine, IReactor,
    ExtendedScreenHandlerFactory<PacketByteBuf>, IRedstoneControlSupport, IOwned {

    override val tier: Int = NuclearReactorSync.REACTOR_TIER

    override var ownerUuid: java.util.UUID? = null

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    private var tickOffset: Int = 0

    private var emitHeatBuffer: Int = 0

    private var outputAccumulator: Float = 0f

    private var pendingEnergyOutput: Long = 0L
    private var cycleStartHeatSnapshot: Int = 0
    private var inventoryChangedSinceLastCycle: Boolean = false
    private var debugPass: Int = -1
    private var debugHeatRun: Boolean = false
    private var debugSlotX: Int = -1
    private var debugSlotY: Int = -1
    private var debugComponentId: Identifier? = null

    private var cycleAddHeatTotal: Int = 0
    private var cycleSetHeatDeltaTotal: Int = 0
    private var cycleEmitHeatTotal: Int = 0

    override var redstoneInverted: Boolean = false

    /** 燃料棒是否活跃（受红石控制）；其他器件始终工作 */
    private var fuelActive: Boolean = true

    private var totalHeatProduced: Int = 0

    private var totalHeatDissipated: Int = 0

    val slotHeatInfo = mutableMapOf<Int, SlotHeatEnergyInfo>()

    // 热模式相关字段
    private var thermalModeCache: Boolean = false
    private var lastModeCheckTick: Long = -1L
    private var ventDissipatedHeat: Int = 0  // 散热片散失的热量，用于冷却液转换

    // 红石接口状态管理：存储所有红石接口的位置及其允许运行状态
    private val redstonePortStates = mutableMapOf<BlockPos, Boolean>()

    // 流体存储（冷却液和热冷却液）
    val inputTank = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = mbToDroplets(NuclearReactorSync.COOLANT_TANK_CAPACITY_MB)

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == ModFluids.COOLANT_STILL

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.inputCoolantMb = dropletsToMb(amount)
            markDirty()
        }
    }

    val outputTank = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity = mbToDroplets(NuclearReactorSync.COOLANT_TANK_CAPACITY_MB)

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity
        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == ModFluids.HOT_COOLANT_STILL

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun canExtract(variant: FluidVariant): Boolean = true

        override fun onFinalCommit() {
            sync.outputHotCoolantMb = dropletsToMb(amount)
            markDirty()
        }
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            val target = when (resource.fluid) {
                ModFluids.COOLANT_STILL, ModFluids.COOLANT_FLOWING -> FluidVariant.of(ModFluids.COOLANT_STILL)
                else -> return 0L
            }
            return inputTank.insert(target, maxAmount, transaction)
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            val target = when (resource.fluid) {
                ModFluids.HOT_COOLANT_STILL, ModFluids.HOT_COOLANT_FLOWING -> FluidVariant.of(ModFluids.HOT_COOLANT_STILL)
                else -> return 0L
            }
            return outputTank.extract(target, maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()

            if (!inputTank.variant.isBlank && inputTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = inputTank.variant
                    override fun getAmount(): Long = inputTank.amount
                    override fun getCapacity(): Long = inputTank.capacity
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }

            if (!outputTank.variant.isBlank && outputTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = outputTank.variant
                    override fun getAmount(): Long = outputTank.amount
                    override fun getCapacity(): Long = outputTank.capacity
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        outputTank.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }

            return views.iterator()
        }
    }

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = NuclearReactorSync(
        syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        NuclearReactorBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun setStack(slot: Int, stack: ItemStack) {
        val oldStack = getStack(slot).copy()
        if (!stack.isEmpty) {
            if (slot < MAX_SLOTS) {
                if (stack.item !is IBaseReactorComponent) return
                if (stack.item is IBaseReactorComponent && !(stack.item as IBaseReactorComponent).canBePlacedIn(
                        stack,
                        this
                    )
                ) return
                if (slot >= currentCapacity()) return
            }
        }
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        if (slot < MAX_SLOTS && !ItemStack.areItemsAndComponentsEqual(oldStack, stack)) {
            inventoryChangedSinceLastCycle = true
            val oldId = Registries.ITEM.getId(oldStack.item)
            val newId = Registries.ITEM.getId(stack.item)
            LOG.info(
                "[燃料棒变更] pos={} slot={} old={}x{} new={}x{}",
                pos,
                slot,
                oldId,
                oldStack.count,
                newId,
                stack.count
            )
        }
        markDirty()
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() {
        super.markDirty()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return true
        if (slot < MAX_SLOTS) {
            if (stack.item !is IBaseReactorComponent) return false
            if (!(stack.item as IBaseReactorComponent).canBePlacedIn(stack, this)) return false
            if (slot >= currentCapacity()) return false
        }
        return true
    }

    override fun getScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeVarInt(currentCapacity())
        buf.writeBoolean(isThermalMode())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.nuclear_reactor")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        NuclearReactorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            currentCapacity(),
            this,
            isThermalMode()
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(NuclearReactorSync.NBT_ENERGY_STORED).coerceIn(0L, NuclearReactorSync.ENERGY_CAPACITY)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.temperature = nbt.getInt(NuclearReactorSync.NBT_HEAT_STORED).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        tickOffset = if (nbt.contains("TickOffset")) nbt.getInt("TickOffset").coerceIn(0, 19) else -1
        pendingEnergyOutput = nbt.getLong("PendingEnergyOutput").coerceIn(0L, NuclearReactorSync.ENERGY_CAPACITY)
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false

        // 读取热模式和流体数据
        thermalModeCache = nbt.getBoolean("ThermalMode")
        lastModeCheckTick = nbt.getLong("LastModeCheckTick")

        // 读取流体储罐
        if (nbt.contains("InputCoolant")) {
            inputTank.variant = FluidVariant.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("InputCoolant")).result().map { it.first }.orElse(FluidVariant.blank())
        }
        inputTank.amount = nbt.getLong("InputCoolantAmount")

        if (nbt.contains("OutputHotCoolant")) {
            outputTank.variant = FluidVariant.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("OutputHotCoolant")).result().map { it.first }.orElse(FluidVariant.blank())
        }
        outputTank.amount = nbt.getLong("OutputHotCoolantAmount")
        ownerUuid = if (nbt.containsUuid("OwnerUUID")) nbt.getUuid("OwnerUUID") else null
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(NuclearReactorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NuclearReactorSync.NBT_HEAT_STORED, sync.temperature)
        if (tickOffset >= 0) nbt.putInt("TickOffset", tickOffset)
        nbt.putLong("PendingEnergyOutput", pendingEnergyOutput)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)

        // 写入热模式和流体数据
        nbt.putBoolean("ThermalMode", thermalModeCache)
        nbt.putLong("LastModeCheckTick", lastModeCheckTick)

        // 写入流体储罐
        nbt.put("InputCoolant", FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, inputTank.variant).result().orElse(NbtCompound()))
        nbt.putLong("InputCoolantAmount", inputTank.amount)
        nbt.put("OutputHotCoolant", FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, outputTank.variant).result().orElse(NbtCompound()))
        nbt.putLong("OutputHotCoolantAmount", outputTank.amount)
        ownerUuid?.let { nbt.putUuid("OwnerUUID", it) }
    }

    private fun currentCapacity(): Int {
        val w = world ?: return NuclearReactorSync.BASE_SLOTS
        var chamberCount = 0
        for (dir in Direction.values()) {
            if (w.getBlockState(pos.offset(dir)).block is ReactorChamberBlock) chamberCount++
        }
        return NuclearReactorSync.BASE_SLOTS + chamberCount * NuclearReactorSync.SLOTS_PER_CHAMBER
    }

    override fun getWorld(): World? = world
    override fun getPos(): BlockPos = pos
    override fun getHeat(): Int = sync.temperature
    override fun setHeat(heat: Int) {
        val old = sync.temperature
        sync.temperature = heat.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val delta = sync.temperature - old
        cycleSetHeatDeltaTotal += delta
        if (sync.isThermalMode == 1 && delta != 0) {
            LOG.info(
                "[热量轨迹] type=setHeat delta={} old={} new={} pass={} heatRun={} slot=({}, {}) component={}",
                delta,
                old,
                sync.temperature,
                debugPass,
                debugHeatRun,
                debugSlotX,
                debugSlotY,
                debugComponentId ?: "unknown"
            )
        }
    }

    override fun addHeat(amount: Int): Int {
        val old = sync.temperature
        sync.temperature = (sync.temperature + amount).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val applied = sync.temperature - old
        cycleAddHeatTotal += applied
        if (sync.isThermalMode == 1 && applied != 0) {
            LOG.info(
                "[热量轨迹] type=addHeat req={} applied={} old={} new={} pass={} heatRun={} slot=({}, {}) component={}",
                amount,
                applied,
                old,
                sync.temperature,
                debugPass,
                debugHeatRun,
                debugSlotX,
                debugSlotY,
                debugComponentId ?: "unknown"
            )
        }
        return sync.temperature
    }

    override fun getMaxHeat(): Int {
        val base = NuclearReactorSync.HEAT_CAPACITY
        var bonus = 0
        val cols = getReactorCols()

        // 统计反应堆隔板的热量加成
        for (y in 0 until 9) {
            for (x in 0 until cols) {
                val stack = getItemAt(x, y) ?: continue
                when (stack.item) {
                    is ic2_120.content.item.ReactorPlatingItem -> bonus += ic2_120.content.item.ReactorPlatingItem.HEAT_BONUS
                    is ic2_120.content.item.ReactorHeatPlatingItem -> bonus += ic2_120.content.item.ReactorHeatPlatingItem.HEAT_BONUS
                }
            }
        }

        return base + bonus
    }
    override fun setMaxHeat(maxHeat: Int) {}
    override fun addEmitHeat(heat: Int) {
        emitHeatBuffer += heat
        cycleEmitHeatTotal += heat
        if (sync.isThermalMode == 1 && heat != 0) {
            LOG.info(
                "[热量轨迹] type=emitHeat delta={} emitBufferNow={} pass={} heatRun={} slot=({}, {}) component={}",
                heat,
                emitHeatBuffer,
                debugPass,
                debugHeatRun,
                debugSlotX,
                debugSlotY,
                debugComponentId ?: "unknown"
            )
        }
    }

    override fun getHeatEffectModifier(): Float = 1f
    override fun setHeatEffectModifier(hem: Float) {}
    override fun getReactorEnergyOutput(): Float = outputAccumulator
    override fun addOutput(energy: Float): Float {
        outputAccumulator += energy
        return outputAccumulator
    }

    override fun getReactorCols(): Int = currentCapacity() / 9
    override fun getReactorRows(): Int = 9
    override fun getItemAt(x: Int, y: Int): ItemStack? {
        if (x < 0 || x >= getReactorCols() || y < 0 || y >= 9) return null
        val stack = getStack(x * 9 + y)
        return if (stack.isEmpty) null else stack
    }

    override fun setItemAt(x: Int, y: Int, stack: ItemStack?) {
        if (x in 0 until getReactorCols() && y in 0 until 9) {
            setStack(x * 9 + y, stack ?: ItemStack.EMPTY)
        }
    }

    override fun produceEnergy(): Boolean = fuelActive
    override fun getTickRate(): Int = 20
    override fun isFluidCooled(): Boolean = isThermalMode()
    override fun getCycleStartHeat(): Int = cycleStartHeatSnapshot
    override fun getEffectiveHeatForDrain(): Int = (sync.temperature + emitHeatBuffer).coerceAtLeast(0)

    override fun hasCoolant(): Boolean {
        return if (isThermalMode()) inputTank.amount > 0 else false
    }

    private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L

    private fun dropletsToMb(amount: Long): Int =
        (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)

    /**
     * 检测是否为热模式（5×5×5 外壳结构）
     * 结构要求：反应堆必须在 5×5×5 立方体的中心，外壳由 vessel/redstone_port/fluid_port/access_hatch 组成。
     */
    private fun detectThermalMode(): Boolean {
        val w = world ?: return false
        // 验证 BE 位置：当前方块必须是反应堆，否则 BE 可能创建在错误位置
        val blockAtPos = w.getBlockState(pos).block
        if (blockAtPos !is NuclearReactorBlock) {
            // spawnErrorParticles(w, pos)
            return false
        }

        val centerOffset = 2  // 5×5×5 = ±2 from center
        var fluidPortCount = 0

        // 检查外层 5×5×5（跳过中心）
        for (dx in -centerOffset..centerOffset) {
            for (dy in -centerOffset..centerOffset) {
                for (dz in -centerOffset..centerOffset) {
                    if (dx == 0 && dy == 0 && dz == 0) continue  // 跳过中心反应堆
                    // 跳过内部 3×3×3 核心区域
                    if (dx in -1..1 && dy in -1..1 && dz in -1..1) continue

                    val checkPos = pos.add(dx, dy, dz)
                    val block = w.getBlockState(checkPos).block

                    // 必须是四种容器方块之一：压力容器、红石接口、流体接口、访问接口
                    if (block !is ReactorVesselBlock &&
                        block !is ReactorRedstonePortBlock &&
                        block !is ReactorFluidPortBlock &&
                        block !is ReactorAccessHatchBlock
                    ) {
                        // spawnErrorParticles(w, checkPos)
                        return false
                    }
                    if (block is ReactorFluidPortBlock) fluidPortCount++
                }
            }
        }

        // 外壳上至少需要 2 个 FluidPort（1 输入冷却液 + 1 输出热冷却液）
        if (fluidPortCount < 2) {
            // spawnErrorParticles(w, pos)
            return false
        }
        return true
    }

    /**
     * 在指定方块位置生成错误提示粒子，用于调试热模式结构问题。
     */
//    private fun spawnErrorParticles(world: World, blockPos: BlockPos) {
//        val serverWorld = world as? ServerWorld ?: return
//        val x = blockPos.x + 0.5
//        val y = blockPos.y + 0.5
//        val z = blockPos.z + 0.5
//        serverWorld.spawnParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.2, 0.2, 0.2, 0.02)
//        serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 4, 0.15, 0.15, 0.15, 0.01)
//    }

    /**
     * 判断当前是否为热模式（带缓存）
     */
    private fun isThermalMode(): Boolean {
        val w = world ?: return false
        val currentTime = w.time

        // tickOffset 应优先存储并采用
        val offset = if (tickOffset in 0..19) tickOffset else ((pos.asLong() xor 0x5DEECE66DL).toInt() and 19)
        if ((currentTime + offset) % 20 == 0L) {
            //20tick检测一次
            thermalModeCache = detectThermalMode()
            lastModeCheckTick = currentTime
            sync.isThermalMode = if (thermalModeCache) 1 else 0
            markDirty()
        }
        return thermalModeCache
    }

    /**
     * 判断给定位置是否在反应堆的 5×5×5 结构内部。
     * 结构以反应堆为中心，范围 (pos-2, pos+2)，中心为反应堆本身。
     */
    fun isPositionInStructure(checkPos: BlockPos): Boolean {
        val dx = kotlin.math.abs(checkPos.x - pos.x)
        val dy = kotlin.math.abs(checkPos.y - pos.y)
        val dz = kotlin.math.abs(checkPos.z - pos.z)
        return dx <= 2 && dy <= 2 && dz <= 2 && (dx != 0 || dy != 0 || dz != 0)
    }

    /**
     * 获取流体存储（用于流体接口）
     */
    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant> {
        if (!isThermalMode()) return Storage.empty()
        // side=null（如探针/Jade）时返回组合存储，便于同时展示冷却液与热冷却液。
        return when (side) {
            // Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST -> inputTank
            // Direction.DOWN -> outputTank
            // null -> ioStorage
            // else -> Storage.empty()
            else -> ioStorage
        }
    }

    /**
     * 更新红石接口的状态
     * @param portPos 红石接口的位置
     * @param allowsRun 该红石接口是否允许运行（基于红石信号和反转设置）
     */
    fun updateRedstonePortState(port: ReactorRedstonePortBlockEntity, allowsRun: Boolean) {
        val portPos = port.pos
        redstonePortStates[portPos] = allowsRun
        markDirty()
    }

    /**
     * 检查所有红石接口是否允许运行
     * 如果有任何红石接口禁止运行，则反应堆不能运行
     */
    private fun checkRedstonePortsAllowRun(): Boolean {
        // 如果没有红石接口，默认允许运行
        if (redstonePortStates.isEmpty()) return true

        // 如果有任何红石接口禁止运行，则反应堆不能运行
        return redstonePortStates.values.all { it }
    }

    override fun addHeatProduced(amount: Int) {
        totalHeatProduced += amount
    }

    override fun addHeatDissipated(amount: Int) {
        totalHeatDissipated += amount

        // 热模式下，记录散失的热量用于冷却液转换
        if (isThermalMode()) {
            ventDissipatedHeat += amount
        }
    }

    override fun addSlotHeatInfo(slot: Int, heatProduced: Int, heatDissipated: Int, energyOutput: Float) {
        val current = slotHeatInfo.getOrDefault(slot, SlotHeatEnergyInfo(0, 0, 0f))
        slotHeatInfo[slot] = SlotHeatEnergyInfo(
            current.heatProduced + heatProduced,
            current.heatDissipated + heatDissipated,
            current.energyOutput + energyOutput
        )
    }

    /** 热模式：处理流体槽的容器投入与提取 */
    private fun processFluidSlots() {
        handleCoolantInput()
        fillHotCoolantOutput()
    }

    private fun handleCoolantInput() {
        val input = getStack(SLOT_COOLANT_INPUT)
        if (input.isEmpty) return

        val parsed = resolveCoolantInput(input) ?: return
        val space = inputTank.capacity - inputTank.amount
        if (space < FluidConstants.BUCKET) return

        val emptyOutput = getStack(SLOT_COOLANT_OUTPUT)
        if (!canMergeIntoSlot(emptyOutput, parsed.emptyContainer)) return

        val tx = Transaction.openOuter()
        val inserted = inputTank.insert(FluidVariant.of(ModFluids.COOLANT_STILL), FluidConstants.BUCKET, tx)
        if (inserted < FluidConstants.BUCKET) {
            tx.abort()
            return
        }
        tx.commit()

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_COOLANT_INPUT, ItemStack.EMPTY)
        if (emptyOutput.isEmpty) setStack(SLOT_COOLANT_OUTPUT, parsed.emptyContainer.copy())
        else emptyOutput.increment(1)
        markDirty()
    }

    private fun fillHotCoolantOutput() {
        if (outputTank.amount < FluidConstants.BUCKET) return

        val emptyInput = getStack(SLOT_HOT_COOLANT_INPUT)
        if (emptyInput.isEmpty) return

        val filled = resolveHotCoolantOutput(emptyInput) ?: return

        val filledOutput = getStack(SLOT_HOT_COOLANT_OUTPUT)
        if (!canMergeIntoSlot(filledOutput, filled)) return

        val tx = Transaction.openOuter()
        val extracted = outputTank.extract(FluidVariant.of(ModFluids.HOT_COOLANT_STILL), FluidConstants.BUCKET, tx)
        if (extracted < FluidConstants.BUCKET) {
            tx.abort()
            return
        }
        tx.commit()

        emptyInput.decrement(1)
        if (emptyInput.isEmpty) setStack(SLOT_HOT_COOLANT_INPUT, ItemStack.EMPTY)
        if (filledOutput.isEmpty) setStack(SLOT_HOT_COOLANT_OUTPUT, filled.copy())
        else filledOutput.increment(1)
        markDirty()
    }

    private fun resolveCoolantInput(stack: ItemStack): CoolantInputInfo? {
        val emptyCell = ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")))
        return when {
            stack.item == ModFluids.COOLANT_BUCKET -> CoolantInputInfo(ItemStack(Items.BUCKET))
            stack.item == Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "coolant_cell")) -> CoolantInputInfo(emptyCell)
            stack.item is FluidCellItem && stack.getFluidCellVariant()?.fluid?.let {
                it == ModFluids.COOLANT_STILL || it == ModFluids.COOLANT_FLOWING
            } == true -> CoolantInputInfo(emptyCell)
            else -> null
        }
    }

    private fun resolveHotCoolantOutput(emptyContainer: ItemStack): ItemStack? {
        return when {
            emptyContainer.item == Items.BUCKET -> ItemStack(ModFluids.HOT_COOLANT_BUCKET)
            emptyContainer.item == Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell")) ->
                fluidToFilledCellStack(ModFluids.HOT_COOLANT_STILL)
            emptyContainer.item is FluidCellItem && emptyContainer.isFluidCellEmpty() ->
                ItemStack(emptyContainer.item).apply { setFluidCellVariant(FluidVariant.of(ModFluids.HOT_COOLANT_STILL)) }
            else -> null
        }
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.areItemsAndComponentsEqual(current, toInsert) && current.count < current.maxCount)
    }

    private data class CoolantInputInfo(val emptyContainer: ItemStack)

    private data class VentBackfillResult(
        val targetCount: Int,
        val appliedHeat: Long,
        val remainingHeat: Long
    )

    /**
     * 将未能转换为热冷却液的 HU 均分回灌给所有带 selfVent 的散热片，
     * 通过提升其内部热量来等效降低耐久。
     */
    private fun backfillUnconvertedHeatToSelfVents(unconvertedHeat: Long): VentBackfillResult {
        if (unconvertedHeat <= 0L) {
            return VentBackfillResult(0, 0L, 0L)
        }

        val targets = mutableListOf<Int>()
        val cap = currentCapacity()
        for (slot in 0 until cap) {
            val stack = getStack(slot)
            val item = stack.item
            if (!stack.isEmpty && item is ReactorHeatVentBase && item.hasSelfVent()) {
                targets.add(slot)
            }
        }

        if (targets.isEmpty()) {
            return VentBackfillResult(0, 0L, unconvertedHeat)
        }

        val baseShare = unconvertedHeat / targets.size
        var remainder = (unconvertedHeat % targets.size).toInt()
        var applied = 0L

        for (slot in targets) {
            val share = baseShare + if (remainder > 0) {
                remainder--
                1L
            } else {
                0L
            }
            if (share <= 0L) continue

            val stack = getStack(slot)
            val vent = stack.item as? ReactorHeatVentBase ?: continue
            val x = slot / 9
            val y = slot % 9

            val shareInt = share.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val overflow = vent.alterHeat(stack, this, x, y, shareInt)
            val accepted = (shareInt - overflow).coerceAtLeast(0)
            applied += accepted.toLong()
        }

        if (applied > 0L) {
            markDirty()
        }

        val remaining = (unconvertedHeat - applied).coerceAtLeast(0L)
        return VentBackfillResult(targets.size, applied, remaining)
    }

    private fun calculateStoredComponentHeat(): Long {
        val cols = getReactorCols()
        var total = 0L
        for (x in 0 until cols) {
            for (y in 0 until 9) {
                val stack = getItemAt(x, y) ?: continue
                val comp = stack.item as? IReactorComponent ?: continue
                if (!comp.canStoreHeat(stack, this, x, y)) continue
                total += comp.getCurrentHeat(stack, this, x, y).toLong().coerceAtLeast(0L)
            }
        }
        return total
    }

    fun dropOverflowItems(world: World, pos: BlockPos) {
        val cap = currentCapacity()
        if (cap >= MAX_SLOTS) return
        for (i in cap until MAX_SLOTS) {
            val stack = getStack(i)
            if (!stack.isEmpty) {
                net.minecraft.util.ItemScatterer.spawn(
                    world,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    stack
                )
                setStack(i, ItemStack.EMPTY)
            }
        }
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 清理不再存在的红石接口状态
        redstonePortStates.keys.removeIf { portPos ->
            val blockAtPos = world.getBlockState(portPos).block
            blockAtPos !is ReactorRedstonePortBlock
        }

        val newCapacity = currentCapacity()
        sync.capacity1 = newCapacity

        // 更新热模式和流体同步数据
        sync.isThermalMode = if (isThermalMode()) 1 else 0
        sync.inputCoolantMb = dropletsToMb(inputTank.amount)
        sync.outputHotCoolantMb = dropletsToMb(outputTank.amount)

        dropOverflowItems(world, pos)

        if (isThermalMode()) processFluidSlots()

        if (tickOffset < 0) {
            tickOffset = world.random.nextBetween(0, 19)
            markDirty()
        }

        val shouldTick = (world.time + tickOffset) % 20L == 0L
        // 红石控制（参考 ElectricHeatGenerator）：有红石接口时以接口为准；无接口时检查反应堆自身
        val redstonePortsAllowRun = checkRedstonePortsAllowRun()
        val redstoneAllowsRun = if (redstonePortStates.isNotEmpty()) {
            redstonePortsAllowRun
        } else {
            RedstoneControlComponent.canRun(world, pos, this)
        }
        fuelActive = redstoneAllowsRun
        if (shouldTick) {
            dropAllUnfittingStuff(world, pos)
            cycleStartHeatSnapshot = sync.temperature
            cycleAddHeatTotal = 0
            cycleSetHeatDeltaTotal = 0
            cycleEmitHeatTotal = 0
            val tempBefore = sync.temperature
            val componentHeatBefore = calculateStoredComponentHeat()
            outputAccumulator = 0f
            emitHeatBuffer = 0
            totalHeatProduced = 0
            totalHeatDissipated = 0
            ventDissipatedHeat = 0  // 重置散失热量
            slotHeatInfo.clear()

            processChambers()

            // 应用最终热量到堆温（无论电/热模式）
            sync.temperature = (sync.temperature + emitHeatBuffer).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)

            var thermalEffectiveDissipatedHu = 0L

            // 热模式：将散失的热量转换为冷却液加热
            if (isThermalMode()) {
                val HU_PER_BUCKET = 20_000L
                val COOLANT_PER_BUCKET = FluidConstants.BUCKET

                // 热模式下可用于冷却液转换的散热，不能超过本周期实际产热
                val actualProducedHeat = totalHeatProduced.toLong().coerceAtLeast(0L)
                val rawVentDissipatedHeat = ventDissipatedHeat.toLong().coerceAtLeast(0L)
                val dissipatedHeat = minOf(actualProducedHeat, rawVentDissipatedHeat)
                val availableCoolant = inputTank.amount
                val outputSpace = outputTank.capacity - outputTank.amount
                val availableCoolantMb = dropletsToMb(availableCoolant)
                val outputSpaceMb = dropletsToMb(outputSpace)
                var heatActuallyConverted = 0L
                // 每秒日志一次，避免刷屏
                if (world != null && (world!!.time % 20L == 0L)) {
                    LOG.info(
                        "[冷却液转换] producedHeat=$actualProducedHeat " +
                            "ventDissipatedHeat=$rawVentDissipatedHeat " +
                            "effectiveDissipatedHeat=$dissipatedHeat " +
                            "availableCoolant=${availableCoolantMb}mB " +
                            "outputSpace=${outputSpaceMb}mB"
                    )
                }

                if (dissipatedHeat <= 0 || availableCoolant <= 0) {
                    // 跳过时仅在有意向转换时记录
                    if (rawVentDissipatedHeat > 0 && availableCoolant <= 0) {
                        LOG.info("[冷却液转换] 跳过: 无冷却液 availableCoolant=0mB")
                    } else if (rawVentDissipatedHeat <= 0 && availableCoolant > 0) {
                        LOG.info("[冷却液转换] 跳过: 无散热量 ventDissipatedHeat=0 (需散热片)")
                    } else if (actualProducedHeat <= 0 && rawVentDissipatedHeat > 0 && availableCoolant > 0) {
                        LOG.info("[冷却液转换] 跳过: 本周期无实际产热，散热片散热不计入转换")
                    }
                } else {
                    // 计算可转换的热量
                    val maxHeatToProcess = (availableCoolant * HU_PER_BUCKET) / COOLANT_PER_BUCKET
                    val heatToConvert = minOf(dissipatedHeat, maxHeatToProcess)

                    if (heatToConvert > 0) {
                        val coolantNeeded = ((heatToConvert * COOLANT_PER_BUCKET) / HU_PER_BUCKET) * 20L
                        val actualCoolantUsed = minOf(coolantNeeded, availableCoolant, outputSpace)
                        val potentialHeatConverted = (actualCoolantUsed * HU_PER_BUCKET) / COOLANT_PER_BUCKET
                        val outputAccepts = outputTank.variant.isBlank || outputTank.variant.fluid == ModFluids.HOT_COOLANT_STILL

                        if (actualCoolantUsed > 0 && outputAccepts) {
                            heatActuallyConverted = potentialHeatConverted
                            val extracted = actualCoolantUsed
                            inputTank.amount = (inputTank.amount - extracted).coerceAtLeast(0L)
                            if (inputTank.amount == 0L) {
                                inputTank.variant = FluidVariant.blank()
                            }

                            if (outputTank.variant.isBlank) {
                                outputTank.variant = FluidVariant.of(ModFluids.HOT_COOLANT_STILL)
                            }
                            outputTank.amount = (outputTank.amount + extracted).coerceAtMost(outputTank.capacity)

                            sync.inputCoolantMb = dropletsToMb(inputTank.amount)
                            sync.outputHotCoolantMb = dropletsToMb(outputTank.amount)
                            markDirty()

                            val limitReason = when {
                                extracted < coolantNeeded && extracted == outputSpace -> "输出空间不足"
                                extracted < coolantNeeded && extracted == availableCoolant -> "冷却液不足"
                                else -> "无"
                            }
                            LOG.info(
                                "[冷却液转换] 成功: 冷却液 ${dropletsToMb(extracted)}mB " +
                                    "-> 热冷却液 ${dropletsToMb(extracted)}mB " +
                                    "(理论需冷却液 ${dropletsToMb(coolantNeeded)}mB, " +
                                    "理论可转热量 ${heatToConvert}HU, " +
                                    "实际转热量 ${heatActuallyConverted}HU, " +
                                    "限制=$limitReason)"
                            )
                        } else if (!outputAccepts) {
                            LOG.warn("[冷却液转换] 跳过: 输出罐流体类型异常，无法写入热冷却液")
                        }
                    }
                }

                val unconvertedHeat = (dissipatedHeat - heatActuallyConverted).coerceAtLeast(0L)
                if (unconvertedHeat > 0L) {
                    val backfill = backfillUnconvertedHeatToSelfVents(unconvertedHeat)
                    if (backfill.remainingHeat > 0L) {
                        // 热模式下未转换、且未能回灌到散热片的热量不能凭空消失，回加到堆温保持守恒。
                        sync.temperature = (
                            sync.temperature + backfill.remainingHeat.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        ).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
                    }
                    LOG.info(
                        "[冷却液转换] 未转换热量回灌散热片: unconverted=${unconvertedHeat}HU " +
                            "vents=${backfill.targetCount} " +
                            "applied=${backfill.appliedHeat}HU " +
                            "remaining=${backfill.remainingHeat}HU"
                    )
                }
                thermalEffectiveDissipatedHu = heatActuallyConverted
            } else {
                // 电模式：正常的能量输出
                val euTotal = (outputAccumulator * NuclearReactorSync.EU_PER_OUTPUT).toLong()
                pendingEnergyOutput = euTotal.coerceIn(0L, NuclearReactorSync.ENERGY_CAPACITY)
            }

            sync.totalHeatProduced = totalHeatProduced
            sync.totalHeatDissipated = totalHeatDissipated
            sync.actualHeatDissipated = if (isThermalMode()) {
                thermalEffectiveDissipatedHu.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            } else {
                totalHeatDissipated
            }
            sync.thermalHeatOutput = if (isThermalMode()) {
                (thermalEffectiveDissipatedHu * 2L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            } else {
                0
            }

            val tempAfter = sync.temperature
            val componentHeatAfter = calculateStoredComponentHeat()
            val reactorDelta = tempAfter - tempBefore
            val componentDelta = componentHeatAfter - componentHeatBefore
            if (isThermalMode()) {
                LOG.info(
                    "[热量诊断] pos=$pos changedSinceLastCycle=$inventoryChangedSinceLastCycle temp:$tempBefore->$tempAfter(Δ$reactorDelta) " +
                        "componentHeat:$componentHeatBefore->$componentHeatAfter(Δ$componentDelta) " +
                        "produced=$totalHeatProduced dissipatedCapacity=$totalHeatDissipated " +
                        "actualDissipated=${sync.actualHeatDissipated} " +
                        "thermalOutput=${sync.thermalHeatOutput} emitBuffer=$emitHeatBuffer " +
                        "cycleAddHeat=$cycleAddHeatTotal cycleSetHeatDelta=$cycleSetHeatDeltaTotal cycleEmitHeat=$cycleEmitHeatTotal"
                )
            }
            inventoryChangedSinceLastCycle = false

            val displaySlotHeatInfo = slotHeatInfo.mapValues { (_, info) ->
                SlotHeatEnergyInfo(info.heatProduced, info.heatDissipated, info.energyOutput * 5)
            }
            val packet = ReactorHeatInfoPacket(pos, displaySlotHeatInfo)
            for (player in world.players) {
                NetworkManager.sendToClient(player as net.minecraft.server.network.ServerPlayerEntity, packet)
            }

            if (calculateHeatEffects(world, pos)) return
        }

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        if (sync.temperature >= NuclearReactorSync.HEAT_EXPLODE_THRESHOLD) {
            if (tryExplode()) return
        }

        if (shouldTick) {
            applyHeatEffects(world, pos)
        }

        if (pendingEnergyOutput > 0) {
            val euToAdd = pendingEnergyOutput / 20
            if (euToAdd > 0) {
                sync.generateEnergy(euToAdd)
                pendingEnergyOutput -= euToAdd
            }
            if ((world.time + tickOffset) % 20L == 19L) {
                if (pendingEnergyOutput > 0) {
                    sync.generateEnergy(pendingEnergyOutput)
                    pendingEnergyOutput = 0
                }
            }
            markDirty()
        }

        val hasFuel = (0 until newCapacity).any { !getStack(it).isEmpty }
        val active = hasFuel && redstoneAllowsRun
        if (state.get(NuclearReactorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(NuclearReactorBlock.ACTIVE, active))
        }

        sync.syncCurrentTickFlow()
    }

    private fun dropAllUnfittingStuff(world: World, pos: BlockPos) {
        val cap = currentCapacity()
        for (i in 0 until cap) {
            val stack = getStack(i)
            if (!stack.isEmpty && (stack.item !is IBaseReactorComponent || (stack.item is IBaseReactorComponent && !(stack.item as IBaseReactorComponent).canBePlacedIn(
                    stack,
                    this
                )))
            ) {
                setStack(i, ItemStack.EMPTY)
                net.minecraft.util.ItemScatterer.spawn(
                    world,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    stack
                )
            }
        }
        for (i in cap until MAX_SLOTS) {
            val stack = getStack(i)
            if (!stack.isEmpty) {
                setStack(i, ItemStack.EMPTY)
                net.minecraft.util.ItemScatterer.spawn(
                    world,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    stack
                )
            }
        }
    }

    private fun processChambers() {
        val cols = getReactorCols()
        val thermalMode = sync.isThermalMode == 1
        for (pass in 0..1) {
            val beforeTemp = sync.temperature
            val beforeComponentHeat = if (thermalMode) calculateStoredComponentHeat() else 0L
            val beforeEmit = emitHeatBuffer

            fun processAt(x: Int, y: Int) {
                val stack = getItemAt(x, y) ?: return
                val comp = stack.item as? IReactorComponent ?: return
                debugPass = pass
                debugHeatRun = (pass == 1)
                debugSlotX = x
                debugSlotY = y
                debugComponentId = Registries.ITEM.getId(stack.item)
                comp.processChamber(stack, this, x, y, pass == 1)
            }

            if (pass == 1) {
                // heatRun 阶段分两步：先处理非 ReactorVent，再处理 ReactorVent，
                // 避免“先抽旧堆温、后产热”造成的顺序锁温。
                for (y in 0 until 9) {
                    for (x in 0 until cols) {
                        val stack = getItemAt(x, y) ?: continue
                        if (stack.item !is IReactorComponent) continue
                        if (stack.item is ReactorHeatVentBase) continue
                        processAt(x, y)
                    }
                }
                for (y in 0 until 9) {
                    for (x in 0 until cols) {
                        val stack = getItemAt(x, y) ?: continue
                        if (stack.item !is ReactorHeatVentBase) continue
                        processAt(x, y)
                    }
                }
            } else {
                for (y in 0 until 9) {
                    for (x in 0 until cols) {
                        processAt(x, y)
                    }
                }
            }

            debugPass = -1
            debugHeatRun = false
            debugSlotX = -1
            debugSlotY = -1
            debugComponentId = null
            if (thermalMode) {
                val afterTemp = sync.temperature
                val afterComponentHeat = calculateStoredComponentHeat()
                val afterEmit = emitHeatBuffer
                LOG.info(
                    "[反应堆Pass] pos={} pass={} heatRun={} temp:{}->{}(Δ{}) componentHeat:{}->{}(Δ{}) emit:{}->{}(Δ{})",
                    pos,
                    pass,
                    pass == 1,
                    beforeTemp,
                    afterTemp,
                    (afterTemp - beforeTemp),
                    beforeComponentHeat,
                    afterComponentHeat,
                    (afterComponentHeat - beforeComponentHeat),
                    beforeEmit,
                    afterEmit,
                    (afterEmit - beforeEmit)
                )
            }
        }
    }

    private fun calculateHeatEffects(world: World, pos: BlockPos): Boolean {
        if (sync.temperature < NuclearReactorSync.HEAT_FIRE_THRESHOLD) return false
        val power = sync.temperature.toFloat() / NuclearReactorSync.HEAT_CAPACITY
        if (power >= 1f) {
            return tryExplode()
        }
        return false
    }

    private fun tryExplode(): Boolean {
        if (!Ic2Config.current.nuclear.enableReactorExplosion) {
            meltdownWithoutExplosion()
            return true
        }
        explode()
        return true
    }

    override fun explode() {
        if (!Ic2Config.current.nuclear.enableReactorExplosion) {
            meltdownWithoutExplosion()
            return
        }

        var boomPower = 10f
        var boomMod = 1f
        val cols = getReactorCols()

        for (y in 0 until 9) {
            for (x in 0 until cols) {
                val stack = getItemAt(x, y) ?: continue
                if (stack.item is IReactorComponent) {
                    val inf = (stack.item as IReactorComponent).influenceExplosion(stack, this)
                    if (inf > 0f && inf < 1f) boomMod *= inf
                    else if (inf >= 1f) boomPower += inf
                }
                setItemAt(x, y, null)
            }
        }
        boomPower *= boomMod

        val w = world ?: return
        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            if (w.getBlockState(neighborPos).block is ReactorChamberBlock) {
                w.breakBlock(neighborPos, false)
            }
        }
        w.breakBlock(pos, false)
        val cx = pos.x + 0.5
        val cy = pos.y + 0.5
        val cz = pos.z + 0.5
        w.createExplosion(null, cx, cy, cz, boomPower.coerceAtMost(20f), true, World.ExplosionSourceType.BLOCK)
    }

    private fun meltdownWithoutExplosion() {
        val w = world ?: return

        for (slot in 0 until INVENTORY_SIZE) {
            if (!getStack(slot).isEmpty) {
                setStack(slot, ItemStack.EMPTY)
            }
        }

        inputTank.variant = FluidVariant.blank()
        inputTank.amount = 0L
        outputTank.variant = FluidVariant.blank()
        outputTank.amount = 0L
        pendingEnergyOutput = 0L
        emitHeatBuffer = 0

        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            if (w.getBlockState(neighborPos).block is ReactorChamberBlock) {
                w.breakBlock(neighborPos, false)
            }
        }
        w.breakBlock(pos, false)
    }

    private fun applyHeatEffects(world: World, pos: BlockPos) {
        val heat = sync.temperature
        val rng = world.random

        if (heat > NuclearReactorSync.HEAT_FIRE_THRESHOLD) {
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (rng.nextFloat() > 0.02f) continue
                val p = pos.add(dx, dy, dz)
                if (!world.isInBuildLimit(p)) continue
                if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, p, ownerUuid)) continue
                if (AbstractFireBlock.canPlaceAt(world, p, Direction.UP)) {
                    world.setBlockState(p, Blocks.FIRE.defaultState)
                }
            }
        }

        if (heat > NuclearReactorSync.HEAT_EVAPORATE_THRESHOLD) {
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (rng.nextFloat() > 0.02f) continue
                val p = pos.add(dx, dy, dz)
                if (!world.isInBuildLimit(p)) continue
                if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, p, ownerUuid)) continue
                if (world.getBlockState(p).isOf(Blocks.WATER)) {
                    world.setBlockState(p, Blocks.AIR.defaultState)
                }
            }
        }

        if (heat > NuclearReactorSync.HEAT_DAMAGE_THRESHOLD) {
            val box = Box(pos).expand(3.5)
            val entities = world.getEntitiesByClass(LivingEntity::class.java, box) { true }
            for (entity in entities) {
                if (rng.nextFloat() > 0.05f) continue
                if (hasFullHazmat(entity)) continue
                val serverWorld = world as? net.minecraft.server.world.ServerWorld ?: continue
                val damageSource = createNuclearHeatDamageSource(serverWorld)
                entity.damage(damageSource, 2f)
            }
        }

        if (heat > NuclearReactorSync.HEAT_LAVA_THRESHOLD) {
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (rng.nextFloat() > 0.01f) continue
                val p = pos.add(dx, dy, dz)
                if (!world.isInBuildLimit(p)) continue
                if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, p, ownerUuid)) continue
                val state = world.getBlockState(p)
                val block = state.block
                if (block === Blocks.BEDROCK || block is NuclearReactorBlock || block is ReactorChamberBlock || block is MachineBlock || block is BaseCableBlock) continue
                if (state.isSolidBlock(world, p) && state.getHardness(world, p) >= 0f) {
                    world.setBlockState(p, Blocks.LAVA.defaultState)
                }
            }
        }
    }

    private fun hasFullHazmat(entity: LivingEntity): Boolean {
        val helmet = entity.getEquippedStack(EquipmentSlot.HEAD)
        val chest = entity.getEquippedStack(EquipmentSlot.CHEST)
        val legs = entity.getEquippedStack(EquipmentSlot.LEGS)
        fun isHazmat(stack: ItemStack): Boolean {
            if (stack.isEmpty || stack.item !is ArmorItem) return false
            val id = Registries.ITEM.getId(stack.item)
            return "hazmat" in id.path
        }
        return isHazmat(helmet) && isHazmat(chest) && isHazmat(legs)
    }

    private fun createNuclearHeatDamageSource(world: net.minecraft.server.world.ServerWorld): net.minecraft.entity.damage.DamageSource {
        val registry = world.registryManager.get(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE)
        val key = net.minecraft.registry.RegistryKey.of(
            net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,
            Identifier.of(Ic2_120.MOD_ID, "nuclear_heat")
        )
        val entry = registry.getEntry(key).orElse(null)
            ?: registry.getEntry(
                net.minecraft.registry.RegistryKey.of(
                    net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,
                    Identifier.of("minecraft", "lava")
                )
            ).orElseThrow()
        return net.minecraft.entity.damage.DamageSource(entry)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("ic2_120/NuclearReactor")

        const val MAX_SLOTS = 81

        // 流体交互槽索引（热模式使用）
        const val SLOT_COOLANT_INPUT = 81      // 冷却液输入（放入满容器）
        const val SLOT_COOLANT_OUTPUT = 82     // 冷却液输出（返回空容器）
        const val SLOT_HOT_COOLANT_INPUT = 83  // 热冷却液输入（放入空容器）
        const val SLOT_HOT_COOLANT_OUTPUT = 84 // 热冷却液输出（返回满容器）

        const val INVENTORY_SIZE = 85  // 81 反应堆槽 + 4 流体槽

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = NuclearReactorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity(
                { be, side -> be.getFluidStorageForSide(side) },
                type
            )
            fluidLookupRegistered = true
        }
    }
}
