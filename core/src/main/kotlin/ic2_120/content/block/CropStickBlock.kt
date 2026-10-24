package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos

import net.minecraft.core.HolderLookup
import ic2_120.content.crop.CropStats
import ic2_120.content.crop.CropCareTarget
import ic2_120.content.crop.CropSystem
import ic2_120.content.crop.CropType
import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.CropSeedData
import ic2_120.registry.instance
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.Containers
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader

@ModBlock(
    name = "crop_stick",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "crops",
    renderLayer = "cutout"
)
class CropStickBlock : BaseEntityBlock(
    BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)
        .instabreak()
        .noCollission()
         .noOcclusion()
) {
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    init {
        registerDefaultState(stateDefinition.any()().setValue(CROSSING_BASE, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(CROSSING_BASE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(CROSSING_BASE, false)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CropStickBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, CropStickBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun canSurvive(state: BlockState, world: LevelReader, pos: BlockPos): Boolean {
        val below = world.getBlockState(pos.below())
        return below.block is FarmBlock
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (!canPlaceAt(state, world, pos)) {
            return Blocks.AIR.defaultBlockState()
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val stack = player.getItemInHand(player.usedItemHand)
        val isCreative = player.abilities.instabuild

        // 放置第二根作物架：变为杂交底座
        if (!state.getValue(CROSSING_BASE) && stack.item == this.asItem()) {
            if (!isCreative) stack.shrink(1)
            world.setBlock(pos, state.setValue(CROSSING_BASE, true), Block.UPDATE_ALL)
            return InteractionResult.SUCCESS
        }

        // 空架种植基础种子
        if (!state.getValue(CROSSING_BASE)) {
            if (stack.item is CropSeedBagItem) {
                val cropType = CropSeedData.readType(stack)
                if (cropType != null) {
                    val stats = CropSeedData.readStats(stack)
                    val cropState = CropBlock.defaultCropState(cropType, 0)
                    world.setBlock(pos, cropState, Block.UPDATE_ALL)
                    val be = world.getBlockEntity(pos) as? CropBlockEntity
                    be?.stats = stats
                    be?.scanLevel = CropSeedData.readScanLevel(stack)
                    be?.setChanged()
                    if (!isCreative) stack.shrink(1)
                    return InteractionResult.SUCCESS
                }
            }

            val cropType = CropSystem.baseSeed(stack.item)
            if (cropType != null) {
                val cropState = CropBlock.defaultCropState(cropType, 0)
                world.setBlock(pos, cropState, Block.UPDATE_ALL)
                val be = world.getBlockEntity(pos) as? CropBlockEntity
                be?.stats = CropStats(1, 1, 1)
                be?.scanLevel = 0
                be?.setChanged()
                if (!isCreative) stack.shrink(1)
                return InteractionResult.SUCCESS
            }
        }

        // 右键空手可拆除杂交底座并返还一根作物架
        if (state.getValue(CROSSING_BASE) && stack.isEmpty) {
            world.setBlock(pos, state.setValue(CROSSING_BASE, false), Block.UPDATE_ALL)
            Containers.dropItemStack(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), this.asItem().defaultInstance)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    /**
     * 固定周期 tick（对齐原版 performTick），由 BlockEntityTicker 驱动。
     * 原 randomTick 改为固定 TICK_RATE=256 周期，避免 randomTickSpeed 影响杂交频率。
     */
    fun performTick(world: net.minecraft.server.level.ServerLevel, pos: BlockPos, state: BlockState, random: net.minecraft.util.RandomSource) {
        if (!state.getValue(CROSSING_BASE)) {
            val stickBe = world.getBlockEntity(pos) as? CropStickBlockEntity
            if (stickBe?.hasWeedExProtection(random) == true) return
            // IC2: 空作物架有极低概率自发长出杂草。
            if (random.nextInt(100) == 0) {
                world.setBlock(pos, CropBlock.defaultCropState(CropType.WEED, 0), Block.UPDATE_ALL)
                val be = world.getBlockEntity(pos) as? CropBlockEntity
                be?.stats = CropStats(1, 1, 1)
                be?.scanLevel = 0
                be?.setChanged()
            }
            return
        }
        if (attemptCrossing(world, pos, random)) return
        attemptSpreading(world, pos, random)
    }

    private fun attemptCrossing(world: net.minecraft.server.level.ServerLevel, pos: BlockPos, random: net.minecraft.util.RandomSource): Boolean {
        if (random.nextInt(3) != 0) return false
        val sources = mutableListOf<CropBlockEntity>()
        for (dir in HORIZONTAL_DIRS) {
            val be = world.getBlockEntity(pos.offset(dir)) as? CropBlockEntity ?: continue
            val sourcePos = pos.offset(dir)
            if (!be.canParticipateCrossing(world, sourcePos)) continue
            val sourceState = world.getBlockState(sourcePos)
            val sourceType = sourceState.getValue(CropBlock.CROP_TYPE)
            var chance = 4
            if (be.stats.growth >= 16) chance++
            if (be.stats.growth >= 30) chance++
            if (be.stats.resistance >= 28) chance += 27 - be.stats.resistance
            if (chance >= random.nextInt(16)) sources += be
        }

        if (sources.size < 2) return false

        val allTypes = CropSystem.allTypes()
        val cumulative = IntArray(allTypes.size)
        var total = 0
        for (i in allTypes.indices) {
            val target = allTypes[i]
            if (CropSystem.canCross(target)) {
                for (source in sources) {
                    val sourceType = world.getBlockState(source.pos).getValue(CropBlock.CROP_TYPE)
                    total += CropSystem.calculateRatioFor(target, sourceType)
                }
            }
            cumulative[i] = total
        }
        if (total <= 0) return false

        val roll = random.nextInt(total)
        var index = 0
        while (index < cumulative.size && roll >= cumulative[index]) index++
        if (index !in allTypes.indices) return false

        val child = allTypes[index]
        val count = sources.size
        val avgGrowth = (sources.sumOf { it.stats.growth } / count + random.nextInt(1 + 2 * count) - count).coerceIn(0, 31)
        val avgGain = (sources.sumOf { it.stats.gain } / count + random.nextInt(1 + 2 * count) - count).coerceIn(0, 31)
        val avgRes = (sources.sumOf { it.stats.resistance } / count + random.nextInt(1 + 2 * count) - count).coerceIn(0, 31)

        val state = CropBlock.defaultCropState(child, 0)
        world.setBlock(pos, state, Block.UPDATE_ALL)
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return true
        be.stats = CropStats(avgGrowth, avgGain, avgRes)
        be.scanLevel = 0
        be.setChanged()
        return true
    }

    private fun attemptSpreading(world: net.minecraft.server.level.ServerLevel, pos: BlockPos, random: net.minecraft.util.RandomSource): Boolean {
        val neighbors = mutableListOf<CropBlockEntity>()
        for (dir in HORIZONTAL_DIRS) {
            val be = world.getBlockEntity(pos.offset(dir)) as? CropBlockEntity ?: continue
            neighbors += be
        }
        if (neighbors.size != 1) return false

        val parent = neighbors.first()
        if (!parent.canParticipateCrossing(world, parent.pos)) return false
        val parentType = world.getBlockState(parent.pos).getValue(CropBlock.CROP_TYPE)

        var chance = 4
        if (parent.stats.growth >= 16) chance++
        if (parent.stats.growth >= 30) chance++
        if (parent.stats.resistance >= 28) chance += 27 - parent.stats.resistance
        if (chance < random.nextInt(16)) return false

        val state = CropBlock.defaultCropState(parentType, 0)
        world.setBlock(pos, state, Block.UPDATE_ALL)
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return true
        be.stats = CropStats(
            growth = parent.stats.growth.coerceAtLeast(0),
            gain = parent.stats.gain.coerceAtLeast(0),
            resistance = parent.stats.resistance.coerceAtLeast(0)
        )
        be.scanLevel = 0
        be.setChanged()
        return true
    }

    companion object {
        val CROSSING_BASE: BooleanProperty = BooleanProperty.create("crossing_base")
        private val HORIZONTAL_DIRS = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)

        fun defaultStickState(): BlockState = CropStickBlock::class.instance().defaultBlockState().setValue(CROSSING_BASE, false)

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CropStickBlock::class.item(), 2)
                .pattern("   ")
                .pattern("S S")
                .pattern("S S")
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.STICK), has(Items.STICK))
                .save(exporter, CropStickBlock::class.id())
        }
    }
}

@ModBlockEntity(block = CropStickBlock::class)
class CropStickBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), CropCareTarget {

