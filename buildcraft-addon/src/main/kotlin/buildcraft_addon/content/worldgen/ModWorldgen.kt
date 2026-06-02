package buildcraft_addon.content.worldgen

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.block.SpringBlock
import buildcraft_addon.content.block.SpringType
import buildcraft_addon.content.blockentity.OilSpringBlockEntity
import buildcraft_addon.content.fluid.ModFluids
import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.fluid.Fluids
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.Heightmap
import net.minecraft.world.StructureWorldAccess
import net.minecraft.world.gen.feature.ConfiguredFeature
import net.minecraft.world.gen.feature.DefaultFeatureConfig
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.PlacedFeature
import net.minecraft.world.gen.feature.util.FeatureContext

object ModWorldgen {

    lateinit var WATER_SPRING_FEATURE: Feature<DefaultFeatureConfig>
    lateinit var OIL_WELL_FEATURE: Feature<DefaultFeatureConfig>

    val WATER_SPRING_CONFIGURED_KEY: RegistryKey<ConfiguredFeature<*, *>> =
        RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, BuildCraftAddon.id("water_spring"))
    val OIL_WELL_CONFIGURED_KEY: RegistryKey<ConfiguredFeature<*, *>> =
        RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, BuildCraftAddon.id("oil_well"))

    val WATER_SPRING_PLACED_KEY: RegistryKey<PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, BuildCraftAddon.id("water_spring"))
    val OIL_WELL_PLACED_KEY: RegistryKey<PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, BuildCraftAddon.id("oil_well"))

    fun register() {
        WATER_SPRING_FEATURE = Registry.register(
            Registries.FEATURE, BuildCraftAddon.id("water_spring"),
            WaterSpringFeature(DefaultFeatureConfig.CODEC)
        )
        OIL_WELL_FEATURE = Registry.register(
            Registries.FEATURE, BuildCraftAddon.id("oil_well"),
            OilWellFeature(DefaultFeatureConfig.CODEC)
        )
        BuildCraftAddon.LOGGER.info("BuildCraft worldgen registered")
    }
}

// ========== Water Spring ==========

class WaterSpringFeature(codec: Codec<DefaultFeatureConfig>) : Feature<DefaultFeatureConfig>(codec) {

    override fun generate(context: FeatureContext<DefaultFeatureConfig>): Boolean {
        val world = context.world
        val origin = context.origin
        val random = context.random
        val chunkPos = ChunkPos(origin)

        // BC: 2.5% per chunk
        if (random.nextFloat() > 0.025f) return false

        val x = chunkPos.startX + random.nextInt(16)
        val z = chunkPos.startZ + random.nextInt(16)

        // 1.18+ 基岩层在 world.bottomY，向上扫 5 格
        for (y in world.bottomY..(world.bottomY + 4)) {
            val pos = BlockPos(x, y, z)
            if (world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                val springPos = pos.up()
                val springBlock = Registries.BLOCK.get(BuildCraftAddon.id("spring"))
                world.setBlockState(springPos,
                    springBlock.defaultState.with(SpringBlock.SPRING_TYPE, SpringType.WATER),
                    Block.NOTIFY_ALL)

                for (j in y + 2 until world.topY) {
                    val columnPos = BlockPos(x, j, z)
                    if (world.isAir(columnPos)) break
                    world.setBlockState(columnPos, Fluids.WATER.defaultState.blockState, Block.NOTIFY_ALL)
                }
                return true
            }
        }
        return false
    }
}

// ========== Oil Well - 1:1 BC 原版 ==========

class OilWellFeature(codec: Codec<DefaultFeatureConfig>) : Feature<DefaultFeatureConfig>(codec) {

    companion object {
        private val MAGIC_GEN_NUMBER = 0xD046B4E40C7D07CFuL.toLong()
        private const val MAX_CHUNK_RADIUS = 5

        // 排除的维度（BC 原版）
        private val excludedDimensionIds = setOf(-1, 1) // 下界、末地

        // 概率（BC 原版默认值，转为绝对值）
        private val LARGE_PROB = 0.0004
        private val MEDIUM_PROB = 0.001
        private val LAKE_PROB = 0.02

        // 喷口高度（BC 原版默认值）
        private const val LARGE_SPOUT_MIN = 10
        private const val LARGE_SPOUT_MAX = 20
        private const val SMALL_SPOUT_MIN = 6
        private const val SMALL_SPOUT_MAX = 12
    }

