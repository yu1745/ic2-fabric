package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.IronEngineBlock
import buildcraft_addon.content.screen.IronEngineScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * 燃烧引擎（铁引擎）
 *
 * 消耗流体燃料（油/燃油等）产生能量，需要冷却液防止过热。
 *
 * BC 原版数值（新代码 common/buildcraft/energy/tile/TileEngineIron_BC8.java）：
 * - getMaxPower() = 10000 MJ
 * - COOLDOWN_RATE = 0.05
 * - MAX_COOLANT_PER_TICK = 40 mB
 * - MAX_FLUID = 10000 mB（每 tank）
 * - HEAT_PER_MJ = 0.0023（每产出 1 MJ 增加的热量）
 * - 燃料见 BCEnergyRecipes 表
 * - 冷却液：水 0.0023 度/mB，冰 1.5x，浮冰 2.0x
 * - penaltyCooling = 10 ticks（红石信号丢失惩罚）
 * - 爆炸范围：4
 * - 活塞速度：BLUE=0.04, GREEN=0.05, YELLOW=0.06, RED=0.07
 *
 * 能量输出暂留空，待接入 IC2 动能系统。
 */
@ModBlockEntity(block = IronEngineBlock::class)
class IronEngineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ExtendedScreenHandlerFactory {

    constructor(pos: BlockPos, state: BlockState) : this(
        IronEngineBlockEntity::class.type(), pos, state
    )

    companion object {
        const val MIN_HEAT = 20.0
        const val IDEAL_HEAT = 100.0
        const val MAX_HEAT = 250.0
        const val COOLDOWN_RATE = 0.05
        const val MAX_COOLANT_PER_TICK = 40
        const val MAX_FLUID = 10000
        const val HEAT_PER_MJ = 0.0023
        const val PENALTY_COOLING_TICKS = 10

        // BC 原版燃料表（BCEnergyRecipes.java）
        // fuelPower[fluid] = powerPerCycle (MJ/tick), totalBurningTime (ticks/1000mB)
        // Crude Oil:      3 MJ/t,  10000 ticks
        // Fuel (Dense):   4 MJ/t,  30000 ticks
        // Fuel (Light):   6 MJ/t,  15000 ticks
        // Fuel (Gaseous): 8 MJ/t,   1875 ticks
        // Oil Distilled:  1 MJ/t,  37500 ticks
        // Heavy Oil:      2 MJ/t,  40000 ticks
        // Mixed Light:    3 MJ/t,  25000 ticks
        // Mixed Heavy:    5 MJ/t,  19200 ticks
    }

    // === 流体储罐 ===
    var fuelAmount: Int = 0
    var fuelType: String = ""
    var coolantAmount: Int = 0
    var residueAmount: Int = 0
    var currentPowerPerCycle: Double = 0.0   // MJ/tick，由燃料类型决定
    var currentTotalBurnTime: Int = 0        // ticks/1000mB

    // === 热量 ===
    var heat: Double = MIN_HEAT
    var penaltyCooling: Int = 0

    // === 燃烧 ===
    var burnTime: Double = 0.0
    var isBurning: Boolean = false

    // === 活塞动画 ===
    var progress: Float = 0f
    var progressPart: Int = 0
    var currentDirection: Direction = Direction.UP

    // === 红石控制 ===
    var isRedstonePowered: Boolean = false

    // === 能量输出（暂留空） ===
    // BC 原版: getCurrentOutput() = currentFuel.getPowerPerCycle()
    // addPower(getCurrentOutput())
    // heat += currentFuel.getPowerPerCycle() * HEAT_PER_MJ / MjAPI.MJ

    var currentStage: PowerStage = PowerStage.BLUE
        private set

    fun getHeatLevel(): Float = ((heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT)).toFloat()

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
            if (!powered) {
                penaltyCooling = PENALTY_COOLING_TICKS
            }
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

    // === 活塞速度（BC 原版铁引擎） ===
    fun pistonSpeed(stage: PowerStage): Float = when (stage) {
        PowerStage.BLUE -> 0.04f
        PowerStage.GREEN -> 0.05f
        PowerStage.YELLOW -> 0.06f
        PowerStage.RED -> 0.07f
        PowerStage.OVERHEAT -> 0f
        PowerStage.BLACK -> 0f
    }

    // === 燃料系统 ===
    fun setFuel(fluidName: String, powerPerCycle: Double, totalBurnTime: Int) {
        fuelType = fluidName
        currentPowerPerCycle = powerPerCycle
        currentTotalBurnTime = totalBurnTime
        markDirty()
    }

    private fun consumeFuel() {
        if (fuelAmount <= 0 || fuelType.isEmpty()) return
        if (burnTime > 0) return
        if (currentTotalBurnTime <= 0) return

        fuelAmount--
        burnTime = currentTotalBurnTime / 1000.0
        markDirty()
    }

