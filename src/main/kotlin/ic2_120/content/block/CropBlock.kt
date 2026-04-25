package ic2_120.content.block

import ic2_120.content.crop.CropStats
import ic2_120.content.crop.CropBehavior
import ic2_120.content.crop.CropCareTarget
import ic2_120.content.crop.CropSystem
import ic2_120.content.crop.CropType
import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.CropSeedData
import ic2_120.content.item.CropnalyzerItem
import ic2_120.content.item.CoffeeBeans
import ic2_120.content.item.Fertilizer
import ic2_120.content.item.GrinPowder
import ic2_120.content.item.Hops
import ic2_120.content.item.Resin
import ic2_120.content.item.TerraWart
import ic2_120.content.item.Weed
import ic2_120.content.item.WeedEx
import ic2_120.content.item.WeedingSpade
import ic2_120.content.item.SmallCopperDust
import ic2_120.content.item.SmallGoldDust
import ic2_120.content.item.SmallIronDust
import ic2_120.content.item.SmallLeadDust
import ic2_120.content.item.SmallSilverDust
import ic2_120.content.item.SmallTinDust
import ic2_120.registry.instance
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.world.LightType
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.IntProperty
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Box
import net.minecraft.world.World
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld

@ModBlock(
    name = "crop",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "crops",
    renderLayer = "cutout",
    generateBlockLootTable = false
)
class CropBlock : BlockWithEntity(
    AbstractBlock.Settings.copy(Blocks.WHEAT)
        .breakInstantly()
        .noCollision()
        .nonOpaque()
        .luminance { state ->
            if (state.contains(CROP_TYPE) && state.contains(AGE) &&
                state.get(CROP_TYPE) == CropType.RED_WHEAT &&
                state.get(AGE) >= CropSystem.maxAge(CropType.RED_WHEAT)
            ) 7 else 0
        }
        .ticksRandomly()
) {
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
    override fun emitsRedstonePower(state: BlockState): Boolean =
        state.get(CROP_TYPE) == CropType.RED_WHEAT

    override fun getWeakRedstonePower(
        state: BlockState,
        world: net.minecraft.world.BlockView,
        pos: BlockPos,
        direction: Direction
    ): Int {
        if (state.get(CROP_TYPE) != CropType.RED_WHEAT) return 0
        return if (state.get(AGE) >= CropSystem.maxAge(CropType.RED_WHEAT)) 15 else 0
    }

    init {
        defaultState = stateManager.defaultState
            .with(CROP_TYPE, CropType.WHEAT)
            .with(AGE, 0)
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(CROP_TYPE, AGE)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CropBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, CropBlockEntity::class.type()){ w, p, s, be -> (be as CropBlockEntity).tick(w, p, s) }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        val stack = player.getStackInHand(hand)
        // 手持种子扫描仪时，优先走物品 useOnBlock（扫描）逻辑，不触发作物采摘。
        if (stack.item is CropnalyzerItem) return ActionResult.PASS
        // 除草铲：由物品 useOnBlock 清除杂草，不走采摘逻辑
        if (stack.item == WeedingSpade::class.instance()) return ActionResult.PASS
        // 除草剂：由物品 useOnBlock 直接喷洒，不走采摘逻辑
        if (stack.item == WeedEx::class.instance()) return ActionResult.PASS

        if (world.isClient) return ActionResult.SUCCESS
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return ActionResult.PASS
        val isCreative = player.abilities.creativeMode

        if (stack.item == Fertilizer::class.instance()) {
            if (be.applyFertilizerDirect(simulate = false)) {
                if (!isCreative) stack.decrement(1)
                return ActionResult.SUCCESS
            }
        }

        if (!be.canBeHarvested(state)) return ActionResult.PASS

        val result = be.performHarvest(state) ?: return ActionResult.PASS
        result.drops.forEach { ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), it) }
        if (result.ageAfterHarvest != null) {
            world.setBlockState(
                pos,
                state.with(CropBlock.AGE, result.ageAfterHarvest.coerceIn(0, 7)),
                net.minecraft.block.Block.NOTIFY_ALL
            )
        } else {
            world.setBlockState(pos, CropStickBlock.defaultStickState(), net.minecraft.block.Block.NOTIFY_ALL)
        }
        return ActionResult.SUCCESS
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        if (world is ServerWorld && !player.isCreative) {
            val be = world.getBlockEntity(pos) as? CropBlockEntity
            if (be != null) {
                val cropType = state.get(CROP_TYPE)
                if (cropType == CropType.WEED) {
                    ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), Weed::class.instance().defaultStack)
                } else {
                    val seedBag = CropSeedBagItem.createStack(cropType, be.stats, scanLevel = be.scanLevel)
                    ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), seedBag)
                }
                ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), CropStickBlock::class.instance().asItem().defaultStack)
            }
        }
        super.onBreak(world, pos, state, player)
    }

    override fun onStacksDropped(
        state: BlockState,
        world: ServerWorld,
        pos: BlockPos,
        tool: ItemStack,
        dropExperience: Boolean
    ) {
        // 不掉落默认战利品表项；种子袋与作物架由 [onBreak] 处理
    }

    companion object {
        val CROP_TYPE: EnumProperty<CropType> = EnumProperty.of("crop_type", CropType::class.java)
        val AGE: IntProperty = IntProperty.of("stage", 0, 7)

        fun defaultCropState(type: CropType, age: Int): BlockState =
            CropBlock::class.instance().defaultState
                .with(CROP_TYPE, type)
                .with(AGE, age.coerceIn(0, 7))
    }
}