    private enum class GenType { LARGE, MEDIUM, LAKE, NONE }

    override fun generate(context: FeatureContext<DefaultFeatureConfig>): Boolean {
        val world = context.world
        val origin = context.origin
        val rand = context.random

        val chunkX = origin.x shr 4
        val chunkZ = origin.z shr 4

        // BC: 排除维度（下界 -1、末地 1）
        // 通过 FeatureContext 传入的 StructureWorldAccess 运行时为 ServerWorld
        if (world is net.minecraft.world.World) {
            val dimId = world.registryKey.value
            if (dimId.toString().contains("the_nether") || dimId.toString().contains("the_end")) return false
        }

        for (cdx in -MAX_CHUNK_RADIUS..MAX_CHUNK_RADIUS) {
            for (cdz in -MAX_CHUNK_RADIUS..MAX_CHUNK_RADIUS) {
                generateAt(world, chunkX + cdx, chunkZ + cdz)
            }
        }
        return true
    }

    private fun generateAt(world: StructureWorldAccess, cx: Int, cz: Int) {
        // BC: 确定性随机（同种子同坐标必出相同结构）
        // StructureWorldAccess 运行时为 ServerWorld，可安全访问种子
        val wSeed = if (world is net.minecraft.world.World) world.seed else 0L
        val chunkSeed = cx.toLong() * 341873128712L + cz.toLong() * 132897987541L
        val rand = Random.create(wSeed xor chunkSeed xor MAGIC_GEN_NUMBER)

        val x = cx * 16 + 8 + rand.nextInt(16)
        val z = cz * 16 + 8 + rand.nextInt(16)

        // BC: 排除末地中心岛
        val dim = world.dimension
        if (dim == net.minecraft.world.World.END && (Math.abs(x) < 1200 || Math.abs(z) < 1200)) return

        // BC: 按生物群系计算概率加成
        val biomeKey = world.getBiome(BlockPos(x, 0, z)).key
        var bonus = 1.0
        // 简单内置生物群系加成：沙漠/恶地/海洋等
        if (biomeKey.isPresent) {
            val id = biomeKey.get().value.toString()
            // excessiveBiomes: 沙漠、恶地等 → x30
            if (id.contains("desert") || id.contains("badlands") || id.contains("mesa")) {
                bonus *= 30.0
            }
            // surfaceDepositBiomes: 海洋、沼泽等 → x3
            if (id.contains("ocean") || id.contains("swamp") || id.contains("mangrove")) {
                bonus *= 3.0
            }
        }

        // BC: 概率判定
        val type = when {
            rand.nextDouble() < LARGE_PROB * bonus -> GenType.LARGE
            rand.nextDouble() < MEDIUM_PROB * bonus -> GenType.MEDIUM
            rand.nextDouble() < LAKE_PROB * bonus -> GenType.LAKE
            else -> return
        }

        val chunkMin = BlockPos(cx * 16, 0, cz * 16)
        val chunkMax = chunkMin.add(15, world.height - 1, 15)

        // BC: 结构参数
        val lakeRadius: Int
        val tendrilRadius: Int
        when (type) {
            GenType.LARGE -> { lakeRadius = 4; tendrilRadius = 25 + rand.nextInt(20) }
            GenType.LAKE  -> { lakeRadius = 6; tendrilRadius = 25 + rand.nextInt(20) }
            else          -> { lakeRadius = 2; tendrilRadius = 5 + rand.nextInt(10) }
        }

        // 统计本井的所有油块数量（用于 Spring.totalSources）
        var totalOilCount = 0

        // BC: 地表触须
        val pattern = createTendrilPattern(rand, lakeRadius, tendrilRadius)
        val startPos = BlockPos(x, 62, z).add(-tendrilRadius, 0, -tendrilRadius)
        val depth = if (rand.nextDouble() < 0.5) 1 else 2
        totalOilCount += generateTendril(world, pattern, startPos, depth, chunkMin, chunkMax)

        if (type != GenType.LAKE) {
            val wellY = 20 + rand.nextInt(10)

            // BC: 地下油藏球体
            val sphereRadius = if (type == GenType.LARGE) 8 + rand.nextInt(9) else 4 + rand.nextInt(4)
            totalOilCount += generateSphere(world, BlockPos(x, wellY, z), sphereRadius, chunkMin, chunkMax)

            // BC: 喷口
            if (type == GenType.LARGE) {
                val spoutHeight = if (LARGE_SPOUT_MAX > LARGE_SPOUT_MIN) {
                    LARGE_SPOUT_MIN + rand.nextInt(LARGE_SPOUT_MAX - LARGE_SPOUT_MIN)
                } else LARGE_SPOUT_MIN
                totalOilCount += generateSpout(world, BlockPos(x, wellY, z), spoutHeight, 1, chunkMin, chunkMax)

                // BC: 管道到基岩上方（1.18+ 基岩在 bottomY，油管延伸到 bottomY+2）
                val tubeBottom = world.bottomY + 2
                totalOilCount += generateTube(world, BlockPos(x, tubeBottom, z), wellY - tubeBottom, 1, chunkMin, chunkMax)

                // BC: Spring 方块在基岩上方（1.18+ 基岩在 bottomY）
                val springY = world.bottomY + 1
                val springPos = BlockPos(x, springY, z)
                if (inChunk(springPos, chunkMin, chunkMax)) {
                    placeOilSpring(world, springPos, totalOilCount)
                }
            } else {
                val spoutHeight = if (SMALL_SPOUT_MAX > SMALL_SPOUT_MIN) {
                    SMALL_SPOUT_MIN + rand.nextInt(SMALL_SPOUT_MAX - SMALL_SPOUT_MIN)
                } else SMALL_SPOUT_MIN
                totalOilCount += generateSpout(world, BlockPos(x, wellY, z), spoutHeight, 0, chunkMin, chunkMax)
            }
        }
    }

