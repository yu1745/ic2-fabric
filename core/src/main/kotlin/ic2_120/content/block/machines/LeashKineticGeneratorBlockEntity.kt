package ic2_120.content.block.machines

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.LeashKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.sync.LeashKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level

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

    private val syncedData = SyncedData()
    val sync = LeashKineticGeneratorSync(syncedData)

    private var pendingOutputKu: Int = 0
    private var leashedMobUuid: java.util.UUID? = null
    private var knotUuid: java.util.UUID? = null
    private var mobName: String = ""

    private val angleHistory = mutableListOf<Double>()
    private val positionHistory = mutableListOf<Pair<Long, Vec3>>()
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

    override fun getInventory(): net.minecraft.world.Container? = null

    fun findLeashedMob(): Mob? {
        val world = world ?: return null
        val uuid = leashedMobUuid ?: return null
        val serverWorld = world as? ServerLevel ?: return null
        return serverWorld.getEntity(uuid) as? Mob
    }

    fun findKnot(): Entity? {
        val world = world ?: return null
        val uuid = knotUuid ?: return null
        val serverWorld = world as? ServerLevel ?: return null
        return serverWorld.getEntity(uuid)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) {
            return
        }

        val mob = findLeashedMob()
        val knot = findKnot()
        val mobValid = mob != null && mob.isAlive && knot != null && knot.isAlive
        if (!mobValid) {
            if (leashedMobUuid != null) {
                (world as? ServerLevel)?.let { knot?.kill(it) }
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

        val mobPos = mob.position()
        val centerX = pos.x + 0.5
        val centerZ = pos.z + 0.5
        val dx = mobPos.x - centerX
        val dz = mobPos.z - centerZ
        val dy = (mobPos.y + mob.getEyeHeight()) - (pos.y + 1.5)
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

        val currentAngle = Mth.atan2(dz, dx)
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
            setChanged()
        }
        return extracted
    }

    private fun markDirtyAndSync() {
        setChanged()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)
            (world as? ServerLevel)?.chunkSource?.blockChanged(pos)
        }
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        pendingOutputKu = nbt.getIntOr("PendingKu", 0).coerceAtLeast(0)
        leashedMobUuid = nbt.read("LeashedMobUuid", net.minecraft.core.UUIDUtil.CODEC).orElse(null)
        knotUuid = nbt.read("KnotUuid", net.minecraft.core.UUIDUtil.CODEC).orElse(null)
        mobName = nbt.getStringOr("MobName", "")
        syncedData.readNbt(nbt)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        nbt.putInt("PendingKu", pendingOutputKu)
        leashedMobUuid?.let { nbt.store("LeashedMobUuid", net.minecraft.core.UUIDUtil.CODEC, it) }
        knotUuid?.let { nbt.store("KnotUuid", net.minecraft.core.UUIDUtil.CODEC, it) }
        nbt.putString("MobName", mobName)
        syncedData.writeNbt(nbt)
    }

    override fun getUpdateTag(lookup: HolderLookup.Provider): CompoundTag = saveWithFullMetadata(lookup)

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)
}