    // === 冷却系统 ===
    private val propertyDelegate = object : PropertyDelegate {
        override fun get(index: Int): Int = when (index) {
            0 -> fuelAmount
            1 -> coolantAmount
            2 -> residueAmount
            3 -> heat.toInt()
            else -> 0
        }
        override fun set(index: Int, value: Int) {}
        override fun size(): Int = 4
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return IronEngineScreenHandler(syncId, playerInventory, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), propertyDelegate)
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(4)
    }

    override fun getDisplayName(): Text = Text.translatable("block.buildcraft_addon.iron_engine")

    private fun updateHeatLevel() {
        val world = world ?: return
        var targetHeat: Double = heat

        // 冷却逻辑
        if (heat > MIN_HEAT && (!isRedstonePowered || penaltyCooling > 0)) {
            targetHeat -= COOLDOWN_RATE
            if (penaltyCooling > 0) penaltyCooling--
        }

        // 主动冷却（消耗冷却液）
        if (heat > IDEAL_HEAT && coolantAmount > 0) {
            val coolantPerTick = MAX_COOLANT_PER_TICK.coerceAtMost(coolantAmount)
            // 水冷却：0.0023 度/mB
            val cooling = coolantPerTick * 0.0023
            targetHeat -= cooling
            coolantAmount -= coolantPerTick
        }

        heat = targetHeat.coerceAtLeast(MIN_HEAT)

        // 过热爆炸
        if (heat >= MAX_HEAT) {
            explode()
        }
    }

    private fun explode() {
        val world = world ?: return
        if (!world.isClient) {
            world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 4.0f, true, net.minecraft.world.World.ExplosionSourceType.BLOCK)
            world.breakBlock(pos, false)
        }
    }

    // === 服务端 tick ===
    fun serverTick() {
        val world = world ?: return
        if (world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)

        isBurning = false

        if (isRedstonePowered) {
            // 消耗燃料
            if (burnTime <= 0 && fuelAmount > 0) {
                consumeFuel()
            }

            // 燃烧产出
            if (burnTime > 0) {
                val consumed = 0.05.coerceAtMost(burnTime)
                burnTime -= consumed
                isBurning = true

                // 产热
                heat += currentPowerPerCycle * HEAT_PER_MJ

                // 残留物产出（仅脏燃料）
                // TODO: 按燃料类型产出残留物

                // TODO: 接入 IC2 动能系统后在此处添加能量产出
                // addPower(currentPowerPerCycle)
            }
        }

        updateHeatLevel()
        currentStage = computePowerStage()
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
        isBurning = burnTime > 0 && powered

        if (isBurning) {
            if (heat < IDEAL_HEAT) heat += 0.5
            else if (heat < MAX_HEAT * 0.85) heat += 0.05
        } else {
            if (heat > MIN_HEAT) {
                heat -= COOLDOWN_RATE
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
            }
        } else if (isBurning && currentStage != PowerStage.OVERHEAT) {
            progressPart = 1
        }
    }

    // === NBT ===
    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putDouble("heat", heat)
        nbt.putFloat("progress", progress)
        nbt.putInt("progressPart", progressPart)
        nbt.putInt("direction", currentDirection.ordinal)
        nbt.putBoolean("isRedstonePowered", isRedstonePowered)
        nbt.putBoolean("isBurning", isBurning)
        nbt.putInt("powerStage", currentStage.ordinal)
        nbt.putInt("fuelAmount", fuelAmount)
        nbt.putString("fuelType", fuelType)
        nbt.putInt("coolantAmount", coolantAmount)
        nbt.putInt("residueAmount", residueAmount)
        nbt.putDouble("currentPowerPerCycle", currentPowerPerCycle)
        nbt.putInt("currentTotalBurnTime", currentTotalBurnTime)
        nbt.putDouble("burnTime", burnTime)
        nbt.putInt("penaltyCooling", penaltyCooling)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        heat = nbt.getDouble("heat")
        progress = nbt.getFloat("progress")
        progressPart = nbt.getInt("progressPart")
        val dirIdx = nbt.getInt("direction")
        currentDirection = Direction.entries.getOrElse(dirIdx) { Direction.UP }
        isRedstonePowered = nbt.getBoolean("isRedstonePowered")
        isBurning = nbt.getBoolean("isBurning")
        val stageIdx = nbt.getInt("powerStage")
        currentStage = PowerStage.entries.getOrElse(stageIdx) { PowerStage.BLUE }
        fuelAmount = nbt.getInt("fuelAmount")
        fuelType = nbt.getString("fuelType")
        coolantAmount = nbt.getInt("coolantAmount")
        residueAmount = nbt.getInt("residueAmount")
        currentPowerPerCycle = nbt.getDouble("currentPowerPerCycle")
        currentTotalBurnTime = nbt.getInt("currentTotalBurnTime")
        burnTime = nbt.getDouble("burnTime")
        penaltyCooling = nbt.getInt("penaltyCooling")
    }
}