    // ========== 触须生成（BC 原版 1:1） ==========

    private fun createTendrilPattern(rand: Random, lakeRadius: Int, radius: Int): Array<BooleanArray> {
        val diameter = radius * 2 + 1
        val pattern = Array(diameter) { BooleanArray(diameter) }
        val cx = radius
        val cz = radius

        // BC: 中心圆形湖
        for (dx in -lakeRadius..lakeRadius) {
            for (dz in -lakeRadius..lakeRadius) {
                pattern[cx + dx][cz + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius
            }
        }

        // BC: 向外延伸触须
        for (w in 1 until radius) {
            val proba = (radius - w + 4).toFloat() / (radius + 4).toFloat()

            // BC: 四正向
            fillPattern(rand, proba, cx, cz + w, pattern)
            fillPattern(rand, proba, cx, cz - w, pattern)
            fillPattern(rand, proba, cx + w, cz, pattern)
            fillPattern(rand, proba, cx - w, cz, pattern)

            // BC: 对角线方向（原文 8 方向填充）
            for (i in 1..w) {
                fillPattern(rand, proba, cx + i, cz + w, pattern)
                fillPattern(rand, proba, cx + i, cz - w, pattern)
                fillPattern(rand, proba, cx + w, cz + i, pattern)
                fillPattern(rand, proba, cx - w, cz + i, pattern)
                fillPattern(rand, proba, cx - i, cz + w, pattern)
                fillPattern(rand, proba, cx - i, cz - w, pattern)
                fillPattern(rand, proba, cx + w, cz - i, pattern)
                fillPattern(rand, proba, cx - w, cz - i, pattern)
            }
        }
        return pattern
    }

    private fun fillPattern(rand: Random, proba: Float, x: Int, z: Int, pattern: Array<BooleanArray>) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isPatternSet(pattern, x, z - 1) ||
                    isPatternSet(pattern, x, z + 1) ||
                    isPatternSet(pattern, x - 1, z) ||
                    isPatternSet(pattern, x + 1, z)
        }
    }

    private fun isPatternSet(pattern: Array<BooleanArray>, x: Int, z: Int): Boolean {
        if (x !in 0 until pattern.size) return false
        if (z !in 0 until pattern[x].size) return false
        return pattern[x][z]
    }

    private fun generateTendril(
        world: StructureWorldAccess, pattern: Array<BooleanArray>,
        startPos: BlockPos, depth: Int, min: BlockPos, max: BlockPos
    ): Int {
        var count = 0
        for (px in 0 until pattern.size) {
            for (pz in 0 until pattern[px].size) {
                if (!pattern[px][pz]) continue
                val wp = startPos.add(px, 0, pz)
                if (!inChunk(wp, min, max)) continue
                val sy = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, wp.x, wp.z) - 1
                if (sy <= 0) continue

                // BC: 清除地表上方空气
                for (dy in 0..4) {
                    val ap = BlockPos(wp.x, sy + dy, wp.z)
                    if (inChunk(ap, min, max)) {
                        world.setBlockState(ap, Blocks.AIR.defaultState, Block.NOTIFY_LISTENERS)
                    }
                }
                // BC: 放置原油
                for (dy in 0 until depth) {
                    val op = BlockPos(wp.x, sy - dy, wp.z)
                    if (inChunk(op, min, max) && canReplaceForOil(world, op)) {
                        world.setBlockState(op, ModFluids.CRUDE_OIL_BLOCK.defaultState, Block.NOTIFY_LISTENERS)
                        count++
                    }
                }
            }
        }
        return count
    }

    // ========== 球体（BC 原版 1:1） ==========

    private fun generateSphere(world: StructureWorldAccess, center: BlockPos, radius: Int, min: BlockPos, max: BlockPos): Int {
        var count = 0
        val radiusSq = radius * radius + 0.01
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        val pos = center.add(dx, dy, dz)
                        if (inChunk(pos, min, max) && canReplaceForOil(world, pos)) {
                            world.setBlockState(pos, ModFluids.CRUDE_OIL_BLOCK.defaultState, Block.NOTIFY_LISTENERS)
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    // ========== 喷口（BC 原版 1:1） ==========

    private fun generateSpout(world: StructureWorldAccess, start: BlockPos, height: Int, radius: Int, min: BlockPos, max: BlockPos): Int {
        // BC: 找到地表高度
        var surfaceY = start.y
        for (y in start.y until world.height) {
            val pos = BlockPos(start.x, y, start.z)
            val state = world.getBlockState(pos)
            if (!state.isAir && !state.fluidState.isEmpty) break
            if (state.blocksMovement()) { surfaceY = y; break }
        }

        var count = 0
        // BC: 从球体到地表的柱体
        count += generateTube(world, start, surfaceY - start.y, radius, min, max)

        // BC: 地表以上锥形喷口
        var base = BlockPos(start.x, surfaceY, start.z)
        for (r in radius downTo 0) {
            count += generateTube(world, base, height, r, min, max)
            base = base.add(0, height, 0)
        }
        return count
    }

    // ========== 管道/柱体 ==========

    private fun generateTube(world: StructureWorldAccess, start: BlockPos, length: Int, radius: Int, min: BlockPos, max: BlockPos): Int {
        var count = 0
        val rsq = radius * radius
        for (dy in 0..length) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx * dx + dz * dz <= rsq) {
                        val pos = start.add(dx, dy, dz)
                        if (inChunk(pos, min, max) && canReplaceForOil(world, pos)) {
                            world.setBlockState(pos, ModFluids.CRUDE_OIL_BLOCK.defaultState, Block.NOTIFY_LISTENERS)
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    // ========== Spring 放置 ==========

    private fun placeOilSpring(world: StructureWorldAccess, pos: BlockPos, totalOilCount: Int) {
        val springBlock = Registries.BLOCK.get(BuildCraftAddon.id("spring"))
        world.setBlockState(pos, springBlock.defaultState.with(SpringBlock.SPRING_TYPE, SpringType.OIL), Block.NOTIFY_ALL)

        // BC: 设置 totalSources 供抽油泵系统使用
        val be = world.getBlockEntity(pos)
        if (be is OilSpringBlockEntity) {
            be.totalSources = totalOilCount
        }
    }

    // ========== 工具方法 ==========

    private fun canReplaceForOil(world: StructureWorldAccess, pos: BlockPos): Boolean {
        val s = world.getBlockState(pos)
        return s.isAir || s.isOf(Blocks.STONE) || s.isOf(Blocks.DEEPSLATE) ||
                s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.SAND) || s.isOf(Blocks.DIRT) ||
                s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.SANDSTONE)
    }

    private fun inChunk(pos: BlockPos, min: BlockPos, max: BlockPos): Boolean =
        pos.x in min.x..max.x && pos.z in min.z..max.z && pos.y in min.y..max.y
}
