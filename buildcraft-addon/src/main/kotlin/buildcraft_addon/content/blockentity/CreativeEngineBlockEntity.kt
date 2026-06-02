package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.CreativeEngineBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * 创造引擎
 *
 * 无须燃料，始终以最大能量输出。
 * 右键切换 9 档输出功率。
 *
 * BC 原版数值（common/buildcraft/core/tile/TileEngineCreative.java）：
 * - 输出档位：1, 2, 4, 8, 16, 32, 64, 128, 256 MJ/tick
 * - getMaxPower() = getCurrentOutput() * 10000
 * - 无热量系统（始终 BLACK 阶段）
 * - 活塞速度：0.01 到 0.08 之间插值，取决于档位
 * - 爆炸范围：0
 * - 需要红石信号激活
 *
 * 能量输出暂留空，待接入 IC2 动能系统。
 */
@ModBlockEntity(block = CreativeEngineBlock::class)
class CreativeEngineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    constructor(pos: BlockPos, state: BlockState) : this(
        CreativeEngineBlockEntity::class.type(), pos, state
    )

    companion object {
        // BC 原版输出档位（单位：MJ/tick）
        val OUTPUT_LEVELS = listOf(1, 2, 4, 8, 16, 32, 64, 128, 256)
    }

    // === 输出档位 ===
    var outputIndex: Int = 0

    // === 活塞动画 ===
    var progress: Float = 0f
    var progressPart: Int = 0
    var currentDirection: Direction = Direction.UP

    // === 红石控制 ===
    var isRedstonePowered: Boolean = false

    // === 能量输出（暂留空） ===
    // BC 原版:
    // getCurrentOutput() = OUTPUT_LEVELS[outputIndex] (单位: MjAPI.MJ)
    // getMaxPower() = getCurrentOutput() * 10000
    // maxPowerExtracted() = 20 * getCurrentOutput()
    // 当 isRedstonePowered 时: addPower(getCurrentOutput())

    var currentStage: PowerStage = PowerStage.BLACK
        private set

    // 创造引擎无热量系统，始终返回 BLACK

    fun checkRedstonePower() {
        val world = world ?: return
        val powered = world.isReceivingRedstonePower(pos)
        if (powered != isRedstonePowered) {
            isRedstonePowered = powered
            markDirty()
        }
    }

    fun rotateToNextFacing(current: Direction) {
        val world = world ?: return
        val next = Direction.entries[(current.ordinal + 1) % 6]
        currentDirection = next
        world.setBlockState(pos, cachedState.with(Properties.FACING, next))
        markDirty()
    }

    fun cycleOutput() {
        outputIndex = (outputIndex + 1) % OUTPUT_LEVELS.size
        markDirty()
    }

    fun getCurrentOutputMJ(): Int = OUTPUT_LEVELS[outputIndex]

    // === 活塞速度（BC 原版创造引擎，按档位插值） ===
    fun pistonSpeed(): Float {
        return 0.01f + (outputIndex.toFloat() / (OUTPUT_LEVELS.size - 1)) * 0.07f
    }

    // === 服务端 tick ===
    fun serverTick() {
        val world = world ?: return
        if (world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)

        if (isRedstonePowered) {
            // TODO: 接入 IC2 动能系统后在此处添加能量产出
            // addPower(getCurrentOutputMJ())
        }

        tickPiston()
        markDirty()
    }

    // === 客户端 tick ===
    fun clientTick() {
        val world = world ?: return
        if (!world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)

        val powered = world.isReceivingRedstonePower(pos)
        isRedstonePowered = powered

        tickPiston()
    }

    private fun tickPiston() {
        if (progressPart != 0) {
            progress += pistonSpeed()
            if (progress > 0.5f && progressPart == 1) {
                progressPart = 2
            } else if (progress >= 1f) {
                progress = 0f
                progressPart = 0
            }
        } else if (isRedstonePowered) {
            progressPart = 1
        }
    }

    // === NBT ===
    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putFloat("progress", progress)
        nbt.putInt("progressPart", progressPart)
        nbt.putInt("direction", currentDirection.ordinal)
        nbt.putBoolean("isRedstonePowered", isRedstonePowered)
        nbt.putInt("outputIndex", outputIndex)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        progress = nbt.getFloat("progress")
        progressPart = nbt.getInt("progressPart")
        val dirIdx = nbt.getInt("direction")
        currentDirection = Direction.entries.getOrElse(dirIdx) { Direction.UP }
        isRedstonePowered = nbt.getBoolean("isRedstonePowered")
        outputIndex = nbt.getInt("outputIndex")
    }
}
