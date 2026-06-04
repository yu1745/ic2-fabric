package buildcraft_addon.content.blockentity

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.block.PumpBlock
import buildcraft_addon.content.fluid.ModFluids
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.block.transmission.pullKuFromNeighbors
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.*

/**
 * 液泵
 *
 * 算法（自研，已脱离 BC 1:1）：
 * 1. 向下扫第一个流体方块 = foundPos（锚点）
 * 2. buildQueue：从 foundPos 出发 6 邻接 BFS，沿同流体方块走（半径 64 平方剪枝）
 *    收集到的方块按"到 foundPos 的曼哈顿距离降序"入 fluidQueue
 *    复杂度 O(N)，N = 实际流体方块数（油井球体 ~4000）
 * 3. 抽液每 tick 1 桶：
 *    - fluidQueue.removeFirst() 拿一格 dp（远端先抽 → 团块逐渐收缩）
 *    - 仅当 dp 邻居流体数 ≥ 2 时才走 A* 严格验证连通性
 *      （远端表面节点天然孤立/不需 A*，可省下绝大部分搜索）
 *      中间已被抽断的孤立块 → A* 失败 → 跳过
 *    - 抽走 dp，从 fluidSet 移除
 * 4. 泵不主动泵出，仅暴露 FluidStorage 给外部机器/管道主动抽走。
 * 5. foundPos 自身在 fluidSet 里只有"只剩它一格"时才被抽走，避免油田永远剩 1 格
 *
 * 视觉效果：油井表面先收缩 + 内部按 A* 验证顺序塌方 → 玩家看到液体随泵抽取收缩
 * 性能：buildQueue O(N) 仅走真实流体方块；A* 仅对"可疑"（邻居 ≥ 2）节点触发
 */
@ModBlockEntity(block = PumpBlock::class)
class PumpBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), IKineticMachinePort {

    constructor(pos: BlockPos, state: BlockState) : this(PumpBlockEntity::class.type(), pos, state)