@ModBlockEntity(block = CropBlock::class)
class CropBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), CropCareTarget {

    var stats = CropStats()
    var scanLevel: Int = 0
    private var growthPoints: Int = 0
    private var storageNutrients: Int = 0
    private var storageWater: Int = 0
    private var storageWeedEx: Int = 0
    private var terrainHumidity: Int = 0
    private var terrainNutrients: Int = 0
    private var terrainAirQuality: Int = 0

    constructor(pos: BlockPos, state: BlockState) : this(CropBlockEntity::class.type(), pos, state)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        if (world.time % TICK_RATE.toLong() != 0L) return

        val cropType = state.get(CropBlock.CROP_TYPE)
        applyHighResistanceWeedPressure(world, pos)
        val isWeed = cropType == CropType.WEED || (
            cropType == CropType.VENOMILIA &&
                state.get(CropBlock.AGE) >= 4 &&
                stats.growth >= 8
            )
        if (storageNutrients > 0) storageNutrients--
        if (storageWater > 0) storageWater--
        if (storageWeedEx > 0 && world.random.nextInt(10) == 0) storageWeedEx--

        if (isWeed && world.random.nextInt(50) - stats.growth <= 2) {
            performWeedWork(world, pos)
        }

        applySpecialBehavior(world, pos, cropType)

        if (world.time % (TICK_RATE.toLong() * 4L) == 0L) {
            updateTerrainHumidity(world, pos)
        }
        if ((world.time + TICK_RATE) % (TICK_RATE.toLong() * 4L) == 0L) {
            updateTerrainNutrients(world, pos)
        }
        if ((world.time + TICK_RATE * 2L) % (TICK_RATE.toLong() * 4L) == 0L) {
            updateTerrainAirQuality(world, pos)
        }

        val maxAge = CropSystem.maxAge(cropType)
        val age = state.get(CropBlock.AGE)
        if (age >= maxAge) return

        if (!canGrowAt(world, pos, state, cropType)) return

