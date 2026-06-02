package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.RFEngineBlock
import buildcraft_addon.content.screen.RFEngineScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * RF 转换引擎
 *
 * 消耗 Redstone Flux (RF/Forge Energy) 转换为 MJ。
 * 仅存在于 BC 新代码中，旧版 BC 没有此引擎。
 *
 * BC 原版数值（common/buildcraft/energy/tile/TileEngineRF.java）：
 * - getMaxPower() = 1000 MJ
 * - MAX_RF = 10000 RF（内部电池）
 * - HEAT_RATE = 0.06（每 tick 产热量）
 * - COOLDOWN_RATE = 0.01（自然冷却速率）
 * - 基础输出：4 MJ/tick + 升级加成
 * - 铁齿轮升级：+2 MJ/tick，金齿轮升级：+3 MJ/tick
 * - 4 个升级槽位
 * - 爆炸范围：4
 * - 活塞速度：BLUE=0.04, GREEN=0.05, YELLOW=0.06, RED=0.07
 * - 热量上限：200（非 250）
 *
 * 能量输出暂留空，待接入 IC2 动能系统。
 */
@ModBlockEntity(block = RFEngineBlock::class)
class RFEngineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ExtendedScreenHandlerFactory, Inventory {

    constructor(pos: BlockPos, state: BlockState) : this(
        RFEngineBlockEntity::class.type(), pos, state
    )

    companion object {
        const val MIN_HEAT = 20.0
        const val MAX_HEAT_RF = 200.0  // RF 引擎热量上限不同
        const val HEAT_RATE = 0.06
        const val COOLDOWN_RATE = 0.01
        const val MAX_RF = 10000

        // BC 原版:
        // baseOutputMJ = 4 (MjAPI.MJ * 4)
        // ironGearUpgrade = 2 (MjAPI.MJ * 2) per gear
        // goldGearUpgrade = 3 (MjAPI.MJ * 3) per gear
        // rfConsumedRate = getMjPerTick() / mjPerRf (来自配置)
        // mjGenerated = rfConsumed * mjPerRf
    }

    // === RF 电池 ===
    var currentRF: Int = 0

    // === Screen 同步 ===
    private val propertyDelegate = object : PropertyDelegate {
        override fun get(index: Int): Int = when (index) {
            0 -> currentRF
            1 -> heat.toInt()
            else -> 0
        }
        override fun set(index: Int, value: Int) {}
        override fun size(): Int = 2
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return RFEngineScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), propertyDelegate)
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(2)
    }

    override fun getDisplayName(): Text = Text.translatable("block.buildcraft_addon.rf_engine")

    // === 升级栏位 ===
    private val upgradeInventory = DefaultedList.ofSize(4, ItemStack.EMPTY)

    override fun size(): Int = 4
    override fun getStack(slot: Int): ItemStack = upgradeInventory[slot]
    override fun setStack(slot: Int, stack: ItemStack) { upgradeInventory[slot] = stack }
    override fun removeStack(slot: Int, amount: Int): ItemStack = upgradeInventory[slot].split(amount)
    override fun removeStack(slot: Int): ItemStack {
        val stack = upgradeInventory[slot]
        upgradeInventory[slot] = ItemStack.EMPTY
        return stack
    }
    override fun isEmpty(): Boolean = upgradeInventory.all { it.isEmpty }
    override fun clear() { upgradeInventory.fill(ItemStack.EMPTY) }
    override fun canPlayerUse(player: PlayerEntity): Boolean = true

    // === 热量 ===
    var heat: Double = MIN_HEAT

    // === 活塞动画 ===
    var progress: Float = 0f
    var progressPart: Int = 0
    var currentDirection: Direction = Direction.UP

    // === 红石控制 ===
    var isRedstonePowered: Boolean = false
    var isBurning: Boolean = false

    // === 能量输出（暂留空） ===
    // BC 原版:
    // getMjPerTick() = baseOutputMJ + sum(upgrades)
    // getRfConsumptionRate() = getMjPerTick() / mjPerRf
    // 每 tick: rfConsumed = min(currentRF, maxRf)
    //          mjGenerated = rfConsumed * mjPerRf
    //          addPower(mjGenerated)
    //          currentRF -= rfConsumed

    var currentStage: PowerStage = PowerStage.BLUE
        private set

    fun getHeatLevel(): Float = ((heat - MIN_HEAT) / (MAX_HEAT_RF - MIN_HEAT)).toFloat()

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

    fun rotateToNextFacing(current: Direction) {
        val world = world ?: return
        val next = Direction.entries[(current.ordinal + 1) % 6]
        currentDirection = next
        world.setBlockState(pos, cachedState.with(Properties.FACING, next))
        markDirty()
    }

    // === 活塞速度（与铁引擎相同） ===
    fun pistonSpeed(stage: PowerStage): Float = when (stage) {
        PowerStage.BLUE -> 0.04f
        PowerStage.GREEN -> 0.05f
        PowerStage.YELLOW -> 0.06f
        PowerStage.RED -> 0.07f
        PowerStage.OVERHEAT -> 0f
        PowerStage.BLACK -> 0f
    }

    // === 服务端 tick ===
    fun serverTick() {
        val world = world ?: return
        if (world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)

        isBurning = false

        if (isRedstonePowered && currentRF > 0) {
            // 消耗 RF 并产热
            val rfToConsume = currentRF.coerceAtMost(100)  // 每 tick 最多消耗 100 RF
            currentRF -= rfToConsume
            heat += HEAT_RATE
            isBurning = true

            if (heat > MAX_HEAT_RF) {
                heat = MAX_HEAT_RF
            }

            // TODO: 接入 IC2 动能系统后在此处添加能量产出
            // mjGenerated = rfToConsume * mjPerRf
            // addPower(mjGenerated)
        }

        // 冷却
        if (!isBurning && heat > MIN_HEAT) {
            heat -= COOLDOWN_RATE
            if (heat < MIN_HEAT) heat = MIN_HEAT
        }

        // 过热爆炸
        if (heat >= MAX_HEAT_RF) {
            world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 4.0f, true, net.minecraft.world.World.ExplosionSourceType.BLOCK)
            world.breakBlock(pos, false)
        }

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
        isBurning = powered && currentRF > 0

        if (isBurning) {
            heat += HEAT_RATE
            if (heat > MAX_HEAT_RF) heat = MAX_HEAT_RF
        } else {
            if (heat > MIN_HEAT) {
                heat -= COOLDOWN_RATE * 5  // 客户端加速冷却
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
        nbt.putInt("currentRF", currentRF)
        val invNbt = NbtCompound()
        for (i in 0..3) {
            val slotNbt = NbtCompound()
            if (!upgradeInventory[i].isEmpty) {
                upgradeInventory[i].writeNbt(slotNbt)
            }
            invNbt.put("slot_$i", slotNbt)
        }
        nbt.put("upgradeInventory", invNbt)
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
        currentRF = nbt.getInt("currentRF")
        if (nbt.contains("upgradeInventory")) {
            val invNbt = nbt.getCompound("upgradeInventory")
            for (i in 0..3) {
                if (invNbt.contains("slot_$i")) {
                    upgradeInventory[i] = ItemStack.fromNbt(invNbt.getCompound("slot_$i"))
                }
            }
        }
    }
}