    companion object {
        const val TANK_CAPACITY = 16 * FluidConstants.BUCKET
        const val MAX_RADIUS_SQ = 64 * 64
        const val MAX_DEPTH = 512
        const val ENERGY_COST_PER_TICK = 128
        // A* 单次搜索最大步数（防止在超大空腔里跑死）
        private const val A_STAR_MAX_STEPS = 512
        // A* 单次搜索最大候选数（开放集硬上限）
        private const val A_STAR_MAX_OPEN = 2048

        private val TUBE_BLOCK by lazy { Registries.BLOCK.get(BuildCraftAddon.id("tube")) }
        private val SPRING_BLOCK by lazy { Registries.BLOCK.get(BuildCraftAddon.id("spring")) }

        // 3D 6 邻接方向（buildQueue 扫描 + A* 都用）
        private val ALL_DIRS = arrayOf(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

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
    }

    fun getFluidStorageForSide(): Storage<FluidVariant> = tank

    // ========== 状态 ==========
    var isRedstonePowered = false

    // 流体可达集：buildQueue 阶段收半径内所有同流体方块
    // 抽液时按"到锚点距离降序"入队（队首 = 最远）→ removeFirst 拿最远端
    private val fluidSet = HashSet<BlockPos>(512)
    private val fluidQueue = ArrayDeque<BlockPos>(512)

    private var targetFluid: Fluid? = null
    private var foundPos: BlockPos? = null  // 锚点 = 泵下方第一个流体

    var tubeDepth = 0
        private set

    // 客户端平滑插值
    var renderTubeDepth = 0.0
        private set
    private var lastRenderTubeDepth = 0.0
    private var isInfiniteWater = false
    var isActive = false
    var isBlocked = false
    private var receivedKuThisTick: Int = 0

    // === IKineticMachinePort ===
    override fun canInputKuFrom(side: Direction): Boolean = true

    override fun getKuCapacity(side: Direction): Int = ENERGY_COST_PER_TICK

    override fun getMaxInsertableKu(side: Direction): Int =
        if (canInputKuFrom(side)) maxOf(0, ENERGY_COST_PER_TICK - receivedKuThisTick) else 0

    override fun insertKu(side: Direction, amount: Int, simulate: Boolean): Int {
        if (!canInputKuFrom(side) || amount <= 0) return 0
        val space = maxOf(0, ENERGY_COST_PER_TICK - receivedKuThisTick)
        val accepted = minOf(amount, space)
        if (!simulate && accepted > 0) {
            receivedKuThisTick += accepted
            markDirty()
        }
        return accepted
    }
    private var totalPumped = 0
    private var oilPumped = false
    private var hasOilAchievement = false

    private var oilSpringPos: BlockPos? = null

    // ========== 红石 ==========
    fun checkRedstonePower() {
        val w = world ?: return
        val powered = w.isReceivingRedstonePower(pos)
        if (powered != isRedstonePowered) {
            isRedstonePowered = powered
            markDirty()
        }
    }

    // ========== 构建 ==========
    // 1. 向下扫第一个流体方块 = foundPos（锚点）
    // 2. 从 foundPos 出发做 6 邻接 BFS，沿同流体方块走，半径用平方剪枝
    //    复杂度 O(N)，N = 实际流体方块数（油井球体 ~4000），远低于原立方体暴力扫描
    // 3. BFS 后桶排序按距 foundPos 的曼哈顿距离降序填入 fluidQueue（队首 = 最远）
    //    → 抽液时 fluidQueue.removeFirst() 拿到的就是远端
    // 4. 抽液阶段对"邻居流体数 ≥ 2"的 dp 才走 A* 严格验证连通性
    fun buildQueue() {
        fluidSet.clear()
        fluidQueue.clear()
        targetFluid = null
        foundPos = null
        oilSpringPos = null
        isInfiniteWater = false
        val tubeBlock = TUBE_BLOCK

        val w = world ?: return
        var found: BlockPos? = null

        // 1. 向下扫第一个流体
        for (searchY in pos.y - 1 downTo w.bottomY) {
            if (pos.y - searchY > MAX_DEPTH) break
            val check = BlockPos(pos.x, searchY, pos.z)
            val fs = w.getFluidState(check)
            if (!fs.isEmpty) {
                found = check
                targetFluid = fs.fluid
                tubeDepth = pos.y - searchY
                break
            }
            val bs = w.getBlockState(check)
            if (!bs.isAir && !bs.isOf(tubeBlock)) break
        }
        if (found == null || targetFluid == null) {
            foundPos = null
            tubeDepth = 0
            updateLength()
            return
        }
        foundPos = found
        val target = targetFluid!!
        val targetBlock = target.defaultState.blockState.block

        // 2. 6 邻接 BFS 收集半径内所有同流体方块
        // 复杂度 O(N)，N = 实际流体方块数（油井球体 ~4000），远低于原立方体暴力扫描
        val foundX = found.x; val foundY = found.y; val foundZ = found.z
        val bfsQueue: ArrayDeque<BlockPos> = ArrayDeque(512)
        bfsQueue.addLast(found)
        fluidSet.add(found)
        while (bfsQueue.isNotEmpty()) {
            val cur = bfsQueue.removeFirst()
            for (dir in ALL_DIRS) {
                val np = cur.offset(dir)
                if (np in fluidSet) continue
                val dx = np.x - foundX; val dy = np.y - foundY; val dz = np.z - foundZ
                if (dx * dx + dy * dy + dz * dz > MAX_RADIUS_SQ) continue
                val nfs = w.getFluidState(np)
                if (nfs.isEmpty || nfs.fluid == Fluids.EMPTY) continue
                val nf = nfs.fluid
                if (nf !== target && nf.defaultState.blockState.block !== targetBlock) continue
                fluidSet.add(np)
                bfsQueue.addLast(np)
            }
        }

        // 2.5 把 fluidSet 里的方块按"到 foundPos 的曼哈顿距离降序"填入 fluidQueue
        // → 抽液时 fluidQueue.removeFirst() 拿到的就是最远端（远端先抽 → 团块逐渐收缩）
        // 桶排序：最大距离 ≤ 3*64 = 192，桶数固定、O(N) 时间
        // 距离从大到小出桶 → 队首 = 最远；抽液用 removeFirst 弹出最远端
        if (fluidSet.isNotEmpty()) {
            var maxDist = 0
            for (p in fluidSet) {
                val d = kotlin.math.abs(p.x - foundX) +
                        kotlin.math.abs(p.y - foundY) +
                        kotlin.math.abs(p.z - foundZ)
                if (d > maxDist) maxDist = d
            }
            val buckets = arrayOfNulls<ArrayList<BlockPos>>(maxDist + 1)
            for (p in fluidSet) {
                val d = kotlin.math.abs(p.x - foundX) +
                        kotlin.math.abs(p.y - foundY) +
                        kotlin.math.abs(p.z - foundZ)
                val b: ArrayList<BlockPos> = buckets[d] ?: ArrayList<BlockPos>(8).also { buckets[d] = it }
                b.add(p)
            }
            for (d in maxDist downTo 0) {
                buckets[d]?.let { bucket ->
                    for (p in bucket) fluidQueue.addLast(p)
                }
            }
        }

        // 无限水源检测
        if (target == Fluids.WATER) {
            var waterNeighborCount = 0
            for (dir in ALL_DIRS) {
                if (found.offset(dir) in fluidSet) waterNeighborCount++
            }
            if (waterNeighborCount >= 2) {
                val below = w.getBlockState(found.down())
                val belowFluid = below.fluidState.fluid
                if (belowFluid == Fluids.WATER || belowFluid == Fluids.FLOWING_WATER || below.blocksMovement()) {
                    isInfiniteWater = true
                }
            }
        }

        // 原油搜索油泉
        if (target == ModFluids.CRUDE_OIL_STILL) {
            val springBlock = SPRING_BLOCK
            outer1@ for (dx in -10..10) for (dz in -10..10) {
                val sp = BlockPos(found.x + dx, w.bottomY + 1, found.z + dz)
                if (w.getBlockState(sp).isOf(springBlock)) { oilSpringPos = sp; break@outer1 }
            }
        }

        updateLength()
    }

    // ========== 抽取 ==========
    // 泵不主动泵出（仅 FluidStorage 暴露给外部机器/管道主动抽走）。
    // 每 tick 抽 1 桶：
    //   1. fluidQueue.removeFirst() 拿一格 dp（远端先抽 → 桶排序按距离降序入队，队首 = 最远）
    //   2. A* 验证 dp 是否仍能通过流体方块路径连回 foundPos
    //      （已经被中间抽断的不连通方块 → A* 失败 → 跳过）
    //   3. 抽走 dp（不连通方块跳过）
    //   4. 从 fluidSet 移除
    //   5. foundPos 自身在 fluidSet 里只有"只剩它一格"时才被抽走，避免油田永远剩 1 格
    private fun drainFluid() {
        val w = world ?: return
        if (tank.amount >= tank.capacity) return
        val found = foundPos ?: return
        val target = targetFluid ?: return
        val targetBlock = target.defaultState.blockState.block

        while (fluidQueue.isNotEmpty()) {
            val dp = fluidQueue.removeFirst()
            if (dp !in fluidSet) continue
            // foundPos 默认不抽（锚点；buildQueue 重建才更新）
            // 但如果 fluidSet 里只剩 foundPos 一格 → 允许抽走，避免油田永远剩 1 格
            // 跳过后必须 addLast 放回队尾，否则锚点被永久丢弃
            // → 等 fluidSet.size == 1 时再被弹出就能抽走
            if (dp == found && fluidSet.size > 1) {
                fluidQueue.addLast(dp)
                continue
            }

            val fs = w.getFluidState(dp)
            if (fs.isEmpty || fs.fluid == Fluids.EMPTY) {
                fluidSet.remove(dp)
                continue
            }
            val fluid = fs.fluid
            if (nf_check(fluid, target, targetBlock).not()) continue
            if (!tank.isResourceBlank && !tank.variant.isOf(fluid)) continue

            // 优化：远端节点（邻居流体数 ≤ 1）天然是边界/表面，dp 即将与主体断开
            // 这种情况 A* 必然失败（从表面走到 foundPos 至少要穿过整团），直接视为连通放行
            // 球状油田从外向内剥离时 90%+ 节点属于此类，可省下绝大部分 A*
            // 保险起见：邻居数 ≥ 2 才走严格 A*
            val neighborFluidCount = countFluidNeighbors(dp)
            if (neighborFluidCount >= 2) {
                if (!aStarPathExists(w, dp, found, target, targetBlock)) {
                    // 不连通（孤立块）→ 跳过
                    continue
                }
            }

            // 容量检查
            if (tank.amount + FluidConstants.BUCKET > tank.capacity) {
                // 容量满：把 dp 放回队列尾部（下次重试）
                fluidQueue.addLast(dp)
                return
            }

            val variant = FluidVariant.of(fluid)
            val tx = Transaction.openOuter()
            val inserted = tank.insert(variant, FluidConstants.BUCKET, tx)
            if (inserted <= 0) { tx.abort(); return }
            tx.commit()

            if (isInfiniteWater && fluid == Fluids.WATER) {
                // 无限水源不动方块
            } else {
                w.setBlockState(dp, Blocks.AIR.defaultState, Block.NOTIFY_ALL)
                totalPumped++

                if (fluid == ModFluids.CRUDE_OIL_STILL) {
                    oilPumped = true
                    val sp = oilSpringPos
                    if (sp != null) {
                        val obe = w.getBlockEntity(sp)
                        if (obe is OilSpringBlockEntity) obe.onPumpOil("", "")
                    }
                }
            }

            fluidSet.remove(dp)
            // 如果刚抽走的是 foundPos → 清空锚点，下次 tick 触发 buildQueue 重建
            if (dp == found) {
                foundPos = null
                tubeDepth = 0
            }
            return
        }
    }

    private fun nf_check(fluid: Fluid, target: Fluid, targetBlock: net.minecraft.block.Block): Boolean {
        return fluid === target || fluid.defaultState.blockState.block === targetBlock
    }

    /**
     * 统计 dp 周围 6 邻接中在 fluidSet 里的流体方块数。
     * 用于 drainFluid 优化：远端边界节点（≤1 邻居）天然孤立/不需 A* 验证。
     */
    private fun countFluidNeighbors(dp: BlockPos): Int {
        var count = 0
        for (dir in ALL_DIRS) {
            if (dp.offset(dir) in fluidSet) count++
        }
        return count
    }

    // ========== A* 连通性验证 ==========
    // 从 start 出发，走 6 方向流体方块，找回到 goal（必须严格命中 goal 才算连通）
    // 纯连通性检查，不修改任何状态；找到返回 true，否则 false
    private fun aStarPathExists(
        w: World, start: BlockPos, goal: BlockPos,
        target: Fluid, targetBlock: net.minecraft.block.Block
    ): Boolean {
        if (start == goal) return true
        // A* 必须严格命中 goal：之前把"碰到任意 fluidSet 节点"也当连通是错误的
        // （fluidSet 是所有流体方块，命中它 ≈ 一步命中，几乎等于跳过验证）
        // 修正后：连通性由"沿流体方块走 6 邻接能否到达 goal"决定

        val open = java.util.PriorityQueue<AStarNode>(compareBy { it.f })
        val gScore = HashMap<BlockPos, Int>(64)
        val closed = HashSet<BlockPos>(64)
        val startG = 0
        gScore[start] = startG
        open.add(AStarNode(start, startG, startG + heuristic(start, goal)))

        var steps = 0
        while (open.isNotEmpty() && steps < A_STAR_MAX_STEPS) {
            steps++
            val cur = open.poll()
            val curPos = cur.pos

            // 过期节点识别：节点自带的 g 必须等于 gScore 中当前 g
            val curG = gScore[curPos] ?: continue
            if (cur.g != curG) continue
            // 已扩展过则跳过（标准 closed set）
            if (curPos in closed) continue
            closed.add(curPos)

            // 严格命中 goal 才算连通
            if (curPos == goal) return true

            for (dir in ALL_DIRS) {
                val np = curPos.offset(dir)
                val nfs = w.getFluidState(np)
                if (nfs.isEmpty || nfs.fluid == Fluids.EMPTY) continue
                val nf = nfs.fluid
                if (nf !== target && nf.defaultState.blockState.block !== targetBlock) continue

                val dx = np.x - foundPos!!.x; val dy = np.y - foundPos!!.y; val dz = np.z - foundPos!!.z
                if (dx * dx + dy * dy + dz * dz > MAX_RADIUS_SQ) continue

                // goal 直接相邻也允许
                if (np == goal) return true

                val tentativeG = curG + 1
                val prevG = gScore[np]
                if (prevG == null || tentativeG < prevG) {
                    gScore[np] = tentativeG
                    if (open.size >= A_STAR_MAX_OPEN) return false
                    open.add(AStarNode(np, tentativeG, tentativeG + heuristic(np, goal)))
                }
            }
        }
        return false
    }

    private fun heuristic(pos: BlockPos, goal: BlockPos): Int {
        return kotlin.math.abs(pos.x - goal.x) +
                kotlin.math.abs(pos.y - goal.y) +
                kotlin.math.abs(pos.z - goal.z)
    }

    private data class AStarNode(val pos: BlockPos, val g: Int, val f: Int)

    // ========== 管道管理 ==========
    fun updateLength() {
        val w = world ?: return
        val targetY = pos.y - tubeDepth
        val tubeBlock = TUBE_BLOCK

        for (y in pos.y - 1 downTo pos.y - MAX_DEPTH) {
            val bp = BlockPos(pos.x, y, pos.z)
            if (w.getBlockState(bp).isOf(tubeBlock)) {
                w.setBlockState(bp, Blocks.AIR.defaultState, Block.NOTIFY_ALL)
            } else break
        }
        for (y in pos.y - 1 downTo targetY + 1) {
            val bp = BlockPos(pos.x, y, pos.z)
            val bs = w.getBlockState(bp)
            if (bs.isAir || bs.isOf(tubeBlock)) {
                w.setBlockState(bp, tubeBlock.defaultState, Block.NOTIFY_ALL)
            }
        }
    }

    /** 管道清理移至 PumpBlock.onStateReplaced()，不在 markRemoved 中做 */
    override fun markRemoved() {
        super.markRemoved()
    }

    // ========== Tick ==========
    fun serverTick() {
        if (!isRedstonePowered) { isActive = false; isBlocked = false; return }
        val w = world ?: return
        receivedKuThisTick = 0
        pullKuFromNeighbors(w, pos, this)
        if (receivedKuThisTick < ENERGY_COST_PER_TICK) {
            isActive = false; isBlocked = false; return
        }
        isActive = true
        isBlocked = tank.amount >= tank.capacity
        val beforeDepth = tubeDepth

        // 首次 / foundPos 被破坏时重建
        if (foundPos == null || w.getBlockState(foundPos!!).isAir) {
            buildQueue()
        } else if (fluidQueue.isEmpty() && foundPos != null) {
            // 队列空：foundPos 还在但 fluidSet 都被抽完 → 重新扫半径内的流体
            // （一般意味着深处的油井层被抽完了，要重新锚定）
            buildQueue()
        }
        drainFluid()
        if (tubeDepth != beforeDepth) syncTubeDepthToClient()
        markDirty()
    }

    /** 服务端 tubeDepth 变化时主动推送更新包，客户端 readNbt 后会重置 renderTubeDepth */
    private fun syncTubeDepthToClient() {
        val w = world ?: return
        val sw = w as? net.minecraft.server.world.ServerWorld ?: return
        sw.chunkManager.markForUpdate(pos)
    }

    fun clientTick(deltaTick: Int) {
        val target = tubeDepth.toDouble()
        // 服务端 NBT 同步过来后 tubeDepth 突变，直接吸附避免从 0 缓慢爬升
        if (kotlin.math.abs(target - renderTubeDepth) > 1.0) {
            renderTubeDepth = target
            lastRenderTubeDepth = target
            return
        }
        lastRenderTubeDepth = renderTubeDepth
        if (kotlin.math.abs(renderTubeDepth - target) <= 0.01) {
            renderTubeDepth = target
        } else {
            renderTubeDepth += (target - renderTubeDepth) / 7.0
        }
    }

    fun getRenderTubeDepth(partialTick: Float): Double {
        return lastRenderTubeDepth * (1.0 - partialTick) + renderTubeDepth * partialTick
    }

    // ========== 信息 ==========
    fun getTankInfo(): String {
        val name = if (tank.variant.isBlank) "空" else tank.variant.fluid.defaultState.blockState.block.name.string
        return "${name}: ${tank.amount} / ${tank.capacity} mB"
    }

    // ========== NBT ==========
    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putLong("tankAmount", tank.amount)
        if (!tank.variant.isBlank) nbt.putString("tankFluid", Registries.FLUID.getId(tank.variant.fluid).toString())
        nbt.putBoolean("powered", isRedstonePowered)
        nbt.putBoolean("isActive", isActive)
        nbt.putBoolean("isBlocked", isBlocked)
        nbt.putInt("tubeDepth", tubeDepth)
        nbt.putInt("totalPumped", totalPumped)
        nbt.putBoolean("oilPumped", oilPumped)
        nbt.putBoolean("hasOilAchievement", hasOilAchievement)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        val amt = nbt.getLong("tankAmount")
        if (amt > 0 && nbt.contains("tankFluid")) {
            val fid = Identifier(nbt.getString("tankFluid"))
            tank.variant = FluidVariant.of(Registries.FLUID.get(fid))
            tank.amount = amt
        }
        isRedstonePowered = nbt.getBoolean("powered")
        isActive = nbt.getBoolean("isActive")
        isBlocked = nbt.getBoolean("isBlocked")
        tubeDepth = nbt.getInt("tubeDepth")
        totalPumped = nbt.getInt("totalPumped")
        oilPumped = nbt.getBoolean("oilPumped")
        hasOilAchievement = nbt.getBoolean("hasOilAchievement")
    }

    // ========== 客户端 NBT 同步 ==========
    // 客户端渲染需要 tubeDepth，必须把 NBT 同步到客户端。
    // 否则 PumpRenderer 收到的 tubeDepth 永远是 0，管道不显示。
    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)
}
