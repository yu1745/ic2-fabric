package ic2_120.content.block.machines

import net.minecraft.core.registries.BuiltInRegistries

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time
import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world
import net.minecraft.core.HolderLookup
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.TeleporterBlock
import kotlin.math.ceil
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import team.reborn.energy.api.EnergyStorage
import ic2_120.content.screen.TeleporterScreenHandler
import ic2_120.content.sync.TeleporterSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
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
    Container,
    IRoutedSidedInventory,
    ITieredMachine,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    IRedstoneInverterUpgradeSupport, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = TeleporterBlock.ACTIVE
    override fun getInventory(): net.minecraft.world.Container = this

    override val tier: Int = TELEPORTER_TIER
    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var redstoneInverted: Boolean = false

    companion object {
        const val TELEPORTER_TIER = 4
        private val TELEPORTER_CHARGE_SOUND: SoundEvent = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ic2", "machine.teleporter.charge"))
        private val TELEPORTER_USE_SOUND: SoundEvent = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ic2", "machine.teleporter.use"))

        const val SLOT_UPGRADE_0 = 0
        const val SLOT_UPGRADE_1 = 1
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1)
        const val INVENTORY_SIZE = 2

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

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(*SLOT_UPGRADE_INDICES),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

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

    constructor(pos: BlockPos, state: BlockState) : this(
        TeleporterBlockEntity::class.type(),
        pos,
        state
    )

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun setChanged() = super.setChanged()

    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    fun setTarget(pos: BlockPos, dimension: String) {
        targetPos = pos.immutable()
        targetDimension = dimension
        sync.targetSet = 1
        sync.targetX = pos.x
        sync.targetY = pos.y
        sync.targetZ = pos.z
        setChanged()
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.size)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.teleporter")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): net.minecraft.world.inventory.AbstractContainerMenu =
        TeleporterScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
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
        chargingEntityUuid = if (nbt.contains(NBT_CHARGING_ENTITY)) nbt.getUUID(NBT_CHARGING_ENTITY) else null
        chargingTargetPos = if (nbt.contains(NBT_CHARGING_TARGET_X)) {
            BlockPos(nbt.getInt(NBT_CHARGING_TARGET_X), nbt.getInt(NBT_CHARGING_TARGET_Y), nbt.getInt(NBT_CHARGING_TARGET_Z))
        } else {
            null
        }
        chargingEnergyNeed = nbt.getLong(NBT_CHARGING_ENERGY).coerceAtLeast(0L)
        chargeTicksElapsed = nbt.getInt(NBT_CHARGE_ELAPSED).coerceAtLeast(0)
        chargeTicksMax = nbt.getInt(NBT_CHARGE_MAX).coerceAtLeast(0)
        teleportRange = normalizeRange(nbt.getInt(NBT_TELEPORT_RANGE))

        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
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

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
        syncedData.writeNbt(nbt)

        nbt.putBoolean("RedstoneInverted", redstoneInverted)
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

        chargingEntityUuid?.let { nbt.putUUID(NBT_CHARGING_ENTITY, it) }
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

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.teleportRange = teleportRange

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)

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

        val thisDim = world.dimension().value.toString()
        if (thisDim != targetDimension || !world.isChunkLoaded(target.x shr 4, target.z shr 4)) {
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
            val charging = tickCharging(world as? ServerLevel, pos, state, targetBe)
            setActiveState(world, pos, state, charging)
            endTick(world, pos)
            return
        }

        val redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)
        if (!redstoneAllowsRun) {
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val entity = world.getEntities(null, getActivationBox(pos)) {
            it.isAlive && !it.isPassenger() && !it.hasPassengers()
        }
            .minByOrNull { it.distanceToSqr(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) }

        if (entity == null) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        val distance = sqrt(pos.getSquaredDistance(target)).toLong().coerceAtLeast(1L)
        val weight = computeWeight(entity).coerceAtMost(5100)
        // 传送耗能按超频 1.6^n 计（向上取整，整数 EU）
        val energyNeed = ceil(computeEnergyNeed(weight, distance) * energyMultiplier).toLong().coerceAtLeast(1L)
        val availableMfe = simulateAdjacentMfeEnergy(world, pos)
        if (availableMfe < energyNeed) {
            clearCharging()
            setActiveState(world, pos, state, false)
            endTick(world, pos)
            return
        }

        startCharging(entity, target, energyNeed, distance)
        setActiveState(world, pos, state, true)
        endTick(world, pos)
    }

    fun hasAdjacentMfeOrMfsu(world: Level, pos: BlockPos): Boolean {
        for (dir in Direction.entries) {
            val id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos.offset(dir)).block) ?: continue
            if (id.namespace == "ic2_120" && (id.path == "mfe" || id.path == "mfsu" || id.path == "mfe_chargepad" || id.path == "mfsu_chargepad")) return true
        }
        return false
    }

    private fun computeWeight(entity: Entity): Int {
        if (entity is Player) {
            var weight = 1000
            weight += entity.inventory.armor.count { !it.isEmpty } * 100

            var invWeight = 0.0
            for (stack in entity.inventory.items) {
                if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxStackSize.toDouble().coerceAtLeast(1.0))
            }
            for (stack in entity.inventory.offHand) {
                if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxStackSize.toDouble().coerceAtLeast(1.0))
            }
            weight += floor(invWeight).toInt()
            return weight.coerceAtMost(5100)
        }
        return when (entity) {
            is Enemy -> 500
            is Animal -> 100
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
        chargingTargetPos = target.immutable()
        chargingEnergyNeed = energyNeed
        chargeTicksElapsed = 0
        // 充能时间：浮点除法 + 向上取整（原 (40 + distance/20L) 整数除法 + toInt 截断会漏掉超频效果）；
        // [40,120] 下限为设计：超频再快，单次充能也不低于 40 tick（指南书已声明）。
        chargeTicksMax = ceil((40 + distance / 20.0) / speedMultiplier.coerceAtLeast(1f)).toInt().coerceIn(40, 120)
        sync.charging = 1
        sync.chargeProgress = 0
        sync.chargeMax = chargeTicksMax
        world?.playSound(null, pos, TELEPORTER_CHARGE_SOUND, SoundSource.BLOCKS, 0.8f, 1.0f)
        setChanged()
    }

    private fun tickCharging(world: ServerLevel?, pos: BlockPos, state: BlockState, targetBe: TeleporterBlockEntity): Boolean {
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
                    SoundSource.BLOCKS,
                    0.8f,
                    1.0f
                )
            }
            setChanged()
            return true
        }

        val destX = target.x + 0.5
        val destY = target.y + 1.02
        val destZ = target.z + 0.5

        if (!canTeleportTo(serverWorld, entity, destX, destY, destZ)) {
            clearCharging()
            return false
        }

        var success = false
        Transaction.openOuter().use { tx ->
            val extracted = extractFromAdjacentMfe(serverWorld, pos, chargingEnergyNeed, tx)
            if (extracted >= chargingEnergyNeed) {
                tx.commit()
                success = true
            }
        }

        if (success) {
            teleportEntity(entity, destX, destY, destZ)
            serverWorld.playSound(null, pos, TELEPORTER_USE_SOUND, SoundSource.BLOCKS, 1.0f, 1.0f)

            val cooldownTicks = 20
            teleportCooldown = cooldownTicks
            targetBe.teleportCooldown = cooldownTicks
            sync.cooldown = cooldownTicks
            targetBe.sync.cooldown = cooldownTicks
            clearCharging()
            setChanged()
            targetBe.setChanged()
            setActiveState(serverWorld, pos, state, true)
            return true
        }

        clearCharging()
        return false
    }

    fun isCharging(): Boolean = chargingEntityUuid != null && chargingTargetPos != null && chargeTicksMax > 0
    fun getTeleportCooldown(): Int = teleportCooldown

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

    private fun endTick(world: Level, pos: BlockPos) {
        sync.syncCurrentTickFlow()
        ic2_120.content.network.NetworkManager.sendTeleporterVisualStateToNearby(world, pos, this)
    }

    fun getActivationBox(pos: BlockPos): AABB {
        val side = teleportRange.toDouble().coerceIn(TELEPORT_RANGE_MIN.toDouble(), TELEPORT_RANGE_MAX.toDouble())
        val half = side / 2.0
        val centerX = pos.x + 0.5
        val centerY = pos.y + 1.5
        val centerZ = pos.z + 0.5
        return AABB(
            centerX - half, centerY - half, centerZ - half,
            centerX + half, centerY + half, centerZ + half
        )
    }

    fun setTeleportRange(newRange: Int) {
        val clamped = normalizeRange(newRange)
        if (teleportRange == clamped) return
        teleportRange = clamped
        sync.teleportRange = clamped
        setChanged()
    }

    fun getTeleportRange(): Int = teleportRange

    fun getChargingEntityId(): Int {
        val serverWorld = world as? ServerLevel ?: return -1
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

    private fun canTeleportTo(world: Level, entity: Entity, x: Double, y: Double, z: Double): Boolean {
        val offset = entity.boundingBox.offset(x - entity.x, y - entity.y, z - entity.z)
        return world.noCollision(entity, offset)
    }

    private fun extractFromAdjacentMfe(world: Level, pos: BlockPos, amount: Long, tx: Transaction): Long {
        var remaining = amount
        for (dir in Direction.entries) {
            if (remaining <= 0) break
            val neighborPos = pos.offset(dir)
            val id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(neighborPos).block) ?: continue
            if (id.namespace != "ic2_120" ||
                (id.path != "mfe" && id.path != "mfsu" && id.path != "mfe_chargepad" && id.path != "mfsu_chargepad")) continue
            // 绕过单 tick 输出上限，直接修改储电盒内部 amount
            val be = world.getBlockEntity(neighborPos)
            if (be is ic2_120.content.block.storage.EnergyStorageBlockEntity) {
                val container = be.sync
                val available = container.amount
                val toExtract = minOf(remaining, available).coerceAtLeast(0L)
                if (toExtract > 0L) {
                    container.amount = available - toExtract
                    container.syncCommittedAmount()
                    remaining -= toExtract
                }
            }
        }
        return amount - remaining
    }

    fun simulateAdjacentMfeEnergy(world: Level, pos: BlockPos): Long {
        var total = 0L
        for (dir in Direction.entries) {
            val neighborPos = pos.offset(dir)
            val id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(neighborPos).block) ?: continue
            if (id.namespace != "ic2_120" ||
                (id.path != "mfe" && id.path != "mfsu" && id.path != "mfe_chargepad" && id.path != "mfsu_chargepad")) continue
            val be = world.getBlockEntity(neighborPos)
            if (be is ic2_120.content.block.storage.EnergyStorageBlockEntity) {
                total += be.sync.amount
            }
        }
        return total
    }

    private fun teleportEntity(entity: Entity, x: Double, y: Double, z: Double) {
        entity.teleportTo(x, y, z)
        entity.velocity = Vec3.ZERO
        entity.velocityModified = true
    }

}
