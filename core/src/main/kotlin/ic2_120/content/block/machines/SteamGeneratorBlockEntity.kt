package ic2_120.content.block.machines

import ic2_120.content.block.SteamGeneratorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.heat.IHeatConsumer
import ic2_120.content.screen.SteamGeneratorScreenHandler
import ic2_120.content.sync.SteamGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 蒸汽发生器（锅炉）：消耗热量 + 水 → 产出蒸汽/过热蒸汽。
 *
 * 参考 ic2_origin TileEntitySteamGenerator 的数值：
 * - 最大热输入 1200 HU/t
 * - 蒸汽膨胀 100x（1 mB 水 → 100 mB 蒸汽）
 * - 系统温度 >= 374°C 产出过热蒸汽
 * - 最大温度 500°C（超过爆炸）
 * - 水垢上限 100,000 mB（仅普通水累积）
 *
 * 适配 ic2-fabric 热能系统：由 HeatGeneratorBlockEntityBase 推送热量，六面均可输入。
 * 蒸汽自动向相邻流体管道/容器输出。
 *
 * 流体交互（遵循 SolarDistiller 模式）:
 * - @RegisterFluidStorage 注册 Fabric Transfer API 存储
 * - IFluidPipeUpgradeSupport 提供管道网络集成
 * - fluidPipeReceiverEnabled / fluidPipeProviderEnabled 默认 true，无需升级即可用水管输入/输出
 */
