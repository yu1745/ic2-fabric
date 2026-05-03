package ic2_120.content.block.machines

import ic2_120.content.block.LeashKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.sync.LeashKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

@ModBlockEntity(block = LeashKineticGeneratorBlock::class)
class LeashKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IKineticMachinePort {

    companion object {
        private const val MAX_LEASH_RANGE = 10.0
        private const val BASE_KU = 8
        private const val KU_PER_DEG_PER_SEC = 2
        private const val KU_PER_CM = 1
        private const val MIN_ANGULAR_VELOCITY = 5.0
        private const val HISTORY_SIZE = 40
        private const val POSITION_HISTORY_SIZE = 20
    }

    override val tier: Int = 1
    override val activeProperty = LeashKineticGeneratorBlock.ACTIVE

    private val syncedData = SyncedData(this)
    val sync = LeashKineticGeneratorSync(syncedData)

    private var pendingOutputKu: Int = 0
    private var leashedMobUuid: java.util.UUID? = null
    private var knotUuid: java.util.UUID? = null
    private var mobName: String = ""

    private val angleHistory = mutableListOf<Double>()
    private val positionHistory = mutableListOf<Pair<Long, Vec3d>>()
    private var lastTickAngle: Double? = null
    private var syncCounter = 0

    constructor(pos: BlockPos, state: BlockState) : this(
        LeashKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    fun setLeashedMob(mobUuid: java.util.UUID?, name: String, knotUuid: java.util.UUID? = null) {
        leashedMobUuid = mobUuid
        this.knotUuid = knotUuid
        mobName = name
        if (mobUuid == null) {
            angleHistory.clear()
            positionHistory.clear()
            lastTickAngle = null
        }
        markDirtyAndSync()
    }

    fun getLeashedMobUuid(): java.util.UUID? = leashedMobUuid

    fun getMobName(): String = mobName

    fun hasLeashedMob(): Boolean = leashedMobUuid != null

    override fun getInventory(): net.minecraft.inventory.Inventory? = null

    fun findLeashedMob(): MobEntity? {
        val world = world ?: return null
        val uuid = leashedMobUuid ?: return null
        val serverWorld = world as? ServerWorld ?: return null
        return serverWorld.getEntity(uuid) as? MobEntity
    }

    fun findKnot(): Entity? {
        val world = world ?: return null
        val uuid = knotUuid ?: return null
        val serverWorld = world as? ServerWorld ?: return null
        return serverWorld.getEntity(uuid)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) {
            return
        }

        val mob = findLeashedMob()
        val knot = findKnot()
        val mobValid = mob != null && mob.isAlive && knot != null && knot.isAlive
        if (!mobValid) {
            if (leashedMobUuid != null) {
                knot?.kill()
                knot?.remove(Entity.RemovalReason.DISCARDED)
                setLeashedMob(null, "")
            }
            pendingOutputKu = 0
            sync.hasAnimal = false
            sync.outputKu = 0
            sync.generatedKu = 0
            sync.leashLengthCm = 0
            sync.angularVelocityDegPerSec = 0
            setActiveState(world, pos, state, false)
            return
        }

        sync.hasAnimal = true

        val mobPos = mob.pos
        val centerX = pos.x + 0.5
        val centerZ = pos.z + 0.5
        val dx = mobPos.x - centerX
        val dz = mobPos.z - centerZ
        val dy = (mobPos.y + mob.getStandingEyeHeight()) - (pos.y + 1.5)
        val horizontalDist = Math.sqrt(dx * dx + dz * dz)
        val leashLength = Math.sqrt(dx * dx + dy * dy + dz * dz)

        if (horizontalDist < 0.5) {
            pendingOutputKu = 0
            sync.outputKu = 0
            sync.leashLengthCm = (leashLength * 100).toInt()
            sync.angularVelocityDegPerSec = 0
            setActiveState(world, pos, state, false)
            return
        }

        val currentAngle = MathHelper.atan2(dz, dx)
        val previousAngle = lastTickAngle
        lastTickAngle = currentAngle

        if (previousAngle != null) {
            var delta = currentAngle - previousAngle
            if (delta > Math.PI) delta -= 2 * Math.PI
            if (delta < -Math.PI) delta += 2 * Math.PI
            val angularVelocity = kotlin.math.abs(delta * 20.0)
            val degPerSec = Math.toDegrees(angularVelocity)

            angleHistory.add(angularVelocity)
            if (angleHistory.size > HISTORY_SIZE) {
                angleHistory.removeAt(0)
            }

            val avgAngularVelocity = if (angleHistory.isNotEmpty()) {
                angleHistory.sum() / angleHistory.size
            } else 0.0

            val avgDegPerSec = Math.toDegrees(avgAngularVelocity)

            if (avgDegPerSec >= MIN_ANGULAR_VELOCITY) {
                val kuFromVelocity = (avgDegPerSec * KU_PER_DEG_PER_SEC).toInt()
                val kuFromLength = (horizontalDist * 100 * KU_PER_CM / 10).toInt()
                val totalKu = (BASE_KU + kuFromVelocity + kuFromLength).coerceAtMost(512)

                pendingOutputKu += totalKu
                sync.generatedKu = totalKu
                sync.outputKu = totalKu
            } else {
                sync.outputKu = 0
            }

        sync.leashLengthCm = (leashLength * 100).toInt()
        sync.angularVelocityDegPerSec = avgDegPerSec.toInt()
        sync.animalAngle = Math.toDegrees(currentAngle).toFloat()
    }

        val active = pendingOutputKu > 0 && sync.angularVelocityDegPerSec >= MIN_ANGULAR_VELOCITY.toInt()
        setActiveState(world, pos, state, active)

        // Periodic sync for BER rendering (angle, hasAnimal, etc.)
        syncCounter++
        if (syncCounter >= 3) {
            syncCounter = 0
            markDirtyAndSync()
        }
    }

    override fun canOutputKuTo(side: Direction): Boolean = true

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
            markDirty()
        }
        return extracted
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

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        pendingOutputKu = nbt.getInt("PendingKu").coerceAtLeast(0)
        if (nbt.containsUuid("LeashedMobUuid")) {
            leashedMobUuid = nbt.getUuid("LeashedMobUuid")
        }
        if (nbt.containsUuid("KnotUuid")) {
            knotUuid = nbt.getUuid("KnotUuid")
        }
        mobName = nbt.getString("MobName")
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putInt("PendingKu", pendingOutputKu)
        leashedMobUuid?.let { nbt.putUuid("LeashedMobUuid", it) }
        knotUuid?.let { nbt.putUuid("KnotUuid", it) }
        nbt.putString("MobName", mobName)
        syncedData.writeNbt(nbt)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)
}
