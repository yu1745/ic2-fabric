package ic2_120.content.block.machines

import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.block.transmission.BevelGearBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.block.transmission.IKineticRotorProvider
import ic2_120.content.block.transmission.TransmissionShaftBlock
import ic2_120.content.screen.WindKineticGeneratorScreenHandler
import ic2_120.content.sync.WindKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.content.network.NetworkManager
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
@ModBlockEntity(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IKineticMachinePort, IKineticRotorProvider,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = WindKineticGeneratorBlock.ACTIVE

    companion object {
        private val LOG = LoggerFactory.getLogger("ic2_120/WindKineticGenerator")
        const val ROTOR_SLOT = 0
        private val ALLOWED_ROTORS = setOf("wooden_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
        private const val PIXEL = 1.0f / 16.0f
        private const val BASE_SCAN_INTERVAL_TICKS = 20L
        private const val STUCK_SCAN_INTERVAL_TICKS = 10L
        private const val SAMPLE_COUNT = 5
        private const val ROTOR_RPM_DEGREES_PER_TICK = 1.8f
        private const val STUCK_DISPLAY_ANGLE = 45f
        private val BLADE_ANGLES = floatArrayOf(0f, 90f, 180f, 270f)
        private const val MAX_ROTOR_RADIUS = 5.0
        private const val ROTOR_PLANE_TOLERANCE = 0.001
        private const val ROTOR_OVERLAP_EPSILON = 1.0 / 64.0
        const val WIND_UPDATE_INTERVAL_TICKS = 200L
        const val WIND_GUST_MIN = 0.5
        const val WIND_GUST_MAX = 1.5
        const val WIND_MEAN_FLOOR = 0.03
        const val WIND_GAUSSIAN_PEAK_Y = 150.0
        const val WIND_GAUSSIAN_SIGMA = 35.0
        const val BASE_KU_AT_PEAK = 128.0
        private const val BASE_START_THRESHOLD = 0.10
        private const val STOP_THRESHOLD_FACTOR = 0.85
        private const val ROTOR_WEAR_REMAINDER_KEY = "RotorWearRemainder"
        private const val TICKS_PER_HOUR = 72_000.0

        private data class GustState(
            var lastUpdateBucket: Long = Long.MIN_VALUE,
            var factor: Double = 1.0
        )

        private val gustByWorldAndChunk: MutableMap<String, MutableMap<Long, GustState>> = HashMap()

        fun weatherMultiplier(world: World): Double =
            when {
                world.isThundering -> 1.5
                world.isRaining -> 1.2
                else -> 1.0
            }

        fun meanWindFromY(y: Int): Double {
            val dy = y - WIND_GAUSSIAN_PEAK_Y
            val gauss = exp(-(dy * dy) / (2.0 * WIND_GAUSSIAN_SIGMA * WIND_GAUSSIAN_SIGMA))
            return WIND_MEAN_FLOOR + (1.0 - WIND_MEAN_FLOOR) * gauss
        }

        fun worldGustFactor(world: World, pos: BlockPos): Double {
            val worldKey = world.registryKey.value.toString()
            val chunkPos = ChunkPos(pos)
            val chunkKey = chunkPos.toLong()
            val byChunk = gustByWorldAndChunk.getOrPut(worldKey) { HashMap() }
            val state = byChunk.getOrPut(chunkKey) { GustState() }
            val chunkOffset = chunkUpdateOffset(chunkPos)
            val effectiveTime = world.time - chunkOffset
            val tickBucket = if (effectiveTime >= 0L) effectiveTime / WIND_UPDATE_INTERVAL_TICKS else -1L
            if (state.lastUpdateBucket != tickBucket) {
                state.lastUpdateBucket = tickBucket
                state.factor = WIND_GUST_MIN + chunkRandom01(chunkPos, tickBucket) * (WIND_GUST_MAX - WIND_GUST_MIN)
            }
            return state.factor
        }

        private fun chunkUpdateOffset(chunkPos: ChunkPos): Long =
            (mixChunkSeed(chunkPos.toLong()) and Long.MAX_VALUE) % WIND_UPDATE_INTERVAL_TICKS

        private fun chunkRandom01(chunkPos: ChunkPos, tickBucket: Long): Double {
            val seed = mixChunkSeed(chunkPos.toLong() xor tickBucket)
            val bits = (seed ushr 11) and ((1L shl 53) - 1)
            return bits.toDouble() / (1L shl 53).toDouble()
        }

        private fun mixChunkSeed(input: Long): Long {
            var x = input
            x = (x xor (x ushr 30)) * -4658895280553007687L
            x = (x xor (x ushr 27)) * -7723592293110705685L
            return x xor (x ushr 31)
        }

        fun startThresholdForMultiplier(multiplier: Int): Double =
            BASE_START_THRESHOLD * multiplier.coerceAtLeast(1)
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)

    /** 同步数据（服务端） */
    private val syncedData = SyncedData(this)
    private val kineticSync = WindKineticGeneratorSync(syncedData)

    /** 转子是否被阻挡（true = 卡住停转） */
    var isStuck: Boolean
        get() = kineticSync.isStuck != 0
        private set(value) {
            kineticSync.isStuck = if (value) 1 else 0
        }

    /** 卡住时的转子角度（度，0-359） */
    var stuckAngle: Float
        get() = kineticSync.stuckAngle.toFloat()
        private set(value) {
            kineticSync.stuckAngle = value.toInt()
        }

    /** 卡住时的 world.time（用于解除后连续旋转，无瞬移） */
    private var stuckWorldTime: Long = 0L
    private val scanOffset: Int = (pos.asLong().toInt() and Int.MAX_VALUE) % BASE_SCAN_INTERVAL_TICKS.toInt()
    private var lastCollisionResult: Boolean = false
    private var lastCollisionCheckTick: Long = Long.MIN_VALUE
    private var pendingOutputKu: Int = 0
    /**
     * 客户端接收网络包更新转子状态
     */
    fun receiveRotorState(isStuck: Boolean, stuckAngle: Float) {
        kineticSync.isStuck = if (isStuck) 1 else 0
        kineticSync.stuckAngle = stuckAngle.toInt()
        markDirty()
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        WindKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    override fun canOutputKuTo(side: Direction): Boolean {
        val world = world ?: return false
        val state = world.getBlockState(pos)
        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)
        // 风轮在前，动能端口在背。
        return side == facing.opposite
    }

    override fun getStoredKu(side: Direction): Int =
        if (canOutputKuTo(side)) pendingOutputKu.coerceAtLeast(0) else 0

    override fun getKuCapacity(side: Direction): Int =
        if (canOutputKuTo(side)) Int.MAX_VALUE else 0

    override fun getMaxExtractableKu(side: Direction): Int =
        if (canOutputKuTo(side)) pendingOutputKu.coerceAtLeast(0) else 0

    override fun extractKu(side: Direction, amount: Int, simulate: Boolean): Int {
        if (!canOutputKuTo(side) || amount <= 0) return 0
        val extracted = minOf(amount, pendingOutputKu.coerceAtLeast(0))
        if (!simulate && extracted > 0) {
            pendingOutputKu = (pendingOutputKu - extracted).coerceAtLeast(0)
            kineticSync.outputKu = (kineticSync.outputKu + extracted).coerceAtLeast(0)
            markDirty()
        }
        return extracted
    }

    fun getGeneratedKu(): Int = kineticSync.generatedKu.coerceAtLeast(0)

    fun getOutputKu(): Int = kineticSync.outputKu.coerceAtLeast(0)

    override fun getRotorStack(): ItemStack = inventory[ROTOR_SLOT]

    fun getRotorRemainingClearHours(): Double {
        val rotor = inventory[ROTOR_SLOT]
        if (rotor.isEmpty || !rotor.isDamageable) return 0.0
        val remainder = rotor.nbt?.getDouble(ROTOR_WEAR_REMAINDER_KEY) ?: 0.0
        return ((rotor.maxDamage - rotor.damage).toDouble() - remainder).coerceAtLeast(0.0) / TICKS_PER_HOUR
    }

    fun onUse(player: PlayerEntity, hand: Hand): ActionResult {
        val held = player.getStackInHand(hand)
        val rotor = inventory[ROTOR_SLOT]

        // 空手右键取出转子
        if (held.isEmpty) {
            if (rotor.isEmpty) return ActionResult.PASS
            val extracted = rotor.copy()
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
            markDirtyAndSync()
            if (!player.giveItemStack(extracted)) {
                player.dropItem(extracted, false)
            }
            return ActionResult.SUCCESS
        }

        // 手持非转子物品：不处理
        if (!isAllowedRotor(held)) return ActionResult.PASS

        // 已有转子时不覆盖
        if (!rotor.isEmpty) return ActionResult.PASS

        val inserted = held.copy()
        inserted.count = 1
        inventory[ROTOR_SLOT] = inserted
        if (!player.abilities.creativeMode) {
            held.decrement(1)
        }
        markDirtyAndSync()
        return ActionResult.SUCCESS
    }

    private fun isAllowedRotor(stack: ItemStack): Boolean {
        val id = Registries.ITEM.getId(stack.item)
        return ALLOWED_ROTORS.contains(id.path)
    }

    override fun getRotorRadius(stack: ItemStack): Float {
        return when (Registries.ITEM.getId(stack.item).path) {
            "wooden_rotor" -> 2.0f
            "iron_rotor" -> 3.0f
            "steel_rotor" -> 4.0f
            "carbon_rotor" -> 5.0f
            else -> 0.0f
        }
    }

    private fun getRotorMultiplier(stack: ItemStack): Int {
        return when (Registries.ITEM.getId(stack.item).path) {
            "wooden_rotor" -> 1
            "iron_rotor" -> 2
            "steel_rotor" -> 3
            "carbon_rotor" -> 4
            else -> 0
        }
    }

    /**
     * 服务端 tick：检测转子是否被前方的方块阻挡。
     *
     * 检查逻辑：
     * - 以转子中心（在方块前方 0.5 格）为原点，在垂直于朝向的平面上，
     *   检查半径范围内的所有固体方块。
     * - 若某方块位于叶片旋转路径上，则认为叶片被卡住，
     *   计算并记录当前转子的实际旋转角度。
     * - 卡住后转子停止旋转，渲染器固定显示在 stuckAngle。
     */
    fun tick(world: World, pos: BlockPos, state: BlockState) {
        kineticSync.outputKu = 0
        val rotor = inventory[ROTOR_SLOT]
        if (rotor.isEmpty) {
            pendingOutputKu = 0
            kineticSync.generatedKu = 0
            kineticSync.rotorLifetimeTenthsHours = 0
            if (isStuck) {
                isStuck = false
                kineticSync.stuckAngle = 0
                markDirtyAndSync()
                NetworkManager.sendWindRotorStateToNearby(world, pos, false, 0f)
            }
            lastCollisionResult = false
            return
        }

        val rotorRadius = getRotorRadius(rotor)
        if (rotorRadius <= 0f) return
        kineticSync.rotorLifetimeTenthsHours = (getRotorRemainingClearHours() * 10.0).toInt().coerceAtLeast(0)

        if (world.isClient) return

        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        // 碰撞检测仍按连续旋转角度进行；显示角度在卡住后固定为常量，避免再求“精确卡住角度”。
        val currentAngle: Float = if (isStuck) {
            stuckAngle
        } else {
            ((stuckAngle + (world.time - stuckWorldTime) * ROTOR_RPM_DEGREES_PER_TICK) % 360.0f).let {
                if (it < 0) it + 360.0f else it
            }
        }

        val interval = if (isStuck) STUCK_SCAN_INTERVAL_TICKS else BASE_SCAN_INTERVAL_TICKS
        val shouldScan = (world.time + scanOffset) % interval == 0L
        val collision = if (shouldScan || lastCollisionCheckTick == Long.MIN_VALUE) {
            val scanned = hasRotorCollision(world, pos, facing, rotorRadius) ||
                findBladeCollisionWithBlocks(world, pos, facing, rotorRadius, currentAngle)
            lastCollisionResult = scanned
            lastCollisionCheckTick = world.time
            scanned
        } else {
            lastCollisionResult
        }

        if (collision) {
            if (!isStuck) {
                isStuck = true
                // 记录实际阻挡角度，而不是固定角度
                stuckAngle = currentAngle
                markDirtyAndSync()
                NetworkManager.sendWindRotorStateToNearby(world, pos, true, stuckAngle)
            }
        } else {
            if (isStuck) {
                isStuck = false
                stuckAngle = STUCK_DISPLAY_ANGLE
                stuckWorldTime = world.time
                markDirtyAndSync()
                NetworkManager.sendWindRotorStateToNearby(world, pos, false, 0f)
            }
        }

        val active = if (!isStuck) {
            val rotorMultiplier = getRotorMultiplier(rotor)
            val startThreshold = BASE_START_THRESHOLD * rotorMultiplier
            val stopThreshold = startThreshold * STOP_THRESHOLD_FACTOR
            val weather = weatherMultiplier(world)
            val effectiveWind = meanWindFromY(pos.y) * worldGustFactor(world, pos) * weather
            val wasActive = state.contains(WindKineticGeneratorBlock.ACTIVE) && state.get(WindKineticGeneratorBlock.ACTIVE)
            val minWind = if (wasActive) stopThreshold else startThreshold
            val kuOut = if (effectiveWind >= minWind) {
                floor(BASE_KU_AT_PEAK * rotorMultiplier * effectiveWind).toInt().coerceAtLeast(0)
            } else {
                0
            }
            kineticSync.generatedKu = kuOut
            pendingOutputKu = kuOut
            if (kuOut > 0 && applyRotorWear(rotor, weather)) {
                inventory[ROTOR_SLOT] = ItemStack.EMPTY
                kineticSync.generatedKu = 0
                kineticSync.outputKu = 0
                kineticSync.rotorLifetimeTenthsHours = 0
                pendingOutputKu = 0
                markDirtyAndSync()
                false
            } else {
                kineticSync.rotorLifetimeTenthsHours = (getRotorRemainingClearHours() * 10.0).toInt().coerceAtLeast(0)
                kuOut > 0
            }
        } else {
            kineticSync.generatedKu = 0
            pendingOutputKu = 0
            kineticSync.rotorLifetimeTenthsHours = (getRotorRemainingClearHours() * 10.0).toInt().coerceAtLeast(0)
            false
        }
        setActiveState(world, pos, state, active)
    }

    /**
     * 转子只有在真正转动发电时才损耗寿命。
     * 基准是晴天 1 tick = 1 点寿命；天气加成直接等比放大寿命损耗。
     * 小数部分写入转子 NBT，避免 1.2x / 1.5x 长时间累计失真。
     *
     * @return true 表示本 tick 后转子损坏
     */
    private fun applyRotorWear(rotor: ItemStack, wearMultiplier: Double): Boolean {
        if (!rotor.isDamageable || wearMultiplier <= 0.0) return false
        val nbt = rotor.orCreateNbt
        val accumulated = (nbt.getDouble(ROTOR_WEAR_REMAINDER_KEY) + wearMultiplier).coerceAtLeast(0.0)
        val wear = floor(accumulated).toInt()
        val remainder = accumulated - wear

        if (wear <= 0) {
            nbt.putDouble(ROTOR_WEAR_REMAINDER_KEY, remainder)
            return false
        }

        if (rotor.damage + wear >= rotor.maxDamage) {
            nbt.remove(ROTOR_WEAR_REMAINDER_KEY)
            return true
        }

        rotor.damage += wear
        if (remainder > 0.0) {
            nbt.putDouble(ROTOR_WEAR_REMAINDER_KEY, remainder)
        } else {
            nbt.remove(ROTOR_WEAR_REMAINDER_KEY)
        }
        markDirty()
        return false
    }

    /**
     * 检测叶片是否被前方的固体方块阻挡。
     *
     * 检查转子旋转平面内的所有固体方块，通过采样叶片路径上的多个点来判断碰撞。
     *
     * @param currentAngle 当前转子旋转角度（度）
     * @return 是否有碰撞
     */
    private fun findBladeCollisionWithBlocks(
        world: World,
        pos: BlockPos,
        facing: Direction,
        rotorRadius: Float,
        currentAngle: Float
    ): Boolean {
        val cx = pos.x + 0.5f
        val cy = pos.y + 0.5f
        val cz = pos.z + 0.5f
        val bladeInnerOffset = PIXEL
        val stepSize = (rotorRadius - bladeInnerOffset) / SAMPLE_COUNT
        val mutablePos = BlockPos.Mutable()

        for (bladeAngle in BLADE_ANGLES) {
            // 叶片旋转角度（加上当前旋转角度）
            val bladeRad = Math.toRadians((bladeAngle + currentAngle).toDouble()).toFloat()
            val cosV = cos(bladeRad.toDouble()).toFloat()
            val sinV = sin(bladeRad.toDouble()).toFloat()

            for (i in 0..SAMPLE_COUNT) {
                val distance = bladeInnerOffset + stepSize * i

                // 根据朝向计算叶片点在旋转平面上的坐标
                val checkX: Int
                val checkY: Int
                val checkZ: Int

                when (facing) {
                    Direction.NORTH -> {
                        // 朝北：转子在 z-1 平面，叶片在 x-y 平面旋转
                        val pointX = cx + cosV * distance
                        val pointY = cy + sinV * distance
                        checkX = floor(pointX).toInt()
                        checkY = floor(pointY).toInt()
                        checkZ = pos.z - 1
                    }
                    Direction.SOUTH -> {
                        // 朝南：转子在 z+1 平面，叶片在 x-y 平面旋转（但旋转了 180°）
                        val pointX = cx - cosV * distance
                        val pointY = cy + sinV * distance
                        checkX = floor(pointX).toInt()
                        checkY = floor(pointY).toInt()
                        checkZ = pos.z + 1
                    }
                    Direction.EAST -> {
                        // 朝东：转子在 x+1 平面，叶片在 y-z 平面旋转
                        val pointY = cy + cosV * distance
                        val pointZ = cz - sinV * distance
                        checkX = pos.x + 1
                        checkY = floor(pointY).toInt()
                        checkZ = floor(pointZ).toInt()
                    }
                    Direction.WEST -> {
                        // 朝西：转子在 x-1 平面，叶片在 y-z 平面旋转
                        val pointY = cy + cosV * distance
                        val pointZ = cz + sinV * distance
                        checkX = pos.x - 1
                        checkY = floor(pointY).toInt()
                        checkZ = floor(pointZ).toInt()
                    }
                    else -> continue
                }

                // 检查该方块是否为实体方块（会阻挡转子旋转）
                mutablePos.set(checkX, checkY, checkZ)
                val blockState = world.getBlockState(mutablePos)

                if (blockState.isOpaqueFullCube(world, mutablePos)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * 风机之间的碰撞使用更粗粒度的圆盘判定：
     * 只要两个转子处在同一旋转平面，且两个圆在该平面内发生重叠，就直接判定卡住。
     * 这里不再做叶片逐点采样，目的是把“风机打架”控制在常数级几何计算里。
     */
    private fun hasRotorCollision(
        world: World,
        pos: BlockPos,
        facing: Direction,
        rotorRadius: Float
    ): Boolean {
        val center = rotorCenter(pos, facing)
        val axis = facing.axis
        val searchRadius = kotlin.math.ceil(rotorRadius + MAX_ROTOR_RADIUS).toInt()
        val normalMin: Int
        val normalMax: Int
        val firstMin: Int
        val firstMax: Int
        val secondMin: Int
        val secondMax: Int

        when (axis) {
            Direction.Axis.X -> {
                normalMin = floor(center.x).toInt() - 1
                normalMax = floor(center.x).toInt() + 1
                firstMin = pos.y - searchRadius
                firstMax = pos.y + searchRadius
                secondMin = pos.z - searchRadius
                secondMax = pos.z + searchRadius
            }
            Direction.Axis.Z -> {
                normalMin = floor(center.z).toInt() - 1
                normalMax = floor(center.z).toInt() + 1
                firstMin = pos.x - searchRadius
                firstMax = pos.x + searchRadius
                secondMin = pos.y - searchRadius
                secondMax = pos.y + searchRadius
            }
            else -> return false
        }

        val mutablePos = BlockPos.Mutable()
        for (normal in normalMin..normalMax) {
            for (first in firstMin..firstMax) {
                for (second in secondMin..secondMax) {
                    when (axis) {
                        Direction.Axis.X -> mutablePos.set(normal, first, second)
                        Direction.Axis.Z -> mutablePos.set(first, second, normal)
                        else -> continue
                    }

                    if (mutablePos == pos) continue
                    val other = world.getBlockEntity(mutablePos) as? IKineticRotorProvider ?: continue
                    val otherRotor = other.getRotorStack()
                    if (otherRotor.isEmpty) continue

                    val otherFacing = world.getBlockState(mutablePos).getOrEmpty(Properties.HORIZONTAL_FACING)
                        .orElse(Direction.NORTH)
                    if (otherFacing.axis != axis) continue

                    val otherRadius = other.getRotorRadius(otherRotor)
                    if (otherRadius <= 0f) continue

                    val otherCenter = rotorCenter(mutablePos, otherFacing)
                    if (planeGap(center, otherCenter, axis) > ROTOR_PLANE_TOLERANCE) continue

                    val distanceSq = perpendicularDistanceSq(center, otherCenter, axis)
                    val overlapRadius = (rotorRadius + otherRadius - ROTOR_OVERLAP_EPSILON).coerceAtLeast(0.0)
                    if (distanceSq < overlapRadius * overlapRadius) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun rotorCenter(pos: BlockPos, facing: Direction): Vec3d =
        Vec3d(
            pos.x + 0.5 + facing.offsetX.toDouble(),
            pos.y + 0.5,
            pos.z + 0.5 + facing.offsetZ.toDouble()
        )

    private fun planeGap(a: Vec3d, b: Vec3d, axis: Direction.Axis): Double =
        when (axis) {
            Direction.Axis.X -> kotlin.math.abs(a.x - b.x)
            Direction.Axis.Y -> kotlin.math.abs(a.y - b.y)
            Direction.Axis.Z -> kotlin.math.abs(a.z - b.z)
        }

    private fun perpendicularDistanceSq(a: Vec3d, b: Vec3d, axis: Direction.Axis): Double =
        when (axis) {
            Direction.Axis.X -> {
                val dy = a.y - b.y
                val dz = a.z - b.z
                dy * dy + dz * dz
            }
            Direction.Axis.Y -> {
                val dx = a.x - b.x
                val dz = a.z - b.z
                dx * dx + dz * dz
            }
            Direction.Axis.Z -> {
                val dx = a.x - b.x
                val dy = a.y - b.y
                dx * dx + dy * dy
            }
        }

    /** 复制渲染器的 yawFromFacing 逻辑，保证碰撞检测与渲染方向一致 */
    private fun yawFromFacing(facing: Direction): Float =
        when (facing) {
            Direction.NORTH -> 0.0f
            Direction.SOUTH -> 180.0f
            Direction.WEST -> 90.0f
            Direction.EAST -> -90.0f
            else -> 0.0f
        }

    private fun markDirtyAndSync() {
        markDirty()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
        }
    }

    override fun size(): Int = 1

    override fun isEmpty(): Boolean = inventory[ROTOR_SLOT].isEmpty

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val result = Inventories.splitStack(inventory, slot, amount)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun removeStack(slot: Int): ItemStack {
        val result = Inventories.removeStack(inventory, slot)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot != ROTOR_SLOT) return
        if (stack.isEmpty) {
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
        } else if (isAllowedRotor(stack)) {
            val copy = stack.copy()
            copy.count = 1
            inventory[ROTOR_SLOT] = copy
        }
        markDirtyAndSync()
    }

    override fun clear() {
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        markDirtyAndSync()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        // Inventories.readNbt 不会在 nbt 中缺失该槽位时自动清空旧值，先清槽避免客户端残留渲染状态
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.wind_kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WindKineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world!!, pos),
            syncedData
        )
}
