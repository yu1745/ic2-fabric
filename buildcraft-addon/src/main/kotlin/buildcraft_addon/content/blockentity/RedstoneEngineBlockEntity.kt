package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.RedstoneEngineBlock
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

enum class PowerStage {
    BLUE, GREEN, YELLOW, RED, OVERHEAT, BLACK
}

@ModBlockEntity(block = RedstoneEngineBlock::class)
class RedstoneEngineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    constructor(pos: BlockPos, state: BlockState) : this(
        RedstoneEngineBlockEntity::class.type(), pos, state
    )

    companion object {
        const val MIN_HEAT = 20.0
        const val MAX_HEAT = 250.0

        fun pistonSpeed(stage: PowerStage): Float {
            return when (stage) {
                PowerStage.BLUE -> 0.01f
                PowerStage.GREEN -> 0.02f
                PowerStage.YELLOW -> 0.04f
                PowerStage.RED -> 0.06f
                PowerStage.OVERHEAT -> 0f
                PowerStage.BLACK -> 0f
            }
        }
    }

    var heat: Double = MIN_HEAT
    var progress: Float = 0f
    var progressPart: Int = 0
    var currentDirection: Direction = Direction.UP
    var isRedstonePowered: Boolean = false
    var isPumping: Boolean = false

    var currentStage: PowerStage = PowerStage.BLUE
        private set

    fun getHeatLevel(): Float {
        return ((heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT)).toFloat()
    }

    private fun computePowerStage(): PowerStage {
        val hl = getHeatLevel()
        return when {
            hl < 0.25f -> PowerStage.BLUE
            hl < 0.5f -> PowerStage.GREEN
            hl < 0.75f -> PowerStage.YELLOW
            hl < 0.85f -> PowerStage.RED
            else -> PowerStage.OVERHEAT
        }
    }

    fun checkRedstonePower() {
        val world = world ?: return
        val powered = world.isReceivingRedstonePower(pos)
        if (powered != isRedstonePowered) {
            isRedstonePowered = powered
            markDirty()
        }
    }

    fun autoRotate() {
        val world = world ?: return
        for (dir in Direction.entries) {
            val neighbor = world.getBlockState(pos.offset(dir))
            if (!neighbor.isAir) {
                currentDirection = dir
                world.setBlockState(pos, cachedState.with(Properties.FACING, dir))
                markDirty()
                return
            }
        }
    }

    fun rotateToNextFacing(current: Direction) {
        val world = world ?: return
        val next = Direction.entries[(current.ordinal + 1) % 6]
        currentDirection = next
        world.setBlockState(pos, cachedState.with(Properties.FACING, next))
        markDirty()
    }

    fun serverTick() {
        val world = world ?: return
        if (world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)

        if (isRedstonePowered) {
            if (world.time % 16L == 0L && getHeatLevel() < 0.8f) {
                heat += 4.0
            }
        } else {
            if (heat > MIN_HEAT) {
                heat -= 0.2
                if (heat < MIN_HEAT) heat = MIN_HEAT
            }
        }

        currentStage = computePowerStage()
        tickPiston()
        markDirty()
    }

    fun clientTick() {
        val world = world ?: return
        if (!world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)

        val powered = world.isReceivingRedstonePower(pos)
        isRedstonePowered = powered

        if (powered) {
            if (heat < 100.0) heat += 0.25
        } else {
            if (heat > MIN_HEAT) {
                heat -= 0.5
                if (heat < MIN_HEAT) heat = MIN_HEAT
            }
        }

        currentStage = computePowerStage()
        tickPiston()
    }

    private fun tickPiston() {
        if (progressPart != 0) {
            progress += pistonSpeed(currentStage)
            if (progress > 0.5f && progressPart == 1) {
                progressPart = 2
            } else if (progress >= 1f) {
                progress = 0f
                progressPart = 0
                isPumping = false
            }
        } else if (isRedstonePowered && currentStage != PowerStage.OVERHEAT) {
            progressPart = 1
            isPumping = true
        } else {
            isPumping = false
        }
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putDouble("heat", heat)
        nbt.putFloat("progress", progress)
        nbt.putInt("progressPart", progressPart)
        nbt.putInt("direction", currentDirection.ordinal)
        nbt.putBoolean("isPumping", isPumping)
        nbt.putInt("powerStage", currentStage.ordinal)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        heat = nbt.getDouble("heat")
        progress = nbt.getFloat("progress")
        progressPart = nbt.getInt("progressPart")
        val dirIdx = nbt.getInt("direction")
        currentDirection = Direction.entries.getOrElse(dirIdx) { Direction.UP }
        isPumping = nbt.getBoolean("isPumping")
        val stageIdx = nbt.getInt("powerStage")
        currentStage = PowerStage.entries.getOrElse(stageIdx) { PowerStage.BLUE }
    }
}
