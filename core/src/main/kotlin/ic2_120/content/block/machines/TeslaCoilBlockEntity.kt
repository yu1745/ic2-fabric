package ic2_120.content.block.machines

import ic2_120.content.block.TeslaCoilBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.sync.TeslaCoilSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.item.ArmorItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level

/**
 * 特斯拉线圈方块实体。无物品槽，仅能量存储。
 * 红石激活时，每秒对 9 格范围内生物造成伤害。
 * 新目标 24 点伤害，重复目标 10~11 点；盔甲可抵挡部分伤害，每次攻击消耗盔甲 4~6 耐久。
 */
@ModBlockEntity(block = TeslaCoilBlock::class)
class TeslaCoilBlockEntity(
    type: net.minecraft.world.level.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    override val tier: Int = 2

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = TeslaCoilSync(syncedData) { world?.time }

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    /** 距离下次放电的 tick 计数 */
    private var shotCooldown = 0

    /** 上一次攻击的目标 UUID，用于伤害衰减 */
    private var lastHitTargetUuid: java.util.UUID? = null

    constructor(pos: BlockPos, state: BlockState) : this(
        TeslaCoilBlockEntity::class.type(),
        pos,
        state
    )

    override fun readNbt(nbt: CompoundTag) {
        super.readNbt(nbt)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(TeslaCoilSync.NBT_ENERGY_STORED).coerceIn(0L, TeslaCoilSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        shotCooldown = nbt.getInt("ShotCooldown")
        lastHitTargetUuid = if (nbt.contains("LastHitTarget")) nbt.getUuid("LastHitTarget") else null
    }

    override fun writeNbt(nbt: CompoundTag) {
        super.writeNbt(nbt)
        syncedData.writeNbt(nbt)
        nbt.putLong(TeslaCoilSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("ShotCooldown", shotCooldown)
        if (lastHitTargetUuid != null) nbt.putUuid("LastHitTarget", lastHitTargetUuid!!)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        adjacentEnergyTransfer.tick()

        val powered = state.get(BlockStateProperties.POWERED)
        if (!powered) {
            shotCooldown = 0
            lastHitTargetUuid = null
            if (state.get(TeslaCoilBlock.ACTIVE)) {
                world.setBlockState(pos, state.with(TeslaCoilBlock.ACTIVE, false))
            }
            sync.syncCurrentTickFlow()
            return
        }

        if (shotCooldown > 0) {
            shotCooldown--
            if (state.get(TeslaCoilBlock.ACTIVE)) {
                world.setBlockState(pos, state.with(TeslaCoilBlock.ACTIVE, false))
            }
            markDirty()
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.amount < TeslaCoilSync.MIN_ENERGY_TO_OPERATE) {
            if (state.get(TeslaCoilBlock.ACTIVE)) {
                world.setBlockState(pos, state.with(TeslaCoilBlock.ACTIVE, false))
            }
            sync.syncCurrentTickFlow()
            return
        }

        val serverWorld = world as? ServerLevel ?: run {
            sync.syncCurrentTickFlow()
            return
        }
        val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        val range = 9.0
        val box = AABB(
            center.x - range, center.y - range, center.z - range,
            center.x + range, center.y + range, center.z + range
        )

        val targets = world.getEntitiesByClass(
            LivingEntity::class.java,
            box
        ) { e -> e.isAlive && !e.isSpectator }

        val target = targets.minByOrNull { it.squaredDistanceTo(center) }
        if (target != null) {
            sync.consumeEnergy(TeslaCoilSync.ENERGY_PER_SHOT)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

            val isRepeatTarget = lastHitTargetUuid == target.uuid
            lastHitTargetUuid = target.uuid

            val damageAmount = if (isRepeatTarget) 10.5f else 24f
            val damageSource = createTeslaDamageSource(serverWorld)

            target.damage(damageSource, damageAmount)
            damageArmorDurability(world, target, 4, 6)

            val lightning = EntityType.LIGHTNING_BOLT.create(serverWorld)
            if (lightning != null) {
                lightning.refreshPositionAndAngles(target.x, target.y, target.z, 0f, 0f)
                serverWorld.spawnEntity(lightning)
            }

            shotCooldown = TeslaCoilSync.SHOT_INTERVAL
            world.setBlockState(pos, state.with(TeslaCoilBlock.ACTIVE, true))
        } else {
            lastHitTargetUuid = null
            if (state.get(TeslaCoilBlock.ACTIVE)) {
                world.setBlockState(pos, state.with(TeslaCoilBlock.ACTIVE, false))
            }
        }
        markDirty()
        sync.syncCurrentTickFlow()
    }

    private fun createTeslaDamageSource(world: ServerLevel): DamageSource {
        val registry = world.registryManager.get(Registries.DAMAGE_TYPE)
        val key = ResourceKey.of(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("ic2_120", "tesla_coil"))
        val entry = registry.getEntry(key).orElseThrow { IllegalStateException("Tesla coil damage type not registered") }
        return DamageSource(entry)
    }

    private fun damageArmorDurability(world: Level, entity: LivingEntity, min: Int, max: Int) {
        val amount = if (min >= max) min else world.random.nextBetween(min, max)
        for (slot in EquipmentSlot.entries) {
            if (slot.type != EquipmentSlot.Type.ARMOR) continue
            val stack = entity.getEquippedStack(slot)
            if (!stack.isEmpty && stack.item is ArmorItem) {
                stack.damage(amount, entity) { it.sendEquipmentBreakStatus(slot) }
            }
        }
    }
}




