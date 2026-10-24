package buildcraft_addon.content.worldgen

import net.minecraft.core.registries.Registries

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.block.SpringBlock
import buildcraft_addon.content.block.SpringType
import buildcraft_addon.content.blockentity.OilSpringBlockEntity
import buildcraft_addon.content.fluid.ModFluids
import com.mojang.serialization.Codec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluids
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext

object ModWorldgen {

    lateinit var WATER_SPRING_FEATURE: Feature<NoneFeatureConfiguration>
    lateinit var OIL_WELL_FEATURE: Feature<NoneFeatureConfiguration>

    val WATER_SPRING_CONFIGURED_KEY: ResourceKey<ConfiguredFeature<*, *>> =
        ResourceKey.create(Registries.CONFIGURED_FEATURE, BuildCraftAddon.id("water_spring"))
    val OIL_WELL_CONFIGURED_KEY: ResourceKey<ConfiguredFeature<*, *>> =
        ResourceKey.create(Registries.CONFIGURED_FEATURE, BuildCraftAddon.id("oil_well"))

    val WATER_SPRING_PLACED_KEY: ResourceKey<PlacedFeature> =
        ResourceKey.create(Registries.PLACED_FEATURE, BuildCraftAddon.id("water_spring"))
    val OIL_WELL_PLACED_KEY: ResourceKey<PlacedFeature> =
        ResourceKey.create(Registries.PLACED_FEATURE, BuildCraftAddon.id("oil_well"))

    fun register() {
        WATER_SPRING_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE, BuildCraftAddon.id("water_spring"),
            WaterSpringFeature(NoneFeatureConfiguration.CODEC)
        )
        OIL_WELL_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE, BuildCraftAddon.id("oil_well"),
            OilWellFeature(NoneFeatureConfiguration.CODEC)
        )
        BuildCraftAddon.LOGGER.info("BuildCraft worldgen registered")
    }
}

// ========== Water Spring ==========

class WaterSpringFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val world = context.level()
        val origin = context.origin()
        val random = context.random()
        val chunkPos = ChunkPos(origin)

        // BC: 2.5% per chunk
        if (random.nextFloat() > 0.025f) return false

        val x = chunkPos.minBlockX + random.nextInt(16)
        val z = chunkPos.minBlockZ + random.nextInt(16)

        // 1.18+ 基岩层在 world.minBuildHeight，向上扫 5 格
        for (y in world.minBuildHeight..(world.minBuildHeight + 4)) {
            val pos = BlockPos(x, y, z)
            if (world.getBlockState(pos).`is`(Blocks.BEDROCK)) {
                val springPos = pos.above()
                val springBlock = BuiltInRegistries.BLOCK.get(BuildCraftAddon.id("spring"))
                world.setBlock(springPos,
                    springBlock.defaultBlockState().setValue(SpringBlock.SPRING_TYPE, SpringType.WATER),
                    Block.UPDATE_ALL)

                for (j in y + 2 until world.maxBuildHeight) {
                    val columnPos = BlockPos(x, j, z)
                    if (world.isEmptyBlock(columnPos)) break
                    world.setBlock(columnPos, Fluids.WATER.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL)
                }
                return true
            }
        }
        return false
    }
}

// ========== Oil Well - 1:1 BC 原版 ==========

class OilWellFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    companion object {
        private val MAGIC_GEN_NUMBER = 0xD046B4E40C7D07CFuL.toLong()
        private const val MAX_CHUNK_RADIUS = 5

        // BC 噪声斑块参数（GenLayerAddOilDesert / GenLayerAddOilOcean）
        private const val OIL_DESERT_SCALE = 0.001
        private const val OIL_DESERT_THRESHOLD = 0.7
        private const val OIL_OCEAN_SCALE = 0.0005
        private const val OIL_OCEAN_THRESHOLD = 0.9
        private const val OFFSET_RANGE = 500000

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