    companion object {
        /** 固定周期，与 CropBlock.TICK_RATE 对齐（原版 tickrate=256）。 */
        private const val STICK_TICK_RATE = 256L
    }

    private var storageNutrients: Int = 0
    private var storageWater: Int = 0
    private var storageWeedEx: Int = 0

    constructor(pos: BlockPos, state: BlockState) : this(CropStickBlockEntity::class.type(), pos, state)

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        if (world.time % STICK_TICK_RATE.toLong() != 0L) return
        val block = state.block as? CropStickBlock ?: return
        val serverWorld = world as? net.minecraft.server.level.ServerLevel ?: return
        block.performTick(serverWorld, pos, state, world.random)
    }

    fun storageSnapshot(): Triple<Int, Int, Int> = Triple(storageNutrients, storageWater, storageWeedEx)

    fun hasWeedExProtection(random: net.minecraft.util.RandomSource): Boolean {
        if (storageWeedEx <= 0) return false
        if (random.nextInt(10) == 0) {
            storageWeedEx = (storageWeedEx - 5).coerceAtLeast(0)
            setChanged()
        }
        return true
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
            setChanged()
        }
        return used
    }

    override fun applyHydration(amount: Int, simulate: Boolean): Int {
        val cap = 200
        val accepted = (cap - storageWater).coerceAtLeast(0).coerceAtMost(amount.coerceAtLeast(0))
        if (!simulate && accepted > 0) {
            storageWater += accepted
            setChanged()
        }
        return accepted
    }

    override fun applyWeedEx(amount: Int, simulate: Boolean): Int {
        val cap = 150
        val accepted = (cap - storageWeedEx).coerceAtLeast(0).coerceAtMost(amount.coerceAtLeast(0))
        if (!simulate && accepted > 0) {
            storageWeedEx += accepted
            setChanged()
        }
        return accepted
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        nbt.putInt("nutrients", storageNutrients)
        nbt.putInt("water", storageWater)
        nbt.putInt("weed_ex", storageWeedEx)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        storageNutrients = nbt.getInt("nutrients")
        storageWater = nbt.getInt("water")
        storageWeedEx = nbt.getInt("weed_ex")
    }
}
