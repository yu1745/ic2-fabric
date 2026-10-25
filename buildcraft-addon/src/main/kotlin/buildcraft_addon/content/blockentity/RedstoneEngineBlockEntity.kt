package buildcraft_addon.content.blockentity

import ic2_120.compat.LegacyBlockEntity26

import net.minecraft.core.HolderLookup
import buildcraft_addon.content.block.RedstoneEngineBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import ic2_120.compat.*
import net.minecraft.world.level.block.Block

enum class PowerStage {
    BLUE, GREEN, YELLOW, RED, OVERHEAT, BLACK
}

@ModBlockEntity(block = RedstoneEngineBlock::class)
class RedstoneEngineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : LegacyBlockEntity26(type, pos, state), IKineticMachinePort {

    constructor(pos: BlockPos, state: BlockState) : this(
        RedstoneEngineBlockEntity::class.type(), pos, state
    )

    companion object {
        const val MIN_HEAT = 20.0
        const val MAX_HEAT = 250.0
        const val MJ_TO_KU = 128

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
    var pendingOutputKu: Int = 0

    var currentStage: PowerStage = PowerStage.BLUE
        private set

    // === IKineticMachinePort ===
    override fun canOutputKuTo(side: Direction): Boolean = side == currentDirection

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
        val powered = world.hasNeighborSignal(pos)
        if (powered != isRedstonePowered) {
            isRedstonePowered = powered
            setChanged()
        }
    }

    fun autoRotate() {
        val world = world ?: return
        for (dir in Direction.entries) {
            val neighbor = world.getBlockState(pos.relative(dir))
            if (!neighbor.isAir) {
                currentDirection = dir
                world.setBlock(pos, cachedState.setValue(BlockStateProperties.FACING, dir), Block.UPDATE_ALL)
                setChanged()
                return
            }
        }
    }

    fun rotateToNextFacing(current: Direction) {
        val world = world ?: return
        val next = Direction.entries[(current.ordinal + 1) % 6]
        currentDirection = next
        world.setBlock(pos, cachedState.setValue(BlockStateProperties.FACING, next), Block.UPDATE_ALL)
        setChanged()
    }

    fun serverTick() {
        val world = world ?: return
        if (world.isClient) return
        currentDirection = cachedState.getValue(BlockStateProperties.FACING)

        pendingOutputKu = 0

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
        if (isRedstonePowered && currentStage == PowerStage.RED) {
            pendingOutputKu = MJ_TO_KU
        }
        tickPiston()
        setChanged()
    }

    fun clientTick() {
        val world = world ?: return
        if (!world.isClient) return
        currentDirection = cachedState.getValue(BlockStateProperties.FACING)

        val powered = world.hasNeighborSignal(pos)
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

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        nbt.putDouble("heat", heat)
        nbt.putFloat("progress", progress)
        nbt.putInt("progressPart", progressPart)
        nbt.putInt("direction", currentDirection.ordinal)
        nbt.putBoolean("isPumping", isPumping)
        nbt.putInt("powerStage", currentStage.ordinal)
        nbt.putInt("pendingOutputKu", pendingOutputKu)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        heat = nbt.getDoubleOr("heat", 0.0)
        progress = nbt.getFloatOr("progress", 0f)
        progressPart = nbt.getIntOr("progressPart", 0)
        val dirIdx = nbt.getIntOr("direction", 0)
        currentDirection = Direction.entries.getOrElse(dirIdx) { Direction.UP }
        isPumping = nbt.getBooleanOr("isPumping", false)
        val stageIdx = nbt.getIntOr("powerStage", 0)
        currentStage = PowerStage.entries.getOrElse(stageIdx) { PowerStage.BLUE }
        pendingOutputKu = nbt.getIntOr("pendingOutputKu", 0).coerceAtLeast(0)
    }
}
