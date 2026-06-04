package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.StoneEngineBlock
import buildcraft_addon.content.screen.StoneEngineScreenHandler
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
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
 * 斯特林引擎（石引擎）
 *
 * 消耗固体燃料（熔炉燃料）产生能量。
 * 使用 PID 控制器调节输出以匹配目标能量水平。
 *
 * BC 原版数值（新代码 common/buildcraft/energy/tile/TileEngineStone_BC8.java）：
 * - MAX_OUTPUT = 1 MJ/tick
 * - MIN_OUTPUT = MAX_OUTPUT / 3 ≈ 0.333 MJ/tick
 * - getMaxPower() = 1000 MJ
 * - eLimit = (MAX_OUTPUT - MIN_OUTPUT) * 20
 * - PID: e = 3/8 * maxPower - power, esum = clamp(esum + e, -eLimit, eLimit)
 * - 输出 = clamp(e + esum/20, MIN_OUTPUT, MAX_OUTPUT)
 * - 燃料：所有熔炉可燃烧物品（TileEntityFurnace.getItemBurnTime）
 * - 爆炸范围：2
 * - 活塞速度：BLUE=0.02, GREEN=0.04, YELLOW=0.08, RED=0.12
 *
 * 能量输出暂留空，待接入 IC2 动能系统。
 */
@ModBlockEntity(block = StoneEngineBlock::class)
class StoneEngineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory<PacketByteBuf>, IKineticMachinePort {

    constructor(pos: BlockPos, state: BlockState) : this(
        StoneEngineBlockEntity::class.type(), pos, state
    )

    companion object {
        const val MIN_HEAT = 20.0
        const val MAX_HEAT = 250.0
        const val IDEAL_HEAT = 100.0
        const val SLOT_FUEL = 0
        const val MJ_TO_KU = 128
    }

