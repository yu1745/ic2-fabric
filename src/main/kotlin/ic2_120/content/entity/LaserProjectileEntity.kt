package ic2_120.content.entity

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
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.explosion.Explosion

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

        private val LASER_HIT_SOUND: SoundEvent = SoundEvent.of(Identifier("ic2", "item.laser.hit"))

        private val MODE: TrackedData<String> = DataTracker.registerData(
            LaserProjectileEntity::class.java, TrackedDataHandlerRegistry.STRING
        )

        /** 不会被镭射破坏的方块硬度阈值（黑曜石=50，防爆石等更高） */
        private const val MAX_HARDNESS = 40.0f
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

    override fun initDataTracker() {
        dataTracker.startTracking(MODE, LaserMode.DEFAULT.name)
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

        // 实体碰撞检测
        val entityHit = ProjectileUtil.getCollision(this) { entity: Entity ->
            entity.isAlive && entity.canHit() && !entity.isSpectator
        }
        if (entityHit != null && entityHit.type == HitResult.Type.ENTITY) {
            setPosition(entityHit.pos)
            onEntityHit(entityHit as EntityHitResult)
            return
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

        // 爆破模式
        if (currentMode.explosionPower > 0) {
            world.createExplosion(
                null,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                currentMode.explosionPower,
                World.ExplosionSourceType.TNT
            )
            discard()
            return
        }

        // 超级热线模式：烧制方块
        if (currentMode == LaserMode.SUPER_HEAT) {
            smeltBlock(pos, state)
            discard()
            return
        }

        // 低聚焦模式：有几率点燃
        val isFlammable = state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.PLANKS) || state.isIn(BlockTags.WOOL)
        if (currentMode == LaserMode.LOW_FOCUS && isFlammable) {
            if (world.random.nextFloat() < 0.3f) {
                world.setBlockState(pos, Blocks.FIRE.defaultState)
            } else if (canBreak(state)) {
                world.breakBlock(pos, true, owner)
            }
            world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
            discard()
            return
        }

        // 其他模式：破坏方块
        if (canBreak(state)) {
            world.breakBlock(pos, true, owner)
            world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
        }

        discard()
    }

    override fun onEntityHit(hitResult: EntityHitResult) {
        if (world.isClient) return
        val currentMode = mode

        if (currentMode.entityDamage > 0) {
            val target = hitResult.entity
            if (target is LivingEntity) {
                // 穿甲效果：直接设置伤害而非调用 hurt（绕过护甲）
                if (currentMode == LaserMode.EXPLOSIVE) {
                    target.health -= currentMode.entityDamage
                } else {
                    target.damage(world.damageSources.generic(), currentMode.entityDamage)
                }
            }
        }

        // 爆破模式在命中实体时也会爆炸
        if (currentMode.explosionPower > 0) {
            world.createExplosion(
                null,
                hitResult.entity.x, hitResult.entity.y, hitResult.entity.z,
                currentMode.explosionPower,
                World.ExplosionSourceType.TNT
            )
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

        val serverWorld = world as? ServerWorld ?: return
        val blockItem = state.block.asItem()
        if (blockItem == net.minecraft.item.Items.AIR) return

        val inputStack = ItemStack(blockItem)
        val inventory = SimpleInventory(inputStack)
        val recipe = serverWorld.recipeManager.getFirstMatch(RecipeType.SMELTING, inventory, serverWorld).orElse(null)

        if (recipe != null) {
            val output = recipe.craft(inventory, serverWorld.registryManager)
            if (!output.isEmpty) {
                world.breakBlock(pos, false)
                net.minecraft.block.Block.dropStacks(state, serverWorld, pos)
                net.minecraft.block.Block.dropStack(serverWorld, pos, output.copy())
                world.playSound(null, pos, LASER_HIT_SOUND, SoundCategory.PLAYERS, 0.8f, 1.2f)
                return
            }
        }

        // 没有熔炼配方的方块直接破坏
        world.breakBlock(pos, true, owner)
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

    override fun canHit(): Boolean = mode.entityDamage > 0

    override fun isCollidable(): Boolean = false
    override fun canUsePortals(): Boolean = false
}