@ModBlockEntity(block = SteamGeneratorBlock::class)
class SteamGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IHeatConsumer, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = SteamGeneratorBlock.ACTIVE
    override val tier: Int = 2
    override fun getInventory(): net.minecraft.inventory.Inventory? = null

    // IFluidPipeUpgradeSupport — 默认开启推拉，无需流体升级
    override var fluidPipeProviderEnabled: Boolean = true
    override var fluidPipeReceiverEnabled: Boolean = true
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 1
    override var fluidPipePullingCount: Int = 1

    companion object {
        private const val NBT_WATER_TANK = "WaterTank"
        private const val NBT_STEAM_TANK = "SteamTank"
        private const val NBT_SYSTEM_HEAT_MILLI = "SystemHeatMilli"
        private const val NBT_CALCIFICATION = "Calcification"
        private const val NBT_INPUT_MB = "InputMB"
        private const val NBT_PRESSURE = "Pressure"

        /** 每 HU 升温度数 (milli-°C/HU)，对齐 ic2_origin heatPerHu = 5.0E-4 °C/HU → 0.5 ≈ 1 */
        private const val HEAT_PER_HU_MILLI = 1L
        /** 水罐容量 (mB) */
        private const val WATER_TANK_CAPACITY = FluidConstants.BUCKET * 10  // 10,000 mB
        /** 蒸汽罐容量 (mB) */
        private const val STEAM_TANK_CAPACITY = FluidConstants.BUCKET * 100  // 100,000 mB

        @Volatile
        private var fluidLookupRegistered = false

        /** 注册 Fabric Transfer API 流体存储 (SolarDistiller 模式) */
        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val beType = SteamGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, beType)
            fluidLookupRegistered = true
        }


        fun mbToDroplets(mb: Long): Long = mb * FluidConstants.BUCKET / 1000
    }

    private val syncedData = SyncedData(this)
    val sync = SteamGeneratorSync(syncedData)

    /** 系统温度 (milli-°C)，初始为生物群系温度（默认 20°C） */
    private var systemHeatMilli: Long = 20_000L

    /** 蒸汽产出目标流体（null = 无产出, false = 普通蒸汽, true = 过热蒸汽） */
    private var producingSuperheated: Boolean = false

    /** 水垢累积 (累计处理水量 mB) */
    private var calcification: Long = 0L

    /** 热量缓冲区：跨 tick 累积，不随 tick 重置丢失 */
    private var heatBuffer: Long = 0L

    /** 上次消耗的水是否为蒸馏水（用于蒸汽积压时回收水） */
    private var consumedWaterIsDistilled: Boolean = false

    // ==== 流体槽 ====

    /** 水输入槽 — 接受水和蒸馏水，不可外部提取 */
    private val waterTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = WATER_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean =
            ModFluids.isFluid(variant.fluid) && variant.fluid == net.minecraft.fluid.Fluids.WATER

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        /** 是否为蒸馏水 */
        fun isDistilled(): Boolean = variant.fluid == ModFluids.DISTILLED_WATER_STILL

        /** 消耗指定 mB 水量，返回是否成功 */
        fun consumeMb(mb: Long): Boolean {
            val droplets = mbToDroplets(mb)
            if (droplets <= 0L || variant.isBlank) return false
            val actual = minOf(droplets, amount)
            if (actual <= 0L) return false
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return true
        }

        /** NBT 恢复 */
        fun readFromNbt(nbt: NbtCompound) {
            variant = FluidVariant.fromNbt(nbt.getCompound("variant"))
            amount = nbt.getLong("amount")
        }

        /** NBT 保存 */
        fun writeToNbt(): NbtCompound = NbtCompound().also {
            it.put("variant", variant.toNbt())
            it.putLong("amount", amount)
        }
    }

    /** 蒸汽输出槽 — 蒸汽只能由机器内部产出，外部可提取 */
    private val steamTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = STEAM_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean = false  // 只能由机器产出

        override fun canExtract(variant: FluidVariant): Boolean = ModFluids.isSteam(variant.fluid)

        override fun onFinalCommit() {
            sync.steamAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        /** 机器内部产出蒸汽 */
        fun produceSteam(droplets: Long): Long {
            if (droplets <= 0L) return 0L
            val space = STEAM_TANK_CAPACITY - amount
            val actual = minOf(droplets, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) {
                variant = if (producingSuperheated)
                    FluidVariant.of(ModFluids.SUPERHEATED_STEAM_STILL)
                else
                    FluidVariant.of(ModFluids.STEAM_STILL)
            }
            sync.steamAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        /** NBT 恢复 */
        fun readFromNbt(nbt: NbtCompound) {
            variant = FluidVariant.fromNbt(nbt.getCompound("variant"))
            amount = nbt.getLong("amount")
        }

        /** NBT 保存 */
        fun writeToNbt(): NbtCompound = NbtCompound().also {
            it.put("variant", variant.toNbt())
            it.putLong("amount", amount)
        }
    }

    /** 对外流体 I/O 接口 — 统一处理管道/容器的流体访问 (SolarDistiller 模式) */
    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != Fluids.WATER && resource.fluid != ModFluids.DISTILLED_WATER_STILL) return 0L
            return waterTank.insert(resource, maxAmount, transaction)
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.STEAM_STILL && resource.fluid != ModFluids.SUPERHEATED_STEAM_STILL) return 0L
            return steamTank.extract(resource, maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!waterTank.variant.isBlank && waterTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = waterTank.variant
                    override fun getAmount(): Long = waterTank.amount
                    override fun getCapacity(): Long = WATER_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!steamTank.variant.isBlank && steamTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = steamTank.variant
                    override fun getAmount(): Long = steamTank.amount
                    override fun getCapacity(): Long = STEAM_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        steamTank.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    /** 流体存储方向访问 (SolarDistiller 模式) — 所有面均可访问，不限制正面 */
    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? = ioStorage

    constructor(pos: BlockPos, state: BlockState) : this(
        SteamGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    // ==== IHeatConsumer — 六面均可输入热量 ====

    override fun getHeatTransferFace(): Direction {
        return cachedState.getOrEmpty(net.minecraft.state.property.Properties.HORIZONTAL_FACING)
            .orElse(Direction.NORTH)
    }

    override fun receiveHeat(hu: Long, fromSide: Direction): Long {
        if (hu <= 0L) return 0L
        if (calcification >= SteamGeneratorSync.MAX_CALCIFICATION) return 0L
        return receiveHeatInternal(hu)
    }

    private fun receiveHeatInternal(hu: Long): Long {
        heatBuffer = (heatBuffer + hu).coerceAtMost(SteamGeneratorSync.MAX_HEAT_INPUT)
        return hu
    }

    // ==== ScreenHandler ====

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.steam_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SteamGeneratorScreenHandler(
            syncId,
            playerInventory,
            net.minecraft.inventory.SimpleInventory(0),
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    // ==== NBT ====

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        syncedData.readNbt(nbt)
        systemHeatMilli = nbt.getLong(NBT_SYSTEM_HEAT_MILLI)
        calcification = nbt.getLong(NBT_CALCIFICATION)
        sync.inputMB = nbt.getInt(NBT_INPUT_MB).coerceIn(0, 1000)
        sync.pressure = nbt.getInt(NBT_PRESSURE).coerceIn(0, SteamGeneratorSync.MAX_PRESSURE)

        if (nbt.contains(NBT_WATER_TANK)) waterTank.readFromNbt(nbt.getCompound(NBT_WATER_TANK))
        if (nbt.contains(NBT_STEAM_TANK)) steamTank.readFromNbt(nbt.getCompound(NBT_STEAM_TANK))
        sync.waterAmount = waterTank.amount.toInt().coerceAtLeast(0)
        sync.steamAmount = steamTank.amount.toInt().coerceAtLeast(0)
        sync.calcification = calcification.coerceIn(0L, SteamGeneratorSync.MAX_CALCIFICATION).toInt()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_SYSTEM_HEAT_MILLI, systemHeatMilli)
        nbt.putLong(NBT_CALCIFICATION, calcification)
        nbt.putInt(NBT_INPUT_MB, sync.inputMB)
        nbt.putInt(NBT_PRESSURE, sync.pressure)

        nbt.put(NBT_WATER_TANK, waterTank.writeToNbt())
        nbt.put(NBT_STEAM_TANK, steamTank.writeToNbt())
    }

    // ==== Tick ====

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 取出跨 tick 累积的热量用于本 tick 处理
        val heatAvailableThisTick = heatBuffer.coerceAtMost(SteamGeneratorSync.MAX_HEAT_INPUT)
        heatBuffer = 0L

        // 流体管道输出蒸汽 (默认 fluidPipeProviderEnabled=true, 无需升级)
        FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, steamTank,
            fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)

        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, waterTank, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }

        // 0. 群系温度（对齐 ic2_origin BiomeUtil.getBiomeTemperature）
        val biomeTemp = getBiomeTemperatureMilli(world, pos)
        // 每 tick 温度下限
        systemHeatMilli = systemHeatMilli.coerceAtLeast(biomeTemp)

        // 1. 计算当前温度效果
        val pressure = sync.pressure.coerceIn(0, SteamGeneratorSync.MAX_PRESSURE)

        // 2. 进水速率
        val inputMB = sync.inputMB.coerceIn(0, 1000)

        // 3. 加热/冷却逻辑
        if (waterTank.amount <= 0L || inputMB <= 0) {
            // 没水：所有热量用于升温（无上限，对齐 ic2_origin）
            if (heatAvailableThisTick > 0L) {
                systemHeatMilli += heatAvailableThisTick * HEAT_PER_HU_MILLI
            }
            // 自然冷却 0.01°C/t（对齐 ic2_origin cooldown(0.01F)）
            systemHeatMilli = (systemHeatMilli - 10L).coerceAtLeast(biomeTemp)
        } else {
            // 3a. 低压出水模式 — 对齐 ic2_origin work() 低压分支
            if (pressure == 0 && systemHeatMilli < 100_000L) {
                val inputDroplets = mbToDroplets(inputMB.toLong())
                val waterToOutput = minOf(inputDroplets, waterTank.amount)
                if (waterToOutput > 0L) {
                    val variant = waterTank.variant
                    if (!variant.isBlank) {
                        val droplets = waterToOutput
                        var ejected = 0L
                        for (side in Direction.entries) {
                            if (ejected >= droplets) break
                            val neighborStorage = FluidStorage.SIDED.find(world, pos.offset(side), side.opposite)
                            if (neighborStorage != null && neighborStorage.supportsInsertion()) {
                                val remaining = droplets - ejected
                                Transaction.openOuter().use { tx ->
                                    val accepted = neighborStorage.insert(variant, remaining, tx)
                                    if (accepted > 0) {
                                        tx.commit()
                                        ejected += accepted
                                    }
                                }
                            }
                        }
                        if (ejected > 0) {
                            waterTank.amount -= ejected
                            sync.waterAmount = waterTank.amount.toInt().coerceAtLeast(0)
                            consumedWaterIsDistilled = waterTank.isDistilled()
                            if (waterTank.amount <= 0L) waterTank.variant = FluidVariant.blank()
                            sync.outputMB = ejected.toInt().coerceAtLeast(0) * SteamGeneratorSync.STEAM_EXPANSION
                            markDirty()
                        }
                    }
                }
            }
            // 有水 + 低压出水未处理完毕：计算蒸汽产出
            if (waterTank.amount > 0L && (pressure > 0 || systemHeatMilli >= 100_000L)) {
                // 有效温度（受压力影响）：effectiveTemp = 100 + (pressure/220) * 100 * 2.74
                val pressureFactor = pressure.toDouble() / 220.0
                val effectiveTempMilli = (100_000L + (pressureFactor * 100 * 2.74 * 1000).toLong())

                // 需要加热水到有效温度
                val heatNeeded = (effectiveTempMilli - systemHeatMilli).coerceAtLeast(0L)
                val heatAvailable = heatAvailableThisTick * HEAT_PER_HU_MILLI

                if (heatAvailable >= heatNeeded && waterTank.amount > 0L) {
                    // 有足够热量产蒸汽
                    producingSuperheated = systemHeatMilli >= SteamGeneratorSync.SUPERHEATED_THRESHOLD_MILLI

                    // 实际消耗水量 = min(进水速率, 可用水量)
                    val waterToConsume = minOf(inputMB.toLong(), waterTank.amount.toInt().coerceAtLeast(0).toLong())
                    if (waterToConsume > 0L) {
                        consumedWaterIsDistilled = waterTank.isDistilled()
                        if (waterTank.consumeMb(waterToConsume)) {
                            // 产出蒸汽 = 水量 * 膨胀倍率
                            val steamProduced = waterToConsume * SteamGeneratorSync.STEAM_EXPANSION
                            val steamDroplets = mbToDroplets(steamProduced)
                            steamTank.produceSteam(steamDroplets)
                            sync.outputMB = steamProduced.toInt()

                            // 冷却 = water × (100 + pressure/220×100) × 0.5, 上限 2000 milli-°C(2°C)
                            // 对齐 ic2_origin var4 + maxCooling
                            val huPerMb = 100L + pressure * 100L / 220L
                            val coolingMilli = (waterToConsume * huPerMb * HEAT_PER_HU_MILLI).coerceAtMost(2000L)
                            systemHeatMilli = (systemHeatMilli - coolingMilli).coerceAtLeast(biomeTemp)

                            // 水垢累积（仅普通水）
                            if (!waterTank.isDistilled()) {
                                calcification = (calcification + waterToConsume).coerceAtMost(SteamGeneratorSync.MAX_CALCIFICATION)
                            }
                        }
                    }
                } else if (heatAvailable > 0L && waterTank.amount > 0L) {
                    // 热量不足产蒸汽，全部用于升温
                    systemHeatMilli += heatAvailable
                } else if (heatAvailableThisTick == 0L) {
                    // 无热源：缓慢冷却 0.01°C/t
                    systemHeatMilli = (systemHeatMilli - 10L).coerceAtLeast(biomeTemp)
                }
            }
        }

        // 4. 超温爆炸 — 对齐 ic2_origin heatup(): >500°C 必爆, 半径 10, 不毁地形
        if (systemHeatMilli > SteamGeneratorSync.MAX_SYSTEM_HEAT_MILLI) {
            world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                10f, World.ExplosionSourceType.NONE)
        }

        // 5. 蒸汽未及时排出 → 对齐 ic2_origin TileEntitySteamGenerator.work()
        if (sync.outputMB > 0) {
            // 先尝试排出刚产出的蒸汽
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, steamTank,
                fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
            val remainingMb = steamTank.amount.toInt().coerceAtLeast(0)
            if (remainingMb > 0) {
                if (world.random.nextInt(10) == 0) {
                    world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                        1f, World.ExplosionSourceType.NONE)
                } else {
                    val waterRecover = remainingMb / SteamGeneratorSync.STEAM_EXPANSION
                    if (waterRecover > 0) {
                        val recoveredDroplets = mbToDroplets(waterRecover.toLong())
                        val fillFluid = if (consumedWaterIsDistilled) ModFluids.DISTILLED_WATER_STILL
                            else net.minecraft.fluid.Fluids.WATER
                        val fillVariant = FluidVariant.of(fillFluid)
                        val space = WATER_TANK_CAPACITY - waterTank.amount
                        val actual = minOf(recoveredDroplets, space)
                        if (actual > 0L) {
                            if (waterTank.variant.isBlank) waterTank.variant = fillVariant
                            waterTank.amount += actual
                            sync.waterAmount = waterTank.amount.toInt().coerceAtLeast(0)
                            markDirty()
                        }
                    }
                }
            }
        }

        // 7. 更新同步数据
        sync.systemHeatMilli = systemHeatMilli.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        sync.heatInput = heatAvailableThisTick.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        sync.waterAmount = waterTank.amount.toInt().coerceAtLeast(0)
        sync.steamAmount = steamTank.amount.toInt().coerceAtLeast(0)
        sync.calcification = calcification.coerceIn(0L, SteamGeneratorSync.MAX_CALCIFICATION).toInt()
        sync.calcified = if (calcification >= SteamGeneratorSync.MAX_CALCIFICATION) 1 else 0
        sync.isSuperheated = if (producingSuperheated) 1 else 0
        sync.isWaterDistilled = if (waterTank.isDistilled()) 1 else 0
        sync.pressure = pressure.coerceIn(0, SteamGeneratorSync.MAX_PRESSURE)

        // 8. 活跃状态
        val active = heatAvailableThisTick > 0L &&
            waterTank.amount > 0L &&
            calcification < SteamGeneratorSync.MAX_CALCIFICATION &&
            systemHeatMilli <= SteamGeneratorSync.MAX_SYSTEM_HEAT_MILLI
        setActiveState(world, pos, state, active)

        markDirty()
    }

    /** 获取群系温度（milli-°C），对齐 ic2_origin BiomeUtil.getBiomeTemperature */
    private fun getBiomeTemperatureMilli(world: World, pos: BlockPos): Long {
        val temp = world.getBiome(pos).value().temperature  // temp 是 base biome temperature
        return when {
            temp >= 0.8f -> 45_000L  // 炎热群系 (沙漠/热带)
            temp <= 0.3f -> 0L        // 寒冷群系 (雪原/冰原)
            else -> 25_000L           // 温带群系 (默认)
        }
    }

    /**
     * 处理客户端按钮事件 — 1:1 对齐 ic2_origin TileEntitySteamGenerator.onNetworkEvent。
     *
     * 事件值含义:
     *   [-2000, 2000] → 调整 inputMB (进水速率 0-1000)
     *   > 2000         → 调整 pressure (蒸汽阀 += (event - 2000), max 300)
     *   < -2000        → 调整 pressure (蒸汽阀 += (event + 2000), min 0)
     */
    fun onNetworkEvent(event: Int) {
        if (event in -2000..2000) {
            sync.inputMB = (sync.inputMB + event).coerceIn(0, 1000)
        } else if (event > 2000) {
            sync.pressure = (sync.pressure + (event - 2000)).coerceAtMost(SteamGeneratorSync.MAX_PRESSURE)
        } else if (event < -2000) {
            sync.pressure = (sync.pressure + event + 2000).coerceAtLeast(0)
        }
    }

    /** 获取当前温度显示 (°C) */
    fun getTemperatureCelsius(): Float = systemHeatMilli / 1000f

    /** 获取水垢百分比 */
    fun getCalcificationPercent(): Float =
        (calcification.toFloat() / SteamGeneratorSync.MAX_CALCIFICATION).coerceIn(0f, 1f)
}
