package ic2_120.content.block

import com.mojang.serialization.MapCodec
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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import net.minecraft.registry.RegistryWrapper

@ModBlock(
    name = "crop_stick",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "crops",
    renderLayer = "cutout"
)
class CropStickBlock : BlockWithEntity(
    AbstractBlock.Settings.copy(Blocks.WHEAT)
        .breakInstantly()
        .noCollision()
        .nonOpaque()
        .ticksRandomly()
) {
    override fun getCodec(): MapCodec<out BlockWithEntity> = CROP_STICK_CODEC

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    init {
        defaultState = stateManager.defaultState.with(CROSSING_BASE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(CROSSING_BASE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(CROSSING_BASE, false)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CropStickBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
        val below = world.getBlockState(pos.down())
        return below.block is FarmlandBlock
    }

    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: WorldAccess,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (!canPlaceAt(state, world, pos)) {
            return Blocks.AIR.defaultState
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val stack = player.mainHandStack
        val isCreative = player.abilities.creativeMode

        if (!state.get(CROSSING_BASE) && stack.item == this.asItem()) {
            if (!isCreative) stack.decrement(1)
            world.setBlockState(pos, state.with(CROSSING_BASE, true), Block.NOTIFY_ALL)
            return ActionResult.SUCCESS
        }

        if (!state.get(CROSSING_BASE)) {
            if (stack.item is CropSeedBagItem) {
                val cropType = CropSeedData.readType(stack)
                if (cropType != null) {
                    val stats = CropSeedData.readStats(stack)
                    val cropState = CropBlock.defaultCropState(cropType, 0)
                    world.setBlockState(pos, cropState, Block.NOTIFY_ALL)
                    val be = world.getBlockEntity(pos) as? CropBlockEntity
                    be?.stats = stats
                    be?.scanLevel = CropSeedData.readScanLevel(stack)
                    be?.markDirty()
                    if (!isCreative) stack.decrement(1)
                    return ActionResult.SUCCESS
                }
            }

            val cropType = CropSystem.baseSeed(stack.item)
            if (cropType != null) {
                val cropState = CropBlock.defaultCropState(cropType, 0)
                world.setBlockState(pos, cropState, Block.NOTIFY_ALL)
                val be = world.getBlockEntity(pos) as? CropBlockEntity
                be?.stats = CropStats(1, 1, 1)
                be?.scanLevel = 0
                be?.markDirty()
                if (!isCreative) stack.decrement(1)
                return ActionResult.SUCCESS
            }
        }

        if (state.get(CROSSING_BASE) && stack.isEmpty) {
            world.setBlockState(pos, state.with(CROSSING_BASE, false), Block.NOTIFY_ALL)
            ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), this.asItem().defaultStack)
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    override fun randomTick(state: BlockState, world: net.minecraft.server.world.ServerWorld, pos: BlockPos, random: net.minecraft.util.math.random.Random) {
        if (!state.get(CROSSING_BASE)) {
            val stickBe = world.getBlockEntity(pos) as? CropStickBlockEntity
            if (stickBe?.hasWeedExProtection(random) == true) return
            if (random.nextInt(100) == 0) {
                world.setBlockState(pos, CropBlock.defaultCropState(CropType.WEED, 0), Block.NOTIFY_ALL)
                val be = world.getBlockEntity(pos) as? CropBlockEntity
                be?.stats = CropStats(1, 1, 1)
                be?.scanLevel = 0
                be?.markDirty()
            }
            return
        }
        if (attemptCrossing(world, pos, random)) return
        attemptSpreading(world, pos, random)
    }

    private fun attemptCrossing(world: net.minecraft.server.world.ServerWorld, pos: BlockPos, random: net.minecraft.util.math.random.Random): Boolean {
        if (random.nextInt(3) != 0) return false
        val sources = mutableListOf<CropBlockEntity>()
        for (dir in HORIZONTAL_DIRS) {
            val be = world.getBlockEntity(pos.offset(dir)) as? CropBlockEntity ?: continue
            val sourcePos = pos.offset(dir)
            if (!be.canParticipateCrossing(world, sourcePos)) continue
            val sourceState = world.getBlockState(sourcePos)
            val sourceType = sourceState.get(CropBlock.CROP_TYPE)
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
                    val sourceType = world.getBlockState(source.pos).get(CropBlock.CROP_TYPE)
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
        world.setBlockState(pos, state, Block.NOTIFY_ALL)
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return true
        be.stats = CropStats(avgGrowth, avgGain, avgRes)
        be.scanLevel = 0
        be.markDirty()
        return true
    }

    private fun attemptSpreading(world: net.minecraft.server.world.ServerWorld, pos: BlockPos, random: net.minecraft.util.math.random.Random): Boolean {
        val neighbors = mutableListOf<CropBlockEntity>()
        for (dir in HORIZONTAL_DIRS) {
            val be = world.getBlockEntity(pos.offset(dir)) as? CropBlockEntity ?: continue
            neighbors += be
        }
        if (neighbors.size != 1) return false

        val parent = neighbors.first()
        if (!parent.canParticipateCrossing(world, parent.pos)) return false
        val parentType = world.getBlockState(parent.pos).get(CropBlock.CROP_TYPE)

        var chance = 4
        if (parent.stats.growth >= 16) chance++
        if (parent.stats.growth >= 30) chance++
        if (parent.stats.resistance >= 28) chance += 27 - parent.stats.resistance
        if (chance < random.nextInt(16)) return false

        val state = CropBlock.defaultCropState(parentType, 0)
        world.setBlockState(pos, state, Block.NOTIFY_ALL)
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return true
        be.stats = CropStats(
            growth = parent.stats.growth.coerceAtLeast(0),
            gain = parent.stats.gain.coerceAtLeast(0),
            resistance = parent.stats.resistance.coerceAtLeast(0)
        )
        be.scanLevel = 0
        be.markDirty()
        return true
    }

    companion object {
        val CROP_STICK_CODEC: MapCodec<CropStickBlock> = Block.createCodec { error("CropStickBlock cannot be deserialized from JSON") }
        val CROSSING_BASE: BooleanProperty = BooleanProperty.of("crossing_base")
        private val HORIZONTAL_DIRS = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)

        fun defaultStickState(): BlockState = CropStickBlock::class.instance().defaultState.with(CROSSING_BASE, false)

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CropStickBlock::class.item(), 2)
                .pattern("   ")
                .pattern("S S")
                .pattern("S S")
                .input('S', Items.STICK)
                .criterion(hasItem(Items.STICK), conditionsFromItem(Items.STICK))
                .offerTo(exporter, CropStickBlock::class.id())
        }
    }
}

@ModBlockEntity(block = CropStickBlock::class)
class CropStickBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), CropCareTarget {
    private var storageNutrients: Int = 0
    private var storageWater: Int = 0
    private var storageWeedEx: Int = 0

    constructor(pos: BlockPos, state: BlockState) : this(CropStickBlockEntity::class.type(), pos, state)

    fun storageSnapshot(): Triple<Int, Int, Int> = Triple(storageNutrients, storageWater, storageWeedEx)

    fun hasWeedExProtection(random: net.minecraft.util.math.random.Random): Boolean {
        if (storageWeedEx <= 0) return false
        if (random.nextInt(10) == 0) {
            storageWeedEx = (storageWeedEx - 5).coerceAtLeast(0)
            markDirty()
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
            markDirty()
        }
        return used
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

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("nutrients", storageNutrients)
        nbt.putInt("water", storageWater)
        nbt.putInt("weed_ex", storageWeedEx)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        storageNutrients = nbt.getInt("nutrients")
        storageWater = nbt.getInt("water")
        storageWeedEx = nbt.getInt("weed_ex")
    }
}
