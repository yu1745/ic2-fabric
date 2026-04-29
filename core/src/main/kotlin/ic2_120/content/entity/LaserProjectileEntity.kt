package ic2_120.content.entity

import net.minecraft.block.AbstractFireBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ItemStack
import net.minecraft.recipe.input.SingleStackRecipeInput
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World

/**
 * 采矿镭射枪弹射实体。
 * 以速度 v 沿发射方向飞行，命中方块/实体后根据模式执行不同行为。
 */
class LaserProjectileEntity(
    entityType: EntityType<out LaserProjectileEntity>,
    world: World
) : ProjectileEntity(entityType, world) {

    companion object {
        /** 视觉长度 (blocks)，渲染时使用 */
        const val VISUAL_LENGTH = 1.2
        /** 最大存活 tick 数 */
        private const val MAX_LIFE = 200

        private val LASER_HIT_SOUND: SoundEvent = SoundEvent.of(Identifier.of("ic2", "item.laser.hit"))

        private val MODE: TrackedData<String> = DataTracker.registerData(
            LaserProjectileEntity::class.java, TrackedDataHandlerRegistry.STRING
        )

        /** 不会被镭射破坏的方块硬度阈值（黑曜石=50，防爆石等更高） */
        private const val MAX_HARDNESS = 40.0f

        /** 方块被移除后，尝试在该格生成火的概率（与旧低聚焦「点燃」概率一致） */
        private const val POST_BREAK_IGNITE_CHANCE = 0.3f
    }

    /** 当前模式 */
    var mode: LaserMode
        get() = try { LaserMode.valueOf(dataTracker.get(MODE)) } catch (_: Exception) { LaserMode.DEFAULT }
        set(value) = dataTracker.set(MODE, value.name)

    /** 弹体颜色 (ARGB)，从模式获取 */
    val color: Int get() = mode.color

    /** 剩余射程 */
    private var remainingRange: Double = 0.0
    /** 已飞行 tick */
    private var lifeTicks: Int = 0

    override fun initDataTracker(builder: DataTracker.Builder) {
        builder.add(MODE, LaserMode.DEFAULT.name)
    }

    /**
     * 从发射者位置和视角方向初始化弹射，指定模式。
     */
    fun init(owner: Entity, pitch: Float, yaw: Float, laserMode: LaserMode) {
        setOwner(owner)
        mode = laserMode
        remainingRange = laserMode.range

        val offsetX = -Math.sin(yaw.toDouble() * 0.017453292) * 0.1
        val offsetY = 0.1
        val offsetZ = Math.cos(yaw.toDouble() * 0.017453292) * 0.1
        setPosition(owner.x + offsetX, owner.eyeY - offsetY, owner.z + offsetZ)

        val radYaw = yaw.toDouble() * 0.017453292
        val radPitch = pitch.toDouble() * 0.017453292
        val velX = -Math.sin(radYaw) * Math.cos(radPitch)
        val velY = -Math.sin(radPitch)
        val velZ = Math.cos(radYaw) * Math.cos(radPitch)
        val dir = Vec3d(velX, velY, velZ).normalize()
        this.velocity = dir.multiply(laserMode.speed)
        setVelocity(this.velocity)
    }

    override fun tick() {
        super.tick()

        if (!world.isClient) {
            lifeTicks++
            if (lifeTicks > MAX_LIFE || remainingRange <= 0) {
                discard()
                return
            }
        }

        // 方块碰撞检测 - 使用 RaycastContext
        val currentPos = pos
        val nextPos = currentPos.add(velocity)
        val raycastContext = RaycastContext(
            currentPos,
            nextPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            this
        )
        val blockHit = world.raycast(raycastContext)
        if (blockHit != null && blockHit.type == HitResult.Type.BLOCK) {
            setPosition(blockHit.pos)
            onBlockHit(blockHit)
            return
        }

        // 实体碰撞：使用 AABB 碰撞检测飞行路径上的实体
        // 纯采矿类模式（无伤害、无爆炸）跳过实体碰撞，避免先击中实体导致后方方块无法被挖
        if (mode.entityDamage > 0f || mode.explosionPower > 0f) {
            val expand = velocity.length() + 0.5
            val searchBox = boundingBox.expand(expand, expand, expand)
            val entities = world.getEntitiesByClass(
                LivingEntity::class.java,
                searchBox
            ) { entity: Entity ->
                entity.isAlive && entity.canHit() && !entity.isSpectator && entity != owner
            }
            if (entities.isNotEmpty()) {
                val target = entities.minByOrNull { it.pos.distanceTo(pos) }!!
                val hitPos = target.pos
                setPosition(hitPos.x, hitPos.y + target.height * 0.5, hitPos.z)
                onEntityHit(EntityHitResult(target, Vec3d(hitPos.x, hitPos.y + target.height * 0.5, hitPos.z)))
                return
            }
        }

        // 移动（直接设置位置，不走碰撞物理）
        val dist = velocity.length()
        remainingRange -= dist
        setPosition(x + velocity.x, y + velocity.y, z + velocity.z)

        // 粒子拖尾（客户端）
        if (world.isClient && lifeTicks % 2 == 0) {
            val col = color
            val r = ((col shr 16) and 0xFF) / 255f
            val g = ((col shr 8) and 0xFF) / 255f
            val b = (col and 0xFF) / 255f
            world.addParticle(
                net.minecraft.particle.DustParticleEffect(
                    org.joml.Vector3f(r, g, b), 0.4f
                ),
                x, y, z, 0.0, 0.0, 0.0
            )
        }
    }

    override fun onBlockHit(hitResult: BlockHitResult) {
        if (world.isClient) return
        val pos = hitResult.blockPos
        val state = world.getBlockState(pos)
        val currentMode = mode

        // 爆破模式：与原版 TNT 一致传入「爆炸源实体」与命中点坐标（见 TntEntity.explode）
        if (currentMode.explosionPower > 0) {
            world.createExplosion(
                this,
                x, y, z,
                currentMode.explosionPower,
                World.ExplosionSourceType.TNT
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
        val isFlammable = state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.PLANKS) || state.isIn(BlockTags.WOOL)
        if (currentMode == LaserMode.LOW_FOCUS && isFlammable) {
            if (canBreak(state) && !ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, pos, owner)) {
                world.breakBlock(pos, true, owner)
                tryRandomIgniteAfterBreak(pos)
            }
            world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
            discard()
            return
        }

        // 采矿模式：按方块硬度消耗射程，可连续击穿多个方块（与 LaserMode 描述一致）
        if (currentMode == LaserMode.MINING && canBreak(state)) {
            if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, pos, owner)) {
                discard()
                return
            }
            val hardness = state.block.hardness.coerceAtLeast(0.1f)
            remainingRange -= hardness.toDouble()
            world.breakBlock(pos, true, owner)
            tryRandomIgniteAfterBreak(pos)
            world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
            if (remainingRange > 0) {
                nudgePastBlock(hitResult)
                return
            }
            discard()
            return
        }

        // 其他模式：破坏方块
        if (canBreak(state) && !ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, pos, owner)) {
            world.breakBlock(pos, true, owner)
            tryRandomIgniteAfterBreak(pos)
            world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
        }

        discard()
    }

    /** 方块已移除后，在原位按概率尝试生成火（需满足原版放火条件）。 */
    private fun tryRandomIgniteAfterBreak(pos: BlockPos) {
        if (world.random.nextFloat() >= POST_BREAK_IGNITE_CHANCE) return
        if (!world.isInBuildLimit(pos)) return
        if (!AbstractFireBlock.canPlaceAt(world, pos, Direction.UP)) return
        if (!world.getBlockState(pos).isReplaceable) return
        world.setBlockState(pos, Blocks.FIRE.defaultState)
    }

    /** 击穿方块后沿飞行方向略微前移，避免同一 tick 再次命中同一格 */
    private fun nudgePastBlock(hitResult: BlockHitResult) {
        val dir = velocity.normalize()
        val nudge = 0.05
        setPosition(x + dir.x * nudge, y + dir.y * nudge, z + dir.z * nudge)
    }

    override fun onEntityHit(hitResult: EntityHitResult) {
        if (world.isClient) return
        val currentMode = mode

        if (currentMode.entityDamage > 0) {
            val target = hitResult.entity
            if (target is LivingEntity) {
                // 穿甲效果：爆炸模式使用爆炸伤害源，触发量子/纳米护甲减伤逻辑
                val damageSource = if (currentMode == LaserMode.EXPLOSIVE) {
                    world.damageSources.explosion(this, null)
                } else {
                    world.damageSources.generic()
                }
                target.damage(damageSource, currentMode.entityDamage)
            }
        }

        // 爆破模式在命中实体时也会爆炸（中心在弹体命中点，与方块命中一致）
        if (currentMode.explosionPower > 0) {
            world.createExplosion(
                this,
                x, y, z,
                currentMode.explosionPower,
                World.ExplosionSourceType.TNT
            )
            tryRandomIgniteAfterBreak(BlockPos.ofFloored(x, y, z))
        }

        discard()
    }

    /**
     * 超级热线模式：查找方块的熔炼配方，将方块替换为熔炼产物。
     * 对原木无效。
     */
    private fun smeltBlock(pos: net.minecraft.util.math.BlockPos, state: BlockState) {
        if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LOGS_THAT_BURN)) return
        if (!canBreak(state)) return
        if (ic2_120.integration.ftbchunks.ClaimProtection.isProtected(world, pos, owner)) return

        val serverWorld = world as? ServerWorld ?: return
        val blockItem = state.block.asItem()
        if (blockItem == net.minecraft.item.Items.AIR) return

        val inputStack = ItemStack(blockItem)
        val recipe = serverWorld.recipeManager.getFirstMatch(RecipeType.SMELTING, SingleStackRecipeInput(inputStack), serverWorld).map { it.value }.orElse(null)

        if (recipe != null) {
            val output = recipe.getResult(serverWorld.registryManager)
            if (!output.isEmpty) {
                world.breakBlock(pos, false)
                net.minecraft.block.Block.dropStack(serverWorld, pos, output.copy())
                tryRandomIgniteAfterBreak(pos)
                world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
                return
            }
        }

        // 没有熔炼配方的方块直接破坏
        world.breakBlock(pos, true, owner)
        tryRandomIgniteAfterBreak(pos)
        world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
    }

    private fun canBreak(state: BlockState): Boolean {
        if (state.isAir) return false
        if (state.isIn(BlockTags.WITHER_IMMUNE) || state.isIn(BlockTags.DRAGON_IMMUNE)) return false
        // 黑曜石、防爆石、核反应堆压力容器等高硬度方块
        if (state.block.hardness >= MAX_HARDNESS) return false
        return state.block.hardness >= 0
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        remainingRange = nbt.getDouble("RemainingRange")
        lifeTicks = nbt.getInt("LifeTicks")
        try {
            mode = LaserMode.valueOf(nbt.getString("Mode"))
        } catch (_: Exception) {
            mode = LaserMode.DEFAULT
        }
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        nbt.putDouble("RemainingRange", remainingRange)
        nbt.putInt("LifeTicks", lifeTicks)
        nbt.putString("Mode", mode.name)
    }

    override fun isCollidable(): Boolean = false
    override fun canUsePortals(allowVehicles: Boolean): Boolean = false
}
