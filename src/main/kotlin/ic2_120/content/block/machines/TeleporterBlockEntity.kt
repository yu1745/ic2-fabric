package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.TeleporterBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.TeleporterScreenHandler
import ic2_120.content.sync.TeleporterSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.UUID
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

@ModBlockEntity(block = TeleporterBlock::class)
class TeleporterBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    ITieredMachine,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = TeleporterBlock.ACTIVE
    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = TELEPORTER_TIER
    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val TELEPORTER_TIER = 4
        private val TELEPORTER_CHARGE_SOUND: SoundEvent = SoundEvent.of(Identifier("ic2", "machine.teleporter.charge"))
        private val TELEPORTER_USE_SOUND: SoundEvent = SoundEvent.of(Identifier("ic2", "machine.teleporter.use"))

        const val SLOT_DISCHARGING = 0
        const val SLOT_UPGRADE_0 = 1
        const val SLOT_UPGRADE_1 = 2
        const val SLOT_UPGRADE_2 = 3
        const val SLOT_UPGRADE_3 = 4
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 5

        private const val NBT_TARGET_SET = "TargetSet"
        private const val NBT_TARGET_X = "TargetX"
        private const val NBT_TARGET_Y = "TargetY"
        private const val NBT_TARGET_Z = "TargetZ"
        private const val NBT_TARGET_DIM = "TargetDim"
        private const val NBT_COOLDOWN = "TeleportCooldown"

        private const val NBT_CHARGING_ENTITY = "ChargingEntity"
        private const val NBT_CHARGING_TARGET_X = "ChargingTargetX"
        private const val NBT_CHARGING_TARGET_Y = "ChargingTargetY"
        private const val NBT_CHARGING_TARGET_Z = "ChargingTargetZ"
        private const val NBT_CHARGING_ENERGY = "ChargingEnergy"
        private const val NBT_CHARGE_ELAPSED = "ChargeElapsed"
        private const val NBT_CHARGE_MAX = "ChargeMax"
        private const val NBT_TELEPORT_RANGE = "TeleportRange"

        const val TELEPORT_RANGE_MIN = 1
        const val TELEPORT_RANGE_MAX = 3

        private fun normalizeRange(value: Int): Int = if (value >= 2) TELEPORT_RANGE_MAX else TELEPORT_RANGE_MIN
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = TeleporterSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(TELEPORTER_TIER + voltageTierBonus) }
    )

    var targetPos: BlockPos? = null
        private set
    var targetDimension: String = ""
        private set
    private var teleportCooldown: Int = 0

    private var chargingEntityUuid: UUID? = null
    private var chargingTargetPos: BlockPos? = null
    private var chargingEnergyNeed: Long = 0L
    private var chargeTicksElapsed: Int = 0
    private var chargeTicksMax: Int = 0
    private var teleportRange: Int = TELEPORT_RANGE_MIN

    var clientCharging: Boolean = false
        private set
    var clientChargeProgress: Int = 0
        private set
    var clientChargeMax: Int = 0
        private set
    var clientTeleportRange: Int = TELEPORT_RANGE_MIN
        private set
    var clientChargingEntityId: Int = -1
        private set

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { TELEPORTER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        TeleporterBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun markDirty() = super.markDirty()

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    fun setTarget(pos: BlockPos, dimension: String) {
        targetPos = pos.toImmutable()
        targetDimension = dimension
        sync.targetSet = 1
        sync.targetX = pos.x
        sync.targetY = pos.y
        sync.targetZ = pos.z
        markDirty()
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.teleporter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): net.minecraft.screen.ScreenHandler =
        TeleporterScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)

        sync.amount = nbt.getLong(TeleporterSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        if (nbt.getBoolean(NBT_TARGET_SET)) {
            targetPos = BlockPos(nbt.getInt(NBT_TARGET_X), nbt.getInt(NBT_TARGET_Y), nbt.getInt(NBT_TARGET_Z))
            targetDimension = nbt.getString(NBT_TARGET_DIM)
        } else {
            targetPos = null
            targetDimension = ""
        }

        teleportCooldown = nbt.getInt(NBT_COOLDOWN).coerceAtLeast(0)
        chargingEntityUuid = if (nbt.contains(NBT_CHARGING_ENTITY)) nbt.getUuid(NBT_CHARGING_ENTITY) else null
        chargingTargetPos = if (nbt.contains(NBT_CHARGING_TARGET_X)) {
            BlockPos(nbt.getInt(NBT_CHARGING_TARGET_X), nbt.getInt(NBT_CHARGING_TARGET_Y), nbt.getInt(NBT_CHARGING_TARGET_Z))
        } else {
            null
        }
        chargingEnergyNeed = nbt.getLong(NBT_CHARGING_ENERGY).coerceAtLeast(0L)
        chargeTicksElapsed = nbt.getInt(NBT_CHARGE_ELAPSED).coerceAtLeast(0)
        chargeTicksMax = nbt.getInt(NBT_CHARGE_MAX).coerceAtLeast(0)
        teleportRange = normalizeRange(nbt.getInt(NBT_TELEPORT_RANGE))

        sync.cooldown = teleportCooldown
        sync.targetSet = if (targetPos != null) 1 else 0
        sync.targetX = targetPos?.x ?: 0
        sync.targetY = targetPos?.y ?: 0
        sync.targetZ = targetPos?.z ?: 0
        sync.charging = if (isCharging()) 1 else 0
        sync.chargeProgress = chargeTicksElapsed
        sync.chargeMax = chargeTicksMax
        sync.teleportRange = teleportRange
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)

        nbt.putLong(TeleporterSync.NBT_ENERGY_STORED, sync.amount)
        val target = targetPos
        nbt.putBoolean(NBT_TARGET_SET, target != null)
        if (target != null) {
            nbt.putInt(NBT_TARGET_X, target.x)
            nbt.putInt(NBT_TARGET_Y, target.y)
            nbt.putInt(NBT_TARGET_Z, target.z)
            nbt.putString(NBT_TARGET_DIM, targetDimension)
        }
        nbt.putInt(NBT_COOLDOWN, teleportCooldown)

        chargingEntityUuid?.let { nbt.putUuid(NBT_CHARGING_ENTITY, it) }
        chargingTargetPos?.let {
            nbt.putInt(NBT_CHARGING_TARGET_X, it.x)
            nbt.putInt(NBT_CHARGING_TARGET_Y, it.y)
            nbt.putInt(NBT_CHARGING_TARGET_Z, it.z)
        }
        nbt.putLong(NBT_CHARGING_ENERGY, chargingEnergyNeed)
        nbt.putInt(NBT_CHARGE_ELAPSED, chargeTicksElapsed)
        nbt.putInt(NBT_CHARGE_MAX, chargeTicksMax)
        nbt.putInt(NBT_TELEPORT_RANGE, teleportRange)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        sync.teleportRange = teleportRange

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        if (teleportCooldown > 0) {
            teleportCooldown--
            sync.cooldown = teleportCooldown
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        if (!hasAdjacentMfeOrMfsu(world, pos)) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val target = targetPos
        if (target == null || targetDimension.isBlank()) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val thisDim = world.registryKey.value.toString()
        if (thisDim != targetDimension || !world.isChunkLoaded(target)) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val targetBe = world.getBlockEntity(target) as? TeleporterBlockEntity
        if (targetBe == null || targetBe.teleportCooldown > 0) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        if (isCharging()) {
            val charging = tickCharging(world as? ServerWorld, pos, state, targetBe)
            setActiveState(world, pos, state, charging)
            endTick(world, pos)
            return
        }

        val powered = world.isReceivingRedstonePower(pos)
        if (!powered) {
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val entity = world.getOtherEntities(null, getActivationBox(pos)) {
            it.isAlive && !it.hasVehicle() && !it.hasPassengers()
        }
            .minByOrNull { it.squaredDistanceTo(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) }

        if (entity == null) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val distance = sqrt(pos.getSquaredDistance(target).toDouble()).toLong().coerceAtLeast(1L)
        val weight = computeWeight(entity).coerceAtMost(5100)
        val energyNeed = computeEnergyNeed(weight, distance)
        if (sync.amount < energyNeed) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        startCharging(entity, target, energyNeed, distance)
        setActiveState(world, pos, state, true)
        endTick(world, pos)
    }

    private fun hasAdjacentMfeOrMfsu(world: World, pos: BlockPos): Boolean {
        for (dir in Direction.entries) {
            val id = Registries.BLOCK.getId(world.getBlockState(pos.offset(dir)).block)
            if (id.namespace == "ic2_120" && (id.path == "mfe" || id.path == "mfsu")) return true
        }
        return false
    }

    private fun computeWeight(entity: Entity): Int {
        if (entity is PlayerEntity) {
            var weight = 1000
            weight += entity.inventory.armor.count { !it.isEmpty } * 100

            var invWeight = 0.0
            for (stack in entity.inventory.main) {
                if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxCount.toDouble().coerceAtLeast(1.0))
            }
            for (stack in entity.inventory.offHand) {
                if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxCount.toDouble().coerceAtLeast(1.0))
            }
            weight += floor(invWeight).toInt()
            return weight.coerceAtMost(5100)
        }
        return when (entity) {
            is Monster -> 500
            is AnimalEntity -> 100
            else -> 500
        }
    }

    private fun computeEnergyNeed(weight: Int, distance: Long): Long {
        val x = floor(weight.toDouble()).coerceAtLeast(1.0)
        val l = floor(distance.toDouble()).coerceAtLeast(1.0)
        val y = floor(5.0 * x * (l + 10.0).pow(0.7))
        return y.toLong().coerceAtLeast(1L)
    }

    private fun startCharging(entity: Entity, target: BlockPos, energyNeed: Long, distance: Long) {
        chargingEntityUuid = entity.uuid
        chargingTargetPos = target.toImmutable()
        chargingEnergyNeed = energyNeed
        chargeTicksElapsed = 0
        chargeTicksMax = ((40 + distance / 20L) / speedMultiplier.coerceAtLeast(1f)).toInt().coerceIn(40, 120)
        sync.charging = 1
        sync.chargeProgress = 0
        sync.chargeMax = chargeTicksMax
        markDirty()
    }

    private fun tickCharging(world: ServerWorld?, pos: BlockPos, state: BlockState, targetBe: TeleporterBlockEntity): Boolean {
        val serverWorld = world ?: run {
            clearCharging()
            return false
        }
        val entityUuid = chargingEntityUuid ?: run {
            clearCharging()
            return false
        }
        val target = chargingTargetPos ?: run {
            clearCharging()
            return false
        }
        val entity = serverWorld.getEntity(entityUuid)
        if (entity == null || !entity.isAlive) {
            clearCharging()
            return false
        }

        val sourceBox = getActivationBox(pos)
        if (!sourceBox.intersects(entity.boundingBox)) {
            clearCharging()
            return false
        }

        chargeTicksElapsed++
        sync.charging = 1
        sync.chargeProgress = chargeTicksElapsed
        sync.chargeMax = chargeTicksMax

        if (chargeTicksElapsed < chargeTicksMax) {
            if (chargeTicksElapsed % 20 == 0) {
                serverWorld.playSound(
                    null,
                    pos,
                    TELEPORTER_CHARGE_SOUND,
                    SoundCategory.BLOCKS,
                    0.8f,
                    1.0f
                )
            }
            markDirty()
            return true
        }

        val destX = target.x + 0.5
        val destY = target.y + 1.02
        val destZ = target.z + 0.5

        if (sync.amount < chargingEnergyNeed || !canTeleportTo(serverWorld, entity, destX, destY, destZ)) {
            clearCharging()
            return false
        }

        if (sync.consumeEnergy(chargingEnergyNeed) > 0L) {
            sync.energy = sync.amount.toInt().coerceAtLeast(0)
            teleportEntity(entity, destX, destY, destZ)
            serverWorld.playSound(null, pos, TELEPORTER_USE_SOUND, SoundCategory.BLOCKS, 1.0f, 1.0f)

            val cooldownTicks = 20
            teleportCooldown = cooldownTicks
            targetBe.teleportCooldown = cooldownTicks
            sync.cooldown = cooldownTicks
            targetBe.sync.cooldown = cooldownTicks
            clearCharging()
            markDirty()
            targetBe.markDirty()
            setActiveState(serverWorld, pos, state, true)
            return true
        }

        clearCharging()
        return false
    }

    private fun isCharging(): Boolean = chargingEntityUuid != null && chargingTargetPos != null && chargeTicksMax > 0

    private fun clearCharging() {
        chargingEntityUuid = null
        chargingTargetPos = null
        chargingEnergyNeed = 0L
        chargeTicksElapsed = 0
        chargeTicksMax = 0
        sync.charging = 0
        sync.chargeProgress = 0
        sync.chargeMax = 0
    }

    private fun endTick(world: World, pos: BlockPos) {
        sync.syncCurrentTickFlow()
        ic2_120.content.network.NetworkManager.sendTeleporterVisualStateToNearby(world, pos, this)
    }

    private fun getActivationBox(pos: BlockPos): Box {
        val side = teleportRange.toDouble().coerceIn(TELEPORT_RANGE_MIN.toDouble(), TELEPORT_RANGE_MAX.toDouble())
        val half = side / 2.0
        val centerX = pos.x + 0.5
        val centerY = pos.y + 1.5
        val centerZ = pos.z + 0.5
        return Box(
            centerX - half, centerY - half, centerZ - half,
            centerX + half, centerY + half, centerZ + half
        )
    }

    fun setTeleportRange(newRange: Int) {
        val clamped = normalizeRange(newRange)
        if (teleportRange == clamped) return
        teleportRange = clamped
        sync.teleportRange = clamped
        markDirty()
    }

    fun getTeleportRange(): Int = teleportRange

    fun getChargingEntityId(): Int {
        val serverWorld = world as? ServerWorld ?: return -1
        val uuid = chargingEntityUuid ?: return -1
        return serverWorld.getEntity(uuid)?.id ?: -1
    }

    fun applyClientVisualState(charging: Boolean, progress: Int, max: Int, range: Int, entityId: Int) {
        clientCharging = charging
        clientChargeProgress = progress.coerceAtLeast(0)
        clientChargeMax = max.coerceAtLeast(0)
        clientTeleportRange = normalizeRange(range)
        clientChargingEntityId = entityId
    }

    private fun canTeleportTo(world: World, entity: Entity, x: Double, y: Double, z: Double): Boolean {
        val offset = entity.boundingBox.offset(x - entity.x, y - entity.y, z - entity.z)
        return world.isSpaceEmpty(entity, offset)
    }

    private fun teleportEntity(entity: Entity, x: Double, y: Double, z: Double) {
        entity.requestTeleport(x, y, z)
        entity.velocity = Vec3d.ZERO
        entity.velocityModified = true
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return
        val request = minOf(space, TransformerUpgradeComponent.maxInsertForTier(TELEPORTER_TIER + voltageTierBonus))
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
}