        // 噪声偏移量缓存（同世界种子同偏移）
        private var cachedNoiseSeed = Long.MIN_VALUE
        private var noiseXOff = 0
        private var noiseZOff = 0
    }

    private enum class GenType { LARGE, MEDIUM, LAKE, NONE }

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val world = context.level()
        val origin = context.origin()

        val chunkX = origin.x shr 4
        val chunkZ = origin.z shr 4

        // BC: 排除维度
        if (world is net.minecraft.world.level.Level) {
            val dimId = world.dimension().location()
            if (dimId.toString().contains("the_nether") || dimId.toString().contains("the_end")) return false
        }

        // 当前区块边界：所有 setBlock 限制在此范围内，消除 "far chunk" 警告
        val genMin = BlockPos(chunkX * 16, world.minBuildHeight, chunkZ * 16)
        val genMax = genMin.offset(15, world.height - 1, 15)

        for (cdx in -MAX_CHUNK_RADIUS..MAX_CHUNK_RADIUS) {
            for (cdz in -MAX_CHUNK_RADIUS..MAX_CHUNK_RADIUS) {
                val isCenter = cdx == 0 && cdz == 0
                generateAt(world, chunkX + cdx, chunkZ + cdz, genMin, genMax, isCenter)
            }
        }
        return true
    }

    private fun generateAt(world: WorldGenLevel, cx: Int, cz: Int, genMin: BlockPos, genMax: BlockPos, isCenter: Boolean = false) {
        // BC: 确定性随机（同种子同坐标必出相同结构）
        // WorldGenLevel 运行时为 ServerLevel，可安全访问种子
        val wSeed = if (world is net.minecraft.world.level.Level) world.seed else 0L
        val chunkSeed = cx.toLong() * 341873128712L + cz.toLong() * 132897987541L
        val rand = RandomSource.create(wSeed xor chunkSeed xor MAGIC_GEN_NUMBER)

        val x = cx * 16 + 8 + rand.nextInt(16)
        val z = cz * 16 + 8 + rand.nextInt(16)

        // BC: 排除末地中心岛
        val dim = (world as? net.minecraft.world.level.Level)?.dimension()
        if (dim == net.minecraft.world.level.Level.END && (Math.abs(x) < 1200 || Math.abs(z) < 1200)) return

        // BC: 按生物群系 + 噪声斑块计算概率加成
        val biomeKey = world.getBiome(BlockPos(x, 0, z)).unwrapKey()
        var bonus = 1.0
        var oilBiome = false // surfaceDepositBiomes (3x + 允许 LAKE)
        if (biomeKey.isPresent) {
            val id = biomeKey.get().location().toString()
            val isDesert = id.contains("desert") || id.contains("badlands") || id.contains("mesa")
            val isOcean = id.contains("ocean") || id.contains("swamp") || id.contains("mangrove")

            if (isDesert || isOcean) {
                // 缓存噪声偏移量（基于世界种子，同种子同偏移，匹配 BC GenLayerBiomeReplacer）
                if (cachedNoiseSeed != wSeed) {
                    val randOff = java.util.Random(wSeed)
                    noiseXOff = randOff.nextInt(OFFSET_RANGE) - OFFSET_RANGE / 2
                    noiseZOff = randOff.nextInt(OFFSET_RANGE) - OFFSET_RANGE / 2
                    cachedNoiseSeed = wSeed
                }

                if (isDesert) {
                    val noise = SimplexNoise.noise((x + noiseXOff) * OIL_DESERT_SCALE, (z + noiseZOff) * OIL_DESERT_SCALE)
                    if (noise > OIL_DESERT_THRESHOLD) bonus *= 30.0
                }
                if (isOcean) {
                    val noise = SimplexNoise.noise((x + noiseXOff) * OIL_OCEAN_SCALE, (z + noiseZOff) * OIL_OCEAN_SCALE)
                    if (noise > OIL_OCEAN_THRESHOLD) bonus *= 30.0
                }
            }
        }

        // BC: 概率判定
        val type = when {
            rand.nextDouble() < LARGE_PROB * bonus -> GenType.LARGE
            rand.nextDouble() < MEDIUM_PROB * bonus -> GenType.MEDIUM
            oilBiome && rand.nextDouble() < LAKE_PROB * bonus -> GenType.LAKE
            else -> return
        }

        if (isCenter) {
            val surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1
            BuildCraftAddon.LOGGER.info("OilWellFeature.generateAt() -> {} at chunk [{}, {}], center (x={}, y={}, z={})", type, cx, cz, x, surfaceY, z)
        }

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
        val startPos = BlockPos(x, 62, z).offset(-tendrilRadius, 0, -tendrilRadius)
        val depth = if (rand.nextDouble() < 0.5) 1 else 2
        totalOilCount += generateTendril(world, pattern, startPos, depth, genMin, genMax)

        if (type != GenType.LAKE) {
            // wellY = 距 world.minBuildHeight 的偏移（BC 原版底部为 Y=0，wellY 即绝对 Y）
            val wellY = 20 + rand.nextInt(10)

            // BC: 地下油藏球体
            val sphereRadius = if (type == GenType.LARGE) 8 + rand.nextInt(9) else 4 + rand.nextInt(4)
            totalOilCount += generateSphere(world, BlockPos(x, world.minBuildHeight + wellY, z), sphereRadius, genMin, genMax)

            // BC: 喷口
            if (type == GenType.LARGE) {
                val spoutHeight = if (LARGE_SPOUT_MAX > LARGE_SPOUT_MIN) {
                    LARGE_SPOUT_MIN + rand.nextInt(LARGE_SPOUT_MAX - LARGE_SPOUT_MIN)
                } else LARGE_SPOUT_MIN
                totalOilCount += generateSpout(world, BlockPos(x, world.minBuildHeight + wellY, z), spoutHeight, 1, genMin, genMax)

                // BC: 管道从底部+2 延伸到球体高度（wellY = 距离底部的偏移）
                val tubeBottom = world.minBuildHeight + 1
                totalOilCount += generateTube(world, BlockPos(x, tubeBottom, z), wellY, 1, genMin, genMax)

                // BC: Spring 方块在底部
                val springPos = BlockPos(x, world.minBuildHeight, z)
                if (inChunk(springPos, genMin, genMax)) {
                    placeOilSpring(world, springPos, totalOilCount)
                }
            } else {
                val spoutHeight = if (SMALL_SPOUT_MAX > SMALL_SPOUT_MIN) {
                    SMALL_SPOUT_MIN + rand.nextInt(SMALL_SPOUT_MAX - SMALL_SPOUT_MIN)
                } else SMALL_SPOUT_MIN
                totalOilCount += generateSpout(world, BlockPos(x, world.minBuildHeight + wellY, z), spoutHeight, 0, genMin, genMax)
            }
        }
    }

    // ========== 触须生成（BC 原版 1:1） ==========

    private fun createTendrilPattern(rand: RandomSource, lakeRadius: Int, radius: Int): Array<BooleanArray> {
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

    private fun fillPattern(rand: RandomSource, proba: Float, x: Int, z: Int, pattern: Array<BooleanArray>) {
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
        world: WorldGenLevel, pattern: Array<BooleanArray>,
        startPos: BlockPos, depth: Int, min: BlockPos, max: BlockPos
    ): Int {
        var count = 0
        for (px in 0 until pattern.size) {
            for (pz in 0 until pattern[px].size) {
                if (!pattern[px][pz]) continue
                val wp = startPos.offset(px, 0, pz)
                if (!inChunk(wp, min, max)) continue
                val topY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wp.x, wp.z) - 1
                if (topY <= 0) continue

                // BC: 清除地表上方空气
                for (dy in 0..4) {
                    val ap = BlockPos(wp.x, topY + dy, wp.z)
                    if (inChunk(ap, min, max)) {
                        world.setBlock(ap, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
                    }
                }
                // BC: 放置原油
                for (dy in 0 until depth) {
                    val op = BlockPos(wp.x, topY - dy, wp.z)
                    if (inChunk(op, min, max) && canReplaceForOil(world, op)) {
                        world.setBlock(op, ModFluids.CRUDE_OIL_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS)
                        count++
                    }
                }
            }
        }
        return count
    }

    // ========== 球体（BC 原版 1:1） ==========

    private fun generateSphere(world: WorldGenLevel, center: BlockPos, radius: Int, min: BlockPos, max: BlockPos): Int {
        var count = 0
        val radiusSq = radius * radius + 0.01
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        val pos = center.offset(dx, dy, dz)
                        if (inChunk(pos, min, max) && canReplaceForOil(world, pos)) {
                            world.setBlock(pos, ModFluids.CRUDE_OIL_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS)
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    // ========== 喷口（BC 原版 1:1） ==========

    private fun generateSpout(world: WorldGenLevel, start: BlockPos, height: Int, radius: Int, min: BlockPos, max: BlockPos): Int {
        // BC: 使用高度图找到地表（从顶向下，与 BC 原版一致）
        val surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, start.x, start.z) - 1
        if (surfaceY <= start.y) return 0

        var count = 0
        // BC: 从球体到地表的柱体
        count += generateTube(world, start, surfaceY - start.y, radius, min, max)

        // BC: 地表以上锥形喷口
        var base = BlockPos(start.x, surfaceY, start.z)
        for (r in radius downTo 0) {
            count += generateTube(world, base, height, r, min, max)
            base = base.offset(0, height, 0)
        }
        return count
    }

    // ========== 管道/柱体 ==========

    private fun generateTube(world: WorldGenLevel, start: BlockPos, length: Int, radius: Int, min: BlockPos, max: BlockPos): Int {
        var count = 0
        val rsq = radius * radius
        for (dy in 0..length) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx * dx + dz * dz <= rsq) {
                        val pos = start.offset(dx, dy, dz)
                        if (inChunk(pos, min, max) && canReplaceForOil(world, pos)) {
                            world.setBlock(pos, ModFluids.CRUDE_OIL_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS)
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    // ========== Spring 放置 ==========

    private fun placeOilSpring(world: WorldGenLevel, pos: BlockPos, totalOilCount: Int) {
        val springBlock = BuiltInRegistries.BLOCK.get(BuildCraftAddon.id("spring"))
        world.setBlock(pos, springBlock.defaultBlockState().setValue(SpringBlock.SPRING_TYPE, SpringType.OIL), Block.UPDATE_ALL)

        // BC: 设置 totalSources 供抽油泵系统使用
        val be = world.getBlockEntity(pos)
        if (be is OilSpringBlockEntity) {
            be.totalSources = totalOilCount
        }
    }

    // ========== 工具方法 ==========

    private fun canReplaceForOil(world: WorldGenLevel, pos: BlockPos): Boolean {
        // BC: ReplaceType.ALWAYS — 无条件替换任何方块（含矿石、tuff、granite、流体等）
        return true
    }

    private fun inChunk(pos: BlockPos, min: BlockPos, max: BlockPos): Boolean =
        pos.x in min.x..max.x && pos.z in min.z..max.z && pos.y in min.y..max.y
}