    // === Screen 同步 ===
    private val propertyDelegate = object : PropertyDelegate {
        override fun get(index: Int): Int = when (index) {
            0 -> burnTime
            1 -> totalBurnTime
            else -> 0
        }
        override fun set(index: Int, value: Int) {}
        override fun size(): Int = 2
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return StoneEngineScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), propertyDelegate)
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(2)
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.buildcraft_addon.stone_engine")

    // === 燃料 ===
    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)
    var burnTime: Int = 0
    var totalBurnTime: Int = 0

    // === 热量 ===
    var heat: Double = MIN_HEAT

    // === 活塞动画 ===
    var progress: Float = 0f
    var progressPart: Int = 0
    var currentDirection: Direction = Direction.UP

    // === 红石控制 ===
    var isRedstonePowered: Boolean = false

    // === 能量输出（暂留空） ===
    // BC 原版:
    // MAX_OUTPUT_MJ = 1 (MjAPI.MJ)
    // MIN_OUTPUT_MJ = MAX_OUTPUT_MJ / 3
    // maxPower = 1000 (MjAPI.MJ * 1000)
    // eLimit = (MAX_OUTPUT_MJ - MIN_OUTPUT_MJ) * 20
    // esum = clamp(esum + e, -eLimit, eLimit)
    // 每 tick: e = maxPower * 3/8 - power; output = clamp(e + esum/20, MIN_OUTPUT_MJ, MAX_OUTPUT_MJ)
    // addPower(output)

    var currentStage: PowerStage = PowerStage.BLUE
        private set

    // === 动能输出 ===
    var pendingOutputKu: Int = 0
        private set

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
            markDirty()
        }
        return extracted
    }

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

    // === 活塞速度（BC 原版石引擎） ===
    fun pistonSpeed(stage: PowerStage): Float = when (stage) {
        PowerStage.BLUE -> 0.02f
        PowerStage.GREEN -> 0.04f
        PowerStage.YELLOW -> 0.08f
        PowerStage.RED -> 0.12f
        PowerStage.OVERHEAT -> 0f
        PowerStage.BLACK -> 0f
    }

    // === 燃料系统 ===
    private fun getFuelBurnTime(stack: ItemStack): Int {
        return when (stack.item) {
            Items.COAL -> 1600
            Items.CHARCOAL -> 1600
            Items.COAL_BLOCK -> 16000
            Items.LAVA_BUCKET -> 20000
            Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS,
            Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
            Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS,
            Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS -> 300
            Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING,
            Items.CHERRY_SAPLING -> 100
            Items.STICK -> 100
            Items.BLAZE_ROD -> 2400
            Items.DRIED_KELP_BLOCK -> 4001
            Items.BAMBOO -> 50
            else -> 0
        }
    }

    private fun isFuel(stack: ItemStack): Boolean = getFuelBurnTime(stack) > 0

    private fun consumeFuel() {
        if (burnTime > 0) return
        val fuel = inventory[SLOT_FUEL]
        if (fuel.isEmpty || !isFuel(fuel)) return

        val time = getFuelBurnTime(fuel)
        if (time <= 0) return

        burnTime = time
        totalBurnTime = time

        // 处理容器物品（如空桶）
        val containerItem = fuel.item.recipeRemainder
        if (containerItem != Items.AIR) {
            inventory[SLOT_FUEL] = ItemStack(containerItem)
        } else {
            fuel.decrement(1)
        }
        markDirty()
    }

    // === 服务端 tick ===
    fun serverTick() {
        val world = world ?: return
        if (world.isClient) return
        currentDirection = cachedState.get(Properties.FACING)
        pendingOutputKu = 0

        // 燃料消耗
        if (isRedstonePowered) {
            consumeFuel()
        }

        // 燃烧
        if (burnTime > 0 && isRedstonePowered) {
            burnTime--
            // 产热与能量输出
            // BC 原版热力模型：heat = (MAX_HEAT - MIN_HEAT) * getPowerLevel() + MIN_HEAT
            // 其中 getPowerLevel() 由 PID 控制器决定输出水平
            // 简化版：运行时 heat = IDEAL_HEAT 为基础，随燃烧上升
            if (heat < IDEAL_HEAT) {
                heat += 0.5
            } else if (heat < MAX_HEAT * 0.85) {
                heat += 0.05
            }
            // 接入 IC2 动能系统：每 tick 产出 1 MJ = 128 KU
            pendingOutputKu = MJ_TO_KU
        } else {
            if (heat > MIN_HEAT) {
                heat -= 0.1
                if (heat < MIN_HEAT) heat = MIN_HEAT
            }
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

        if (powered && burnTime > 0) {
            if (heat < 100.0) heat += 0.5
            else if (heat < 215.0) heat += 0.05
        } else {
            if (heat > MIN_HEAT) {
                heat -= 0.2
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
        } else if (burnTime > 0 && isRedstonePowered) {
            progressPart = 1
        }
    }

    // === Inventory ===
    override fun size(): Int = 1
    override fun getStack(slot: Int): ItemStack = inventory[slot]
    override fun setStack(slot: Int, stack: ItemStack) { inventory[slot] = stack }
    override fun removeStack(slot: Int, amount: Int): ItemStack = inventory[slot].split(amount)
    override fun removeStack(slot: Int): ItemStack {
        val stack = inventory[slot]
        inventory[slot] = ItemStack.EMPTY
        return stack
    }
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun clear() { inventory.fill(ItemStack.EMPTY) }
    override fun canPlayerUse(player: net.minecraft.entity.player.PlayerEntity): Boolean = true

    // === NBT ===
    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putDouble("heat", heat)
        nbt.putFloat("progress", progress)
        nbt.putInt("progressPart", progressPart)
        nbt.putInt("direction", currentDirection.ordinal)
        nbt.putInt("burnTime", burnTime)
        nbt.putInt("totalBurnTime", totalBurnTime)
        nbt.putBoolean("isRedstonePowered", isRedstonePowered)
        nbt.putInt("powerStage", currentStage.ordinal)
        nbt.putInt("pendingOutputKu", pendingOutputKu)
        val fuelStack = inventory[SLOT_FUEL]
        if (!fuelStack.isEmpty) {
            nbt.put("fuel", fuelStack.encode(lookup))
        }
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        heat = nbt.getDouble("heat")
        progress = nbt.getFloat("progress")
        progressPart = nbt.getInt("progressPart")
        val dirIdx = nbt.getInt("direction")
        currentDirection = Direction.entries.getOrElse(dirIdx) { Direction.UP }
        burnTime = nbt.getInt("burnTime")
        totalBurnTime = nbt.getInt("totalBurnTime")
        isRedstonePowered = nbt.getBoolean("isRedstonePowered")
        val stageIdx = nbt.getInt("powerStage")
        currentStage = PowerStage.entries.getOrElse(stageIdx) { PowerStage.BLUE }
        pendingOutputKu = nbt.getInt("pendingOutputKu").coerceAtLeast(0)
        if (nbt.contains("fuel")) {
            inventory[SLOT_FUEL] = ItemStack.fromNbt(lookup, nbt.getCompound("fuel")).orElse(ItemStack.EMPTY)
        }
    }
}
