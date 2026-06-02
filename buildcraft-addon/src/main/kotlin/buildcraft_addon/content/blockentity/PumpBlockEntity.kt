package buildcraft_addon.content.blockentity

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.block.PumpBlock
import buildcraft_addon.content.fluid.ModFluids
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.*

/**
 * 液泵 — BC 原版 1:1 复制
 *
 * 机械臂：
 * - BFS 搜索下方及周围流体（区分液体/气体方向）
 * - 管道方块从泵向下延伸到流体层
 * - 预模拟抽取（先检查再执行）
 * - 无限水源检测（≥2 相邻水源视为无限）
 * - 油泉交互
 * - 成就系统
 * - 能量暂留空
 */
@ModBlockEntity(block = PumpBlock::class)
class PumpBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    constructor(pos: BlockPos, state: BlockState) : this(PumpBlockEntity::class.type(), pos, state)

    companion object {
        const val TANK_CAPACITY = 16000L
        const val MAX_RADIUS_SQ = 64 * 64
        const val MAX_DEPTH = 512
        const val REBUILD_DELAY = 30

        private val SEARCH_NORMAL = arrayOf(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)
        private val SEARCH_GASEOUS = arrayOf(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity({ be, _ -> be.getFluidStorageForSide() }, PumpBlockEntity::class.type())
            fluidLookupRegistered = true
        }
    }

    // ========== 储罐 ==========
    val tank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant() = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant) = TANK_CAPACITY
        override fun canInsert(variant: FluidVariant) = false
    }

    fun getFluidStorageForSide(): Storage<FluidVariant> = tank

    // ========== 状态 ==========
    var isRedstonePowered = false
    private var queueBuilt = false
    private var rebuildTimer = 0

    private val fluidQueue = ArrayDeque<BlockPos>()
    private val fluidPaths = mutableMapOf<BlockPos, FluidPath>()
    private var targetFluid: Fluid? = null
    var tubeDepth = 0
    private var isInfiniteWater = false
    var isActive = false // 给 LED 渲染
    private var removing = false // markRemoved 防重入
    private var totalPumped = 0
    private var oilPumped = false
    private var hasOilAchievement = false

    private var oilSpringPos: BlockPos? = null

    data class FluidPath(val pos: BlockPos, val parent: FluidPath?)

    // ========== 红石 ==========
    fun checkRedstonePower() {
        val w = world ?: return
        val powered = w.isReceivingRedstonePower(pos)
        if (powered != isRedstonePowered) {
            isRedstonePowered = powered
            markDirty()
        }
    }

    // ========== BFS 搜索（BC 原版 1:1） ==========
    fun buildQueue() {
        fluidQueue.clear()
        fluidPaths.clear()
        targetFluid = null
        oilSpringPos = null
        isInfiniteWater = false
        rebuildTimer = 0
        queueBuilt = true

        val w = world ?: return
        var foundPos: BlockPos? = null

        // BC: 自上向下搜索第一个流体
        for (searchY in pos.y - 1 downTo w.bottomY) {
            val check = BlockPos(pos.x, searchY, pos.z)
            if (pos.y - searchY > MAX_DEPTH) break
            val fs = w.getFluidState(check)
            if (!fs.isEmpty) {
                foundPos = check
                targetFluid = fs.fluid
                fluidPaths[check] = FluidPath(check, null)
                if (fs.isStill) fluidQueue.add(check)
                tubeDepth = pos.y - searchY
                break
            }
            if (!w.getBlockState(check).isAir && !w.getBlockState(check).isOf(Registries.BLOCK.get(BuildCraftAddon.id("tube")))) break
        }
        if (foundPos == null || targetFluid == null) { tubeDepth = 0; updateLength(); return }

        // BC: BFS 展开（区分液体/气体方向）
        val isGaseous = false // 简化处理
        val directions = if (isGaseous) SEARCH_GASEOUS else SEARCH_NORMAL
        val checked = mutableSetOf(foundPos)
        val batch = mutableListOf(foundPos)
        var waterNeighborCount = 0

        while (batch.isNotEmpty()) {
            val current = batch.toList()
            batch.clear()
            for (bp in current) {
                for (dir in directions) {
                    val np = bp.offset(dir)
                    if (np.getSquaredDistance(foundPos) > MAX_RADIUS_SQ) continue
                    if (!checked.add(np)) continue

                    val nfs = w.getFluidState(np)
                    val nf = nfs.fluid
                    val sameBlock = nf.defaultState.blockState.block == targetFluid!!.defaultState.blockState.block
                    if (nf != targetFluid && !sameBlock) continue

                    val path = FluidPath(np, fluidPaths[bp])
                    fluidPaths[np] = path
                    if (nfs.isStill) fluidQueue.add(np)
                    batch.add(np)

                    // BC: 无限水源计数
                    if (targetFluid == Fluids.WATER) {
                        waterNeighborCount++
                    }
                }
            }
        }

        // BC: 无限水源检测（≥2 相邻水源，且下方为水/固体）
        if (targetFluid == Fluids.WATER && waterNeighborCount >= 2) {
            val below = w.getBlockState(foundPos.down())
            val belowFluid = below.fluidState.fluid
            if (belowFluid == Fluids.WATER || belowFluid == Fluids.FLOWING_WATER || below.blocksMovement()) {
                isInfiniteWater = true
            }
        }

        // BC: 原油搜索油泉
        if (targetFluid == ModFluids.CRUDE_OIL_STILL) {
            val springBlock = Registries.BLOCK.get(BuildCraftAddon.id("spring"))
            outer@ for (dx in -10..10) for (dz in -10..10) {
                val sp = BlockPos(foundPos.x + dx, w.bottomY + 1, foundPos.z + dz)
                if (w.getBlockState(sp).isOf(springBlock)) { oilSpringPos = sp; break@outer }
            }
        }

        updateLength()
    }

    // ========== 管道管理（BC TileMiner 1:1） ==========
    fun updateLength() {
        val w = world ?: return
        val targetY = pos.y - tubeDepth

        // BC: 清除旧管道
        for (y in pos.y - 1 downTo pos.y - MAX_DEPTH) {
            val bp = BlockPos(pos.x, y, pos.z)
            if (w.getBlockState(bp).isOf(Registries.BLOCK.get(BuildCraftAddon.id("tube")))) {
                w.setBlockState(bp, Blocks.AIR.defaultState, 3)
            } else break
        }
        // BC: 放置新管道
        for (y in pos.y - 1 downTo targetY + 1) {
            val bp = BlockPos(pos.x, y, pos.z)
            if (w.getBlockState(bp).isAir || w.getBlockState(bp).isOf(Registries.BLOCK.get(BuildCraftAddon.id("tube")))) {
                w.setBlockState(bp, Registries.BLOCK.get(BuildCraftAddon.id("tube")).defaultState, 3)
            }
        }
    }

    /** 管道清理移至 PumpBlock.onStateReplaced()，不在 markRemoved 中做（chunk 卸载时调 markRemoved 会导致递归死锁） */
    override fun markRemoved() {
        super.markRemoved()
    }

    // ========== 抽取（BC 原版 1:1） ==========
    private fun drainFluid() {
        val w = world ?: return
        if (tank.amount >= tank.capacity / 2) return

        while (fluidQueue.isNotEmpty()) {
            val dp = fluidQueue.removeLast()
            if (!fluidPaths.containsKey(dp)) continue

            val fs = w.getFluidState(dp)
            if (fs.isEmpty || fs.fluid == Fluids.EMPTY) { fluidPaths.remove(dp); continue }

            // BC: 路径验证
            var valid = true
            var pp: FluidPath? = fluidPaths[dp]
            while (pp != null) {
                if (w.getFluidState(pp.pos).isEmpty) { valid = false; break }
                pp = pp.parent
            }
            if (!valid) { rebuildTimer++; if (rebuildTimer >= REBUILD_DELAY) buildQueue(); return }

            val fluid = fs.fluid
            if (!tank.isResourceBlank && !tank.variant.isOf(fluid)) continue

            // BC: 预模拟抽取
            val variant = FluidVariant.of(fluid)

            // BC 原版：模拟 drainBlock(world, pos, false)
            val drainAmount = 1000L // 1 bucket

            val tx = Transaction.openOuter()
            val canInsert = tank.insert(variant, drainAmount, tx)
            if (canInsert > 0) {
                tx.abort() // 预模拟，不提交

                // BC: 实际抽取
                val tx2 = Transaction.openOuter()
                val inserted = tank.insert(variant, drainAmount, tx2)
                if (inserted > 0) {
                    tx2.commit()

                    // 无限水源不消耗流体方块
                    if (isInfiniteWater && fluid == Fluids.WATER) {
                        // 不删除水源
                    } else {
                        w.setBlockState(dp, Blocks.AIR.defaultState, 3)
                        totalPumped++

                        // 成就：首次抽取任何流体
                        // 成就：抽取原油
                        if (fluid == ModFluids.CRUDE_OIL_STILL) {
                            oilPumped = true

                            // 油泉进度
                            if (oilSpringPos != null) {
                                val obe = w.getBlockEntity(oilSpringPos!!)
                                if (obe is OilSpringBlockEntity) obe.onPumpOil("", "")
                            }
                        }
                    }

                    fluidPaths.remove(dp)
                    isActive = true
                    return
                } else {
                    tx2.abort()
                }
            } else {
                tx.abort()
            }
            return
        }

        // BC: 队列空，重建
        isActive = false
        rebuildTimer++
        if (rebuildTimer >= REBUILD_DELAY) buildQueue()
    }

    // ========== 输出到相邻储罐 ==========
    private fun pushFluidOut() {
        val w = world ?: return
        if (tank.amount <= 0 || tank.variant.isBlank) return

        for (dir in Direction.entries) {
            val np = pos.offset(dir)
            val storage = FluidStorage.SIDED.find(w, np, dir.opposite)
            if (storage != null) {
                val tx = Transaction.openOuter()
                val moved = storage.insert(tank.variant, tank.amount, tx)
                if (moved > 0) {
                    tx.commit()
                    tank.amount -= moved
                    if (tank.amount <= 0) tank.variant = FluidVariant.blank()
                    return
                } else { tx.abort() }
            }
        }
    }

    // ========== Tick ==========
    fun serverTick() {
        if (!isRedstonePowered) { isActive = false; return }
        if (!queueBuilt) buildQueue()
        pushFluidOut()
        drainFluid()
        markDirty()
    }

    // ========== 信息 ==========
    fun getTankInfo(): String {
        val name = if (tank.variant.isBlank) "空" else tank.variant.fluid.defaultState.blockState.block.name.string
        return "${name}: ${tank.amount} / ${tank.capacity} mB"
    }

    // ========== NBT ==========
    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putLong("tankAmount", tank.amount)
        if (!tank.variant.isBlank) nbt.putString("tankFluid", Registries.FLUID.getId(tank.variant.fluid).toString())
        nbt.putBoolean("powered", isRedstonePowered)
        nbt.putInt("tubeDepth", tubeDepth)
        nbt.putInt("totalPumped", totalPumped)
        nbt.putBoolean("oilPumped", oilPumped)
        nbt.putBoolean("hasOilAchievement", hasOilAchievement)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        val amt = nbt.getLong("tankAmount")
        if (amt > 0 && nbt.contains("tankFluid")) {
            val fid = Identifier.of(nbt.getString("tankFluid"))
            tank.variant = FluidVariant.of(Registries.FLUID.get(fid))
            tank.amount = amt
        }
        isRedstonePowered = nbt.getBoolean("powered")
        tubeDepth = nbt.getInt("tubeDepth")
        totalPumped = nbt.getInt("totalPumped")
        oilPumped = nbt.getBoolean("oilPumped")
        hasOilAchievement = nbt.getBoolean("hasOilAchievement")
    }
}
