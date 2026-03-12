package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.WindGeneratorScreenHandler
import ic2_120.content.sync.WindGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 风力发电机方块实体。
 *
 * 发电量算法：
 * - 风力强度 s：0~30，每 128 tick 刷新
 * - 有效高度 h = y - c（y 为坐标，c 为障碍数）
 * - 发电量 p = w * s * (h - 64) / 750，负数为 0
 * - w：晴天 1.0，雨天 1.2，雷雨 1.5
 * - 障碍范围：向下 2、向上 4、四周 4，即 9x9x7
 */
@ModBlockEntity(block = WindGeneratorBlock::class)
class WindGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    companion object {
        const val GENERATOR_TIER = 1
        const val BATTERY_SLOT = 0

        /** 风力/发电量刷新间隔（tick） */
        private const val REFRESH_INTERVAL = 128

        /** 障碍检测范围：向下 2、向上 4、四周 4 */
        private const val OBSTACLE_DOWN = 2
        private const val OBSTACLE_UP = 4
        private const val OBSTACLE_HORIZ = 4
    }

    override val tier: Int = GENERATOR_TIER

    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = WindGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    /** 风力强度 0~30，每 128 tick 刷新 */
    private var windStrength: Int = 15

    /** 当前输出（milli EU/t），每 128 tick 更新，支持小数如 2.5 EU/t = 2500 */
    private var currentOutputMilliEuPerTick: Int = 0

    /** 分数 EU 累积（milli EU），满 1000 时产生 1 EU */
    private var euAccum: Int = 0

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.extractEnergy(requested) },
        canChargeNow = { sync.amount > 0 }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(WindGeneratorBlockEntity::class),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    override fun size(): Int = 1
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            BATTERY_SLOT -> stack.item is IBatteryItem || stack.item is IElectricTool
            else -> false
        }
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.wind_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WindGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(WindGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, WindGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        windStrength = nbt.getInt("WindStrength").coerceIn(0, 30)
        currentOutputMilliEuPerTick = nbt.getInt("CurrentOutputMilliEuPerTick").coerceIn(0, 20000)
        euAccum = nbt.getInt("EuAccum").coerceIn(0, 999)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(WindGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("WindStrength", windStrength)
        nbt.putInt("CurrentOutputMilliEuPerTick", currentOutputMilliEuPerTick)
        nbt.putInt("EuAccum", euAccum)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 每 128 tick 刷新风力强度和发电量（世界时间对齐，所有风力机同步）
        if (world.time % REFRESH_INTERVAL == 0L) {
            updateWindStrength(world)
            updatePowerOutput(world, pos)
        }

        val canGenerate = currentOutputMilliEuPerTick > 0
        sync.isGenerating = if (canGenerate) 1 else 0

        if (canGenerate) {
            val space = (WindGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space > 0L) {
                // 每 tick 产生 currentOutputMilliEuPerTick/1000 EU（支持小数）
                euAccum += currentOutputMilliEuPerTick
                var euToAdd = 0L
                while (euAccum >= 1000 && space > euToAdd) {
                    euAccum -= 1000
                    euToAdd++
                }
                if (euToAdd > 0L) {
                    sync.generateEnergy(euToAdd)
                    sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                    markDirty()
                }
            }
        }

        batteryCharger.tick()

        val active = canGenerate
        if (state.get(WindGeneratorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(WindGeneratorBlock.ACTIVE, active))
        }
        // 同步当前 tick 的实际输出/输入
        sync.syncCurrentTickFlow()

    }

    /**
     * 更新风力强度：0~30，按 IC2 算法每 128 tick 有概率 ±1。
     */
    private fun updateWindStrength(world: World) {
        val r = world.random.nextInt(100)
        when {
            windStrength in 0..19 && r < 10 -> windStrength++
            windStrength in 20..30 && r < 10 -> windStrength--
            windStrength in 1..9 && r < windStrength -> windStrength--
            windStrength in 21..29 && r < (30 - windStrength) -> windStrength++
        }
        windStrength = windStrength.coerceIn(0, 30)
    }

    /**
     * 更新发电量：p = w * s * (h - 64) / 750，h = y - c。
     */
    private fun updatePowerOutput(world: World, pos: BlockPos) {
        val y = pos.y
        if (y < 64) {
            currentOutputMilliEuPerTick = 0
            return
        }

        val obstacleCount = countObstacles(world, pos)
        val effectiveHeight = y - obstacleCount
        val h = effectiveHeight - 64
        if (h <= 0) {
            currentOutputMilliEuPerTick = 0
            return
        }

        val weatherFactor = when {
            world.isThundering -> 1.5
            world.isRaining -> 1.2
            else -> 1.0
        }

        // p = w * s * h / 750，结果为 EU/t，存为 milli EU/t
        val p = weatherFactor * windStrength * h / 750.0
        currentOutputMilliEuPerTick = (p * 1000).toInt().coerceIn(0, 20000)
    }

    /**
     * 统计障碍方块数：以发电机为中心，向下 2、向上 4、四周 4 的 9x9x7 空间内非空气方块（不含自身）。
     */
    private fun countObstacles(world: World, center: BlockPos): Int {
        var count = 0
        for (dx in -OBSTACLE_HORIZ..OBSTACLE_HORIZ) {
            for (dy in -OBSTACLE_DOWN..OBSTACLE_UP) {
                for (dz in -OBSTACLE_HORIZ..OBSTACLE_HORIZ) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val p = center.add(dx, dy, dz)
                    if (world.isInBuildLimit(p)) {
                        val blockState = world.getBlockState(p)
                        if (!blockState.isAir && blockState.block != Blocks.VOID_AIR) {
                            count++
                        }
                    }
                }
            }
        }
        return count
    }
}


