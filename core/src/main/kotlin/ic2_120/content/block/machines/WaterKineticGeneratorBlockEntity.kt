package ic2_120.content.block.machines

import ic2_120.compat.ContainerHelper26

import net.minecraft.core.registries.BuiltInRegistries

import net.minecraft.core.HolderLookup


import net.minecraft.server.level.ServerPlayer


import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.block.transmission.IKineticRotorProvider
import ic2_120.content.screen.WaterKineticGeneratorScreenHandler
import ic2_120.content.sync.WaterKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.content.network.NetworkManager
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag

import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import ic2_120.editCustomData
import ic2_120.getCustomData
import net.minecraft.network.FriendlyByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = WaterKineticGeneratorBlock::class)
class WaterKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IKineticMachinePort, IKineticRotorProvider,
    net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = WaterKineticGeneratorBlock.ACTIVE

    companion object {
        const val INVENTORY_SIZE = 1
        const val ROTOR_SLOT = 0
        private val ALLOWED_ROTORS = setOf("wooden_rotor", "bronze_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
        private const val PIXEL = 1.0f / 16.0f
        private const val BASE_SCAN_INTERVAL_TICKS = 5L
        private const val STUCK_SCAN_INTERVAL_TICKS = 2L
        private const val SAMPLE_COUNT = 5
        private const val ROTOR_RPM_DEGREES_PER_TICK = 1.8f
        private const val STUCK_DISPLAY_ANGLE = 45f
        private val BLADE_ANGLES = floatArrayOf(0f, 90f, 180f, 270f)
        private const val MAX_ROTOR_RADIUS = 5.0
        private const val ROTOR_PLANE_TOLERANCE = 0.001
        private const val ROTOR_OVERLAP_EPSILON = 1.0 / 64.0
        private const val BASE_KU_AT_PEAK = 128.0
        private const val BASE_WEAR_PER_TICK = 1.0
        private const val WATER_FLOW_BONUS_MULTIPLIER = 1.5
        private const val ROTOR_WEAR_REMAINDER_KEY = "RotorWearRemainder"
        private const val TICKS_PER_HOUR = 72_000.0
    }

    override val tier: Int = 1
    private val inventory = NonNullList.withSize(1, ItemStack.EMPTY)

    private val syncedData = SyncedData()
    private val kineticSync = WaterKineticGeneratorSync(syncedData)

    var isStuck: Boolean
        get() = kineticSync.isStuck != 0
        private set(value) {
            kineticSync.isStuck = if (value) 1 else 0
        }

    var stuckAngle: Float
        get() = kineticSync.stuckAngle.toFloat()
        private set(value) {
            kineticSync.stuckAngle = value.toInt()
        }

    var isSubmerged: Boolean
        get() = kineticSync.isSubmerged != 0
        private set(value) {
            kineticSync.isSubmerged = if (value) 1 else 0
        }

    var waterFlowBonus: Boolean
        get() = kineticSync.waterFlowBonus != 0
        private set(value) {
            kineticSync.waterFlowBonus = if (value) 1 else 0
        }

    private var stuckWorldTime: Long = 0L
    private val scanOffset: Int = (pos.asLong().toInt() and Int.MAX_VALUE) % BASE_SCAN_INTERVAL_TICKS.toInt()
    private var lastCollisionResult: Boolean = false
    private var lastCollisionCheckTick: Long = Long.MIN_VALUE
    private var pendingOutputKu: Int = 0

    fun receiveRotorState(isStuck: Boolean, stuckAngle: Float) {
        kineticSync.isStuck = if (isStuck) 1 else 0
        kineticSync.stuckAngle = stuckAngle.toInt()
        setChanged()
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        WaterKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this

    override fun canOutputKuTo(side: Direction): Boolean {
        val world = world ?: return false
        val state = world.getBlockState(pos)
        val facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)
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
            setChanged()
        }
        return extracted
    }

    fun getGeneratedKu(): Int = kineticSync.generatedKu.coerceAtLeast(0)

    fun getOutputKu(): Int = kineticSync.outputKu.coerceAtLeast(0)

    override fun getRotorStack(): ItemStack = inventory[ROTOR_SLOT]

    fun getRotorRemainingClearHours(): Double {
        val rotor = inventory[ROTOR_SLOT]
        if (rotor.isEmpty || !rotor.isDamageableItem) return 0.0
        val remainder = rotor.getCustomData()?.getDoubleOr(ROTOR_WEAR_REMAINDER_KEY, 0.0) ?: 0.0
        val remainingTicks = ((rotor.maxDamage - rotor.damageValue).toDouble() - remainder).coerceAtLeast(0.0)
        // 显示静水条件下的寿命（与风力显示晴天寿命一致，都是理想条件）
        return remainingTicks / TICKS_PER_HOUR / BASE_WEAR_PER_TICK
    }

    fun useWithoutItem(player: Player, hand: InteractionHand): InteractionResult {
        val held = player.getItemInHand(player.usedItemHand)
        val rotor = inventory[ROTOR_SLOT]

        if (held.isEmpty) {
            if (rotor.isEmpty) return InteractionResult.PASS
            val extracted = rotor.copy()
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
            markDirtyAndSync()
            if (!player.getInventory().add(extracted)) {
                (player.level() as? net.minecraft.server.level.ServerLevel)?.let { player.spawnAtLocation(it, extracted) }
            }
            return InteractionResult.SUCCESS
        }

        if (!isAllowedRotor(held)) return InteractionResult.PASS

        if (!rotor.isEmpty) return InteractionResult.PASS

        val inserted = held.copy()
        inserted.count = 1
        inventory[ROTOR_SLOT] = inserted
        if (!player.abilities.instabuild) {
            held.shrink(1)
        }
        markDirtyAndSync()
        return InteractionResult.SUCCESS
    }

    private fun isAllowedRotor(stack: ItemStack): Boolean {
        val id = BuiltInRegistries.ITEM.getKey(stack.item)
        return ALLOWED_ROTORS.contains(id.path)
    }

    override fun getRotorRadius(stack: ItemStack): Float {
        return when (BuiltInRegistries.ITEM.getKey(stack.item)!!.path) {
            "wooden_rotor" -> 1.0f
            "bronze_rotor" -> 1.5f
            "iron_rotor" -> 1.5f
            "steel_rotor" -> 2.0f
            "carbon_rotor" -> 2.5f
            else -> 0.0f
        }
    }

    private fun getRotorMultiplier(stack: ItemStack): Int {
        return when (BuiltInRegistries.ITEM.getKey(stack.item)!!.path) {
            "wooden_rotor" -> 1
            "bronze_rotor" -> 2
            "iron_rotor" -> 2
            "steel_rotor" -> 3
            "carbon_rotor" -> 4
            else -> 0
        }
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        kineticSync.outputKu = 0
        val rotor = inventory[ROTOR_SLOT]
        if (rotor.isEmpty) {
            pendingOutputKu = 0
            kineticSync.generatedKu = 0
            kineticSync.rotorLifetimeTenthsHours = 0
            kineticSync.isSubmerged = 0
            kineticSync.waterFlowBonus = 0
            if (isStuck) {
                isStuck = false
                kineticSync.stuckAngle = 0
                markDirtyAndSync()
                NetworkManager.sendWaterRotorStateToNearby(world, pos, false, 0f)
            }
            lastCollisionResult = false
            setActiveState(world, pos, state, false)
            return
        }

        val rotorRadius = getRotorRadius(rotor)
        if (rotorRadius <= 0f) return
        kineticSync.rotorLifetimeTenthsHours = (getRotorRemainingClearHours() * 10.0).toInt().coerceAtLeast(0)

        if (world.isClient) return

        val facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        val currentAngle: Float = if (isStuck) {
            stuckAngle
        } else {
            ((stuckAngle + (world.time - stuckWorldTime) * ROTOR_RPM_DEGREES_PER_TICK) % 360.0f).let {
                if (it < 0) it + 360.0f else it
            }
        }

        val submersionInfo = checkSubmersion(world, pos, facing, rotorRadius)
        isSubmerged = submersionInfo.isSubmerged
        waterFlowBonus = submersionInfo.hasFlowingWater

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
                NetworkManager.sendWaterRotorStateToNearby(world, pos, true, stuckAngle)
            }
        } else {
            if (isStuck) {
                isStuck = false
                stuckAngle = STUCK_DISPLAY_ANGLE
                stuckWorldTime = world.time
                markDirtyAndSync()
                NetworkManager.sendWaterRotorStateToNearby(world, pos, false, 0f)
            }
        }

        val active = if (!isStuck && submersionInfo.isSubmerged) {
            val rotorMultiplier = getRotorMultiplier(rotor)
            val flowBonus = if (submersionInfo.hasFlowingWater) WATER_FLOW_BONUS_MULTIPLIER else 1.0
            val kuOut = floor(BASE_KU_AT_PEAK * rotorMultiplier * flowBonus).toInt().coerceAtLeast(0)
            kineticSync.generatedKu = kuOut
            pendingOutputKu = kuOut
            val wearMultiplier = BASE_WEAR_PER_TICK * flowBonus
            if (kuOut > 0 && applyRotorWear(rotor, wearMultiplier)) {
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

    private data class SubmersionInfo(val isSubmerged: Boolean, val hasFlowingWater: Boolean)

    private fun checkSubmersion(world: Level, pos: BlockPos, facing: Direction, rotorRadius: Float): SubmersionInfo {
        val cx = pos.x + 0.5
        val cy = pos.y + 0.5
        val cz = pos.z + 0.5

        var allWater = true
        var anyFlowing = false

        val normalOffset = 1
        val bladeInnerOffset = PIXEL
        val stepSize = (rotorRadius - bladeInnerOffset) / SAMPLE_COUNT

        for (i in 0..SAMPLE_COUNT) {
            val distance = bladeInnerOffset + stepSize * i

            for (bladeAngle in BLADE_ANGLES) {
                val cosV = cos(Math.toRadians(bladeAngle.toDouble())).toFloat()
                val sinV = sin(Math.toRadians(bladeAngle.toDouble())).toFloat()

                when (facing) {
                    Direction.NORTH -> {
                        val pointX = cx + cosV * distance
                        val pointY = cy + sinV * distance
                        val checkX = floor(pointX).toInt()
                        val checkY = floor(pointY).toInt()
                        val checkZ = pos.z - normalOffset
                        if (!isWaterBlock(world, checkX, checkY, checkZ)) {
                            allWater = false
                        } else if (isFlowingWater(world, checkX, checkY, checkZ)) {
                            anyFlowing = true
                        }
                    }
                    Direction.SOUTH -> {
                        val pointX = cx - cosV * distance
                        val pointY = cy + sinV * distance
                        val checkX = floor(pointX).toInt()
                        val checkY = floor(pointY).toInt()
                        val checkZ = pos.z + normalOffset
                        if (!isWaterBlock(world, checkX, checkY, checkZ)) {
                            allWater = false
                        } else if (isFlowingWater(world, checkX, checkY, checkZ)) {
                            anyFlowing = true
                        }
                    }
                    Direction.EAST -> {
                        val pointY = cy + cosV * distance
                        val pointZ = cz - sinV * distance
                        val checkX = pos.x + normalOffset
                        val checkY = floor(pointY).toInt()
                        val checkZ = floor(pointZ).toInt()
                        if (!isWaterBlock(world, checkX, checkY, checkZ)) {
                            allWater = false
                        } else if (isFlowingWater(world, checkX, checkY, checkZ)) {
                            anyFlowing = true
                        }
                    }
                    Direction.WEST -> {
                        val pointY = cy + cosV * distance
                        val pointZ = cz + sinV * distance
                        val checkX = pos.x - normalOffset
                        val checkY = floor(pointY).toInt()
                        val checkZ = floor(pointZ).toInt()
                        if (!isWaterBlock(world, checkX, checkY, checkZ)) {
                            allWater = false
                        } else if (isFlowingWater(world, checkX, checkY, checkZ)) {
                            anyFlowing = true
                        }
                    }
                    else -> {
                        allWater = false
                    }
                }

                if (!allWater && anyFlowing) break
            }
            if (!allWater && anyFlowing) break
        }

        return SubmersionInfo(allWater, anyFlowing)
    }

    private fun isWaterBlock(world: Level, x: Int, y: Int, z: Int): Boolean {
        val fluidState = world.getFluidState(BlockPos(x, y, z))
        return fluidState.`is`(Fluids.WATER) || fluidState.`is`(Fluids.FLOWING_WATER)
    }

    private fun isFlowingWater(world: Level, x: Int, y: Int, z: Int): Boolean {
        val fluidState = world.getFluidState(BlockPos(x, y, z))
        if (!fluidState.`is`(Fluids.WATER) && !fluidState.`is`(Fluids.FLOWING_WATER)) return false
        return !fluidState.isSource
    }

    private fun applyRotorWear(rotor: ItemStack, wearMultiplier: Double): Boolean {
        if (!rotor.isDamageableItem || wearMultiplier <= 0.0) return false
        val accumulated = ((rotor.getCustomData()?.getDoubleOr(ROTOR_WEAR_REMAINDER_KEY, 0.0) ?: 0.0) + wearMultiplier).coerceAtLeast(0.0)
        val wear = floor(accumulated).toInt()
        val remainder = accumulated - wear

        if (wear <= 0) {
            rotor.editCustomData { it.putDouble(ROTOR_WEAR_REMAINDER_KEY, remainder) }
            return false
        }

        if (rotor.damageValue + wear >= rotor.maxDamage) {
            rotor.editCustomData { it.remove(ROTOR_WEAR_REMAINDER_KEY) }
            return true
        }

        rotor.damageValue += wear
        rotor.editCustomData { nbt ->
            if (remainder > 0.0) {
                nbt.putDouble(ROTOR_WEAR_REMAINDER_KEY, remainder)
            } else {
                nbt.remove(ROTOR_WEAR_REMAINDER_KEY)
            }
        }
        setChanged()
        return false
    }

    private fun findBladeCollisionWithBlocks(
        world: Level,
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
        val mutablePos = BlockPos.MutableBlockPos()

        for (bladeAngle in BLADE_ANGLES) {
            val bladeRad = Math.toRadians((bladeAngle + currentAngle).toDouble()).toFloat()
            val cosV = cos(bladeRad.toDouble()).toFloat()
            val sinV = sin(bladeRad.toDouble()).toFloat()

            for (i in 0..SAMPLE_COUNT) {
                val distance = bladeInnerOffset + stepSize * i

                val checkX: Int
                val checkY: Int
                val checkZ: Int

                when (facing) {
                    Direction.NORTH -> {
                        val pointX = cx + cosV * distance
                        val pointY = cy + sinV * distance
                        checkX = floor(pointX).toInt()
                        checkY = floor(pointY).toInt()
                        checkZ = pos.z - 1
                    }
                    Direction.SOUTH -> {
                        val pointX = cx - cosV * distance
                        val pointY = cy + sinV * distance
                        checkX = floor(pointX).toInt()
                        checkY = floor(pointY).toInt()
                        checkZ = pos.z + 1
                    }
                    Direction.EAST -> {
                        val pointY = cy + cosV * distance
                        val pointZ = cz - sinV * distance
                        checkX = pos.x + 1
                        checkY = floor(pointY).toInt()
                        checkZ = floor(pointZ).toInt()
                    }
                    Direction.WEST -> {
                        val pointY = cy + cosV * distance
                        val pointZ = cz + sinV * distance
                        checkX = pos.x - 1
                        checkY = floor(pointY).toInt()
                        checkZ = floor(pointZ).toInt()
                    }
                    else -> continue
                }

                mutablePos.set(checkX, checkY, checkZ)
                val blockState = world.getBlockState(mutablePos)

                if (blockState.isSolidRender) {
                    return true
                }
            }
        }

        return false
    }

    private fun hasRotorCollision(
        world: Level,
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

        val mutablePos = BlockPos.MutableBlockPos()
        for (normal in normalMin..normalMax) {
            for (first in firstMin..firstMax) {
                for (second in secondMin..secondMax) {
                    when (axis) {
                        Direction.Axis.X -> mutablePos.set(normal, first, second)
                        Direction.Axis.Y -> continue
                        Direction.Axis.Z -> mutablePos.set(first, second, normal)
                    }

                    if (mutablePos == pos) continue

                    val other = world.getBlockEntity(mutablePos) as? IKineticRotorProvider ?: continue
                    val otherRotor = other.getRotorStack()
                    if (otherRotor.isEmpty) continue

                    val otherRadius = other.getRotorRadius(otherRotor)
                    if (otherRadius <= 0f) continue

                    val otherState = world.getBlockState(mutablePos)
                    val otherFacing = otherState.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)
                    if (otherFacing.axis != axis) continue

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

    private fun rotorCenter(pos: BlockPos, facing: Direction): Vec3 =
        Vec3(
            pos.x + 0.5 + facing.stepX.toDouble(),
            pos.y + 0.5,
            pos.z + 0.5 + facing.stepZ.toDouble()
        )

    private fun planeGap(a: Vec3, b: Vec3, axis: Direction.Axis): Double =
        when (axis) {
            Direction.Axis.X -> kotlin.math.abs(a.x - b.x)
            Direction.Axis.Y -> kotlin.math.abs(a.y - b.y)
            Direction.Axis.Z -> kotlin.math.abs(a.z - b.z)
        }

    private fun perpendicularDistanceSq(a: Vec3, b: Vec3, axis: Direction.Axis): Double =
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

    private fun markDirtyAndSync() {
        setChanged()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)
        }
    }

    override fun getContainerSize(): Int = 1

    override fun isEmpty(): Boolean = inventory[ROTOR_SLOT].isEmpty

    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val result = ContainerHelper.removeItem(inventory, slot, amount)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val result = ContainerHelper.takeItem(inventory, slot)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun setItem(slot: Int, stack: ItemStack) {
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

    override fun clearContent() {
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        markDirtyAndSync()
    }

    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
    }

    override fun getUpdateTag(lookup: HolderLookup.Provider): CompoundTag = saveWithFullMetadata(lookup)

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.count)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.water_kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        WaterKineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            ContainerLevelAccess.create(world!!, pos),
            syncedData
        )
}