        performGrowthTick(world, cropType)
        if (growthPoints >= CropSystem.growthDuration(cropType, age)) {
            growthPoints = 0
            world.setBlockState(pos, state.with(CropBlock.AGE, (age + 1).coerceAtMost(7)), net.minecraft.block.Block.NOTIFY_ALL)
            markDirty()
        }
    }

    private fun performWeedWork(world: World, pos: BlockPos) {
        val dirs = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
        val dir = dirs[world.random.nextInt(dirs.size)]
        val targetPos = pos.offset(dir)
        val targetState = world.getBlockState(targetPos)
        val targetBlock = targetState.block

        if (targetBlock is CropStickBlock) {
            if (world.random.nextInt(4) == 0) {
                transformToCrop(world, targetPos, CropType.WEED, 0, CropStats(6, 0, 10))
            }
            return
        }

        if (targetBlock is CropBlock) {
            val targetBe = world.getBlockEntity(targetPos) as? CropBlockEntity ?: return
            val targetType = targetState.get(CropBlock.CROP_TYPE)
            if (targetType != CropType.WEED && !targetBe.hasWeedEx()) {
                if (world.random.nextInt(32) >= targetBe.stats.resistance) {
                    val growth = maxOf(stats.growth, targetBe.stats.growth).let {
                        if (it < 31 && world.random.nextBoolean()) it + 1 else it
                    }
                    transformToCrop(world, targetPos, CropType.WEED, 0, CropStats(growth, 0, 0))
                }
            }
        }
    }

    private fun transformToCrop(world: World, pos: BlockPos, type: CropType, age: Int, newStats: CropStats) {
        world.setBlockState(pos, CropBlock.defaultCropState(type, age), net.minecraft.block.Block.NOTIFY_ALL)
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return
        be.stats = newStats
        be.scanLevel = 0
        be.growthPoints = 0
        be.storageNutrients = 0
        be.storageWater = 0
        be.storageWeedEx = 0
        be.terrainHumidity = 0
        be.terrainNutrients = 0
        be.terrainAirQuality = 0
        be.markDirty()
    }

    data class HarvestResult(
        val drops: List<ItemStack>,
        val ageAfterHarvest: Int?
    )

    fun canBeHarvested(state: BlockState): Boolean {
        val cropType = state.get(CropBlock.CROP_TYPE)
        val age = state.get(CropBlock.AGE)
        return CropSystem.canBeHarvested(cropType, age)
    }

    fun performHarvest(state: BlockState): HarvestResult? {
        val cropType = state.get(CropBlock.CROP_TYPE)
        val age = state.get(CropBlock.AGE)
        if (!canBeHarvested(state)) return null

        val out = mutableListOf<ItemStack>()
        val w = world
        if (w != null) {
            val dropChance = CropSystem.dropGainChance(cropType) * Math.pow(1.03, stats.gain.toDouble())
            val gaussian = kotlin.runCatching { w.random.nextGaussian() }.getOrDefault(0.0)
            val dropRolls = kotlin.math.max(0, kotlin.math.round(gaussian * dropChance * 0.6827 + dropChance).toInt())
            repeat(dropRolls) {
                val gains = createGainStacks(cropType, age, w)
                gains.forEach { stack ->
                    if (!stack.isEmpty && w.random.nextInt(100) <= stats.gain) stack.increment(1)
                    if (!stack.isEmpty) out += stack
                }
            }
        }

        val nextAge = CropSystem.ageAfterHarvest(cropType, age, world?.random?.nextInt(2) ?: 0)
        return HarvestResult(out, nextAge)
    }

    override fun applyFertilizer(amount: Int, simulate: Boolean): Int {
        val request = amount.coerceAtLeast(0)
        if (request <= 0) return 0
        if (storageNutrients >= 100) return 0

        var working = storageNutrients
        var used = 0
        while (used < request && working < 100) {
            working += 90
            used++
        }
        if (!simulate && used > 0) {
            storageNutrients = working
            markDirty()
        }
        return used
    }

    fun applyFertilizerDirect(simulate: Boolean): Boolean {
        if (storageNutrients >= 100) return false
        if (!simulate) {
            storageNutrients += 100
            markDirty()
        }
        return true
    }

    override fun applyHydration(amount: Int, simulate: Boolean): Int {
        val cap = 200
        val accepted = (cap - storageWater).coerceAtLeast(0).coerceAtMost(amount.coerceAtLeast(0))
        if (!simulate && accepted > 0) {
            storageWater += accepted
            markDirty()
        }
        return accepted
    }

    override fun applyWeedEx(amount: Int, simulate: Boolean): Int {
        val cap = 150
        val accepted = (cap - storageWeedEx).coerceAtLeast(0).coerceAtMost(amount.coerceAtLeast(0))
        if (!simulate && accepted > 0) {
            storageWeedEx += accepted
            markDirty()
        }
        return accepted
    }

    fun applyWeedExDirect(amount: Int, simulate: Boolean): Int {
        val cap = 100
        val accepted = (cap - storageWeedEx).coerceAtLeast(0).coerceAtMost(amount.coerceAtLeast(0))
        if (!simulate && accepted > 0) {
            storageWeedEx += accepted
            markDirty()
        }
        return accepted
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("growth", stats.growth)
        nbt.putInt("gain", stats.gain)
        nbt.putInt("resistance", stats.resistance)
        nbt.putInt("scan_level", scanLevel)
        nbt.putInt("growth_points", growthPoints)
        nbt.putInt("nutrients", storageNutrients)
        nbt.putInt("water", storageWater)
        nbt.putInt("weed_ex", storageWeedEx)
        nbt.putInt("terrain_humidity", terrainHumidity)
        nbt.putInt("terrain_nutrients", terrainNutrients)
        nbt.putInt("terrain_air_quality", terrainAirQuality)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        stats = CropStats(
            growth = nbt.getInt("growth"),
            gain = nbt.getInt("gain"),
            resistance = nbt.getInt("resistance"),
        )
        scanLevel = nbt.getInt("scan_level").coerceIn(0, 4)
        growthPoints = nbt.getInt("growth_points")
        storageNutrients = nbt.getInt("nutrients")
        storageWater = nbt.getInt("water")
        storageWeedEx = nbt.getInt("weed_ex")
        terrainHumidity = nbt.getInt("terrain_humidity")
        terrainNutrients = nbt.getInt("terrain_nutrients")
        terrainAirQuality = nbt.getInt("terrain_air_quality")
    }

    fun storageSnapshot(): Triple<Int, Int, Int> = Triple(storageNutrients, storageWater, storageWeedEx)

    data class GrowthEstimate(
        val cropType: CropType,
        val age: Int,
        val maxAge: Int,
        val progressPercent: Int,
        val canGrowNow: Boolean,
        val isMature: Boolean,
        val remainingSeconds: Double?
    )

    fun estimateGrowth(world: World, pos: BlockPos, state: BlockState): GrowthEstimate {
        val cropType = state.get(CropBlock.CROP_TYPE)
        val age = state.get(CropBlock.AGE)
        val maxAge = CropSystem.maxAge(cropType)
        val clampedMax = maxOf(1, maxAge)
        val progressPercent = ((age * 100.0) / clampedMax).toInt().coerceIn(0, 100)
        if (age >= maxAge) {
            return GrowthEstimate(
                cropType = cropType,
                age = age,
                maxAge = maxAge,
                progressPercent = 100,
                canGrowNow = false,
                isMature = true,
                remainingSeconds = 0.0
            )
        }

        val canGrow = canGrowAt(world, pos, state, cropType)
        if (!canGrow) {
            return GrowthEstimate(
                cropType = cropType,
                age = age,
                maxAge = maxAge,
                progressPercent = progressPercent,
                canGrowNow = false,
                isMature = false,
                remainingSeconds = null
            )
        }

        val humidity = sampleTerrainHumidity(world, pos)
        val nutrients = sampleTerrainNutrients(world, pos)
        val airQuality = sampleTerrainAirQuality(world, pos)

        var minQuality = (CropSystem.definition(cropType).tier - 1) * 4 + stats.growth + stats.gain + stats.resistance
        if (minQuality < 0) minQuality = 0
        val providedQuality = CropSystem.getWeightInfluence(cropType, humidity, nutrients, airQuality) * 5

        val expectedBaseGrowth = 6.0 + stats.growth // 3 + E[rand(0..6)] + growth
        val expectedDeltaPerCycle = if (providedQuality >= minQuality) {
            expectedBaseGrowth * (100.0 + (providedQuality - minQuality)) / 100.0
        } else {
            val penalty = (minQuality - providedQuality) * 4
            (expectedBaseGrowth * (100.0 - penalty) / 100.0).coerceAtLeast(0.0)
        }

        if (expectedDeltaPerCycle <= 0.0) {
            return GrowthEstimate(
                cropType = cropType,
                age = age,
                maxAge = maxAge,
                progressPercent = progressPercent,
                canGrowNow = false,
                isMature = false,
                remainingSeconds = null
            )
        }

        var remainingPoints = 0.0
        val currentStageNeed = (CropSystem.growthDuration(cropType, age) - growthPoints).coerceAtLeast(0)
        remainingPoints += currentStageNeed.toDouble()
        for (stage in (age + 1) until maxAge) {
            remainingPoints += CropSystem.growthDuration(cropType, stage).toDouble()
        }

        val specialBonusPerCycle = specialBonusPerCycle(world, pos, cropType, age, maxAge)
        val effectiveDelta = (expectedDeltaPerCycle + specialBonusPerCycle).coerceAtLeast(0.0001)
        val remainingCycles = remainingPoints / effectiveDelta
        val remainingSeconds = remainingCycles * (TICK_RATE / 20.0)

        return GrowthEstimate(
            cropType = cropType,
            age = age,
            maxAge = maxAge,
            progressPercent = progressPercent,
            canGrowNow = true,
            isMature = false,
            remainingSeconds = remainingSeconds
        )
    }

    fun canParticipateCrossing(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        val type = state.get(CropBlock.CROP_TYPE)
        if (!CropSystem.canCross(type, state.get(CropBlock.AGE))) return false
        return true
    }

    data class RequirementIssue(val key: String, val args: Array<Any> = emptyArray())

    fun getGrowthRequirements(world: World, pos: BlockPos, state: BlockState, cropType: CropType): List<RequirementIssue> {
        val age = state.get(CropBlock.AGE)
        val max = CropSystem.maxAge(cropType)
        val light = world.getLightLevel(pos.up())
        val issues = mutableListOf<RequirementIssue>()

        if (age >= max) return listOf(RequirementIssue("mature"))

        when (cropType) {
            CropType.WHEAT, CropType.CARROTS, CropType.BEETROOTS, CropType.PUMPKIN, CropType.MELON,
            CropType.REED, CropType.STICKY_REED, CropType.FLAX, CropType.HOPS, CropType.COFFEE,
            CropType.OAK_SAPLING, CropType.SPRUCE_SAPLING, CropType.BIRCH_SAPLING, CropType.JUNGLE_SAPLING,
            CropType.ACACIA_SAPLING, CropType.DARK_OAK_SAPLING -> {
                if (light < 9) issues.add(RequirementIssue("light_low_9", arrayOf(light)))
            }
            CropType.POTATO -> {
                if (age >= 4) issues.add(RequirementIssue("potato_max_age"))
                if (light < 9) issues.add(RequirementIssue("light_low_9", arrayOf(light)))
            }
            CropType.DANDELION, CropType.POPPY, CropType.BLACKTHORN, CropType.TULIP, CropType.CYAZINT -> {
                if (light < 12) issues.add(RequirementIssue("light_low_12", arrayOf(light)))
            }
            CropType.COCOA -> {
                if (storageNutrients < 3) issues.add(RequirementIssue("nutrients_low_3", arrayOf(storageNutrients)))
            }
            CropType.RED_WHEAT -> {
                val redstonePower = world.getReceivedRedstonePower(pos)
                if (redstonePower !in 5..10) issues.add(RequirementIssue("redstone_out_of_range", arrayOf(redstonePower)))
            }
            CropType.VENOMILIA -> {
                if (age <= 3 && light < 12) issues.add(RequirementIssue("light_low_12", arrayOf(light)))
            }
            CropType.FERRU -> {
                if (age == max - 1 && !hasMetalRoot(world, pos, METAL_IRON)) issues.add(RequirementIssue("missing_iron_root"))
            }
            CropType.CYPRIUM -> {
                if (age == max - 1 && !hasMetalRoot(world, pos, METAL_COPPER)) issues.add(RequirementIssue("missing_copper_root"))
            }
            CropType.STAGNIUM -> {
                if (age == max - 1 && !hasMetalRoot(world, pos, METAL_TIN)) issues.add(RequirementIssue("missing_tin_root"))
            }
            CropType.PLUMBISCUS -> {
                if (age == max - 1 && !hasMetalRoot(world, pos, METAL_LEAD)) issues.add(RequirementIssue("missing_lead_root"))
            }
            CropType.AURELIA -> {
                if (age == max - 1 && !hasMetalRoot(world, pos, METAL_GOLD)) issues.add(RequirementIssue("missing_gold_root"))
            }
            CropType.SHINING -> {
                if (age == max - 1 && !hasMetalRoot(world, pos, METAL_SILVER)) issues.add(RequirementIssue("missing_silver_root"))
            }
            CropType.EATING_PLANT -> {
                if (age < 2) {
                    if (light <= 10) issues.add(RequirementIssue("light_low_gt_10", arrayOf(light)))
                } else {
                    val below = world.getBlockState(pos.down()).block
                    if (below != Blocks.GRASS_BLOCK) issues.add(RequirementIssue("need_grass_below"))
                    if (light <= 10) issues.add(RequirementIssue("light_low_gt_10", arrayOf(light)))
                }
            }
            else -> Unit
        }
        return issues
    }

    private fun canGrowAt(world: World, pos: BlockPos, state: BlockState, cropType: CropType): Boolean {
        val age = state.get(CropBlock.AGE)
        val max = CropSystem.maxAge(cropType)
        val light = world.getLightLevel(pos.up())
        if (age >= max) return false

        return when (cropType) {
            CropType.WHEAT, CropType.CARROTS, CropType.BEETROOTS, CropType.PUMPKIN, CropType.MELON,
            CropType.REED, CropType.STICKY_REED, CropType.FLAX, CropType.HOPS, CropType.COFFEE,
            CropType.OAK_SAPLING, CropType.SPRUCE_SAPLING, CropType.BIRCH_SAPLING, CropType.JUNGLE_SAPLING,
            CropType.ACACIA_SAPLING, CropType.DARK_OAK_SAPLING -> light >= 9
            CropType.POTATO -> age < 4 && light >= 9
            CropType.DANDELION, CropType.POPPY, CropType.BLACKTHORN, CropType.TULIP, CropType.CYAZINT -> light >= 12
            CropType.COCOA -> storageNutrients >= 3
            CropType.RED_WHEAT -> {
                val redstonePower = world.getReceivedRedstonePower(pos)
                redstonePower in 5..10
            }
            CropType.VENOMILIA -> (age <= 3 && light >= 12) || age == 4
            CropType.FERRU -> age < max - 1 || (age == max - 1 && hasMetalRoot(world, pos, METAL_IRON))
            CropType.CYPRIUM -> age < max - 1 || (age == max - 1 && hasMetalRoot(world, pos, METAL_COPPER))
            CropType.STAGNIUM -> age < max - 1 || (age == max - 1 && hasMetalRoot(world, pos, METAL_TIN))
            CropType.PLUMBISCUS -> age < max - 1 || (age == max - 1 && hasMetalRoot(world, pos, METAL_LEAD))
            CropType.AURELIA -> age < max - 1 || (age == max - 1 && hasMetalRoot(world, pos, METAL_GOLD))
            CropType.SHINING -> age < max - 1 || (age == max - 1 && hasMetalRoot(world, pos, METAL_SILVER))
            CropType.EATING_PLANT -> {
                if (age < 2) light > 10
                else world.getBlockState(pos.down()).block == Blocks.GRASS_BLOCK && light > 10
            }
            else -> true
        }
    }

    private fun hasMetalRoot(world: World, pos: BlockPos, group: MetalRootGroup): Boolean {
        for (depth in 1..5) {
            val below = world.getBlockState(pos.down(depth))
            if (group.matches(below.block, below)) return true
        }
        return false
    }

    private enum class MetalRootGroup {
        IRON,
        COPPER,
        TIN,
        LEAD,
        GOLD,
        SILVER;

        fun matches(block: Block, state: BlockState): Boolean {
            return when (this) {
                IRON ->
                    state.isIn(BlockTags.IRON_ORES) ||
                        block == Blocks.IRON_BLOCK ||
                        state.isIn(TAG_ORES_IRON) ||
                        state.isIn(TAG_STORAGE_IRON)
                COPPER ->
                    state.isIn(BlockTags.COPPER_ORES) ||
                        block == Blocks.COPPER_BLOCK ||
                        state.isIn(TAG_ORES_COPPER) ||
                        state.isIn(TAG_STORAGE_COPPER)
                TIN ->
                    state.isIn(TAG_ORES_TIN) ||
                        state.isIn(TAG_STORAGE_TIN)
                LEAD ->
                    state.isIn(TAG_ORES_LEAD) ||
                        state.isIn(TAG_STORAGE_LEAD)
                GOLD ->
                    state.isIn(BlockTags.GOLD_ORES) ||
                        block == Blocks.GOLD_BLOCK ||
                        state.isIn(TAG_ORES_GOLD) ||
                        state.isIn(TAG_STORAGE_GOLD)
                SILVER ->
                    state.isIn(TAG_ORES_SILVER) ||
                        state.isIn(TAG_STORAGE_SILVER)
            }
        }
    }

    private fun createGainStacks(cropType: CropType, age: Int, world: World): List<ItemStack> {
        return when (cropType) {
            CropType.DANDELION -> listOf(ItemStack(Items.YELLOW_DYE, 1))
            CropType.POPPY -> listOf(ItemStack(Items.RED_DYE, 1))
            CropType.BLACKTHORN -> listOf(ItemStack(Items.BLACK_DYE, 1))
            CropType.TULIP -> listOf(ItemStack(Items.PURPLE_DYE, 1))
            CropType.CYAZINT -> listOf(ItemStack(Items.BLUE_DYE, 1))
            CropType.REED -> listOf(ItemStack(Items.SUGAR_CANE, age.coerceAtLeast(1)))
            CropType.STICKY_REED -> {
                if (age >= CropSystem.maxAge(cropType)) listOf(ItemStack(Resin::class.instance(), 1))
                else listOf(ItemStack(Items.SUGAR_CANE, age.coerceAtLeast(1)))
            }
            CropType.PUMPKIN -> listOf(ItemStack(Items.PUMPKIN))
            CropType.MELON -> {
                if (world.random.nextInt(3) == 0) listOf(ItemStack(Items.MELON))
                else listOf(ItemStack(Items.MELON_SLICE, world.random.nextInt(4) + 2))
            }
            CropType.COFFEE -> {
                val max = CropSystem.maxAge(cropType)
                if (age == max - 1) emptyList() else listOf(ItemStack(CoffeeBeans::class.instance(), 1))
            }
            CropType.HOPS -> listOf(ItemStack(Hops::class.instance(), 1))
            CropType.FLAX -> listOf(ItemStack(Items.STRING, 1))
            CropType.VENOMILIA -> {
                if (age == 4) listOf(ItemStack(GrinPowder::class.instance(), 1))
                else if (age >= 3) listOf(ItemStack(Items.PURPLE_DYE, 1))
                else emptyList()
            }
            CropType.POTATO -> {
                val max = CropSystem.maxAge(cropType)
                if (age >= max && world.random.nextInt(20) == 0) listOf(ItemStack(Items.POISONOUS_POTATO, 1))
                else if (age >= max - 1) listOf(ItemStack(Items.POTATO, 1))
                else emptyList()
            }
            CropType.RED_WHEAT -> {
                val sky = world.getLightLevel(LightType.SKY, pos)
                if (sky <= 0 && !world.random.nextBoolean()) listOf(ItemStack(Items.WHEAT, 1))
                else listOf(ItemStack(Items.REDSTONE, 1))
            }
            CropType.EATING_PLANT -> listOf(ItemStack(Items.MELON, 1))
            CropType.TERRA_WART -> listOf(ItemStack(TerraWart::class.instance(), 1))
            CropType.FERRU -> listOf(ItemStack(SmallIronDust::class.instance(), 1))
            CropType.CYPRIUM -> listOf(ItemStack(SmallCopperDust::class.instance(), 1))
            CropType.STAGNIUM -> listOf(ItemStack(SmallTinDust::class.instance(), 1))
            CropType.PLUMBISCUS -> listOf(ItemStack(SmallLeadDust::class.instance(), 1))
            CropType.AURELIA -> listOf(ItemStack(SmallGoldDust::class.instance(), 1))
            CropType.SHINING -> listOf(ItemStack(SmallSilverDust::class.instance(), 1))
            CropType.OAK_SAPLING -> {
                val out = mutableListOf(ItemStack(Items.OAK_SAPLING))
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.OAK_SAPLING)
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.APPLE)
                out
            }
            CropType.SPRUCE_SAPLING -> {
                val out = mutableListOf(ItemStack(Items.SPRUCE_SAPLING))
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.SPRUCE_SAPLING)
                out
            }
            CropType.BIRCH_SAPLING -> {
                val out = mutableListOf(ItemStack(Items.BIRCH_SAPLING))
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.BIRCH_SAPLING)
                out
            }
            CropType.JUNGLE_SAPLING -> {
                val out = mutableListOf(ItemStack(Items.JUNGLE_SAPLING))
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.JUNGLE_SAPLING)
                out
            }
            CropType.ACACIA_SAPLING -> {
                val out = mutableListOf(ItemStack(Items.ACACIA_SAPLING))
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.ACACIA_SAPLING)
                out
            }
            CropType.DARK_OAK_SAPLING -> {
                val out = mutableListOf(ItemStack(Items.DARK_OAK_SAPLING))
                if (world.random.nextInt(100) >= 75) out += ItemStack(Items.DARK_OAK_SAPLING)
                out
            }
            else -> CropSystem.definition(cropType).gainItem?.let { listOf(ItemStack(it, 1)) } ?: emptyList()
        }
    }

    private fun calculateEnvBonus(world: World, pos: BlockPos, cropType: CropType): Int {
        val behavior = CropSystem.behavior(cropType)
        var bonus = storageNutrients / 40 + storageWater / 40
        val light = world.getLightLevel(pos.up())
        when (behavior) {
            CropBehavior.HIGH_HYDRATION -> {
                if (storageWater < 40) bonus -= 2
                else bonus += 1
            }
            CropBehavior.DARKNESS_LOVING -> {
                if (light > 10) bonus -= 2
                if (light < 8) bonus += 1
            }
            CropBehavior.METAL_CROP -> {
                if (storageNutrients < 50) bonus -= 2
                else bonus += 1
            }
            else -> Unit
        }
        return bonus
    }

    private fun performGrowthTick(world: World, cropType: CropType) {
        val baseGrowth = 3 + world.random.nextInt(7) + stats.growth
        var minQuality = (CropSystem.definition(cropType).tier - 1) * 4 + stats.growth + stats.gain + stats.resistance
        if (minQuality < 0) minQuality = 0
        val providedQuality = CropSystem.getWeightInfluence(cropType, terrainHumidity, terrainNutrients, terrainAirQuality) * 5
        val growthDelta = if (providedQuality >= minQuality) {
            baseGrowth * (100 + (providedQuality - minQuality)) / 100
        } else {
            val penalty = (minQuality - providedQuality) * 4
            if (penalty > 100 && world.random.nextInt(32) > stats.resistance) {
                resetToStick(world)
                0
            } else {
                (baseGrowth * (100 - penalty) / 100).coerceAtLeast(0)
            }
        }
        growthPoints += growthDelta
    }

    private fun updateTerrainHumidity(world: World, pos: BlockPos) {
        terrainHumidity = sampleTerrainHumidity(world, pos)
    }

    private fun updateTerrainNutrients(world: World, pos: BlockPos) {
        terrainNutrients = sampleTerrainNutrients(world, pos)
    }

    private fun updateTerrainAirQuality(world: World, pos: BlockPos) {
        terrainAirQuality = sampleTerrainAirQuality(world, pos)
    }

    private fun sampleTerrainHumidity(world: World, pos: BlockPos): Int {
        val biome = world.getBiome(pos).value()
        var value = 0
        if (world.isRaining) value += 2
        if (biome.temperature in 0.2f..1.0f) value += 2
        val below = world.getBlockState(pos.down())
        if (below.block is net.minecraft.block.FarmlandBlock) {
            val moisture = below.get(net.minecraft.block.FarmlandBlock.MOISTURE)
            if (moisture >= 7) value += 2
        }
        if (storageWater >= 5) value += 2
        value += (storageWater + 24) / 25
        return value.coerceAtLeast(0)
    }

    private fun sampleTerrainNutrients(world: World, pos: BlockPos): Int {
        val biome = world.getBiome(pos).value()
        val temperature = biome.temperature
        var value = ((1.2f - kotlin.math.abs(temperature - 0.8f)) * 5f).toInt().coerceAtLeast(0)
        for (d in 1..4) {
            val block = world.getBlockState(pos.down(d)).block
            if (block == net.minecraft.block.Blocks.DIRT) value++ else break
        }
        value += (storageNutrients + 19) / 20
        return value.coerceAtLeast(0)
    }

    private fun sampleTerrainAirQuality(world: World, pos: BlockPos): Int {
        var value = kotlin.math.floor((pos.y - 40).toDouble() / 15.0).toInt().coerceIn(0, 2)
        var open = 9
        for (x in pos.x - 1..pos.x + 1) {
            for (z in pos.z - 1..pos.z + 1) {
                if (open <= 0) break
                val p = BlockPos(x, pos.y, z)
                val opaque = world.getBlockState(p).isOpaque
                val isCrop = world.getBlockEntity(p) is CropBlockEntity
                if (opaque || isCrop) open--
            }
        }
        value += open / 2
        if (world.isSkyVisible(pos.up())) value += 4
        return value.coerceAtLeast(0)
    }

    private fun specialBonusPerCycle(world: World, pos: BlockPos, cropType: CropType, age: Int, maxAge: Int): Double {
        if (age >= maxAge) return 0.0
        return when (cropType) {
            CropType.NETHER_WART -> if (world.getBlockState(pos.down()).block == Blocks.SOUL_SAND) 100.0 else 0.0
            CropType.TERRA_WART -> if (world.getBlockState(pos.down()).block == Blocks.SNOW_BLOCK) 100.0 else 0.0
            else -> 0.0
        }
    }

    private fun hasWeedEx(): Boolean {
        if (storageWeedEx > 0) {
            storageWeedEx = (storageWeedEx - 5).coerceAtLeast(0)
            return true
        }
        return false
    }

    private fun resetToStick(world: World) {
        world.setBlockState(pos, CropStickBlock.defaultStickState(), net.minecraft.block.Block.NOTIFY_ALL)
    }

    private fun applyHighResistanceWeedPressure(world: World, pos: BlockPos) {
        // IC2: 高生长值(Gr)作物会显著提高杂草污染强度。
        if (stats.growth < 24) return
        if (world.random.nextInt(3) != 0) return

        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val targetPos = pos.add(dx, 0, dz)
                val soilPos = targetPos.down()
                val soilState = world.getBlockState(soilPos)

                if (soilState.block is net.minecraft.block.FarmlandBlock || soilState.block == Blocks.DIRT) {
                    world.setBlockState(soilPos, Blocks.GRASS_BLOCK.defaultState, net.minecraft.block.Block.NOTIFY_ALL)

                    if (world.getBlockState(targetPos).isAir) {
                        if (world.random.nextInt(4) == 0 && world.getBlockState(targetPos.up()).isAir) {
                            world.setBlockState(targetPos, Blocks.TALL_GRASS.defaultState, net.minecraft.block.Block.NOTIFY_ALL)
                        } else {
                            world.setBlockState(targetPos, Blocks.GRASS.defaultState, net.minecraft.block.Block.NOTIFY_ALL)
                        }
                    }
                }

                val targetState = world.getBlockState(targetPos)
                if (targetState.block is CropStickBlock) {
                    world.setBlockState(targetPos, CropBlock.defaultCropState(CropType.WEED, 0), net.minecraft.block.Block.NOTIFY_ALL)
                    val be = world.getBlockEntity(targetPos) as? CropBlockEntity ?: continue
                    be.stats = CropStats(
                        growth = maxOf(6, stats.growth),
                        gain = 0,
                        resistance = 10
                    )
                    be.scanLevel = 0
                    be.markDirty()
                }
            }
        }
    }

    private fun applySpecialBehavior(world: World, pos: BlockPos, cropType: CropType) {
        val state = cachedState
        when (cropType) {
            CropType.NETHER_WART -> {
                val below = world.getBlockState(pos.down()).block
                if (below == Blocks.SOUL_SAND) {
                    if (state.get(CropBlock.AGE) < CropSystem.maxAge(cropType)) {
                        growthPoints += 100
                    }
                } else if (below == Blocks.SNOW_BLOCK && world.random.nextInt(300) == 0) {
                    world.setBlockState(pos, CropBlock.defaultCropState(CropType.TERRA_WART, state.get(CropBlock.AGE)), net.minecraft.block.Block.NOTIFY_ALL)
                    return
                }
            }
            CropType.TERRA_WART -> {
                val below = world.getBlockState(pos.down()).block
                if (below == Blocks.SNOW_BLOCK) {
                    if (state.get(CropBlock.AGE) < CropSystem.maxAge(cropType)) {
                        growthPoints += 100
                    }
                } else if (below == Blocks.SOUL_SAND && world.random.nextInt(300) == 0) {
                    world.setBlockState(pos, CropBlock.defaultCropState(CropType.NETHER_WART, state.get(CropBlock.AGE)), net.minecraft.block.Block.NOTIFY_ALL)
                    return
                }
            }
            else -> Unit
        }

        when (CropSystem.behavior(cropType)) {
            CropBehavior.POISONOUS -> {
                if (cropType == CropType.EATING_PLANT) {
                    val age = state.get(CropBlock.AGE)
                    if (age <= 0) return
                    val cx = pos.x + 0.5
                    val cy = pos.y + 0.5
                    val cz = pos.z + 0.5
                    val box = Box(cx - 1.0, pos.y.toDouble(), cz - 1.0, cx + 1.0, pos.y + 2.0, cz + 1.0)
                    val entities = world.getNonSpectatingEntities(net.minecraft.entity.LivingEntity::class.java, box)
                    for (e in entities) {
                        if (e is net.minecraft.entity.player.PlayerEntity && e.abilities.creativeMode) continue
                        e.addVelocity((cx - e.x) * 0.5, minOf(e.velocity.y, -0.05), (cz - e.z) * 0.5)
                        e.damage(world.damageSources.cactus(), (age + 1) * 2f)
                        if (canGrowAt(world, pos, state, cropType)) {
                            growthPoints += 100
                        }
                        break
                    }
                    return
                }

                if (cropType == CropType.VENOMILIA) {
                    if (state.get(CropBlock.AGE) == 4 && world.time % 20L == 0L) {
                        val box = Box(pos).expand(1.0, 0.2, 1.0)
                        val entities = world.getNonSpectatingEntities(net.minecraft.entity.LivingEntity::class.java, box)
                        for (e in entities) {
                            if (e is net.minecraft.entity.player.PlayerEntity && e.abilities.creativeMode) continue
                            e.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.POISON, (world.random.nextInt(10) + 5) * 20, 0))
                            world.setBlockState(pos, state.with(CropBlock.AGE, 3), net.minecraft.block.Block.NOTIFY_ALL)
                            break
                        }
                    }
                    return
                }
            }
            else -> Unit
        }
    }

    companion object {
        private const val TICK_RATE = 256
        private val TAG_ORES_IRON: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/iron"))
        private val TAG_STORAGE_IRON: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "storage_blocks/iron"))
        private val TAG_ORES_COPPER: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/copper"))
        private val TAG_STORAGE_COPPER: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "storage_blocks/copper"))
        private val TAG_ORES_TIN: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/tin"))
        private val TAG_STORAGE_TIN: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "storage_blocks/tin"))
        private val TAG_ORES_LEAD: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/lead"))
        private val TAG_STORAGE_LEAD: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "storage_blocks/lead"))
        private val TAG_ORES_GOLD: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/gold"))
        private val TAG_STORAGE_GOLD: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "storage_blocks/gold"))
        private val TAG_ORES_SILVER: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/silver"))
        private val TAG_STORAGE_SILVER: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "storage_blocks/silver"))
        private val METAL_IRON = MetalRootGroup.IRON
        private val METAL_COPPER = MetalRootGroup.COPPER
        private val METAL_TIN = MetalRootGroup.TIN
        private val METAL_LEAD = MetalRootGroup.LEAD
        private val METAL_GOLD = MetalRootGroup.GOLD
        private val METAL_SILVER = MetalRootGroup.SILVER
    }
}
