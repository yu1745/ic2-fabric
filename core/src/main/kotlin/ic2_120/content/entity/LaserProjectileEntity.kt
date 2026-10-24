package ic2_120.content.entity

import ic2_120.config.Ic2Config
import ic2_120.integration.ftbchunks.ClaimProtection
import ic2_120.registry.annotation.ModEntity
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.tags.BlockTags
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level

/**
 * 采矿镭射枪弹射实体。
 * 以速度 v 沿发射方向飞行，命中方块/实体后根据模式执行不同行为。
 */
@ModEntity(name = "laser_projectile", width = 0.1f, height = 0.1f, maxTrackingRange = 256, trackingTickInterval = 1, dataFixerType = "minecraft:arrow")
class LaserProjectileEntity(
    entityType: EntityType<out LaserProjectileEntity>,
    world: Level
) : Projectile(entityType, world) {

    companion object {
        /** 视觉长度 (blocks)，渲染时使用 */
        val VISUAL_LENGTH: Double get() = Ic2Config.current.miningLaser.visualLength
        /** 最大存活 tick 数 */
        private val MAX_LIFE: Int get() = Ic2Config.current.miningLaser.maxLife

        private val LASER_HIT_SOUND: SoundEvent = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ic2", "item.laser.hit"))

        private val MODE: EntityDataAccessor<String> = EntityDataAccessor.defineId(
            LaserProjectileEntity::class.java, EntityDataSerializers.STRING
        )

        /** 不会被镭射破坏的方块硬度阈值（黑曜石=50，防爆石等更高） */
        private const val MAX_HARDNESS = 40.0f

        /** 方块被移除后，尝试在该格生成火的概率（与旧低聚焦「点燃」概率一致） */
        private const val POST_BREAK_IGNITE_CHANCE = 0.3f
    }

    /** 当前模式 */
    var mode: LaserMode
        get() = try { LaserMode.valueOf(entityData.get(MODE)) } catch (_: Exception) { LaserMode.DEFAULT }
        set(value) = entityData.set(MODE, value.name)

    /** 弹体颜色 (ARGB)，从模式获取 */
    val color: Int get() = mode.color

    /** 剩余射程 */
    private var remainingRange: Double = 0.0
    /** 已飞行 tick */
    private var lifeTicks: Int = 0

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(MODE, LaserMode.DEFAULT.name)
    }

    /**
     * 从发射者位置和视角方向初始化弹射，指定模式。
     */
    fun init(owner: Entity, pitch: Float, yaw: Float, laserMode: LaserMode, spreadOffset: Vec3 = Vec3.ZERO) {
        setOwner(owner)
        mode = laserMode
        remainingRange = laserMode.range

        val offsetX = -Math.sin(yaw.toDouble() * 0.017453292) * 0.1
        val offsetY = 0.1
        val offsetZ = Math.cos(yaw.toDouble() * 0.017453292) * 0.1
        setPos(
            owner.x + offsetX + spreadOffset.x,
            owner.eyeY - offsetY + spreadOffset.y,
            owner.z + offsetZ + spreadOffset.z
        )

        val radYaw = yaw.toDouble() * 0.017453292
        val radPitch = pitch.toDouble() * 0.017453292
        val velX = -Math.sin(radYaw) * Math.cos(radPitch)
        val velY = -Math.sin(radPitch)
        val velZ = Math.cos(radYaw) * Math.cos(radPitch)
        val dir = Vec3(velX, velY, velZ).normalize()
        setDeltaMovement(dir.scale(laserMode.speed))
    }

    override fun tick() {
        super.tick()

        if (!level().isClientSide) {
            lifeTicks++
            if (lifeTicks > MAX_LIFE || remainingRange <= 0) {
                discard()
                return
            }
        }

        // 方块碰撞检测 - 使用 ClipContext
        val currentPos = position
        val nextPos = currentPos.add(deltaMovement)
        val raycastContext = ClipContext(
            currentPos,
            nextPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this
        )
        val blockHit = level().clip(raycastContext)
        if (blockHit.type == HitResult.Type.BLOCK) {
            setPos(blockHit.location)
            onHitBlock(blockHit)
            return
        }

        // 实体碰撞：沿飞行路径进行射线碰撞检测
        // 纯采矿类模式（无伤害、无爆炸）跳过实体碰撞，避免先击中实体导致后方方块无法被挖
        if (mode.entityDamage > 0f || mode.explosionPower > 0f) {
            val entityHit = ProjectileUtil.getEntityHitResult(
                world,
                this,
                currentPos,
                nextPos,
                getBoundingBox().expandTowards(deltaMovement.x, deltaMovement.y, deltaMovement.z).inflate(1.0),
                { entity -> entity is LivingEntity && entity.isAlive && entity.canHit() && !entity.isSpectator && entity != owner }
            )
            if (entityHit != null) {
                setPos(entityHit.location)
                onHitEntity(entityHit)
                return
            }
        }

        // 移动（直接设置位置，不走碰撞物理）
        val dist = deltaMovement.length()
        remainingRange -= dist
        setPos(x + deltaMovement.x, y + deltaMovement.y, z + deltaMovement.z)

        // 粒子拖尾（客户端）
        if (level().isClientSide && lifeTicks % 2 == 0) {
            val col = color
            val r = ((col shr 16) and 0xFF) / 255f
            val g = ((col shr 8) and 0xFF) / 255f
            val b = (col and 0xFF) / 255f
            level().addParticle(
                net.minecraft.core.particles.DustParticleOptions(
                    org.joml.Vector3f(r, g, b), 0.4f
                ),
                x, y, z, 0.0, 0.0, 0.0
            )
        }
    }

    override fun onHitBlock(hitResult: BlockHitResult) {
        if (level().isClientSide) return
        val pos = hitResult.blockPos
        val state = level().getBlockState(pos)
        val currentMode = mode

        // 爆破模式：与原版 TNT 一致传入「爆炸源实体」与命中点坐标（见 TntEntity.explode）
        if (currentMode.explosionPower > 0) {
            if (!explosionAllowedAt(BlockPos.containing(x, y, z), currentMode.explosionPower)) {
                discard()
                return
            }
            level().explode(
                this,
                x, y, z,
                currentMode.explosionPower,
                Level.ExplosionInteraction.TNT
            )
            tryRandomIgniteAfterBreak(pos)
            discard()
            return
        }

        // 超级热线模式：烧制方块
        if (currentMode == LaserMode.SUPER_HEAT) {
            smeltBlock(pos, state)
            discard()
            return
        }

        // 低聚焦模式：易燃物先破坏再在空格上随机点火（不可先放火再破坏，否则火随方块一起没了）
        val isFlammable = state.`is`(BlockTags.LEAVES) || state.`is`(BlockTags.PLANKS) || state.`is`(BlockTags.WOOL)
        if (currentMode == LaserMode.LOW_FOCUS && isFlammable) {
            if (canBreak(state) && !ClaimProtection.isProtected(world, pos, owner, ClaimProtection.EDIT_BLOCK)) {
                level().destroyBlock(pos, true, owner)
                tryRandomIgniteAfterBreak(pos)
            }
            level().playSound(null, pos, LASER_HIT_SOUND, SoundSource.PLAYERS, 0.8f, 1.2f)
            discard()
            return
        }

        // 采矿模式：按方块硬度消耗射程，可连续击穿多个方块（与 LaserMode 描述一致）
        if (currentMode == LaserMode.MINING && canBreak(state)) {
            if (ClaimProtection.isProtected(world, pos, owner, ClaimProtection.EDIT_BLOCK)) {
                discard()
                return
            }
            val hardness = state.block.defaultDestroyTime().coerceAtLeast(0.1f)
            remainingRange -= hardness.toDouble()
            level().destroyBlock(pos, true, owner)
            level().playSound(null, pos, LASER_HIT_SOUND, SoundSource.PLAYERS, 0.8f, 1.2f)
            if (remainingRange > 0) {
                nudgePastBlock(hitResult)
                return
            }
            discard()
            return
        }

        // 其他模式：破坏方块
        if (canBreak(state) && !ClaimProtection.isProtected(world, pos, owner, ClaimProtection.EDIT_BLOCK)) {
            level().destroyBlock(pos, true, owner)
            level().playSound(null, pos, LASER_HIT_SOUND, SoundSource.PLAYERS, 0.8f, 1.2f)
        }

        discard()
    }

    /** 方块已移除后，在原位按概率尝试生成火（需满足原版放火条件）。 */
    private fun tryRandomIgniteAfterBreak(pos: BlockPos) {
        if (level().random.nextFloat() >= POST_BREAK_IGNITE_CHANCE) return
        if (!level().isInWorldBounds(pos)) return
        if (ClaimProtection.isProtected(world, pos, owner, ClaimProtection.EDIT_BLOCK)) return
        if (!BaseFireBlock.canBePlacedAt(world, pos, Direction.UP)) return
        if (!level().getBlockState(pos).canBeReplaced()) return
        level().setBlock(pos, Blocks.FIRE.defaultBlockState())
    }

    /** 击穿方块后沿飞行方向略微前移，避免同一 tick 再次命中同一格 */
    private fun nudgePastBlock(hitResult: BlockHitResult) {
        val dir = deltaMovement.normalize()
        val nudge = 0.05
        setPos(x + dir.x * nudge, y + dir.y * nudge, z + dir.z * nudge)
    }

    override fun onHitEntity(hitResult: EntityHitResult) {
        if (level().isClientSide) return
        val currentMode = mode

        if (currentMode.entityDamage > 0) {
            val target = hitResult.entity
            if (target is LivingEntity) {
                // 穿甲效果：爆炸模式使用爆炸伤害源，触发量子/纳米护甲减伤逻辑
                val damageSource = if (currentMode == LaserMode.EXPLOSIVE) {
                    level().damageSources().explosion(this, null)
                } else {
                    level().damageSources().generic()
                }
                target.hurt(damageSource, currentMode.entityDamage)
            }
        }

        // 爆破模式在命中实体时也会爆炸（中心在弹体命中点，与方块命中一致）
        if (currentMode.explosionPower > 0) {
            if (!explosionAllowedAt(BlockPos.containing(x, y, z), currentMode.explosionPower)) {
                discard()
                return
            }
            level().explode(
                this,
                x, y, z,
                currentMode.explosionPower,
                Level.ExplosionInteraction.TNT
            )
            tryRandomIgniteAfterBreak(BlockPos.containing(x, y, z))
        }

        discard()
    }

    /**
     * 超级热线模式：查找方块的熔炼配方，将方块替换为熔炼产物。
     * 对原木无效。
     */
    private fun smeltBlock(pos: net.minecraft.core.BlockPos, state: BlockState) {
        if (state.`is`(BlockTags.LOGS) || state.`is`(BlockTags.LOGS_THAT_BURN)) return
        if (!canBreak(state)) return
        if (ClaimProtection.isProtected(world, pos, owner, ClaimProtection.EDIT_BLOCK)) return

        val serverWorld = world as? ServerLevel ?: return
        val blockItem = state.block.asItem()
        if (blockItem == net.minecraft.level().item.Items.AIR) return

        val inputStack = ItemStack(blockItem)
        val inventory = SimpleContainer(inputStack)
        val recipeHolder = serverWorld.recipeManager.getRecipeFor(RecipeType.SMELTING, inventory, serverWorld).orElse(null)
        val recipe = recipeHolder?.value

        if (recipe != null) {
            val output = recipe.assemble(inventory, serverWorld.registryAccess())
            if (!output.isEmpty) {
                level().destroyBlock(pos, false)
                net.minecraft.level().level.block.Block.spawnAtLocation(serverWorld, pos, output.copy())
                tryRandomIgniteAfterBreak(pos)
                level().playSound(null, pos, LASER_HIT_SOUND, SoundSource.PLAYERS, 0.8f, 1.2f)
                return
            }
        }

        // 没有熔炼配方的方块直接破坏
        level().destroyBlock(pos, true, owner)
        tryRandomIgniteAfterBreak(pos)
        level().playSound(null, pos, LASER_HIT_SOUND, SoundSource.PLAYERS, 0.8f, 1.2f)
    }

    private fun explosionAllowedAt(center: BlockPos, power: Float): Boolean {
        val radius = kotlin.math.ceil(power.toDouble()).toInt().coerceAtLeast(1)
        val positions = BlockPos.iterate(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        ).map { it.immutable() }.toList()
        return ClaimProtection.allExplosionAllowed(world, positions, owner?.uuid)
    }

    private fun canBreak(state: BlockState): Boolean {
        if (state.isAir) return false
        if (state.`is`(BlockTags.WITHER_IMMUNE) || state.`is`(BlockTags.DRAGON_IMMUNE)) return false
        // 黑曜石、防爆石、核反应堆压力容器等高硬度方块
        if (state.block.defaultDestroyTime() >= MAX_HARDNESS) return false
        return state.block.defaultDestroyTime() >= 0
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        remainingRange = nbt.getDouble("RemainingRange")
        lifeTicks = nbt.getInt("LifeTicks")
        try {
            mode = LaserMode.valueOf(nbt.getString("Mode"))
        } catch (_: Exception) {
            mode = LaserMode.DEFAULT
        }
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        nbt.putDouble("RemainingRange", remainingRange)
        nbt.putInt("LifeTicks", lifeTicks)
        nbt.putString("Mode", mode.name)
    }

    override fun canBeCollidedWith(): Boolean = false
    override fun canUsePortal(allowPassengers: Boolean): Boolean = false

    /**
     * 覆盖渲染距离检查。
     *
     * 默认实现会将 render distance 乘以 bounding box 平均边长（~0.1），导致
     * 射弹在约 10 格外就不可见。这里直接使用 256 格作为裁减距离。
     */
    override fun shouldRender(cameraX: Double, cameraY: Double, cameraZ: Double): Boolean {
        // Entity.shouldRender(double,double,double) — cameraX/Y/Z 是相机坐标
        val dx = x - cameraX
        val dy = y - cameraY
        val dz = z - cameraZ
        val distSq = dx * dx + dy * dy + dz * dz
        // 碰撞箱 0.1×0.1 导致默认渲染距离 ~10 格，覆盖为匹配配置的渲染距离
        val maxDist = Ic2Config.current.miningLaser.renderDistance
        return distSq < maxDist * maxDist
    }
}
