package ic2_120.content.block.machines

import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.block.transmission.IKineticRotorProvider
import ic2_120.content.screen.WaterKineticGeneratorScreenHandler
import ic2_120.content.sync.WaterKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.content.network.NetworkManager
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
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
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import ic2_120.editCustomData
import ic2_120.getCustomData
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = WaterKineticGeneratorBlock::class)
class WaterKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IKineticMachinePort, IKineticRotorProvider,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = WaterKineticGeneratorBlock.ACTIVE

    companion object {
        const val ROTOR_SLOT = 0
        private val ALLOWED_ROTORS = setOf("wooden_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
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
        private const val BASE_KU_AT_PEAK = 64.0
        private const val BASE_WEAR_PER_TICK = 2.0
        private const val WATER_FLOW_BONUS_MULTIPLIER = 1.5
        private const val ROTOR_WEAR_REMAINDER_KEY = "RotorWearRemainder"
        private const val TICKS_PER_HOUR = 72_000.0
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)

    private val syncedData = SyncedData(this)
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
        markDirty()
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        WaterKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    override fun canOutputKuTo(side: Direction): Boolean {
        val world = world ?: return false
        val state = world.getBlockState(pos)
        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)
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
        val remainder = rotor.getCustomData()?.getDouble(ROTOR_WEAR_REMAINDER_KEY) ?: 0.0
        val remainingTicks = ((rotor.maxDamage - rotor.damage).toDouble() - remainder).coerceAtLeast(0.0)
        // 显示静水条件下的寿命（与风力显示晴天寿命一致，都是理想条件）
        return remainingTicks / TICKS_PER_HOUR / BASE_WEAR_PER_TICK
    }

    fun onUse(player: PlayerEntity, hand: Hand): ActionResult {
        val held = player.getStackInHand(hand)
        val rotor = inventory[ROTOR_SLOT]

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

        if (!isAllowedRotor(held)) return ActionResult.PASS

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
            "wooden_rotor" -> 1.0f
            "iron_rotor" -> 1.5f
            "steel_rotor" -> 2.0f
            "carbon_rotor" -> 2.5f
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

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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

        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)

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

    private fun checkSubmersion(world: World, pos: BlockPos, facing: Direction, rotorRadius: Float): SubmersionInfo {
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

    private fun isWaterBlock(world: World, x: Int, y: Int, z: Int): Boolean {
        val blockState = world.getBlockState(BlockPos(x, y, z))
        return blockState.block == Blocks.WATER
    }

    private fun isFlowingWater(world: World, x: Int, y: Int, z: Int): Boolean {
        val blockState = world.getBlockState(BlockPos(x, y, z))
        if (blockState.block != Blocks.WATER) return false
        return blockState.contains(net.minecraft.state.property.Properties.LEVEL_1_8) &&
            blockState.get(net.minecraft.state.property.Properties.LEVEL_1_8) > 0
    }

    private fun applyRotorWear(rotor: ItemStack, wearMultiplier: Double): Boolean {
        if (!rotor.isDamageable || wearMultiplier <= 0.0) return false
        val accumulated = ((rotor.getCustomData()?.getDouble(ROTOR_WEAR_REMAINDER_KEY) ?: 0.0) + wearMultiplier).coerceAtLeast(0.0)
        val wear = floor(accumulated).toInt()
        val remainder = accumulated - wear

        if (wear <= 0) {
            rotor.editCustomData { it.putDouble(ROTOR_WEAR_REMAINDER_KEY, remainder) }
            return false
        }

        if (rotor.damage + wear >= rotor.maxDamage) {
            rotor.editCustomData { it.remove(ROTOR_WEAR_REMAINDER_KEY) }
            return true
        }

        rotor.damage += wear
        rotor.editCustomData { nbt ->
            if (remainder > 0.0) {
                nbt.putDouble(ROTOR_WEAR_REMAINDER_KEY, remainder)
            } else {
                nbt.remove(ROTOR_WEAR_REMAINDER_KEY)
            }
        }
        markDirty()
        return false
    }

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

                if (blockState.isOpaqueFullCube(world, mutablePos)) {
                    return true
                }
            }
        }

        return false
    }

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

                    val otherRadius = other.getRotorRadius(otherRotor)
                    if (otherRadius <= 0f) continue

                    val otherState = world.getBlockState(mutablePos)
                    val otherFacing = otherState.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)
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

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
    }

    override fun toInitialChunkDataNbt(lookup: RegistryWrapper.WrapperLookup): NbtCompound = createNbt(lookup)

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.water_kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WaterKineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world!!, pos),
            syncedData
        )
}
